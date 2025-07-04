/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;

public final class FilterCrystalsSetting extends EntityFilterCheckbox
{
	public FilterCrystalsSetting(String description, boolean checked)
	{
		super("过滤末影水晶", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof EndCrystalEntity);
	}
	
	public static FilterCrystalsSetting genericCombat(boolean checked)
	{
		return new FilterCrystalsSetting(
			"description.wurst.setting.generic.filter_crystals_combat",
			checked);
	}
}
