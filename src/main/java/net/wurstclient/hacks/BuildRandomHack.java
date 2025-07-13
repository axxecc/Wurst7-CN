/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Random;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FacingSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockPlacer;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"build random", "RandomBuild", "random build", "PlaceRandom",
	"place random", "RandomPlace", "random place"})
public final class BuildRandomHack extends Hack
	implements UpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("范围", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private SliderSetting maxAttempts = new SliderSetting("最大尝试次数",
		"随机建造在一次更新中尝试放置块的最大随机位置数\n\n较高的值会加快构建过程，但代价是增加延迟",
		128, 1, 1024, 1, ValueDisplay.INTEGER);
	
	private final CheckboxSetting checkItem =
		new CheckboxSetting("检查保留物品",
			"仅在您实际持有方块时构建\n关闭此选项以使用火、水、熔岩、刷怪蛋构建，或者如果您只想在随机位置用空手右键单击",
			true);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("检查视线",
			"确保随机建造不会尝试在墙后放置块",
			false);
	
	private final FacingSetting facing = FacingSetting.withoutPacketSpam(
		"随机建造应该如何面对随机放置的块\n\n\u00a7l关闭\u00a7r - 根本不面对块。将被反作弊插件检测到\n\n\u00a7l服务器\u00a7r - 面对服务器端的障碍，同时仍然允许您在客户端自由移动摄像机\n\n\u00a7l客户端\u00a7r - 通过在客户端移动您的摄像机来面对障碍。这是最合法的选择");
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.SERVER);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("始终快速放置",
			"即使未启用快速放置，也会像启用快速放置一样建造", false);
	
	private final CheckboxSetting placeWhileBreaking = new CheckboxSetting(
		"破坏时放置",
		"即使在你打破方块时也能建造\n可以通过 hacks 进行，但在原版中不起作用",
		false);
	
	private final CheckboxSetting placeWhileRiding = new CheckboxSetting(
		"骑行时放置",
		"即使在你骑车时也能建造\n可以通过 hacks 进行，但在原版中不起作用",
		false);
	
	private final CheckboxSetting indicator = new CheckboxSetting("指示器",
		"显示随机建造放置块的位置", true);
	
	private final Random random = new Random();
	private BlockPos lastPos;
	
	public BuildRandomHack()
	{
		super("随机建造");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(maxAttempts);
		addSetting(checkItem);
		addSetting(checkLOS);
		addSetting(facing);
		addSetting(swingHand);
		addSetting(fastPlace);
		addSetting(placeWhileBreaking);
		addSetting(placeWhileRiding);
		addSetting(indicator);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		lastPos = null;
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		lastPos = null;
		
		if(WURST.getHax().freecamHack.isEnabled())
			return;
		
		if(!fastPlace.isChecked() && MC.itemUseCooldown > 0)
			return;
		
		if(checkItem.isChecked() && !MC.player.isHolding(
			stack -> !stack.isEmpty() && stack.getItem() instanceof BlockItem))
			return;
		
		if(!placeWhileBreaking.isChecked()
			&& MC.interactionManager.isBreakingBlock())
			return;
		
		if(!placeWhileRiding.isChecked() && MC.player.isRiding())
			return;
		
		int maxAttempts = this.maxAttempts.getValueI();
		int blockRange = range.getValueCeil();
		int bound = blockRange * 2 + 1;
		BlockPos pos;
		int attempts = 0;
		
		do
		{
			// generate random position
			pos = BlockPos.ofFloored(RotationUtils.getEyesPos()).add(
				random.nextInt(bound) - blockRange,
				random.nextInt(bound) - blockRange,
				random.nextInt(bound) - blockRange);
			attempts++;
			
		}while(attempts < maxAttempts && !tryToPlaceBlock(pos));
	}
	
	private boolean tryToPlaceBlock(BlockPos pos)
	{
		if(!BlockUtils.getState(pos).isReplaceable())
			return false;
		
		BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
		if(params == null || params.distanceSq() > range.getValueSq())
			return false;
		if(checkLOS.isChecked() && !params.lineOfSight())
			return false;
		
		MC.itemUseCooldown = 4;
		facing.getSelected().face(params.hitVec());
		lastPos = pos;
		
		InteractionSimulator.rightClickBlock(params.toHitResult(),
			swingHand.getSelected());
		return true;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(lastPos == null || !indicator.isChecked())
			return;
		
		// Get colors
		float red = partialTicks * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		// Draw box
		Box box = new Box(lastPos);
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
}
