/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PlayerAttacksEntityListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"AttributeSwap"})
public final class AttributeSwapHack extends Hack
	implements PlayerAttacksEntityListener, UpdateListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("模式",
		"简单: 攻击时切换到固定快捷栏槽\n智能: 检查目标并自动使用最好的物品",
		Mode.values(), Mode.SIMPLE);
	
	private final SliderSetting targetSlot = new SliderSetting("目标槽位",
		"快捷栏槽切换到 (简单模式)", 1, 1, 9, 1,
		ValueDisplay.INTEGER);
	
	private final CheckboxSetting swapBack = new CheckboxSetting("切换回去",
		"延迟后切换回回原快捷栏", true);
	
	private final SliderSetting swapBackDelay =
		new SliderSetting("切回延迟",
			"要等多少Tick才能切换回去", 2, 0, 20, 1,
			ValueDisplay.INTEGER.withSuffix(" Tick").withLabel(0, "instant"));
	
	private final CheckboxSetting breachSwapping = new CheckboxSetting(
		"破甲重锤",
		"切换成带破甲重锤以造成更多伤害 (智能模式)", true);
	
	private final CheckboxSetting shieldBreaker =
		new CheckboxSetting("自动破盾",
			"当目标格挡时切换成斧头 (智能模式)", true);
	
	private final CheckboxSetting itemSaver = new CheckboxSetting("物品保护",
		"切换到不可损坏的物品以节省主武器耐久度 (智能模式)",
		true);
	
	private final CheckboxSetting onlyWithKillAura = new CheckboxSetting(
		"仅杀戮光环", "只有在杀戮光环被启用时才会激活", false);
	
	private int backTimer;
	private boolean awaitingBack;
	private int originalSlot;
	
	public AttributeSwapHack()
	{
		super("自动秒切");
		setCategory(Category.COMBAT);
		addSetting(mode);
		addSetting(targetSlot);
		addSetting(swapBack);
		addSetting(swapBackDelay);
		addSetting(shieldBreaker);
		addSetting(itemSaver);
		addSetting(breachSwapping);
		addSetting(onlyWithKillAura);
	}
	
	@Override
	protected void onEnable()
	{
		backTimer = 0;
		awaitingBack = false;
		originalSlot = -1;
		
		EVENTS.add(PlayerAttacksEntityListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PlayerAttacksEntityListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		
		if(awaitingBack)
		{
			doSwapBack();
			awaitingBack = false;
		}
		backTimer = 0;
		originalSlot = -1;
	}
	
	@Override
	public void onPlayerAttacksEntity(Entity target)
	{
		if(onlyWithKillAura.isChecked()
			&& !WURST.getHax().killauraHack.isEnabled())
			return;
		
		performSwap(target);
	}
	
	@Override
	public void onUpdate()
	{
		if(!awaitingBack)
			return;
		if(backTimer-- > 0)
			return;
		
		doSwapBack();
		awaitingBack = false;
	}
	
	private void performSwap(Entity target)
	{
		if(awaitingBack)
			return;
		
		if(mode.getSelected() == Mode.SIMPLE)
		{
			doSwap(targetSlot.getValueI() - 1);
			return;
		}
		
		doSwap(getSmartSlot(target));
	}
	
	private void doSwap(int slotIndex)
	{
		if(awaitingBack)
			return;
		if(slotIndex < 0 || slotIndex > 8)
			return;
		
		int current = MC.player.getInventory().getSelectedSlot();
		if(slotIndex == current)
			return;
		
		originalSlot = current;
		MC.player.getInventory().setSelectedSlot(slotIndex);
		
		if(swapBack.isChecked())
		{
			awaitingBack = true;
			backTimer = swapBackDelay.getValueI();
		}
	}
	
	private void doSwapBack()
	{
		if(originalSlot >= 0 && originalSlot <= 8)
			MC.player.getInventory().setSelectedSlot(originalSlot);
		
		originalSlot = -1;
	}
	
	private int getSmartSlot(Entity target)
	{
		ItemStack current = MC.player.getMainHandItem();
		
		if(target instanceof LivingEntity living && shieldBreaker.isChecked()
			&& living.isBlocking())
		{
			if(current.getItem() instanceof AxeItem)
				return -1;
			
			int axeSlot =
				InventoryUtils.indexOf(s -> s.getItem() instanceof AxeItem, 9);
			if(axeSlot != -1)
				return axeSlot;
		}
		
		if(breachSwapping.isChecked() && target instanceof LivingEntity le
			&& le.getAttributes().getValue(Attributes.ARMOR) > 0)
		{
			int breachSlot =
				InventoryUtils.indexOf(s -> s.getItem() instanceof MaceItem
					&& getEnchantLevel(s, Enchantments.BREACH) > 0, 9);
			
			if(breachSlot != -1)
				return breachSlot;
		}
		
		int bestSlot = -1;
		int bestScore = getDurabilityScore(current);
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == MC.player.getInventory().getSelectedSlot())
				continue;
			
			ItemStack stack = MC.player.getInventory().getItem(slot);
			if(stack.isEmpty())
				continue;
			
			int score = getDurabilityScore(stack);
			if(score > bestScore)
			{
				bestScore = score;
				bestSlot = slot;
			}
		}
		
		return bestSlot;
	}
	
	private int getDurabilityScore(ItemStack stack)
	{
		if(!itemSaver.isChecked())
			return 0;
		if(!stack.isDamageableItem())
			return 4;
		
		return 0;
	}
	
	private int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key)
	{
		if(MC.level == null)
			return 0;
		
		return MC.level.registryAccess().lookup(Registries.ENCHANTMENT)
			.flatMap(reg -> reg.get(key)).map(holder -> EnchantmentHelper
				.getItemEnchantmentLevel(holder, stack))
			.orElse(0);
	}
	
	private enum Mode
	{
		SIMPLE("简单"),
		SMART("智能");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
