/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.AirStrafingSpeedListener;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"生存飞行", "FlyHack"})
public final class FlightHack extends Hack implements UpdateListener,
	IsPlayerInWaterListener, AirStrafingSpeedListener, MouseScrollListener
{
	private final SliderSetting horizontalSpeed = new SliderSetting(
		"水平速度", "使用飞行时的水平移动速度",
		1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting verticalSpeed =
		new SliderSetting("垂直速度",
			"使用飞行时的垂直移动速度(相对于水平移动)", 1, 0.05, 5, 0.05,
			v -> ValueDisplay.DECIMAL.getValueString(getActualVerticalSpeed()));
	
	private final CheckboxSetting allowUnsafeVerticalSpeed =
		new CheckboxSetting("不安全的垂直速度",
			"在生存模式下, 垂直速度可以超过3.95, 即使开启NoFall也会受到坠落伤害",
			false);
	
	private final CheckboxSetting scrollToChangeSpeed =
		new CheckboxSetting("滚动以改变速度",
			"当你滚动鼠标滚轮时, 它会改变你的速度\n\n§l注意: §r启用后, 飞行时滚动鼠标滚轮不会改变你选择的快捷栏物品", true);

	private final CheckboxSetting renderSpeed =
		new CheckboxSetting("列表中显示速度",
			"在HackList中显示你当前的水平和垂直速度", true);

	private final CheckboxSetting antiKick = new CheckboxSetting("防踢",
		"它会让你偶尔下降一下, 防止被踢", false);

	private final SliderSetting antiKickInterval =
		new SliderSetting("防踢间隔",
			"反踢应该多久防止你被踢一次?\n大多数服务器会在80Tick后踢你", 70, 5, 80, 1,
			ValueDisplay.INTEGER.withSuffix(" Tick"));
	
	private final SliderSetting antiKickDistance =
		new SliderSetting("防踢距离",
			"防踢应该让你摔多远\n大多数服务器至少需要0.032m才能阻止你被踢", 0.035, 0.01,
			0.2, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private int tickCounter = 0;
	
	public FlightHack()
	{
		super("生存飞行");
		setCategory(Category.MOVEMENT);
		addSetting(horizontalSpeed);
		addSetting(verticalSpeed);
		addSetting(allowUnsafeVerticalSpeed);
		addSetting(scrollToChangeSpeed);
		addSetting(renderSpeed);
		addSetting(antiKick);
		addSetting(antiKickInterval);
		addSetting(antiKickDistance);
	}
	
	@Override
	public String getRenderName()
	{
		if(!renderSpeed.isChecked())
			return getName();

		return getName() + " [" + horizontalSpeed.getValueString() + ", "
			+ verticalSpeed.getValueString() + "]";
	}

	@Override
	protected void onEnable()
	{
		tickCounter = 0;
		
		WURST.getHax().creativeFlightHack.setEnabled(false);
		WURST.getHax().jetpackHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(AirStrafingSpeedListener.class, this);
		EVENTS.add(MouseScrollListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(AirStrafingSpeedListener.class, this);
		EVENTS.remove(MouseScrollListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		player.setDeltaMovement(Vec3.ZERO);
		player.getAbilities().flying = false;
		
		if(WURST.getHax().freecamHack.isMovingCamera())
			return;

		double vSpeed = getActualVerticalSpeed();
		
		if(MC.options.keyJump.isDown())
			player.addDeltaMovement(new Vec3(0, vSpeed, 0));
		
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
		{
			MC.options.keyShift.setDown(false);
			player.addDeltaMovement(new Vec3(0, -vSpeed, 0));
		}
		
		if(antiKick.isChecked())
			doAntiKick();
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		if(WURST.getHax().freecamHack.isMovingCamera())
			return;

		event.setSpeed(horizontalSpeed.getValueF());
	}

	@Override
	public void onMouseScroll(double amount)
	{
		if(!isControllingScrollEvents())
			return;
		
		if(amount > 0)
			horizontalSpeed.increaseValue();
		else if(amount < 0)
			horizontalSpeed.decreaseValue();
	}

	public boolean isControllingScrollEvents()
	{
		return isEnabled() && scrollToChangeSpeed.isChecked()
			&& MC.screen == null
			&& !WURST.getOtfs().zoomOtf.isControllingScrollEvents()
			&& !WURST.getHax().freecamHack.isMovingCamera();
	}
	
	private void doAntiKick()
	{
		if(tickCounter > antiKickInterval.getValueI() + 1)
			tickCounter = 0;
		
		Vec3 velocity = MC.player.getDeltaMovement();

		switch(tickCounter)
		{
			case 0 ->
			{
				if(velocity.y <= -antiKickDistance.getValue())
					tickCounter = 2;
				else
					MC.player.setDeltaMovement(velocity.x,
						-antiKickDistance.getValue(), velocity.z);
			}
			
			case 1 -> MC.player.setDeltaMovement(velocity.x,
				antiKickDistance.getValue(), velocity.z);
		}
		
		tickCounter++;
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}

	public double getHorizontalSpeed()
	{
		return horizontalSpeed.getValue();
	}

	public double getActualVerticalSpeed()
	{
		boolean limitVerticalSpeed = !allowUnsafeVerticalSpeed.isChecked()
			&& !MC.player.getAbilities().invulnerable;

		return Mth.clamp(horizontalSpeed.getValue() * verticalSpeed.getValue(),
			0.05, limitVerticalSpeed ? 3.95 : 10);
	}
}
