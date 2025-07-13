/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.ChestRaftEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.chestesp.ChestEspBlockGroup;
import net.wurstclient.hacks.chestesp.ChestEspEntityGroup;
import net.wurstclient.hacks.chestesp.ChestEspGroup;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EspStyleSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.chunk.ChunkUtils;

public class ChestEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EspStyleSetting style = new EspStyleSetting();
	
	private final ChestEspBlockGroup basicChests = new ChestEspBlockGroup(
		new ColorSetting("箱子颜色",
			"普通箱子将以这种颜色高亮显示", Color.GREEN),
		null);
	
	private final ChestEspBlockGroup trapChests = new ChestEspBlockGroup(
		new ColorSetting("陷阱箱",
			"陷阱箱将以这种颜色高亮显示",
			new Color(0xFF8000)),
		new CheckboxSetting("显示陷阱箱", true));
	
	private final ChestEspBlockGroup enderChests = new ChestEspBlockGroup(
		new ColorSetting("末影箱颜色",
			"末影箱将以这种颜色高亮显示", Color.CYAN),
		new CheckboxSetting("显示末影箱", true));
	
	private final ChestEspEntityGroup chestCarts =
		new ChestEspEntityGroup(
			new ColorSetting("运输矿车颜色",
				"运输矿车将以这种颜色高亮显示",
				Color.YELLOW),
			new CheckboxSetting("显示运输矿车", true));
	
	private final ChestEspEntityGroup chestBoats =
		new ChestEspEntityGroup(
			new ColorSetting("运输船颜色",
				"运输船将以这种颜色高亮显示",
				Color.YELLOW),
			new CheckboxSetting("显示运输船", true));
	
	private final ChestEspBlockGroup barrels = new ChestEspBlockGroup(
		new ColorSetting("木桶颜色",
			"木桶将以这种颜色高亮显示", Color.GREEN),
		new CheckboxSetting("显示木桶", true));
	
	private final ChestEspBlockGroup pots = new ChestEspBlockGroup(
		new ColorSetting("饰纹陶罐颜色",
			"饰纹陶罐将以这种颜色高亮显示", Color.GREEN),
		new CheckboxSetting("显示饰纹陶罐", false));
	
	private final ChestEspBlockGroup shulkerBoxes = new ChestEspBlockGroup(
		new ColorSetting("潜影盒颜色",
			"潜影盒将将以这种颜色高亮显示", Color.MAGENTA),
		new CheckboxSetting("显示潜影盒", true));
	
	private final ChestEspBlockGroup hoppers = new ChestEspBlockGroup(
		new ColorSetting("漏斗颜色",
			"漏斗将以这种颜色高亮显示", Color.WHITE),
		new CheckboxSetting("显示漏斗", false));
	
	private final ChestEspEntityGroup hopperCarts =
		new ChestEspEntityGroup(
			new ColorSetting("漏斗矿车颜色",
				"漏斗矿车将以这种颜色高亮显示",
				Color.YELLOW),
			new CheckboxSetting("显示漏斗矿车", false));
	
	private final ChestEspBlockGroup droppers = new ChestEspBlockGroup(
		new ColorSetting("投掷器颜色",
			"投掷器将以这种颜色高亮显示", Color.WHITE),
		new CheckboxSetting("显示投掷器", false));
	
	private final ChestEspBlockGroup dispensers = new ChestEspBlockGroup(
		new ColorSetting("发射器颜色",
			"发射器将以这种颜色高亮显示",
			new Color(0xFF8000)),
		new CheckboxSetting("显示发射器", false));
	
	private final ChestEspBlockGroup crafters = new ChestEspBlockGroup(
		new ColorSetting("合成器颜色",
			"合成器将以这种颜色高亮显示", Color.WHITE),
		new CheckboxSetting("显示合成器", false));
	
	private final ChestEspBlockGroup furnaces =
		new ChestEspBlockGroup(new ColorSetting("熔炉颜色",
			"熔炉，烟熏炉和高炉将以这种颜色高亮显示",
			Color.RED), new CheckboxSetting("显示熔炉", false));
	
	private final List<ChestEspGroup> groups =
		Arrays.asList(basicChests, trapChests, enderChests, chestCarts,
			chestBoats, barrels, pots, shulkerBoxes, hoppers, hopperCarts,
			droppers, dispensers, crafters, furnaces);
	
	private final List<ChestEspEntityGroup> entityGroups =
		Arrays.asList(chestCarts, chestBoats, hopperCarts);
	
	public ChestEspHack()
	{
		super("箱子透视");
		setCategory(Category.RENDER);
		
		addSetting(style);
		groups.stream().flatMap(ChestEspGroup::getSettings)
			.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		groups.forEach(ChestEspGroup::clear);
	}
	
	@Override
	public void onUpdate()
	{
		groups.forEach(ChestEspGroup::clear);
		
		ArrayList<BlockEntity> blockEntities =
			ChunkUtils.getLoadedBlockEntities()
				.collect(Collectors.toCollection(ArrayList::new));
		
		for(BlockEntity blockEntity : blockEntities)
			if(blockEntity instanceof TrappedChestBlockEntity)
				trapChests.add(blockEntity);
			else if(blockEntity instanceof ChestBlockEntity)
				basicChests.add(blockEntity);
			else if(blockEntity instanceof EnderChestBlockEntity)
				enderChests.add(blockEntity);
			else if(blockEntity instanceof ShulkerBoxBlockEntity)
				shulkerBoxes.add(blockEntity);
			else if(blockEntity instanceof BarrelBlockEntity)
				barrels.add(blockEntity);
			else if(blockEntity instanceof DecoratedPotBlockEntity)
				pots.add(blockEntity);
			else if(blockEntity instanceof HopperBlockEntity)
				hoppers.add(blockEntity);
			else if(blockEntity instanceof DropperBlockEntity)
				droppers.add(blockEntity);
			else if(blockEntity instanceof DispenserBlockEntity)
				dispensers.add(blockEntity);
			else if(blockEntity instanceof CrafterBlockEntity)
				crafters.add(blockEntity);
			else if(blockEntity instanceof AbstractFurnaceBlockEntity)
				furnaces.add(blockEntity);
			
		for(Entity entity : MC.world.getEntities())
			if(entity instanceof ChestMinecartEntity)
				chestCarts.add(entity);
			else if(entity instanceof HopperMinecartEntity)
				hopperCarts.add(entity);
			else if(entity instanceof ChestBoatEntity
				|| entity instanceof ChestRaftEntity)
				chestBoats.add(entity);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.hasLines())
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		entityGroups.stream().filter(ChestEspGroup::isEnabled)
			.forEach(g -> g.updateBoxes(partialTicks));
		
		if(style.hasBoxes())
			renderBoxes(matrixStack);
		
		if(style.hasLines())
			renderTracers(matrixStack, partialTicks);
	}
	
	private void renderBoxes(MatrixStack matrixStack)
	{
		for(ChestEspGroup group : groups)
		{
			if(!group.isEnabled())
				continue;
			
			List<Box> boxes = group.getBoxes();
			int quadsColor = group.getColorI(0x40);
			int linesColor = group.getColorI(0x80);
			
			RenderUtils.drawSolidBoxes(matrixStack, boxes, quadsColor, false);
			RenderUtils.drawOutlinedBoxes(matrixStack, boxes, linesColor,
				false);
		}
	}
	
	private void renderTracers(MatrixStack matrixStack, float partialTicks)
	{
		for(ChestEspGroup group : groups)
		{
			if(!group.isEnabled())
				continue;
			
			List<Box> boxes = group.getBoxes();
			List<Vec3d> ends = boxes.stream().map(Box::getCenter).toList();
			int color = group.getColorI(0x80);
			
			RenderUtils.drawTracers(matrixStack, partialTicks, ends, color,
				false);
		}
	}
}
