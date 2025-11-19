/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"troll potion", "TrollingPotion", "trolling potion"})
public final class TrollPotionHack extends Hack
{
	private final EnumSetting<PotionType> potionType =
		new EnumSetting<>("药水类型", "要生成的药水类型",
			PotionType.values(), PotionType.SPLASH);
	
	public TrollPotionHack()
	{
		super("恼人药水");
		setCategory(Category.ITEMS);
		addSetting(potionType);
	}
	
	@Override
	protected void onEnable()
	{
		// check gamemode
		if(!MC.player.getAbilities().instabuild)
		{
			ChatUtils.error("仅限创造模式");
			setEnabled(false);
			return;
		}
		
		// generate potion
		ItemStack stack = potionType.getSelected().createPotionStack();
		
		// give potion
		Inventory inventory = MC.player.getInventory();
		int slot = inventory.getFreeSlot();
		if(slot < 0)
			ChatUtils.error("不能给药水, 您的背包已满！");
		else
		{
			InventoryUtils.setCreativeStack(slot, stack);
			ChatUtils.message("已创建药水");
		}
		
		setEnabled(false);
	}

	private enum PotionType
	{
		NORMAL("正常", "", Items.POTION),
		
		SPLASH("喷溅", "喷溅型", Items.SPLASH_POTION),
		
		LINGERING("滞留", "滞留型", Items.LINGERING_POTION),
		
		ARROW("箭矢", "箭矢型", Items.TIPPED_ARROW);
		
		private final String name;
		private final String itemName;
		private final Item item;
		
		private PotionType(String name, String itemName, Item item)
		{
			this.name = name;
			this.itemName = itemName;
			this.item = item;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public ItemStack createPotionStack()
		{
			ItemStack stack = new ItemStack(item);
			
			ArrayList<MobEffectInstance> effects = new ArrayList<>();
			for(int i = 1; i <= 23; i++)
			{
				MobEffect effect =
					BuiltInRegistries.MOB_EFFECT.get(i).get().value();
				Holder<MobEffect> entry =
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
				
				effects.add(new MobEffectInstance(entry, Integer.MAX_VALUE,
					Integer.MAX_VALUE));
			}
			
			stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
				Optional.empty(), Optional.empty(), effects, Optional.empty()));
			
			String name = "\u00a7f" + itemName + "恼人药水";
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
			
			return stack;
		}
	}
}
