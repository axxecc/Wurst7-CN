/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

public final class ModifyCmd extends Command
{
	public ModifyCmd()
	{
		super("modify", "允许你修改物品的组件数据",
			".modify set <类型> <值>", ".modify remove <类型>",
			"用$表示颜色，用$$表示$", "", "示例:",
			".modify set custom_name {\"text\":\"$c红色名字\"}",
			"(将物品名称更改为 \u00a7c红色名字\u00a7r)");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		LocalPlayer player = MC.player;
		if(!player.getAbilities().instabuild)
			throw new CmdError("仅限创造模式");
		if(args.length < 2)
			throw new CmdSyntaxError();
		
		Inventory inventory = player.getInventory();
		int slot = inventory.getSelectedSlot();
		ItemStack stack = inventory.getSelectedItem();
		if(stack == null)
			throw new CmdError("你必须用主手拿着物品");
		
		switch(args[0].toLowerCase())
		{
			case "set":
			set(stack, args);
			break;
			
			case "remove":
			remove(stack, args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		InventoryUtils.setCreativeStack(slot, stack);
		ChatUtils.message("物品已修改");
	}
	
	private void set(ItemStack stack, String[] args) throws CmdException
	{
		if(args.length < 3)
			throw new CmdSyntaxError();
		
		DataComponentType<?> type = parseComponentType(args[1]);
		
		String valueString =
			String.join(" ", Arrays.copyOfRange(args, 2, args.length))
				.replace("$", "\u00a7").replace("\u00a7\u00a7", "$");
		JsonElement valueJson = parseJson(valueString);
		DataResult<?> valueResult =
			type.codec().parse(MC.player.registryAccess()
				.createSerializationContext(JsonOps.INSTANCE), valueJson);
		Object value = valueResult.resultOrPartial().orElse(null);
		
		DataComponentMap.Builder builder = DataComponentMap.builder();
		builder.setUnchecked(type, value);
		stack.applyComponents(builder.build());
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		stack.set(parseComponentType(args[1]), null);
	}
	
	private DataComponentType<?> parseComponentType(String typeName)
		throws CmdError
	{
		DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE
			.get(ResourceLocation.tryParse(typeName));
		
		if(type == null)
			throw new CmdError(
				"组件类型 \"" + typeName + "\" 不存在");
		
		return type;
	}
	
	private JsonElement parseJson(String jsonString) throws CmdError
	{
		try
		{
			return JsonParser.parseString(jsonString);
			
		}catch(JsonParseException e)
		{
			if(e.getCause() != null)
				throw new CmdError(e.getCause().getMessage());
			throw new CmdError(e.getMessage());
		}
	}
}
