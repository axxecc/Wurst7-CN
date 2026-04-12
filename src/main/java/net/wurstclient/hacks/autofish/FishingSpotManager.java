/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

public final class FishingSpotManager
{
	private static final Minecraft MC = WurstClient.MC;
	
	private final CheckboxSetting mcmmoMode = new CheckboxSetting("mcMMO 模式",
		"如果启用了自动钓鱼, 会自动在两个不同的钓鱼点之间切换, 以绕过mcMMO的过度捕捞机制\n\n关闭后, 所有其他mcMMO设置都不会有任何作用",
		false);
	
	private final SliderSetting mcmmoRange = new SliderSetting("mcMMO 范围",
		"mcMMO 的移动范围配置选项, 这是避免过度捕捞所需的两个钓鱼点之间的最小距离\n\nmcMMO只关心鱼饵的位置, 所以除非有其他反挂机插件, 否则你不需要移动角色",
		3, 1, 50, 1, ValueDisplay.INTEGER.withSuffix(" 方块"));
	
	private final CheckboxSetting mcmmoRangeBug =
		new CheckboxSetting("mcMMO 范围修复",
			"在撰写本文时, mcMMO的射程计算存在一个bug, 默认的3格射程实际上只有2格\n\n如果他们修复了这个选项, 请取消勾选",
			true);
	
	private final SliderSetting mcmmoLimit = new SliderSetting("mcMMO 限制",
		"mcMMO 的超额钓鱼配置选项\n\n过度捕捞从这个数值开始, 所以你实际上只能 (限制-1) 在同一地点钓到鱼", 10, 2, 1000,
		1, ValueDisplay.INTEGER);
	
	private final ArrayList<FishingSpot> fishingSpots = new ArrayList<>();
	private FishingSpot lastSpot;
	private FishingSpot nextSpot;
	private PositionAndRotation castPosRot;
	private int fishCaughtAtLastSpot;
	private boolean spot1MsgShown;
	private boolean spot2MsgShown;
	private boolean setupDoneMsgShown;
	
	/**
	 * Changes the player's fishing spot if necessary.
	 *
	 * @return true if it's OK to cast the fishing rod
	 */
	public boolean onCast()
	{
		castPosRot = new PositionAndRotation(MC.player);
		if(!mcmmoMode.isChecked())
			return true;
		
		// allow first cast, tell user to wait
		if(lastSpot == null)
		{
			if(spot1MsgShown)
				return true;
			
			ChatUtils.message("启动自动钓鱼的mcMMO模式");
			ChatUtils.message("请稍候, 等待第一个钓鱼点的开始");
			spot1MsgShown = true;
			return true;
		}
		spot1MsgShown = false;
		
		// set next spot if necessary, instruct user if new spot is needed
		if(nextSpot == null && (nextSpot = chooseNextSpot()) == null)
		{
			if(spot2MsgShown)
				return false;
			
			ChatUtils.message("自动钓鱼mcMMO模式需要另一个钓鱼点");
			ChatUtils.message("移动你的摄像机 (或必要时移动玩家), 使浮标落在红盒子外, 然后抛竿");
			spot2MsgShown = true;
			setupDoneMsgShown = false;
			return false;
		}
		spot2MsgShown = false;
		
		// confirm setup is done
		if(!setupDoneMsgShown)
		{
			ChatUtils.message("全部完成! 自动钓鱼现在会自动运行, 并根据需要切换钓鱼点");
			setupDoneMsgShown = true;
		}
		
		// automatically move to next spot when limit is reached
		if(fishCaughtAtLastSpot >= mcmmoLimit.getValueI() - 1)
		{
			moveToNextSpot();
			return false;
		}
		
		return true;
	}
	
	private void moveToNextSpot()
	{
		IKeyMapping forwardKey = IKeyMapping.get(MC.options.keyUp);
		IKeyMapping jumpKey = IKeyMapping.get(MC.options.keyJump);
		
		PositionAndRotation nextPosRot = nextSpot.input();
		forwardKey.resetPressedState();
		jumpKey.resetPressedState();
		
		// match position
		Vec3 nextPos = nextPosRot.pos();
		double distance = nextPos.distanceTo(castPosRot.pos());
		if(distance > 0.1)
		{
			// face next spot
			Rotation needed =
				RotationUtils.getNeededRotations(nextPos).withPitch(0);
			if(!RotationUtils.isAlreadyFacing(needed))
			{
				RotationUtils.slowlyTurnTowards(needed, 5)
					.applyToClientPlayer();
				return;
			}
			
			// jump if necessary
			jumpKey.setDown(
				MC.player.isInWater() || MC.player.horizontalCollision);
			
			// walk or teleport depending on distance
			if(distance < 0.2)
				MC.player.setPos(nextPos.x, nextPos.y, nextPos.z);
			else if(distance > 0.7 || MC.player.tickCount % 10 == 0)
				forwardKey.setDown(true);
			return;
		}
		
		// match rotation
		Rotation nextRot = nextPosRot.rotation();
		if(!RotationUtils.isAlreadyFacing(nextRot))
		{
			RotationUtils.slowlyTurnTowards(nextRot, 5).applyToClientPlayer();
			return;
		}
		
		// update spot and reset counter
		lastSpot = nextSpot;
		nextSpot = null;
		fishCaughtAtLastSpot = 0;
	}
	
	public void onBite(FishingHook bobber)
	{
		boolean samePlayerInput = lastSpot != null
			&& lastSpot.input().isNearlyIdenticalTo(castPosRot);
		boolean sameBobberPos = lastSpot != null
			&& isInRange(lastSpot.bobberPos(), bobber.position());
		
		// update counter based on bobber position
		if(sameBobberPos)
			fishCaughtAtLastSpot++;
		else
			fishCaughtAtLastSpot = 1;
		
		// register new fishing spot if input changed
		if(!samePlayerInput)
		{
			lastSpot = new FishingSpot(castPosRot, bobber);
			fishingSpots.add(lastSpot);
			return;
		}
		
		// update last spot if same input led to different bobber position
		if(!sameBobberPos)
		{
			FishingSpot updatedSpot = new FishingSpot(lastSpot.input(), bobber);
			fishingSpots.remove(lastSpot);
			fishingSpots.add(updatedSpot);
			lastSpot = updatedSpot;
		}
	}
	
	public void reset()
	{
		fishingSpots.clear();
		lastSpot = null;
		nextSpot = null;
		castPosRot = null;
		fishCaughtAtLastSpot = 0;
		spot1MsgShown = false;
		spot2MsgShown = false;
		setupDoneMsgShown = false;
	}
	
	private FishingSpot chooseNextSpot()
	{
		return fishingSpots.stream().filter(spot -> spot != lastSpot)
			.filter(spot -> !isInRange(spot.bobberPos(), lastSpot.bobberPos()))
			.min(Comparator.comparingDouble(
				spot -> spot.input().differenceTo(lastSpot.input())))
			.orElse(null);
	}
	
	private boolean isInRange(Vec3 pos1, Vec3 pos2)
	{
		double dy = Math.abs(pos1.y - pos2.y);
		if(dy > 2)
			return false;
		
		double dx = Math.abs(pos1.x - pos2.x);
		double dz = Math.abs(pos1.z - pos2.z);
		return Math.max(dx, dz) <= getRange();
	}
	
	public int getRange()
	{
		// rounded down to the nearest even number
		if(mcmmoRangeBug.isChecked())
			return mcmmoRange.getValueI() / 2 * 2;
		
		return mcmmoRange.getValueI();
	}
	
	public FishingSpot getLastSpot()
	{
		return lastSpot;
	}
	
	public boolean isSetupDone()
	{
		return lastSpot != null && nextSpot != null;
	}
	
	public boolean isMcmmoMode()
	{
		return mcmmoMode.isChecked();
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(mcmmoMode, mcmmoRange, mcmmoRangeBug, mcmmoLimit);
	}
	
	public List<FishingSpot> getFishingSpots()
	{
		return Collections.unmodifiableList(fishingSpots);
	}
}
