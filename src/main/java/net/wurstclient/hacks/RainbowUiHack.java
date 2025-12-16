/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"彩虹用户界面", "RainbowGUI"})
public final class RainbowUiHack extends Hack
{
	public RainbowUiHack()
	{
		super("彩虹用户界面");
		setCategory(Category.FUN);
	}
	
	// See ClickGui.updateColors()
}
