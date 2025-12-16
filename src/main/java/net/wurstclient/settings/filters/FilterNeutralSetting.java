/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.monster.piglin.Piglin;

public final class FilterNeutralSetting extends AttackDetectingEntityFilter
{
	private FilterNeutralSetting(String description, Mode selected,
		boolean checked)
	{
		super("过滤中立生物", description, selected, checked);
	}
	
	public FilterNeutralSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof NeutralMob || e instanceof Pufferfish
			|| e instanceof Piglin);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		// special case for pufferfish
		if(e instanceof Pufferfish pfe)
			return pfe.getPuffState() > 0;
		
		if(e instanceof NeutralMob || e instanceof Piglin)
			if(e instanceof Mob me)
				return me.isAggressive();
			
		return true;
	}
	
	public static FilterNeutralSetting genericCombat(Mode selected)
	{
		return new FilterNeutralSetting(
			"当设置为 §l开启§r 时, 中立生物根本不会受到攻击\n\n当设置为 §l如果愤怒§r 时, 中立生物在先攻击之前不会受到攻击. 请注意, 此过滤器无法检测中立生物是否在攻击您或其他人\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何操作, 中立生物可以被攻击",
			selected);
	}
	
	public static FilterNeutralSetting genericVision(Mode selected)
	{
		return new FilterNeutralSetting(
			"当设置为 §l开启§r 时, 中立生物将完全不显示\n\n当设置为 §l如果愤怒§r 时, 中立生物在攻击某物之前不会显示\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何操作, 并且可以显示中立生物",
			selected);
	}
	
	public static FilterNeutralSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterNeutralSetting(description, null, onByDefault);
	}
}
