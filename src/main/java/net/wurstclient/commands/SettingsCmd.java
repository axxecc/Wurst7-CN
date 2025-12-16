/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;

import net.wurstclient.DontBlock;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonException;

@DontBlock
public final class SettingsCmd extends Command
{
	public SettingsCmd()
	{
		super("settings", "允许你创建设置配置文件",
			".settings load-profile <文件>", ".settings save-profile <文件>",
			".settings list-profiles [<页码>]",
			"配置文件保存于 '.minecraft/wurst/settings'.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "load-profile":
			loadProfile(args);
			break;
			
			case "save-profile":
			saveProfile(args);
			break;
			
			case "list-profiles":
			listProfiles(args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void loadProfile(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		
		try
		{
			WURST.loadSettingsProfile(name);
			ChatUtils.message("设置已加载: " + name);
			
		}catch(NoSuchFileException e)
		{
			throw new CmdError("配置文件 '" + name + "' 不存在");
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			throw new CmdError(
				"配置文件 '" + name + "' 已损坏: " + e.getMessage());
			
		}catch(IOException e)
		{
			e.printStackTrace();
			throw new CmdError("无法加载配置文件: " + e.getMessage());
		}
	}
	
	private void saveProfile(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		
		try
		{
			WURST.saveSettingsProfile(name);
			ChatUtils.message("设置已保存: " + name);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			throw new CmdError("无法保存配置文件: " + e.getMessage());
		}
	}
	
	private String parseFileName(String input)
	{
		String fileName = input;
		if(!fileName.endsWith(".json"))
			fileName += ".json";
		
		return fileName;
	}
	
	private void listProfiles(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		ArrayList<Path> files = WURST.listSettingsProfiles();
		int page = parsePage(args);
		int pages = (int)Math.ceil(files.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("无效页码: " + page);

		ChatUtils.message("共有: " + files.size() + " 条配置文件");
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, files.size());
		
		ChatUtils
			.message("设置配置文件列表 (页码 " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(files.get(i).getFileName().toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("不是数字: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}
}
