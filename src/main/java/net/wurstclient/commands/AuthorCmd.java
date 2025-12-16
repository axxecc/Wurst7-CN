/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

public final class AuthorCmd extends Command
{
	public AuthorCmd()
	{
		super("author", "改变书面书籍的作者\n"
			+ "需要创造模式", ".author <作者>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			throw new CmdSyntaxError();
		
		if(!MC.player.getAbilities().instabuild)
			throw new CmdError("仅限创造模式");
		
		ItemStack heldStack = MC.player.getInventory().getSelectedItem();
		if(!heldStack.is(Items.WRITTEN_BOOK))
			throw new CmdError(
				"你必须用主手拿着一本书");
		
		WrittenBookContent oldData =
			heldStack.getComponents().get(DataComponents.WRITTEN_BOOK_CONTENT);
		if(oldData == null)
			throw new CmdError("找不到书籍数据");
		
		String author = String.join(" ", args);
		WrittenBookContent newData = new WrittenBookContent(oldData.title(),
			author, oldData.generation(), oldData.pages(), oldData.resolved());
		heldStack.set(DataComponents.WRITTEN_BOOK_CONTENT, newData);
	}
}
