/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.CmdUtils;
import net.wurstclient.util.MathUtils;

@DontBlock
public final class ItemListCmd extends Command
{
	public ItemListCmd()
	{
		super("itemlist",
			"更改某一功能的ItemList设置\n允许你通过按键绑定来更改这些设置",
			".itemlist <功能> <设置> add <物品>",
			".itemlist <功能> <设置> remove <物品>",
			".itemlist <功能> <设置> list [<页码>]",
			".itemlist <功能> <设置> reset",
			"示例: .itemlist AutoDrop Items add dirt");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 3 || args.length > 4)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting abstractSetting = CmdUtils.findSetting(feature, args[1]);
		ItemListSetting setting =
			getAsItemListSetting(feature, abstractSetting);
		
		switch(args[2].toLowerCase())
		{
			case "add":
			add(feature, setting, args);
			break;
			
			case "remove":
			remove(feature, setting, args);
			break;
			
			case "list":
			list(feature, setting, args);
			break;
			
			case "reset":
			setting.resetToDefaults();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void add(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		Item item = CmdUtils.parseItem(args[3]);
		
		String itemName = BuiltInRegistries.ITEM.getKey(item).toString();
		int index = Collections.binarySearch(setting.getItemNames(), itemName);
		if(index >= 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " 已经包含 " + itemName);
		
		setting.add(item);
	}
	
	private void remove(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		Item item = CmdUtils.parseItem(args[3]);
		
		String itemName = BuiltInRegistries.ITEM.getKey(item).toString();
		int index = Collections.binarySearch(setting.getItemNames(), itemName);
		if(index < 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " 不再包含 " + itemName);
		
		setting.remove(index);
	}
	
	private void list(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length > 4)
			throw new CmdSyntaxError();
		
		List<String> items = setting.getItemNames();
		int page = parsePage(args);
		int pages = (int)Math.ceil(items.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("无效页码: " + page);

		ChatUtils.message("共有: " + items.size() + " 物品");
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, items.size());
		
		ChatUtils.message(feature.getName() + " " + setting.getName()
			+ " (页码 " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(items.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 4)
			return 1;
		
		if(!MathUtils.isInteger(args[3]))
			throw new CmdSyntaxError("不是数字: " + args[3]);
		
		return Integer.parseInt(args[3]);
	}
	
	private ItemListSetting getAsItemListSetting(Feature feature,
		Setting setting) throws CmdError
	{
		if(!(setting instanceof ItemListSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " 不是ItemList设置");
		
		return (ItemListSetting)setting;
	}
}
