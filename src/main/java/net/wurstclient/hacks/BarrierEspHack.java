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

@SearchTags({"barrier esp"})
public class BarrierEspHack extends Hack
{
	public BarrierEspHack()
	{
		super("屏障高亮");
		setCategory(Category.RENDER);
	}
	
	// See ClientWorldMixin
}
