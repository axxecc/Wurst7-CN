/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

public final class FilterSleepingSetting extends EntityFilterCheckbox
{
	public FilterSleepingSetting(String description, boolean checked)
	{
		super("过滤睡眠", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		if(!(e instanceof PlayerEntity pe))
			return true;
		
		return !pe.isSleeping() && pe.getPose() != EntityPose.SLEEPING;
	}
	
	public static FilterSleepingSetting genericCombat(boolean checked)
	{
		return new FilterSleepingSetting(
			"description.wurst.setting.generic.filter_sleeping_combat",
			checked);
	}
	
	public static FilterSleepingSetting genericVision(boolean checked)
	{
		return new FilterSleepingSetting(
			"description.wurst.setting.generic.filter_sleeping_vision",
			checked);
	}
}
