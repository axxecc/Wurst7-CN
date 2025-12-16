/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.CreativeModeTabs;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"自闭小屋", "InvMove"})
public final class InvWalkHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allowClickGUI =
		new CheckboxSetting("允许 ClickGUI",
			"允许你在 Wurst 的 ClickGUI 打开时移动", true);
	
	private final CheckboxSetting allowOther =
		new CheckboxSetting("允许其他屏幕",
			"允许你在其他一些游戏内界面打开时移动 (比如箱子、马匹、村民交易界面等), 除非这个界面有文字输入框", true);
	
	private final CheckboxSetting allowSneak =
		new CheckboxSetting("允许潜行键", true);
	
	private final CheckboxSetting allowSprint =
		new CheckboxSetting("允许疾跑键", true);
	
	private final CheckboxSetting allowJump =
		new CheckboxSetting("允许跳跃键", true);
	
	public InvWalkHack()
	{
		super("自闭小屋");
		setCategory(Category.MOVEMENT);
		addSetting(allowClickGUI);
		addSetting(allowOther);
		addSetting(allowSneak);
		addSetting(allowSprint);
		addSetting(allowJump);
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
	}
	
	@Override
	public void onUpdate()
	{
		Screen screen = MC.screen;
		if(screen == null)
			return;
		
		if(!isAllowedScreen(screen))
			return;
		
		ArrayList<KeyMapping> keys =
			new ArrayList<>(Arrays.asList(MC.options.keyUp, MC.options.keyDown,
				MC.options.keyLeft, MC.options.keyRight));
		
		if(allowSneak.isChecked())
			keys.add(MC.options.keyShift);
		
		if(allowSprint.isChecked())
			keys.add(MC.options.keySprint);
		
		if(allowJump.isChecked())
			keys.add(MC.options.keyJump);
		
		for(KeyMapping key : keys)
			IKeyBinding.get(key).resetPressedState();
	}
	
	private boolean isAllowedScreen(Screen screen)
	{
		if((screen instanceof InventoryScreen
			|| screen instanceof CreativeModeInventoryScreen)
			&& !isCreativeSearchBarOpen(screen))
			return true;
		
		if(allowClickGUI.isChecked() && screen instanceof ClickGuiScreen)
			return true;
		
		if(allowOther.isChecked() && screen instanceof AbstractContainerScreen
			&& !hasTextBox(screen))
			return true;
		
		return false;
	}
	
	private boolean isCreativeSearchBarOpen(Screen screen)
	{
		if(!(screen instanceof CreativeModeInventoryScreen))
			return false;
		
		return CreativeModeInventoryScreen.selectedTab == CreativeModeTabs
			.searchTab();
	}
	
	private boolean hasTextBox(Screen screen)
	{
		return screen.children().stream().anyMatch(EditBox.class::isInstance);
	}
}
