/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.GolemEntity;

public final class FilterGolemsSetting extends EntityFilterCheckbox
{
	public FilterGolemsSetting(String description, boolean checked)
	{
		super("过滤傀儡", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof GolemEntity) || e instanceof ShulkerEntity;
	}
	
	public static FilterGolemsSetting genericCombat(boolean checked)
	{
		return new FilterGolemsSetting(
			"description.wurst.setting.generic.filter_golems_combat", checked);
	}
	
	public static FilterGolemsSetting genericVision(boolean checked)
	{
		return new FilterGolemsSetting(
			"description.wurst.setting.generic.filter_golems_vision", checked);
	}
}
