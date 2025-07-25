/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;

public final class GoToCmd extends Command
	implements UpdateListener, RenderListener
{
	private PathFinder pathFinder;
	private PathProcessor processor;
	private boolean enabled;
	
	public GoToCmd()
	{
		super("goto", "Walks or flies you to a specific location.",
			".goto <x> <y> <z>", ".goto <entity>", ".goto -path",
			"Turn off: .goto");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		// disable if enabled
		if(enabled)
		{
			disable();
			
			if(args.length == 0)
				return;
		}
		
		// set PathFinder
		if(args.length == 1 && args[0].equals("-path"))
		{
			BlockPos goal = WURST.getCmds().pathCmd.getLastGoal();
			if(goal == null)
				throw new CmdError("No previous position on .path.");
			pathFinder = new PathFinder(goal);
		}else
		{
			BlockPos goal = argsToPos(args);
			pathFinder = new PathFinder(goal);
		}
		
		// start
		enabled = true;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	private BlockPos argsToPos(String... args) throws CmdException
	{
		switch(args.length)
		{
			default:
			throw new CmdSyntaxError("Invalid coordinates.");
			
			case 1:
			return argsToEntityPos(args[0]);
			
			case 3:
			return argsToXyzPos(args);
		}
	}
	
	private BlockPos argsToEntityPos(String name) throws CmdError
	{
		LivingEntity entity = StreamSupport
			.stream(MC.world.getEntities().spliterator(), true)
			.filter(LivingEntity.class::isInstance).map(e -> (LivingEntity)e)
			.filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> name.equalsIgnoreCase(e.getDisplayName().getString()))
			.min(
				Comparator.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
			.orElse(null);
		
		if(entity == null)
			throw new CmdError("找不到实体\"" + name + "\"");
		
		return BlockPos.ofFloored(entity.getPos());
	}
	
	private BlockPos argsToXyzPos(String... xyz) throws CmdSyntaxError
	{
		BlockPos playerPos = BlockPos.ofFloored(MC.player.getPos());
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
				throw new CmdSyntaxError("Invalid coordinates.");
			
		return new BlockPos(pos[0], pos[1], pos[2]);
	}
	
	@Override
	public void onUpdate()
	{
		// find path
		if(!pathFinder.isDone())
		{
			PathProcessor.lockControls();
			
			pathFinder.think();
			
			if(!pathFinder.isDone())
			{
				if(pathFinder.isFailed())
				{
					ChatUtils.error("Could not find a path.");
					disable();
				}
				
				return;
			}
			
			pathFinder.formatPath();
			
			// set processor
			processor = pathFinder.getProcessor();
			
			System.out.println("Done");
		}
		
		// check path
		if(processor != null
			&& !pathFinder.isPathStillValid(processor.getIndex()))
		{
			System.out.println("Updating path...");
			pathFinder = new PathFinder(pathFinder.getGoal());
			return;
		}
		
		// process path
		processor.process();
		
		if(processor.isDone())
			disable();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		pathFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
			pathCmd.isDepthTest());
	}
	
	private void disable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		pathFinder = null;
		processor = null;
		PathProcessor.releaseControls();
		
		enabled = false;
	}
	
	public boolean isActive()
	{
		return enabled;
	}
}
