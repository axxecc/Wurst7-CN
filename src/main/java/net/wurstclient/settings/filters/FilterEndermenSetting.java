/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;

public final class FilterEndermenSetting extends AttackDetectingEntityFilter
{
	private FilterEndermenSetting(String description, Mode selected,
		boolean checked)
	{
		super("过滤末影人", description, selected, checked);
	}
	
	public FilterEndermenSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof EnderMan);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		return !(e instanceof EnderMan ee) || ee.isAggressive();
	}
	
	public static FilterEndermenSetting genericCombat(Mode selected)
	{
		return new FilterEndermenSetting(
			"当设置为 §l开启§r 时, 末影人根本不会受到攻击\n\n当设置为 §l如果愤怒§r 时, 末影人在先攻击之前不会受到攻击. 请注意. 此过滤器无法检测末影人是否在攻击您或其他人\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何作, 末影人可以被攻击",
			selected);
	}
	
	public static FilterEndermenSetting genericVision(Mode selected)
	{
		return new FilterEndermenSetting(
			"当设置为 §l开启§r 时, 末影人将完全不显示\n\n当设置为 §l如果愤怒§r 时, 末影人不会显示, 直到他们攻击某物\n\n当设置为 §l关闭§r 时, 此过滤器不执行任何作, 并且可以显示末影人",
			selected);
	}
	
	public static FilterEndermenSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterEndermenSetting(description, null, onByDefault);
	}
}
