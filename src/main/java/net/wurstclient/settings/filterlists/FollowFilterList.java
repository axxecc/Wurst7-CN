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

public final class FollowFilterList extends EntityFilterList
{
	private FollowFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static FollowFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(new FilterPlayersSetting(
			"不会跟随其他玩家", false));
		
		builder.add(new FilterSleepingSetting(
			"不会跟随正在睡觉的玩家", false));
		
		builder.add(new FilterFlyingSetting(
			"不会跟随距离地面至少一定距离的玩家", 0));
		
		builder.add(new FilterHostileSetting(
			"Won't follow hostile mobs like zombies and creepers.", true));
		
		builder.add(FilterNeutralSetting.onOffOnly(
			"不会跟随末影人和狼等中立生物", true));
		
		builder.add(new FilterPassiveSetting(
			"不会跟随猪和牛这样的动物, 不会追踪蝙蝠这样的环境生物, 也不会追踪像鱼、鱿鱼和海豚这样的水生物",
			true));
		
		builder.add(new FilterPassiveWaterSetting(
			"不会跟随被动的水生物, 比如鱼, 鱿鱼, 海豚和美西螈",
			true));
		
		builder.add(new FilterBabiesSetting(
			"不会跟随小猪, 小村民等", true));
		
		builder.add(new FilterBatsSetting(
			"不会跟随蝙蝠和任何其他可能由模组添加的\"环境\"怪物", true));
		
		builder.add(new FilterSlimesSetting("Won't follow slimes.", true));
		
		builder.add(new FilterPetsSetting(
			"不会跟随驯服的狼、驯服的马等", true));
		
		builder.add(new FilterVillagersSetting(
			"不会跟随村民和流浪商人", true));
		
		builder.add(new FilterZombieVillagersSetting(
			"不会跟随僵尸村民", true));
		
		builder.add(new FilterGolemsSetting(
			"不会跟随铁傀儡和雪傀儡", true));
		
		builder
			.add(FilterPiglinsSetting.onOffOnly("不会跟随猪灵", true));
		
		builder.add(FilterZombiePiglinsSetting.onOffOnly(
			"不会跟随僵尸猪灵", true));
		
		builder.add(FilterEndermenSetting.onOffOnly(
			"不会跟随末影人", true));
		
		builder.add(new FilterShulkersSetting(
			"不会跟随潜影贝", true));
		
		builder.add(new FilterAllaysSetting(
			"不会跟随悦灵", true));
		
		builder.add(new FilterInvisibleSetting(
			"不会跟随看不见的实体", false));
		
		builder.add(new FilterArmorStandsSetting(
			"不会跟随盔甲架", true));
		
		builder.add(new FilterMinecartsSetting(
			"不会跟随矿车", true));
		
		return new FollowFilterList(builder);
	}
}
