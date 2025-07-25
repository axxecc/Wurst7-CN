/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.StopUsingItemListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"arrow dmg", "ArrowDamage", "arrow damage"})
public final class ArrowDmgHack extends Hack implements StopUsingItemListener
{
	private final SliderSetting strength = new SliderSetting("强度",
		"description.wurst.setting.arrowdmg.strength", 10, 0.1, 10, 0.1,
		ValueDisplay.DECIMAL);
	
	private final CheckboxSetting yeetTridents =
		new CheckboxSetting("三叉戟模式",
			"description.wurst.setting.arrowdmg.trident_yeet_mode", false);
	
	public ArrowDmgHack()
	{
		super("强力箭矢");
		setCategory(Category.COMBAT);
		addSetting(strength);
		addSetting(yeetTridents);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(StopUsingItemListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(StopUsingItemListener.class, this);
	}
	
	@Override
	public void onStopUsingItem()
	{
		ClientPlayerEntity player = MC.player;
		ClientPlayNetworkHandler netHandler = player.networkHandler;
		
		if(!isValidItem(player.getMainHandStack().getItem()))
			return;
		
		netHandler.sendPacket(
			new ClientCommandC2SPacket(player, Mode.START_SPRINTING));
		
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		
		// See ServerPlayNetworkHandler.onPlayerMove()
		// for why it's using these numbers.
		// Also, let me know if you find a way to bypass that check in 1.21.
		double adjustedStrength = strength.getValue() / 10.0 * Math.sqrt(500);
		Vec3d lookVec = player.getRotationVec(1).multiply(adjustedStrength);
		for(int i = 0; i < 4; i++)
			sendPos(x, y, z, true);
		sendPos(x - lookVec.x, y, z - lookVec.z, true);
		sendPos(x, y, z, false);
	}
	
	private void sendPos(double x, double y, double z, boolean onGround)
	{
		ClientPlayNetworkHandler netHandler = MC.player.networkHandler;
		netHandler.sendPacket(new PositionAndOnGround(x, y, z, onGround,
			MC.player.horizontalCollision));
	}
	
	private boolean isValidItem(Item item)
	{
		if(yeetTridents.isChecked() && item == Items.TRIDENT)
			return true;
		
		return item == Items.BOW;
	}
}
