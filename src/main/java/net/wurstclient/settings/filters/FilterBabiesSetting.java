/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TadpoleEntity;

public final class FilterBabiesSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT = "\n\n此过滤器不会影响幼年僵尸和其他敌对幼年生物";
	
	public FilterBabiesSetting(String description, boolean checked)
	{
		super("过滤幼年", description + EXCEPTIONS_TEXT, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		// never filter out hostile mobs (including hoglins)
		if(e instanceof Monster)
			return true;
		
		// filter out passive entity babies
		if(e instanceof PassiveEntity pe && pe.isBaby())
			return false;
		
		// filter out tadpoles
		if(e instanceof TadpoleEntity)
			return false;
		
		return true;
	}
	
	public static FilterBabiesSetting genericCombat(boolean checked)
	{
		return new FilterBabiesSetting(
			"不会攻击幼年猪、幼年村民等", checked);
	}
}
