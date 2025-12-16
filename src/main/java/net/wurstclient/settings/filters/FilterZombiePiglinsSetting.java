/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;

public final class FilterZombiePiglinsSetting
	extends AttackDetectingEntityFilter
{
	private FilterZombiePiglinsSetting(String description, Mode selected,
		boolean checked)
	{
		super("过滤僵尸猪灵", description, selected, checked);
	}
	
	public FilterZombiePiglinsSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof ZombifiedPiglin);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		return !(e instanceof ZombifiedPiglin zpe) || zpe.isAggressive();
	}
	
	public static FilterZombiePiglinsSetting genericCombat(Mode selected)
	{
		return new FilterZombiePiglinsSetting(
			"当设置为 §l开启§r 时, 僵尸猪灵根本不会受到攻击\n\n当设置为 §l如果愤怒§r 时, 僵尸猪灵在先攻击之前不会受到攻击. 请注意, 此过滤器无法检测僵尸猪灵是否正在攻击您或其他人\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何操作, 僵尸猪灵可能会受到攻击",
			selected);
	}
	
	public static FilterZombiePiglinsSetting genericVision(Mode selected)
	{
		return new FilterZombiePiglinsSetting(
			"当设置为 §l开启§r 时, 僵尸猪灵将完全不显示\n\n当设置为 §l如果愤怒§r 时, 僵尸猪灵不会显示, 直到它们攻击某物\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何作, 僵尸猪灵可以显示",
			selected);
	}
	
	public static FilterZombiePiglinsSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterZombiePiglinsSetting(description, null, onByDefault);
	}
}
