/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filterlists;

import java.util.ArrayList;
import java.util.List;

import net.wurstclient.settings.filters.*;

public final class RemoteViewFilterList extends EntityFilterList
{
	private RemoteViewFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static RemoteViewFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(new FilterPlayersSetting(
			"不会看到其他玩家", false));
		
		builder.add(new FilterSleepingSetting(
			"不会看到正在睡觉的玩家", false));
		
		builder.add(new FilterFlyingSetting(
			"不会看到距离地面至少给定距离的玩家", 0));
		
		builder.add(new FilterHostileSetting(
			"不会把敌对怪物当成僵尸和苦力怕", true));
		
		builder.add(FilterNeutralSetting.onOffOnly(
			"不会看到末影人和狼等中立生物", true));
		
		builder.add(new FilterPassiveSetting("不会看到猪和牛这样的动物, 不会看到蝙蝠这样的环境生物, 也不会看到像鱼, 鱿鱼和海豚这样的水生物", true));
		
		builder.add(new FilterPassiveWaterSetting("不会看到被动的水生物, 比如鱼, 鱿鱼, 海豚和美西螈", true));
		
		builder.add(new FilterBabiesSetting(
			"不会看到小猪, 小村民等", true));
		
		builder.add(new FilterBatsSetting(
			"不会看到蝙蝠和任何其他可能由模组添加的\"环境\"生物", true));
		
		builder.add(new FilterSlimesSetting("不会看到史莱姆", true));
		
		builder.add(new FilterPetsSetting(
			"不会看到驯服的狼、驯服的马等", true));
		
		builder.add(new FilterVillagersSetting(
			"不会看到村民和流浪商人", true));
		
		builder.add(new FilterZombieVillagersSetting(
			"不会看到僵尸村民",
			true));
		
		builder.add(new FilterGolemsSetting(
			"不会看到铁傀儡和雪傀儡", true));
		
		builder
			.add(FilterPiglinsSetting.onOffOnly("不会看猪灵", true));
		
		builder.add(FilterZombiePiglinsSetting.onOffOnly(
			"不会看到僵尸猪灵",
			true));
		
		builder.add(FilterEndermenSetting.onOffOnly(
			"不会看到末影人", true));
		
		builder.add(new FilterShulkersSetting(
			"不会看到潜影贝", true));
		
		builder.add(new FilterAllaysSetting(
			"不会看到悦灵", true));
		
		builder.add(new FilterInvisibleSetting(
			"不会看到不可见的实体", false));
		
		builder.add(new FilterArmorStandsSetting(
			"不会看到盔甲架", true));
		
		return new RemoteViewFilterList(builder);
	}
}
