/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.wurstclient.Category;
import net.wurstclient.Feature;
import net.wurstclient.hack.Hack;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonUtils;

public final class WikiDataExportOtf extends OtherFeature
{
	public WikiDataExportOtf()
	{
		super("维基数据导出",
			"创建一个JSON文件，其中包含有关此版本Wurst中所有不同功能和设置的技术细节，主要用于更新WurstWiki");
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "导出数据";
	}
	
	@Override
	public void doPrimaryAction()
	{
		try
		{
			Path exportFile = WURST.getWurstFolder().resolve("wiki-data.json");
			
			JsonObject json = new JsonObject();
			for(Hack hack : WURST.getHax().getAllHax())
				json.add(hack.getName(), hackToJson(hack));
			
			JsonUtils.toJson(json, exportFile);
			
		}catch(Exception e)
		{
			ChatUtils.error("导出数据失败：" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private JsonObject hackToJson(Hack hack)
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", hack.getName());
		json.addProperty("descriptionKey", hack.getDescriptionKey());
		json.addProperty("type", "Hack");
		Category category = hack.getCategory();
		if(category != null)
			json.addProperty("category", category.getName());
		json.addProperty("keybind", getDefaultKeybind(hack));
		json.addProperty("stateSaved", hack.isStateSaved());
		json.addProperty("class", hack.getClass().getName());
		
		JsonArray settings = new JsonArray();
		for(Setting setting : hack.getSettings().values())
			settings.add(setting.exportWikiData());
		json.add("settings", settings);
		return json;
	}
	
	private String getDefaultKeybind(Feature feature)
	{
		String name = feature.getName().toLowerCase().replace(" ", "_");
		if(name.startsWith("."))
			name = name.substring(1);
		
		for(Keybind keybind : KeybindList.DEFAULT_KEYBINDS)
			if(keybind.getCommands().toLowerCase().contains(name))
				return keybind.getKey();
			
		return null;
	}
}
