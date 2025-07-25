/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class NoWeatherHack extends Hack
{
	private final CheckboxSetting disableRain =
		new CheckboxSetting("禁用下雨", true);
	
	private final CheckboxSetting changeTime =
		new CheckboxSetting("更改时间", false);
	
	private final SliderSetting time =
		new SliderSetting("时间", 6000, 0, 23900, 100, ValueDisplay.INTEGER);
	
	private final CheckboxSetting changeMoonPhase =
		new CheckboxSetting("更改月相", false);
	
	private final SliderSetting moonPhase =
		new SliderSetting("月相", 0, 0, 7, 1, ValueDisplay.INTEGER);
	
	public NoWeatherHack()
	{
		super("天气覆盖");
		setCategory(Category.RENDER);
		
		addSetting(disableRain);
		addSetting(changeTime);
		addSetting(time);
		addSetting(changeMoonPhase);
		addSetting(moonPhase);
	}
	
	public boolean isRainDisabled()
	{
		return isEnabled() && disableRain.isChecked();
	}
	
	public boolean isTimeChanged()
	{
		return isEnabled() && changeTime.isChecked();
	}
	
	public long getChangedTime()
	{
		return time.getValueI();
	}
	
	public boolean isMoonPhaseChanged()
	{
		return isEnabled() && changeMoonPhase.isChecked();
	}
	
	public int getChangedMoonPhase()
	{
		return moonPhase.getValueI();
	}
}
