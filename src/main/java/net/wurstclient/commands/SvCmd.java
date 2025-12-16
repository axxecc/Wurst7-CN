/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.multiplayer.ServerData;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.LastServerRememberer;

public final class SvCmd extends Command
{
	public SvCmd()
	{
		super("sv", "显示你当前连接的服务器版本",
                ".sv");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 0)
			throw new CmdSyntaxError();
		
		ChatUtils.message("服务器版本: " + getVersion());
	}
	
	private String getVersion() throws CmdError
	{
		if(MC.hasSingleplayerServer())
			throw new CmdError("单人模式无法查看服务器版本");
		
		ServerData lastServer = LastServerRememberer.getLastServer();
		if(lastServer == null)
			throw new IllegalStateException(
				"上次的服务器不记得上一个服务器!");
		
		return lastServer.version.getString();
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "获取服务器版本";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("sv");
	}
}
