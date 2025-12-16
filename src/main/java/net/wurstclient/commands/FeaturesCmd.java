/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.util.ChatUtils;

public final class FeaturesCmd extends Command
{
	public FeaturesCmd()
	{
		super("features",
			"显示功能数量及其他统计数据",
			".features");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 0)
			throw new CmdSyntaxError();
		
		if(WurstClient.VERSION.startsWith("7.0pre"))
			ChatUtils.warning(
				"这只是预发布！它还没有 (目前) 拥有Wurst");
		
		int hax = WURST.getHax().countHax();
		int cmds = WURST.getCmds().countCmds();
		int otfs = WURST.getOtfs().countOtfs();
		int all = hax + cmds + otfs;
		
		ChatUtils.message("所有功能: " + all);
		ChatUtils.message("普通功能: " + hax);
		ChatUtils.message("功能命令: " + cmds);
		ChatUtils.message("其他功能: " + otfs);
		
		int settings = 0;
		for(Hack hack : WURST.getHax().getAllHax())
			settings += hack.getSettings().size();
		for(Command cmd : WURST.getCmds().getAllCmds())
			settings += cmd.getSettings().size();
		for(OtherFeature otf : WURST.getOtfs().getAllOtfs())
			settings += otf.getSettings().size();
		
		ChatUtils.message("设置: " + settings);
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "显示统计数据";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("features");
	}
}
