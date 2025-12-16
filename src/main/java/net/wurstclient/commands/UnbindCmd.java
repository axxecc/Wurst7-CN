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

public final class UnbindCmd extends Command
{
	public UnbindCmd()
	{
		super("unbind", "'.binds remove' 的快捷键", ".unbind <键>",
			"使用 .binds 来获得更多选项");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getCmdProcessor()
			.process("解除绑定 " + String.join(" ", args));
	}
}
