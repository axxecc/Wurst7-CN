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

public final class CrystalAuraFilterList extends EntityFilterList
{
	private CrystalAuraFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static CrystalAuraFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		String damageWarning =
			"\n\n如果他们靠近有效目标或现有水晶, 仍然会受到伤害";
		
		builder.add(new FilterPlayersSetting(
			"水晶光环不会锁定其他玩家"
				+ damageWarning,
			false));
		
		builder.add(new FilterHostileSetting("水晶光环不会锁定敌对怪物, 比如僵尸和苦力怕"
			+ damageWarning, true));
		
		builder.add(new FilterNeutralSetting("水晶光环不会锁定中立怪物, 比如末影人和狼"
			+ damageWarning, AttackDetectingEntityFilter.Mode.ON));
		
		builder.add(new FilterPassiveSetting("水晶光环, 不会锁定猪和牛等动物, 蝙蝠等环境生物, 以及鱼, 鱿鱼和海豚等水生物" + damageWarning,
			true));
		
		builder.add(new FilterPassiveWaterSetting("水晶光环不会锁定被动水生物, 比如鱼, 鱿鱼, 海豚和美西螈" + damageWarning, true));
		
		builder.add(new FilterBatsSetting("水晶光环不会锁定蝙蝠和其他\"环境\"怪物" + damageWarning,
			true));
		
		builder.add(new FilterSlimesSetting("水晶光环不会锁定史莱姆" + damageWarning, true));
		
		builder.add(new FilterVillagersSetting("水晶光环不会锁定村民和游荡商人" + damageWarning,
			true));
		
		builder.add(new FilterZombieVillagersSetting("水晶光环不会锁定僵尸村民" + damageWarning, true));
		
		builder.add(new FilterGolemsSetting("水晶光环不会锁定铁傀儡和雪傀儡" + damageWarning, true));
		
		builder.add(new FilterPiglinsSetting("水晶光环不会锁定猪灵" + damageWarning,
			AttackDetectingEntityFilter.Mode.ON));
		
		builder.add(new FilterZombiePiglinsSetting("水晶光环不会锁定僵尸化的猪灵" + damageWarning,
			AttackDetectingEntityFilter.Mode.ON));
		
		builder.add(new FilterShulkersSetting("水晶光环不会锁定潜影贝" + damageWarning, true));
		
		builder.add(new FilterAllaysSetting(
			"水晶光环不会锁定悦灵" + damageWarning,
			true));
		
		builder.add(new FilterInvisibleSetting(
			"水晶光环不会锁定隐形实体"
				+ damageWarning,
			false));
		
		builder.add(new FilterNamedSetting(
			"水晶光环不会锁定带有名称标签的实体"
				+ damageWarning,
			false));
		
		builder.add(new FilterArmorStandsSetting(
			"水晶光环不会锁定护甲架"
				+ damageWarning,
			true));
		
		return new CrystalAuraFilterList(builder);
	}
}
