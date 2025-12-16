/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.StringUtil;
import net.wurstclient.altmanager.AltManager;
import net.wurstclient.altmanager.CrackedAlt;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class AddAltCmd extends Command
{
	public AddAltCmd()
	{
		super("addalt", "会把一个玩家加入你的账户列表", ".addalt <玩家>",
			"添加服务器上的所有玩家: .addalt all");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		String name = args[0];
		
		switch(name)
		{
			case "all":
			addAll();
			break;
			
			default:
			add(name);
			break;
		}
	}
	
	private void add(String name)
	{
		if(name.equalsIgnoreCase("Alexander01998"))
			return;
		
		WURST.getAltManager().add(new CrackedAlt(name));
		ChatUtils.message("加了 1 个账户");
	}
	
	private void addAll()
	{
		int alts = 0;
		AltManager altManager = WURST.getAltManager();
		String playerName = MC.getUser().getName();
		
		for(PlayerInfo entry : MC.player.connection.getOnlinePlayers())
		{
			String name = entry.getProfile().getName();
			name = StringUtil.stripColor(name);
			
			if(altManager.contains(name))
				continue;
			
			if(name.equalsIgnoreCase(playerName)
				|| name.equalsIgnoreCase("Alexander01998"))
				continue;
			
			altManager.add(new CrackedAlt(name));
			alts++;
		}
		
		ChatUtils.message("添加了 " + alts + " 个账户");
	}
}
