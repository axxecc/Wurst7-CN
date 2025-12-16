/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"反挂机", "AntiAFK"})
@DontSaveState
public final class AntiAfkHack extends Hack
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting useAi = new CheckboxSetting("使用AI",
		"使用寻路人工智能自然移动并避开危险\n有时可能会卡住", true);
	
	private final SliderSetting aiRange = new SliderSetting("AI范围",
		"开启使用 AI 时反挂机可以移动的区域", 16, 1, 64, 1,
		ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting nonAiRange = new SliderSetting("无AI范围",
		"当使用 AI 关闭时, 反挂机可以移动的区域\n\n§c§l警告：§r该区域必须完全畅通无阻且没有危险", 1, 1, 64, 1,
		ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting waitTime = new SliderSetting("等待时间",
		"动作之间的时间 (以秒为单位)", 2.5, 0, 60, 0.05,
		ValueDisplay.DECIMAL.withSuffix("s"));
	
	private final SliderSetting waitTimeRand = new SliderSetting(
		"等待时间随机化",
		"等待时间可以随机增加或减少多少时间, 以秒为单位", 0.5, 0, 60,
		0.05, ValueDisplay.DECIMAL.withPrefix("\u00b1").withSuffix("s"));
	
	private final CheckboxSetting showWaitTime =
		new CheckboxSetting("显示等待时间",
			"显示功能列表中剩余的等待时间", true);
	
	private int timer;
	private RandomSource random = RandomSource.createNewThreadLocalInstance();
	private BlockPos start;
	private BlockPos nextBlock;
	
	private RandomPathFinder pathFinder;
	private PathProcessor processor;
	private boolean creativeFlying;
	
	public AntiAfkHack()
	{
		super("反挂机");
		
		setCategory(Category.OTHER);
		addSetting(useAi);
		addSetting(aiRange);
		addSetting(nonAiRange);
		addSetting(waitTime);
		addSetting(waitTimeRand);
		addSetting(showWaitTime);
	}
	
	@Override
	public String getRenderName()
	{
		if(showWaitTime.isChecked() && timer > 0)
			return getName() + " [" + timer * 50 + "ms]";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		start = BlockPos.containing(MC.player.position());
		nextBlock = null;
		pathFinder =
			new RandomPathFinder(randomize(start, aiRange.getValueI(), true));
		creativeFlying = MC.player.getAbilities().flying;
		
		WURST.getHax().autoFishHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		PathProcessor.releaseControls();
		pathFinder = null;
		processor = null;
	}
	
	@Override
	public void onUpdate()
	{
		// check if player died
		if(MC.player.getHealth() <= 0)
		{
			setEnabled(false);
			return;
		}
		
		MC.player.getAbilities().flying = creativeFlying;
		
		if(useAi.isChecked())
		{
			// prevent drowning
			if(MC.player.isUnderWater()
				&& !WURST.getHax().jesusHack.isEnabled())
			{
				MC.options.keyJump.setDown(true);
				return;
			}
			
			// update timer
			if(timer > 0)
			{
				timer--;
				return;
			}
			
			// find path
			if(!pathFinder.isDone() && !pathFinder.isFailed())
			{
				PathProcessor.lockControls();
				
				pathFinder.think();
				
				if(!pathFinder.isDone() && !pathFinder.isFailed())
					return;
				
				pathFinder.formatPath();
				
				// set processor
				processor = pathFinder.getProcessor();
			}
			
			// check path
			if(processor != null
				&& !pathFinder.isPathStillValid(processor.getIndex())
				|| processor.getTicksOffPath() > 20)
			{
				pathFinder = new RandomPathFinder(pathFinder);
				return;
			}
			
			// process path
			if(!processor.isDone())
				processor.process();
			else
			{
				// reset and wait for timer
				PathProcessor.releaseControls();
				pathFinder = new RandomPathFinder(
					randomize(start, aiRange.getValueI(), true));
				setTimer();
			}
		}else
		{
			// set next block
			if(timer <= 0 || nextBlock == null)
			{
				nextBlock = randomize(start, nonAiRange.getValueI(), false);
				setTimer();
			}
			
			// face block
			WURST.getRotationFaker()
				.faceVectorClientIgnorePitch(Vec3.atCenterOf(nextBlock));
			
			// walk
			if(MC.player.distanceToSqr(Vec3.atCenterOf(nextBlock)) > 0.5)
				MC.options.keyUp.setDown(true);
			else
				MC.options.keyUp.setDown(false);
			
			// swim up
			MC.options.keyJump.setDown(MC.player.isInWater());
			
			// update timer
			if(timer > 0)
				timer--;
		}
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!useAi.isChecked())
			return;
		
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		pathFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
			pathCmd.isDepthTest());
	}
	
	private void setTimer()
	{
		int baseTime = (int)(waitTime.getValue() * 20);
		double randTime = waitTimeRand.getValue() * 20;
		int randOffset = (int)(random.nextGaussian() * randTime);
		randOffset = Math.max(randOffset, -baseTime);
		timer = baseTime + randOffset;
	}
	
	private BlockPos randomize(BlockPos pos, int range, boolean includeY)
	{
		int x = random.nextInt(2 * range + 1) - range;
		int y = includeY ? random.nextInt(2 * range + 1) - range : 0;
		int z = random.nextInt(2 * range + 1) - range;
		return pos.offset(x, y, z);
	}
	
	private class RandomPathFinder extends PathFinder
	{
		public RandomPathFinder(BlockPos goal)
		{
			super(goal);
			setThinkTime(10);
			setFallingAllowed(false);
			setDivingAllowed(false);
		}
		
		public RandomPathFinder(PathFinder pathFinder)
		{
			super(pathFinder);
			setFallingAllowed(false);
			setDivingAllowed(false);
		}
		
		@Override
		public ArrayList<PathPos> formatPath()
		{
			failed = true;
			return super.formatPath();
		}
	}
}
