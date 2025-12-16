/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public final class BindCmd extends Command
{
	public BindCmd()
	{
		super("bind", "'.binds add' 的快捷方式", ".bind <键> <功能>",
			".bind <键> <命令>",
			"多个功能/命令必须用';'来区分",
			"用 .binds 来获取更多选项");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getCmdProcessor().process("绑定了 " + String.join(" ", args));
	}
}
