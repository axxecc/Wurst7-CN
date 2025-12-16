/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PlayerAttacksEntityListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"暴击", "Crits"})
public final class CriticalsHack extends Hack
	implements PlayerAttacksEntityListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("模式",
		"\u00a7l数据包\u00a7r模式将数据包发送到服务器, 但实际上根本不会移动您\n\n\u00a7l迷你跳跃\u00a7r模式会进行一次微小的跳跃, 刚好足以获得暴击\n\n§l\u00a7l完全跳跃\u00a7r模式让您正常跳跃",
		Mode.values(), Mode.PACKET);
	
	public CriticalsHack()
	{
		super("暴击");
		setCategory(Category.COMBAT);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + mode.getSelected() + "]";
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PlayerAttacksEntityListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PlayerAttacksEntityListener.class, this);
	}
	
	@Override
	public void onPlayerAttacksEntity(Entity target)
	{
		if(!(target instanceof LivingEntity))
			return;
		
		if(WURST.getHax().maceDmgHack.isEnabled()
			&& MC.player.getMainHandItem().is(Items.MACE))
			return;
		
		if(!MC.player.onGround())
			return;
		
		if(MC.player.isInWater() || MC.player.isInLava())
			return;
		
		switch(mode.getSelected())
		{
			case PACKET:
			doPacketJump();
			break;
			
			case MINI_JUMP:
			doMiniJump();
			break;
			
			case FULL_JUMP:
			doFullJump();
			break;
		}
	}
	
	private void doPacketJump()
	{
		sendFakeY(0.0625, true);
		sendFakeY(0, false);
		sendFakeY(1.1e-5, false);
		sendFakeY(0, false);
	}
	
	private void sendFakeY(double offset, boolean onGround)
	{
		MC.player.connection
			.send(new Pos(MC.player.getX(), MC.player.getY() + offset,
				MC.player.getZ(), onGround, MC.player.horizontalCollision));
	}
	
	private void doMiniJump()
	{
		MC.player.push(0, 0.1, 0);
		MC.player.fallDistance = 0.1F;
		MC.player.setOnGround(false);
	}
	
	private void doFullJump()
	{
		MC.player.jumpFromGround();
	}
	
	private enum Mode
	{
		PACKET("数据包"),
		MINI_JUMP("迷你跳跃"),
		FULL_JUMP("完全跳跃");
		
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
