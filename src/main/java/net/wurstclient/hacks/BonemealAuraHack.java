/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"骨粉光环", "BonemealAura"})
public final class BonemealAuraHack extends Hack implements HandleInputListener
{
	private final SliderSetting range =
		new SliderSetting("范围", 4.25, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode = new EnumSetting<>("模式",
		"\u00a7l快速\u00a7r 模式可以一次在多个块上使用骨粉\n\u00a7l合法\u00a7r模式可以绕过 NoCheat",
		Mode.values(), Mode.FAST);

	private final EnumSetting<AutomationLevel> automationLevel =
		new EnumSetting<>("自动化",
			"要自动化多少骨粉过程\n\u00a7l右键\u00a7r 只需右键点击手里有骨粉的植物\n\u00a7l快捷栏\u00a7r 在快捷栏中选择骨粉\n\u00a7l背包\u00a7r 在物品栏中找到骨粉将他它移动到快捷栏",
			AutomationLevel.values(), AutomationLevel.RIGHT_CLICK);
	
	private final CheckboxSetting saplings =
		new CheckboxSetting("树苗", true);
	
	private final CheckboxSetting crops = new CheckboxSetting("庄稼",
		"小麦、胡萝卜、土豆和甜菜根", true);
	
	private final CheckboxSetting stems =
		new CheckboxSetting("茎类", "南瓜和西瓜", true);
	
	private final CheckboxSetting cocoa = new CheckboxSetting("可可", true);
	
	private final CheckboxSetting other = new CheckboxSetting("其它", false);
	
	public BonemealAuraHack()
	{
		super("骨粉光环");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
		addSetting(automationLevel);
		addSetting(saplings);
		addSetting(crops);
		addSetting(stems);
		addSetting(cocoa);
		addSetting(other);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(HandleInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(HandleInputListener.class, this);
	}
	
	@Override
	public void onHandleInput()
	{
		// wait for right click timer
		if(MC.rightClickDelay > 0)
			return;
		
		if(MC.gameMode.isDestroying() || MC.player.isHandsBusy())
			return;
		
		// get valid blocks
		ArrayList<BlockPos> validBlocks = getValidBlocks();
		
		if(validBlocks.isEmpty())
			return;
		
		// wait for AutoFarm
		if(WURST.getHax().autoFarmHack.isBusy())
			return;
		
		// check held item
		if(!MC.player.isHolding(Items.BONE_MEAL))
		{
			InventoryUtils.selectItem(Items.BONE_MEAL,
				automationLevel.getSelected().maxInvSlot);
			return;
		}
		
		if(mode.getSelected() == Mode.LEGIT)
		{
			// legit mode
			
			// use bone meal on next valid block
			for(BlockPos pos : validBlocks)
				if(rightClickBlockLegit(pos))
					break;
				
		}else
		{
			// fast mode
			
			boolean shouldSwing = false;
			
			// use bone meal on all valid blocks
			for(BlockPos pos : validBlocks)
				if(rightClickBlockSimple(pos))
					shouldSwing = true;
				
			// swing arm
			if(shouldSwing)
				MC.player.swing(InteractionHand.MAIN_HAND);
		}
	}
	
	private ArrayList<BlockPos> getValidBlocks()
	{
		Vec3 eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.containing(eyesVec);
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		// As plants are bone-mealed, they will grow larger and prevent line of
		// sight to other plants behind them. That's why we need to bone-meal
		// the farthest plants first.
		Comparator<BlockPos> farthestFirst = Comparator
			.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(eyesVec))
			.reversed();
		
		return BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
			.filter(pos -> pos.distToCenterSqr(eyesVec) <= rangeSq)
			.filter(this::isCorrectBlock).sorted(farthestFirst)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean isCorrectBlock(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		BlockState state = BlockUtils.getState(pos);
		ClientLevel world = MC.level;
		
		if(!(block instanceof BonemealableBlock fBlock)
			|| !fBlock.isBonemealSuccess(world, world.random, pos, state))
			return false;
		
		if(block instanceof GrassBlock)
			return false;
		
		if(block instanceof SaplingBlock sapling
			&& sapling.isValidBonemealTarget(world, pos, state))
			return saplings.isChecked();
		
		if(block instanceof CropBlock crop
			&& crop.isValidBonemealTarget(world, pos, state))
			return crops.isChecked();
		
		if(block instanceof StemBlock stem
			&& stem.isValidBonemealTarget(world, pos, state))
			return stems.isChecked();
		
		if(block instanceof CocoaBlock cocoaBlock
			&& cocoaBlock.isValidBonemealTarget(world, pos, state))
			return cocoa.isChecked();
		
		return other.isChecked();
	}
	
	private boolean rightClickBlockLegit(BlockPos pos)
	{
		// if breaking or riding, stop and don't try other blocks
		if(MC.gameMode.isDestroying() || MC.player.isHandsBusy())
			return true;
		
		// if this block is unreachable, try the next one
		BlockBreakingParams params = BlockBreaker.getBlockBreakingParams(pos);
		if(params == null || params.distanceSq() > range.getValueSq()
			|| !params.lineOfSight())
			return false;
		
		// face and right click the block
		MC.rightClickDelay = 4;
		WURST.getRotationFaker().faceVectorPacket(params.hitVec());
		InteractionSimulator.rightClickBlock(params.toHitResult());
		return true;
	}
	
	private boolean rightClickBlockSimple(BlockPos pos)
	{
		// if this block is unreachable, try the next one
		BlockBreakingParams params = BlockBreaker.getBlockBreakingParams(pos);
		if(params == null)
			return false;
		
		// right click the block
		InteractionSimulator.rightClickBlock(params.toHitResult(),
			SwingHand.OFF);
		return true;
	}
	
	private enum Mode
	{
		FAST("快速"),
		LEGIT("合法");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private enum AutomationLevel
	{
		RIGHT_CLICK("右键", 0),
		HOTBAR("快捷栏", 9),
		INVENTORY("背包", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private AutomationLevel(String name, int maxInvSlot)
		{
			this.name = name;
			this.maxInvSlot = maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
