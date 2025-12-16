/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.*;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.json.JsonException;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener, RenderListener
{
	private static final AABB BLOCK_BOX =
		new AABB(1 / 16.0, 1 / 16.0, 1 / 16.0, 15 / 16.0, 15 / 16.0, 15 / 16.0);
	
	private final FileSetting templateSetting = new FileSetting("模板",
		"确定要构建的内容\n\n模板只是 JSON 文件, 请随意添加您自己的模板或编辑/删除默认模板\n\n'重置为默认值'按钮或删除文件夹",
		"autobuild", DefaultAutoBuildTemplates::createFiles);
	
	private final SliderSetting range = new SliderSetting("范围",
		"放置方块时可以到达多远\n建议的值: \n6.0 对于Vanilla\n" + "4.25 对于NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"检查视线",
		"确保在放置方块时不要穿过墙壁, 可以帮助 AntiCheat 插件, 但会减慢建造速度",
		false);
	
	private final CheckboxSetting useSavedBlocks = new CheckboxSetting(
		"使用已保存的块",
		"尝试放置保存在模板中的相同块\n\n如果模板未指定块类型, 则将从您持有的任何块构建它",
		true);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("总是快速放置 ",
			"即使未启用快速放置也会加快放置速度", true);
	
	private final CheckboxSetting strictBuildOrder = new CheckboxSetting(
		"严格的构建顺序",
		"按块在模板中的显示顺序完全相同的顺序放置块, 这速度较慢, 但提供更一致的结果",
		false);

	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashMap<BlockPos, Item> remainingBlocks =
		new LinkedHashMap<>();
	
	public AutoBuildHack()
	{
		super("自动建造");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(checkLOS);
		addSetting(useSavedBlocks);
		addSetting(fastPlace);
		addSetting(strictBuildOrder);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		switch(status)
		{
			case NO_TEMPLATE:
			break;
			
			case LOADING:
			name += " [加载中...]";
			break;
			
			case IDLE:
			name += " [" + template.getName() + "]";
			break;
			
			case BUILDING:
			double total = template.size();
			double placed = total - remainingBlocks.size();
			double progress = Math.round(placed / total * 1e4) / 1e2;
			name += " [" + template.getName() + "] " + progress + "%";
			break;
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().instaBuildHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		remainingBlocks.clear();
		
		if(template == null)
			status = Status.NO_TEMPLATE;
		else
			status = Status.IDLE;
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(status != Status.IDLE)
			return;
		
		HitResult hitResult = MC.hitResult;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos =
			hitResultPos.relative(blockHitResult.getDirection());
		Direction direction = MC.player.getDirection();
		remainingBlocks = template.getBlocksToPlace(startPos, direction);
		
		status = Status.BUILDING;
	}
	
	@Override
	public void onUpdate()
	{
		switch(status)
		{
			case NO_TEMPLATE:
			loadSelectedTemplate();
			break;
			
			case LOADING:
			break;
			
			case IDLE:
			if(!template.isSelected(templateSetting))
				loadSelectedTemplate();
			break;
			
			case BUILDING:
			buildNormally();
			break;
		}
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		List<BlockPos> blocksToDraw = remainingBlocks.keySet().stream()
			.filter(pos -> BlockUtils.getState(pos).canBeReplaced()).limit(1024)
			.toList();
		
		int black = 0x80000000;
		List<AABB> outlineBoxes =
			blocksToDraw.stream().map(pos -> BLOCK_BOX.move(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, outlineBoxes, black, true);
		
		int green = 0x2600FF00;
		Vec3 eyesPos = RotationUtils.getEyesPos();
		double rangeSq = range.getValueSq();
		List<AABB> greenBoxes = blocksToDraw.stream()
			.filter(pos -> pos.distToCenterSqr(eyesPos) <= rangeSq)
			.map(pos -> BLOCK_BOX.move(pos)).toList();
		RenderUtils.drawSolidBoxes(matrixStack, greenBoxes, green, true);
	}
	
	private void buildNormally()
	{
		remainingBlocks.keySet()
			.removeIf(pos -> !BlockUtils.getState(pos).canBeReplaced());
		
		if(remainingBlocks.isEmpty())
		{
			status = Status.IDLE;
			return;
		}
		
		if(!fastPlace.isChecked() && MC.rightClickDelay > 0)
			return;
		
		double rangeSq = range.getValueSq();
		for(Map.Entry<BlockPos, Item> entry : remainingBlocks.entrySet())
		{
			BlockPos pos = entry.getKey();
			Item item = entry.getValue();
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq
				|| checkLOS.isChecked() && !params.lineOfSight())
				if(strictBuildOrder.isChecked())
					return;
				else
					continue;
				
			if(useSavedBlocks.isChecked() && item != Items.AIR
				&& !MC.player.getMainHandItem().is(item))
			{
				giveOrSelectItem(item);
				return;
			}
			
			MC.rightClickDelay = 4;
			RotationUtils.getNeededRotations(params.hitVec())
				.sendPlayerLookPacket();
			InteractionSimulator.rightClickBlock(params.toHitResult());
			return;
		}
	}
	
	private void giveOrSelectItem(Item item)
	{
		if(InventoryUtils.selectItem(item, 36, true))
			return;
		
		if(!MC.player.hasInfiniteMaterials())
			return;
		
		Inventory inventory = MC.player.getInventory();
		int slot = inventory.getFreeSlot();
		if(slot < 0)
			slot = inventory.selected;
		
		ItemStack stack = new ItemStack(item);
		InventoryUtils.setCreativeStack(slot, stack);
	}
	
	private void loadSelectedTemplate()
	{
		status = Status.LOADING;
		Path path = templateSetting.getSelectedFile();
		
		try
		{
			template = AutoBuildTemplate.load(path);
			status = Status.IDLE;
			
		}catch(IOException | JsonException e)
		{
			Path fileName = path.getFileName();
			ChatUtils.error("无法加载模板 '" + fileName + "'.");
			
			String simpleClassName = e.getClass().getSimpleName();
			String message = e.getMessage();
			ChatUtils.message(simpleClassName + ": " + message);
			
			e.printStackTrace();
			setEnabled(false);
		}
	}
	
	public Path getFolder()
	{
		return templateSetting.getFolder();
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE,
		BUILDING;
	}
}
