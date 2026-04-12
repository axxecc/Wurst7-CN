/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Items;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"无摔伤", "NoFall"})
public final class NoFallHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allowElytra = new CheckboxSetting("允许鞘翅",
		"还会尝试在你使用鞘翅飞行时防止坠落伤害\n\n§c§l警告：§r这有时会导致你意外停止飞行", false);
	
	private final CheckboxSetting pauseForMace = new CheckboxSetting("暂停使用重锤",
		"手持重锤时暂停\"无摔伤\"效果, 以便你能够利用重锤的坠落距离加成\n\n§c§l警告：§r 手持重锤时，你将无法获得坠落伤害保护. 此外, 在坠落过程中切换至重锤或远离重锤都会中断坠落, 可能导致你受到伤害",
		false);
	
	private final SliderSetting minFallDistance =
		new SliderSetting("最小摔落距离", "NoFall启动的最小坠落距离, 通过忽略那些不会伤害你的小坠落", 1, 0,
			10, 0.1, ValueDisplay.DECIMAL.withSuffix("m").withLabel(0, "关闭"));
	
	private final SliderSetting minFallDistanceElytra = new SliderSetting(
		"最小鞘翅摔落距离", "飞行时NoFall激活的最小坠落距离, 并通过忽略那些不会造成实质性伤害的小坠落, 避免了飞行中断的问题", 2,
		0, 10, 0.1, ValueDisplay.DECIMAL.withSuffix("m").withLabel(0, "关闭"));
	
	public NoFallHack()
	{
		super("无摔伤");
		setCategory(Category.MOVEMENT);
		addSetting(allowElytra);
		addSetting(pauseForMace);
		addSetting(minFallDistance);
		addSetting(minFallDistanceElytra);
	}
	
	@Override
	public String getRenderName()
	{
		if(MC.player != null && isPaused())
			return getName() + " (暂停)";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().antiHungerHack.setEnabled(false);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(isPaused())
			return;
		
		// send packet to stop fall damage
		MC.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(
			true, MC.player.horizontalCollision));
	}
	
	private boolean isPaused()
	{
		// do nothing in creative mode, since there is no fall damage anyway
		LocalPlayer player = MC.player;
		if(player.getAbilities().invulnerable)
			return true;
		
		// pause when flying with elytra, unless allowed
		boolean fallFlying = player.isFallFlying();
		if(fallFlying && !allowElytra.isChecked())
			return true;
		
		// pause when holding a mace, if enabled
		if(pauseForMace.isChecked() && player.getMainHandItem().is(Items.MACE))
			return true;
			
		// ignore small falls that can't cause damage,
		// unless CreativeFlight is enabled in survival mode
		boolean creativeFlying = WURST.getHax().creativeFlightHack.isEnabled()
			&& player.getAbilities().flying;
		if(!creativeFlying && player.fallDistance <= (fallFlying
			? minFallDistanceElytra.getValue() : minFallDistance.getValue()))
			return true;
		
		// attempt to fix elytra weirdness, if allowed
		if(fallFlying && player.isShiftKeyDown()
			&& !isFallingFastEnoughToCauseDamage(player))
			return true;
		
		return false;
	}
	
	private boolean isFallingFastEnoughToCauseDamage(LocalPlayer player)
	{
		return player.getDeltaMovement().y < -0.5;
	}
}
