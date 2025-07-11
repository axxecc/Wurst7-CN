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
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.*;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.json.JsonException;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener, RenderListener
{
	private static final Box BLOCK_BOX =
		new Box(1 / 16.0, 1 / 16.0, 1 / 16.0, 15 / 16.0, 15 / 16.0, 15 / 16.0);
	
	private final FileSetting templateSetting = new FileSetting("模板",
		"确定要构建的内容\n\n模板只是 JSON 文件，请随意添加您自己的模板或编辑/删除默认模板\n\n'重置为默认值'按钮或删除文件夹",
		"autobuild", DefaultAutoBuildTemplates::createFiles);
	
	private final SliderSetting range = new SliderSetting("范围",
		"放置方块时可以到达多远\n建议的值：\n6.0 对于Vanilla\n" + "4.25 对于NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"检查视线",
		"确保在放置方块时不要穿过墙壁，可以帮助 AntiCheat 插件，但会减慢建造速度",
		false);
	
	private final CheckboxSetting instaBuild = new CheckboxSetting("即时建造",
		"立即建造小模板（<= 64 个块）\n为了获得最佳效果，请站在您要放置的块附近",
		true);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("总是快速放置 ",
			"即使未启用快速放置也会加快放置速度", true);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashSet<BlockPos> remainingBlocks = new LinkedHashSet<>();
	
	public AutoBuildHack()
	{
		super("自动建造");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(checkLOS);
		addSetting(instaBuild);
		addSetting(fastPlace);
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
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos = hitResultPos.offset(blockHitResult.getSide());
		Direction direction = MC.player.getHorizontalFacing();
		remainingBlocks = template.getPositions(startPos, direction);
		
		if(instaBuild.isChecked() && template.size() <= 64)
			buildInstantly();
		else
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		List<BlockPos> blocksToDraw = remainingBlocks.stream()
			.filter(pos -> BlockUtils.getState(pos).isReplaceable()).limit(1024)
			.toList();
		
		int black = 0x80000000;
		List<Box> outlineBoxes =
			blocksToDraw.stream().map(pos -> BLOCK_BOX.offset(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, outlineBoxes, black, true);
		
		int green = 0x2600FF00;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = range.getValueSq();
		List<Box> greenBoxes = blocksToDraw.stream()
			.filter(pos -> pos.getSquaredDistance(eyesPos) <= rangeSq)
			.map(pos -> BLOCK_BOX.offset(pos)).toList();
		RenderUtils.drawSolidBoxes(matrixStack, greenBoxes, green, true);
	}
	
	private void buildNormally()
	{
		remainingBlocks
			.removeIf(pos -> !BlockUtils.getState(pos).isReplaceable());
		
		if(remainingBlocks.isEmpty())
		{
			status = Status.IDLE;
			return;
		}
		
		if(!fastPlace.isChecked() && MC.itemUseCooldown > 0)
			return;
		
		double rangeSq = range.getValueSq();
		for(BlockPos pos : remainingBlocks)
		{
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq)
				continue;
			if(checkLOS.isChecked() && !params.lineOfSight())
				continue;
			
			MC.itemUseCooldown = 4;
			RotationUtils.getNeededRotations(params.hitVec())
				.sendPlayerLookPacket();
			InteractionSimulator.rightClickBlock(params.toHitResult());
			break;
		}
	}
	
	private void buildInstantly()
	{
		double rangeSq = range.getValueSq();
		
		for(BlockPos pos : remainingBlocks)
		{
			if(!BlockUtils.getState(pos).isReplaceable())
				continue;
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq)
				continue;
			
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				SwingHand.OFF);
		}
		
		remainingBlocks.clear();
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
