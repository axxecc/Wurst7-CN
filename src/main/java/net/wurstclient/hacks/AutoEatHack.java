/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"自动进食", "AutoEat"})
public final class AutoEatHack extends Hack implements UpdateListener
{
	private final SliderSetting targetHunger = new SliderSetting(
		"饥饿目标", "\"在不浪费任何饥饿值的前提下, 尝试将饥饿条保持在此水准之上", 10,
		0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting minHunger = new SliderSetting("最小饥饿",
		"即便浪费一些补充的饥饿值，也总是将饥饿条保持在此水准之上\n6.5 - 不会导致任何原版食物所补充饥饿值的浪费\n10.0 - 完全忽视饥饿值的浪费，总是将饥饿条补满", 6.5, 0, 10, 0.5,
		ValueDisplay.DECIMAL);
	
	private final SliderSetting injuredHunger = new SliderSetting(
		"受伤饥饿", "当你受伤时将你的饥饿条补充到此水准之上, 即使这会导致食物补充的饥饿值被浪费\n10.0 - 最快的回复速度\n9.0 - 最慢的回复速度\n<9.0 - 不回复生命值\n<3.5 - 无法疾跑",
		10, 0, 10, 0.5, ValueDisplay.DECIMAL);
	
	private final SliderSetting injuryThreshold =
		new SliderSetting("受伤阈值",
			"防止轻微的受伤就浪费掉你所有的食物, 自动进食将只会考虑你失去这么多颗心的情况", 1.5, 0.5, 10,
			0.5, ValueDisplay.DECIMAL);
	
	private final EnumSetting<TakeItemsFrom> takeItemsFrom = new EnumSetting<>(
		"从中获取物品", "决定自动进食从哪寻找食物",
		TakeItemsFrom.values(), TakeItemsFrom.HOTBAR);
	
	private final CheckboxSetting allowOffhand =
		new CheckboxSetting("允许副手", true);
	
	private final CheckboxSetting eatWhileWalking =
		new CheckboxSetting("边走边吃",
			"边走边吃，这会使你减速 (不推荐)", false);
	
	private final CheckboxSetting allowHunger =
		new CheckboxSetting("允许饥饿效果",
			"食用腐肉会带来一个无害的\"饥饿\"效果\n将腐肉作为应急食物安全又有用", true);
	
	private final CheckboxSetting allowPoison =
		new CheckboxSetting("允许中毒效果",
			"食用有毒的食物会带来持续伤害\n不推荐", false);
	
	private final CheckboxSetting allowChorus =
		new CheckboxSetting("允许紫颂果",
			"食用紫颂果会将你随机传送\n不推荐", false);
	
	private int oldSlot = -1;
	
	public AutoEatHack()
	{
		super("自动进食");
		setCategory(Category.ITEMS);
		
		addSetting(targetHunger);
		addSetting(minHunger);
		addSetting(injuredHunger);
		addSetting(injuryThreshold);
		
		addSetting(takeItemsFrom);
		addSetting(allowOffhand);
		
		addSetting(eatWhileWalking);
		addSetting(allowHunger);
		addSetting(allowPoison);
		addSetting(allowChorus);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoSoupHack.setEnabled(false);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		if(isEating())
			stopEating();
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		
		if(!shouldEat())
		{
			if(isEating())
				stopEating();
			
			return;
		}
		
		FoodData hungerManager = player.getFoodData();
		int foodLevel = hungerManager.getFoodLevel();
		int targetHungerI = (int)(targetHunger.getValue() * 2);
		int minHungerI = (int)(minHunger.getValue() * 2);
		int injuredHungerI = (int)(injuredHunger.getValue() * 2);
		
		if(isInjured(player) && foodLevel < injuredHungerI)
		{
			eat(-1);
			return;
		}
		
		if(foodLevel < minHungerI)
		{
			eat(-1);
			return;
		}
		
		if(foodLevel < targetHungerI)
		{
			int maxPoints = targetHungerI - foodLevel;
			eat(maxPoints);
		}
	}
	
	private void eat(int maxPoints)
	{
		Inventory inventory = MC.player.getInventory();
		int foodSlot = findBestFoodSlot(maxPoints);
		
		if(foodSlot == -1)
		{
			if(isEating())
				stopEating();
			
			return;
		}
		
		// select food
		if(foodSlot < 9)
		{
			if(!isEating())
				oldSlot = inventory.getSelectedSlot();
			
			inventory.setSelectedSlot(foodSlot);
			
		}else if(foodSlot == 40)
		{
			if(!isEating())
				oldSlot = inventory.getSelectedSlot();
			
			// off-hand slot, no need to select anything
			
		}else
		{
			InventoryUtils.selectItem(foodSlot);
			return;
		}
		
		// eat food
		MC.options.keyUse.setDown(true);
		IMC.getInteractionManager().rightClickItem();
	}
	
	private int findBestFoodSlot(int maxPoints)
	{
		Inventory inventory = MC.player.getInventory();
		FoodProperties bestFood = null;
		int bestSlot = -1;
		
		int maxInvSlot = takeItemsFrom.getSelected().maxInvSlot;
		
		ArrayList<Integer> slots = new ArrayList<>();
		if(maxInvSlot == 0)
			slots.add(inventory.getSelectedSlot());
		if(allowOffhand.isChecked())
			slots.add(40);
		Stream.iterate(0, i -> i < maxInvSlot, i -> i + 1)
			.forEach(i -> slots.add(i));
		
		Comparator<FoodProperties> comparator =
			Comparator.comparingDouble(FoodProperties::saturation);
		
		for(int slot : slots)
		{
			ItemStack stack = inventory.getItem(slot);
			
			// filter out non-food items
			if(!stack.has(DataComponents.FOOD))
				continue;
			
			if(!isAllowedFood(stack.get(DataComponents.CONSUMABLE)))
				continue;
			
			FoodProperties food = stack.get(DataComponents.FOOD);
			if(maxPoints >= 0 && food.nutrition() > maxPoints)
				continue;
			
			// compare to previously found food
			if(bestFood == null || comparator.compare(food, bestFood) > 0)
			{
				bestFood = food;
				bestSlot = slot;
			}
		}
		
		return bestSlot;
	}
	
	private boolean shouldEat()
	{
		if(MC.player.getAbilities().instabuild)
			return false;
		
		if(!MC.player.canEat(false))
			return false;
		
		if(!eatWhileWalking.isChecked()
			&& (MC.player.zza != 0 || MC.player.xxa != 0))
			return false;
		
		if(isClickable(MC.hitResult))
			return false;
		
		return true;
	}
	
	private void stopEating()
	{
		MC.options.keyUse.setDown(false);
		MC.player.getInventory().setSelectedSlot(oldSlot);
		oldSlot = -1;
	}
	
	private boolean isAllowedFood(Consumable consumable)
	{
		for(ConsumeEffect consumeEffect : consumable.onConsumeEffects())
		{
			if(!allowChorus.isChecked()
				&& consumeEffect instanceof TeleportRandomlyConsumeEffect)
				return false;
			
			if(!(consumeEffect instanceof ApplyStatusEffectsConsumeEffect applyEffectsConsumeEffect))
				continue;
			
			for(MobEffectInstance effect : applyEffectsConsumeEffect.effects())
			{
				Holder<MobEffect> entry = effect.getEffect();
				
				if(!allowHunger.isChecked() && entry == MobEffects.HUNGER)
					return false;
				
				if(!allowPoison.isChecked() && entry == MobEffects.POISON)
					return false;
			}
		}
		
		return true;
	}
	
	public boolean isEating()
	{
		return oldSlot != -1;
	}
	
	private boolean isClickable(HitResult hitResult)
	{
		if(hitResult == null)
			return false;
		
		if(hitResult instanceof EntityHitResult)
		{
			Entity entity = ((EntityHitResult)hitResult).getEntity();
			return entity instanceof Villager
				|| entity instanceof TamableAnimal;
		}
		
		if(hitResult instanceof BlockHitResult)
		{
			BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
			if(pos == null)
				return false;
			
			Block block = MC.level.getBlockState(pos).getBlock();
			return block instanceof BaseEntityBlock
				|| block instanceof CraftingTableBlock;
		}
		
		return false;
	}
	
	private boolean isInjured(LocalPlayer player)
	{
		int injuryThresholdI = (int)(injuryThreshold.getValue() * 2);
		return player.getHealth() < player.getMaxHealth() - injuryThresholdI;
	}
	
	private enum TakeItemsFrom
	{
		HANDS("手中", 0),
		
		HOTBAR("快捷栏", 9),
		
		INVENTORY("物品栏", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private TakeItemsFrom(String name, int maxInvSlot)
		{
			this.name = name;
			this.maxInvSlot = maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
