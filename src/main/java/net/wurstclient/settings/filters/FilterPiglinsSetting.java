/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.Piglin;

public final class FilterPiglinsSetting extends AttackDetectingEntityFilter
{
	private static final String EXCEPTIONS_TEXT =
		"\n\n此过滤器不会影响猪灵蛮兵";
	
	private FilterPiglinsSetting(String description, Mode selected,
		boolean checked)
	{
		super("过滤猪灵", description + EXCEPTIONS_TEXT, selected,
			checked);
	}
	
	public FilterPiglinsSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof Piglin);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		return !(e instanceof Piglin pe) || pe.isAggressive();
	}
	
	public static FilterPiglinsSetting genericCombat(Mode selected)
	{
		return new FilterPiglinsSetting("当设置为 \u00a7lOn\u00a7r 时, 猪灵根本不会受到攻击\n\n当设置为 \u00a7l如果平静\u00a7r 时, 猪灵在先攻击之前不会受到攻击. 请注意, 此过滤器无法检测猪灵是否在攻击您或其他人\n\n当设置为 \u00a7lOff\u00a7r 时, 此过滤器不执行任何操作, 猪灵可能会受到攻击", selected);
	}
	
	public static FilterPiglinsSetting genericVision(Mode selected)
	{
		return new FilterPiglinsSetting("当设置为 \u00a7lOn\u00a7r 时, 猪灵根本不会显示\n\n当设置为 \u00a7l如果平静\u00a7r 时, 猪灵在先攻击之前不会显示. 请注意, 此过滤器无法检测猪灵是否在攻击您或其他人\n\n当设置为 \u00a7lOff\u00a7r 时, 此过滤器不执行任何操作, 猪灵可以显示", selected);
	}
	
	public static FilterPiglinsSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterPiglinsSetting(description, null, onByDefault);
	}
}
