/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Random;

import net.minecraft.entity.player.PlayerModelPart;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"SpookySkin", "spooky skin", "SkinBlinker", "skin blinker"})
public final class SkinDerpHack extends Hack implements UpdateListener
{
	private final Random random = new Random();
	
	public SkinDerpHack()
	{
		super("随机切换皮肤");
		setCategory(Category.FUN);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		for(PlayerModelPart part : PlayerModelPart.values())
			MC.options.setPlayerModelPart(part, true);
	}
	
	@Override
	public void onUpdate()
	{
		if(random.nextInt(4) != 0)
			return;
		
		for(PlayerModelPart part : PlayerModelPart.values())
			MC.options.setPlayerModelPart(part,
				!MC.options.isPlayerModelPartEnabled(part));
	}
}
