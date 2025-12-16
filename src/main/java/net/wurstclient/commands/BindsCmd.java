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
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.wurstclient.DontBlock;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonException;

@DontBlock
public final class BindsCmd extends Command
{
	public BindsCmd()
	{
		super("binds", "它允许你通过聊天管理按键绑定",
			".binds add <键> <功能>", ".binds add <键> <命令>",
			".binds remove <键>", ".binds list [<页码>]",
			".binds load-profile <文件>", ".binds save-profile <文件>",
			".binds list-profiles [<页码>]", ".binds remove-all",
			".binds reset", "多个功能/命令必须用';'分隔",
			"配置文件保存于 '.minecraft/wurst/keybinds'");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "add":
			add(args);
			break;
			
			case "remove":
			remove(args);
			break;
			
			case "list":
			list(args);
			break;
			
			case "load-profile":
			loadProfile(args);
			break;
			
			case "save-profile":
			saveProfile(args);
			break;
			
			case "list-profiles":
			listProfiles(args);
			break;
			
			case "remove-all":
			removeAll();
			break;
			
			case "reset":
			reset();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void add(String[] args) throws CmdException
	{
		if(args.length < 3)
			throw new CmdSyntaxError();
		
		String displayKey = args[1];
		String key = parseKey(displayKey);
		String[] cmdArgs = Arrays.copyOfRange(args, 2, args.length);
		String commands = String.join(" ", cmdArgs);
		
		WURST.getKeybinds().add(key, commands);
		ChatUtils.message("按键绑定: " + displayKey + " -> " + commands);
	}
	
	private void remove(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String displayKey = args[1];
		String key = parseKey(displayKey);
		
		String commands = WURST.getKeybinds().getCommands(key);
		if(commands == null)
			throw new CmdError("没有什么需要移除的");
		
		WURST.getKeybinds().remove(key);
		ChatUtils.message("移除快捷键: " + displayKey + " -> " + commands);
	}
	
	private String parseKey(String displayKey) throws CmdSyntaxError
	{
		String key = displayKey.toLowerCase();
		
		String prefix = "key.keyboard.";
		if(!key.startsWith(prefix))
			key = prefix + key;
		
		try
		{
			InputConstants.getKey(key);
			return key;
			
		}catch(IllegalArgumentException e)
		{
			throw new CmdSyntaxError("未知键: " + displayKey);
		}
	}
	
	private void list(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		List<Keybind> binds = WURST.getKeybinds().getAllKeybinds();
		int page = parsePage(args);
		int pages = (int)Math.ceil(binds.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("无效页码: " + page);

		ChatUtils.message("共有: " + binds.size() + " 快捷键");
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, binds.size());
		
		ChatUtils.message("快捷键列表 (页码 " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(binds.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("不是数字: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}
	
	private void removeAll()
	{
		WURST.getKeybinds().removeAll();
		ChatUtils.message("所有快捷键已移除");
	}
	
	private void reset()
	{
		WURST.getKeybinds().setKeybinds(KeybindList.DEFAULT_KEYBINDS);
		ChatUtils.message("所有快捷键已重置");
	}
	
	private void loadProfile(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		
		try
		{
			WURST.getKeybinds().loadProfile(name);
			ChatUtils.message("加载快捷键: " + name);
			
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
			WURST.getKeybinds().saveProfile(name);
			ChatUtils.message("保存快捷键: " + name);
			
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
		
		ArrayList<Path> files = WURST.getKeybinds().listProfiles();
		int page = parsePage(args);
		int pages = (int)Math.ceil(files.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("无效页码: " + page);

		ChatUtils.message("共有: " + files.size() + " 条配置文件");
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, files.size());
		
		ChatUtils
			.message("快捷键配置文件列表 (页码 " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(files.get(i).getFileName().toString());
	}
}
