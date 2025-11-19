/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.nukers;

import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.hacks.nukers.NukerModeSetting.NukerMode;
import net.wurstclient.hacks.nukers.NukerShapeSetting.NukerShape;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.BlockUtils;

public final class CommonNukerSettings implements LeftClickListener
{
	private static final Minecraft MC = WurstClient.MC;
	
	private final NukerShapeSetting shape = new NukerShapeSetting();
	
	private final CheckboxSetting flat = new CheckboxSetting("平面模式",
		"不会破坏你脚下的任何方块", false);
	
	private final NukerModeSetting mode = new NukerModeSetting();
	
	private final BlockSetting id =
		new BlockSetting("ID", "在ID模式下要破坏的块的类型\nair = 不会破坏任何东西", "minecraft:air", true);
	
	private final CheckboxSetting lockId = new CheckboxSetting("ID 锁",
		"防止通过单击方块或重新启动黑客来更改ID",
		false);
	
	private final NukerMultiIdListSetting multiIdList =
		new NukerMultiIdListSetting();
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(shape, flat, mode, id, lockId, multiIdList);
	}
	
	public void reset()
	{
		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}
	
	public String getRenderNameSuffix()
	{
		return switch(mode.getSelected())
		{
			case ID -> " [ID：" + id.getShortBlockName() + "]";
			case MULTI_ID -> " [多个方块：" + multiIdList.size() + "]";
			case SMASH -> " [破坏]";
			default -> "";
		};
	}
	
	public boolean isIdModeWithAir()
	{
		return mode.getSelected() == NukerMode.ID
			&& id.getBlock() == Blocks.AIR;
	}
	
	public boolean isSphereShape()
	{
		return shape.getSelected() == NukerShape.SPHERE;
	}
	
	public boolean shouldBreakBlock(BlockPos pos)
	{
		if(flat.isChecked() && pos.getY() < MC.player.getY())
			return false;
		
		switch(mode.getSelected())
		{
			default:
			case NORMAL:
			return true;
			
			case ID:
			return BlockUtils.getName(pos).equals(id.getBlockName());
			
			case MULTI_ID:
			return multiIdList.contains(BlockUtils.getBlock(pos));
			
			case SMASH:
			return BlockUtils.getHardness(pos) >= 1;
		}
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(lockId.isChecked() || mode.getSelected() != NukerMode.ID)
			return;
		
		if(!(MC.hitResult instanceof BlockHitResult bHitResult)
			|| bHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		id.setBlockName(BlockUtils.getName(bHitResult.getBlockPos()));
	}
}
