/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;

public final class FilterPassiveSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT = "\n\n此过滤器 不会影响狼、蜜蜂、北极熊、河豚和村民";
	
	public FilterPassiveSetting(String description, boolean checked)
	{
		super("过滤被动生物", description + EXCEPTIONS_TEXT, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		// never filter out hostile mobs (including hoglins)
		if(e instanceof Enemy)
			return true;
		
		// never filter out neutral mobs (including pufferfish)
		if(e instanceof NeutralMob || e instanceof Pufferfish)
			return true;
		
		return !(e instanceof Animal || e instanceof AmbientCreature
			|| e instanceof WaterAnimal || e instanceof AgeableWaterCreature);
	}
	
	public static FilterPassiveSetting genericCombat(boolean checked)
	{
		return new FilterPassiveSetting("不会攻击猪和牛等动物，蝙蝠等环境生物，以及鱼、鱿鱼和海豚等水生生物", checked);
	}
	
	public static FilterPassiveSetting genericVision(boolean checked)
	{
		return new FilterPassiveSetting("不会显示猪和牛等动物，蝙蝠等环境生物，以及鱼、鱿鱼和海豚等生水生物", checked);
	}
}
