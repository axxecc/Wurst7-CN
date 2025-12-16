/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.StreamSupport;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;

public final class PathCmd extends Command
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting debugMode =
		new CheckboxSetting("Debug模式", false);
	
	private final CheckboxSetting depthTest =
		new CheckboxSetting("深度测试", false);
	
	private PathFinder pathFinder;
	private boolean enabled;
	private long startTime;
	private BlockPos lastGoal;
	
	public PathCmd()
	{
		super("path",
			"显示到特定点的最短路径, 对迷宫和洞穴很有用",
			".path <x> <y> <z>", ".path <实体>", ".path -debug",
			".path -depth", ".path -refresh", "关闭: .path");
		
		addSetting(debugMode);
		addSetting(depthTest);
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		// process special commands
		boolean refresh = false;
		if(args.length > 0 && args[0].startsWith("-"))
			switch(args[0])
			{
				case "-debug":
				debugMode.setChecked(!debugMode.isChecked());
				ChatUtils.message("Debug模式已 "
					+ (debugMode.isChecked() ? "启用" : "关闭"));
				return;
				
				case "-depth":
				depthTest.setChecked(!depthTest.isChecked());
				ChatUtils.message("深度测试已 "
					+ (depthTest.isChecked() ? "启用" : "关闭"));
				return;
				
				case "-refresh":
				if(lastGoal == null)
					throw new CmdError("无法刷新: 没有之前的路径");
				refresh = true;
				break;
			}
		
		// disable if enabled
		if(enabled)
		{
			EVENTS.remove(UpdateListener.class, this);
			EVENTS.remove(RenderListener.class, this);
			enabled = false;
			
			if(args.length == 0)
				return;
		}
		
		// set PathFinder
		final BlockPos goal;
		if(refresh)
			goal = lastGoal;
		else
		{
			goal = argsToPos(args);
			lastGoal = goal;
		}
		pathFinder = new PathFinder(goal);
		
		// start
		enabled = true;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		System.out.println("寻路...");
		startTime = System.nanoTime();
	}
	
	private BlockPos argsToPos(String... args) throws CmdException
	{
		switch(args.length)
		{
			default:
			throw new CmdSyntaxError("坐标无效");
			
			case 1:
			return argsToEntityPos(args[0]);
			
			case 3:
			return argsToXyzPos(args);
		}
	}
	
	private BlockPos argsToEntityPos(String name) throws CmdError
	{
		LivingEntity entity = StreamSupport
			.stream(MC.level.entitiesForRendering().spliterator(), true)
			.filter(LivingEntity.class::isInstance).map(e -> (LivingEntity)e)
			.filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> name.equalsIgnoreCase(e.getDisplayName().getString()))
			.min(Comparator.comparingDouble(e -> MC.player.distanceToSqr(e)))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError("找不到实体\"" + name + "\"");
		
		return BlockPos.containing(entity.position());
	}
	
	private BlockPos argsToXyzPos(String... xyz) throws CmdSyntaxError
	{
		BlockPos playerPos = BlockPos.containing(MC.player.position());
		int[] player = {playerPos.getX(), playerPos.getY(), playerPos.getZ()};
		int[] pos = new int[3];
		
		for(int i = 0; i < 3; i++)
			if(MathUtils.isInteger(xyz[i]))
				pos[i] = Integer.parseInt(xyz[i]);
			else if(xyz[i].equals("~"))
				pos[i] = player[i];
			else if(xyz[i].startsWith("~")
				&& MathUtils.isInteger(xyz[i].substring(1)))
				pos[i] = player[i] + Integer.parseInt(xyz[i].substring(1));
			else
				throw new CmdSyntaxError("坐标无效");
			
		return new BlockPos(pos[0], pos[1], pos[2]);
	}
	
	@Override
	public void onUpdate()
	{
		double passedTime = (System.nanoTime() - startTime) / 1e6;
		pathFinder.think();
		boolean foundPath = pathFinder.isDone();
		
		// stop if done or failed
		if(foundPath || pathFinder.isFailed())
		{
			ArrayList<PathPos> path = new ArrayList<>();
			if(foundPath)
				path = pathFinder.formatPath();
			else
				ChatUtils.error("找不到合适的路径");
			
			EVENTS.remove(UpdateListener.class, this);
			
			System.out.println("之后完成 " + passedTime + "ms");
			if(debugMode.isChecked())
				System.out.println("长度: " + path.size() + ", 处理: "
					+ pathFinder.countProcessedBlocks() + ", 队列: "
					+ pathFinder.getQueueSize() + ", 花费: "
					+ pathFinder.getCost(pathFinder.getCurrentPos()));
		}
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		pathFinder.renderPath(matrixStack, debugMode.isChecked(),
			depthTest.isChecked());
	}
	
	public BlockPos getLastGoal()
	{
		return lastGoal;
	}
	
	public boolean isDebugMode()
	{
		return debugMode.isChecked();
	}
	
	public boolean isDepthTest()
	{
		return depthTest.isChecked();
	}
}
