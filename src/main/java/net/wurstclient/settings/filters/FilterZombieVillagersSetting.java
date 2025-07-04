/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieVillagerEntity;

public final class FilterZombieVillagersSetting extends EntityFilterCheckbox
{
	public FilterZombieVillagersSetting(String description, boolean checked)
	{
		super("过滤僵尸村民", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ZombieVillagerEntity);
	}
	
	public static FilterZombieVillagersSetting genericCombat(boolean checked)
	{
		return new FilterZombieVillagersSetting(
			"description.wurst.setting.generic.filter_zombie_villagers_combat",
			checked);
	}
	
	public static FilterZombieVillagersSetting genericVision(boolean checked)
	{
		return new FilterZombieVillagersSetting(
			"description.wurst.setting.generic.filter_zombie_villagers_vision",
			checked);
	}
}
