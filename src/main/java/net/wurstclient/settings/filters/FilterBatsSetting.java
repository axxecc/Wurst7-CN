/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;

public final class FilterBatsSetting extends EntityFilterCheckbox
{
	public FilterBatsSetting(String description, boolean checked)
	{
		super("过滤蝙蝠", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AmbientCreature);
	}
	
	public static FilterBatsSetting genericCombat(boolean checked)
	{
		return new FilterBatsSetting(
			"不会攻击蝙蝠和任何其他可能由模组添加的\"环境\"生物", checked);
	}
	
	public static FilterBatsSetting genericVision(boolean checked)
	{
		return new FilterBatsSetting(
			"不会显示蝙蝠和任何其他可能由模组添加的\"环境\"生物", checked);
	}
}
