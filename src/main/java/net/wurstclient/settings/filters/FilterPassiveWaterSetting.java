/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

public final class FilterPassiveWaterSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT =
		"\n\n此过滤器不会影响守卫者、溺尸和河豚";
	
	public FilterPassiveWaterSetting(String description, boolean checked)
	{
		super("过水生生物", description + EXCEPTIONS_TEXT,
			checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		// never filter out pufferfish
		if(e instanceof Pufferfish)
			return true;
		
		return !(e instanceof WaterAnimal || e instanceof AgeableWaterCreature
			|| e instanceof Axolotl);
	}
	
	public static FilterPassiveWaterSetting genericCombat(boolean checked)
	{
		return new FilterPassiveWaterSetting("不会攻击被动的水生物, 如鱼、鱿鱼、海豚和美西螈", checked);
	}
	
	public static FilterPassiveWaterSetting genericVision(boolean checked)
	{
		return new FilterPassiveWaterSetting("不会显示被动的水生物, 如鱼、鱿鱼、海豚和美西螈", checked);
	}
}
