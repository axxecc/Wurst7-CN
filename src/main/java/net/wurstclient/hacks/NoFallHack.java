/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.item.Items;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"无摔伤", "NoFall"})
public final class NoFallHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allowElytra = new CheckboxSetting(
		"允许鞘翅", "还会尝试在你使用鞘翅飞行时防止坠落伤害\n\nc§lEARNING：§r这有时会导致你意外停止飞行", false);
	
	private final CheckboxSetting pauseForMace =
		new CheckboxSetting("暂停使用重锤",
			"手持重锤时暂停\"无摔伤\"效果, 以便你能够利用重锤的坠落距离加成\n\n§c§l警告：§r 手持重锤时，你将无法获得坠落伤害保护. 此外, 在坠落过程中切换至重锤或远离重锤都会中断坠落, 可能导致你受到伤害", false);
	
	public NoFallHack()
	{
		super("无摔伤");
		setCategory(Category.MOVEMENT);
		addSetting(allowElytra);
		addSetting(pauseForMace);
	}
	
	@Override
	public String getRenderName()
	{
		LocalPlayer player = MC.player;
		if(player == null)
			return getName();
		
		if(player.isFallFlying() && !allowElytra.isChecked())
			return getName() + " (暂停)";
		
		if(player.isCreative())
			return getName() + " (暂停)";
		
		if(pauseForMace.isChecked() && isHoldingMace(player))
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
		// do nothing in creative mode, since there is no fall damage anyway
		LocalPlayer player = MC.player;
		if(player.isCreative())
			return;
		
		// pause when flying with elytra, unless allowed
		boolean fallFlying = player.isFallFlying();
		if(fallFlying && !allowElytra.isChecked())
			return;
		
		// pause when holding a mace, if enabled
		if(pauseForMace.isChecked() && isHoldingMace(player))
			return;
		
		// attempt to fix elytra weirdness, if allowed
		if(fallFlying && player.isShiftKeyDown()
			&& !isFallingFastEnoughToCauseDamage(player))
			return;
		
		// send packet to stop fall damage
		player.connection.send(new StatusOnly(true));
	}
	
	private boolean isHoldingMace(LocalPlayer player)
	{
		return player.getMainHandItem().is(Items.MACE);
	}
	
	private boolean isFallingFastEnoughToCauseDamage(LocalPlayer player)
	{
		return player.getDeltaMovement().y < -0.5;
	}
}
