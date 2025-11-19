/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.nukers;

import net.wurstclient.settings.EnumSetting;

public final class NukerShapeSetting
	extends EnumSetting<NukerShapeSetting.NukerShape>
{
	public NukerShapeSetting()
	{
		super("形状",
			"\u00a7l注意: \u00a7r如果您的范围设置得太高, 立方体形状将开始看起来像一个球体, 因为您无法到达角落。范围 1-3 最适合立方体形状",
			NukerShape.values(), NukerShape.SPHERE);
	}
	
	public enum NukerShape
	{
		SPHERE("球形"),
		CUBE("方形");
		
		private final String name;
		
		private NukerShape(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
