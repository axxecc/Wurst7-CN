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

@SearchTags({"infini chat", "InfiniteChat", "infinite chat"})
public final class InfiniChatHack extends Hack
{
	public InfiniChatHack()
	{
		super("移除聊天限制");
		setCategory(Category.CHAT);
	}
}
