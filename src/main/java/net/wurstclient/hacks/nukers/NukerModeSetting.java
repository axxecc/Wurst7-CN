/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.nukers;

import net.wurstclient.settings.EnumSetting;

public final class NukerModeSetting
	extends EnumSetting<NukerModeSetting.NukerMode>
{
	public NukerModeSetting()
	{
		super("模式",
			"\u00a7l正常\u00a7r模式只会破坏你周围的一切\n\n\u00a7lID\u00a7r模式只会破坏选定的方块类型, 左键点击一个方块将其选中\n\n\u00a7l多个方块\u00a7r模式只会破坏多个方块列表中的方块类型\n\n§l\u00a7l破坏\u00a7r模式只会破坏可以立即摧毁的方块 (例如高草) ",
			NukerMode.values(), NukerMode.NORMAL);
	}

	public enum NukerMode
	{
		NORMAL("正常"),
		ID("ID"),
		MULTI_ID("多个方块"),
		SMASH("破坏");

		private final String name;

		private NukerMode(String name)
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
