/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;

public final class FilterFlyingSetting extends SliderSetting
	implements EntityFilter
{
	public FilterFlyingSetting(String description, double value)
	{
		super("过滤飞行", description, value, 0, 2, 0.05,
			ValueDisplay.DECIMAL.withLabel(0, "关闭"));
	}
	
	@Override
	public boolean test(Entity e)
	{
		if(!(e instanceof Player))
			return true;
		
		AABB box = e.getBoundingBox();
		box = box.minmax(box.move(0, -getValue(), 0));
		return !WurstClient.MC.level.noCollision(box);
	}
	
	@Override
	public boolean isFilterEnabled()
	{
		return getValue() > 0;
	}
	
	@Override
	public Setting getSetting()
	{
		return this;
	}
	
	public static FilterFlyingSetting genericCombat(double value)
	{
		return new FilterFlyingSetting(
			"不会攻击至少离地面给定距离的玩家\n\n对于试图通过在你附近放置飞行机器人来检测你的黑客攻击的服务器很有用", value);
	}
}
