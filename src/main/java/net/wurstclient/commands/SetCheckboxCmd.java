/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.CmdUtils;

@DontBlock
public final class SetCheckboxCmd extends Command
{
	public SetCheckboxCmd()
	{
		super("setcheckbox",
			"更改功能中的复选框设置, 允许你通过快捷键切换复选框",
			".setcheckbox <功能> <设置> (on|off)",
			".setcheckbox <功能> <设置> toggle");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting setting = CmdUtils.findSetting(feature, args[1]);
		CheckboxSetting checkbox = getAsCheckbox(feature, setting);
		setChecked(checkbox, args[2]);
	}
	
	private CheckboxSetting getAsCheckbox(Feature feature, Setting setting)
		throws CmdError
	{
		if(!(setting instanceof CheckboxSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " 不是复选框设置");
		
		return (CheckboxSetting)setting;
	}
	
	private void setChecked(CheckboxSetting checkbox, String value)
		throws CmdSyntaxError
	{
		switch(value.toLowerCase())
		{
			case "on":
			checkbox.setChecked(true);
			break;
			
			case "off":
			checkbox.setChecked(false);
			break;
			
			case "toggle":
			checkbox.setChecked(!checkbox.isChecked());
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
}
