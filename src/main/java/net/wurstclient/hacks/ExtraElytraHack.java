/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"EasyElytra", "extra elytra", "easy elytra"})
public final class ExtraElytraHack extends Hack implements UpdateListener
{
	private final CheckboxSetting instantFly = new CheckboxSetting(
		"Instant fly", "Jump to fly, no weird double-jump needed!", true);
	
	private final CheckboxSetting speedCtrl = new CheckboxSetting(
		"Speed control", "Control your speed with the Forward and Back keys.\n"
			+ "(default: W and S)\n" + "No fireworks needed!",
		true);
	
	private final CheckboxSetting heightCtrl =
		new CheckboxSetting("Height control",
			"Control your height with the Jump and Sneak keys.\n"
				+ "(default: Spacebar and Shift)\n" + "No fireworks needed!",
			false);
	
	private final CheckboxSetting stopInWater =
		new CheckboxSetting("Stop flying in water", true);
	
	private int jumpTimer;
	
	public ExtraElytraHack()
	{
		super("鞘翅飞行");
		setCategory(Category.MOVEMENT);
		addSetting(instantFly);
		addSetting(speedCtrl);
		addSetting(heightCtrl);
		addSetting(stopInWater);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		jumpTimer = 0;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(jumpTimer > 0)
			jumpTimer--;
		
		if(!MC.player.canGlide())
			return;
		
		if(MC.player.isGliding())
		{
			if(stopInWater.isChecked() && MC.player.isTouchingWater())
			{
				sendStartStopPacket();
				return;
			}
			
			controlSpeed();
			controlHeight();
			return;
		}
		
		if(MC.options.jumpKey.isPressed())
			doInstantFly();
	}
	
	private void sendStartStopPacket()
	{
		ClientCommandC2SPacket packet = new ClientCommandC2SPacket(MC.player,
			ClientCommandC2SPacket.Mode.START_FALL_FLYING);
		MC.player.networkHandler.sendPacket(packet);
	}
	
	private void controlHeight()
	{
		if(!heightCtrl.isChecked())
			return;
		
		Vec3d v = MC.player.getVelocity();
		
		if(MC.options.jumpKey.isPressed())
			MC.player.setVelocity(v.x, v.y + 0.08, v.z);
		else if(MC.options.sneakKey.isPressed())
			MC.player.setVelocity(v.x, v.y - 0.04, v.z);
	}
	
	private void controlSpeed()
	{
		if(!speedCtrl.isChecked())
			return;
		
		float yaw = (float)Math.toRadians(MC.player.getYaw());
		Vec3d forward = new Vec3d(-MathHelper.sin(yaw) * 0.05, 0,
			MathHelper.cos(yaw) * 0.05);
		
		Vec3d v = MC.player.getVelocity();
		
		if(MC.options.forwardKey.isPressed())
			MC.player.setVelocity(v.add(forward));
		else if(MC.options.backKey.isPressed())
			MC.player.setVelocity(v.subtract(forward));
	}
	
	private void doInstantFly()
	{
		if(!instantFly.isChecked())
			return;
		
		if(jumpTimer <= 0)
		{
			jumpTimer = 20;
			MC.player.setJumping(false);
			MC.player.setSprinting(true);
			MC.player.jump();
		}
		
		sendStartStopPacket();
	}
}
