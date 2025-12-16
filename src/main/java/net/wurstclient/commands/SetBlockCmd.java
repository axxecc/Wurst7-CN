/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.world.level.block.Block;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.CmdUtils;

@DontBlock
public final class SetBlockCmd extends Command
{
	public SetBlockCmd()
	{
		super("setblock",
			"更改某个功能的块设置, 允许你通过按键绑定来更改这些设置",
			".setblock <功能> <设置> <方块>",
			".setblock <功能> <设置> reset",
			"示例: .setblock Nuker ID dirt");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting setting = CmdUtils.findSetting(feature, args[1]);
		BlockSetting blockSetting = getAsBlockSetting(feature, setting);
		setBlock(blockSetting, args[2]);
	}
	
	private BlockSetting getAsBlockSetting(Feature feature, Setting setting)
		throws CmdError
	{
		if(!(setting instanceof BlockSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " 不是方块设置");
		
		return (BlockSetting)setting;
	}
	
	private void setBlock(BlockSetting setting, String value)
		throws CmdSyntaxError
	{
		if(value.toLowerCase().equals("reset"))
		{
			setting.resetToDefault();
			return;
		}
		
		Block block = BlockUtils.getBlockFromNameOrID(value);
		if(block == null)
			throw new CmdSyntaxError("\"" + value + "\" 不是一个有效的方块");
		
		setting.setBlock(block);
	}
}
