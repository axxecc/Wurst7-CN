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

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
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
		if(!MC.player.getAbilities().creativeMode)
		{
			ChatUtils.error("仅限创造模式");
			setEnabled(false);
			return;
		}
		
		// generate potion
		ItemStack stack = potionType.getSelected().createPotionStack();
		
		// give potion
		PlayerInventory inventory = MC.player.getInventory();
		int slot = inventory.getEmptySlot();
		if(slot < 0)
			ChatUtils.error("不能给药水，您的背包已满！");
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
			
			ArrayList<StatusEffectInstance> effects = new ArrayList<>();
			for(int i = 1; i <= 23; i++)
			{
				StatusEffect effect =
					Registries.STATUS_EFFECT.getEntry(i).get().value();
				RegistryEntry<StatusEffect> entry =
					Registries.STATUS_EFFECT.getEntry(effect);
				
				effects.add(new StatusEffectInstance(entry, Integer.MAX_VALUE,
					Integer.MAX_VALUE));
			}
			
			stack.set(DataComponentTypes.POTION_CONTENTS,
				new PotionContentsComponent(Optional.empty(), Optional.empty(),
					effects, Optional.empty()));
			
			String name = "\u00a7f" + itemName + "恼人药水";
			stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
			
			return stack;
		}
	}
}
