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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

public final class StepHack extends Hack implements UpdateListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("模式",
		"\u00a7l简单\u00a7r模式可以增加多个块（启用高度滑块）\n\u00a7l合法\u00a7r模式可以绕过 NoCheat+",
		Mode.values(), Mode.LEGIT);
	
	private final SliderSetting height =
		new SliderSetting("高度", "仅在\u00a7l简单\u00a7r模式下工作。",
			1, 1, 10, 1, ValueDisplay.INTEGER);
	
	public StepHack()
	{
		super("快速上楼");
		setCategory(Category.MOVEMENT);
		addSetting(mode);
		addSetting(height);
	}
	
	@Override
	protected void onEnable()
	{
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
		if(mode.getSelected() == Mode.SIMPLE)
			return;
		
		ClientPlayerEntity player = MC.player;
		if(!player.horizontalCollision)
			return;
		
		if(!player.isOnGround() || player.isClimbing()
			|| player.isTouchingWater() || player.isInLava())
			return;
		
		if(player.input.getMovementInput().length() <= 1e-5F)
			return;
		
		if(player.jumping)
			return;
		
		Box box = player.getBoundingBox().offset(0, 0.05, 0).expand(0.05);
		if(!MC.world.isSpaceEmpty(player, box.offset(0, 1, 0)))
			return;
		
		double stepHeight = BlockUtils.getBlockCollisions(box)
			.mapToDouble(bb -> bb.maxY).max().orElse(Double.NEGATIVE_INFINITY);
		
		stepHeight -= player.getY();
		
		if(stepHeight < 0 || stepHeight > 1)
			return;
		
		ClientPlayNetworkHandler netHandler = player.networkHandler;
		
		netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
			player.getX(), player.getY() + 0.42 * stepHeight, player.getZ(),
			player.isOnGround(), MC.player.horizontalCollision));
		
		netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
			player.getX(), player.getY() + 0.753 * stepHeight, player.getZ(),
			player.isOnGround(), MC.player.horizontalCollision));
		
		player.setPosition(player.getX(), player.getY() + stepHeight,
			player.getZ());
	}
	
	public float adjustStepHeight(float stepHeight)
	{
		if(isEnabled() && mode.getSelected() == Mode.SIMPLE)
			return height.getValueF();
		
		return stepHeight;
	}
	
	public boolean isAutoJumpAllowed()
	{
		return !isEnabled() && !WURST.getCmds().goToCmd.isActive();
	}
	
	private enum Mode
	{
		SIMPLE("简单"),
		LEGIT("合法");
		
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
