/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.analytics.PlausibleAnalytics;
import net.wurstclient.commands.FriendsCmd;
import net.wurstclient.hacks.XRayHack;
import net.wurstclient.other_features.VanillaSpoofOtf;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.WurstColors;

public class WurstOptionsScreen extends Screen
{
	private Screen prevScreen;
	
	public WurstOptionsScreen(Screen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addRenderableWidget(Button
			.builder(Component.literal("返回"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height / 4 + 144 - 16, 200, 20).build());
		
		addSettingButtons();
		addManagerButtons();
		addLinkButtons();
	}
	
	private void addSettingButtons()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		FriendsCmd friendsCmd = wurst.getCmds().friendsCmd;
		CheckboxSetting middleClickFriends = friendsCmd.getMiddleClickFriends();
		PlausibleAnalytics plausible = wurst.getPlausible();
		VanillaSpoofOtf vanillaSpoofOtf = wurst.getOtfs().vanillaSpoofOtf;
		CheckboxSetting forceEnglish =
			wurst.getOtfs().translationsOtf.getForceEnglish();
		
		new WurstOptionsButton(-154, 24,
			() -> "点击好友："
				+ (middleClickFriends.isChecked() ? "开启" : "关闭"),
			middleClickFriends.getWrappedDescription(200),
			b -> middleClickFriends
				.setChecked(!middleClickFriends.isChecked()));
		
		new WurstOptionsButton(-154, 48,
			() -> "统用户统计：" + (plausible.isEnabled() ? "开启" : "关闭"),
			"统计有多少用户使用 Wurst",
			b -> plausible.setEnabled(!plausible.isEnabled()));
		
		new WurstOptionsButton(-154, 72,
			() -> "客户端欺骗："
				+ (vanillaSpoofOtf.isEnabled() ? "开启" : "关闭"),
			vanillaSpoofOtf.getDescription(),
			b -> vanillaSpoofOtf.doPrimaryAction());
		
		new WurstOptionsButton(-154, 96,
			() -> "翻译：" + (!forceEnglish.isChecked() ? "开启" : "关闭"),
			"使用客户端语言，但我只做了中文汉化\n所以这个选项已经没有意义了",
			b -> forceEnglish.setChecked(!forceEnglish.isChecked()));
	}
	
	private void addManagerButtons()
	{
		XRayHack xRayHack = WurstClient.INSTANCE.getHax().xRayHack;
		
		new WurstOptionsButton(-50, 24, () -> "快捷键",
			"快捷键允许您通过按键来切换功能",
			b -> minecraft.setScreen(new KeybindManagerScreen(this)));
		
		new WurstOptionsButton(-50, 48, () -> "X-Ray 方块",
			"管理 X-Ray 透视的方块",
			b -> xRayHack.openBlockListEditor(this));
		
		new WurstOptionsButton(-50, 72, () -> "缩放",
			"缩放管理器允许您更改缩放键及其放大距离",
			b -> client.setScreen(new ZoomManagerScreen(this)));
	}
	
	private void addLinkButtons()
	{
		OS os = Util.getPlatform();
		
		new WurstOptionsButton(54, 24, () -> "官方网站",
			"§n§lWurstClient.net",
			b -> os.openUri("https://www.wurstclient.net/options-website/"));
		
		new WurstOptionsButton(54, 48, () -> "Wurst Wiki", "§n§lWurst.Wiki",
			b -> os.openUri("https://www.wurstclient.net/options-wiki/"));
		
		new WurstOptionsButton(54, 72, () -> "WurstForum", "§n§lWurstForum.net",
			b -> os.openUri("https://www.wurstclient.net/options-forum/"));
		
		new WurstOptionsButton(54, 96, () -> "Twitter", "@Wurst_Imperium",
			b -> os.openUri("https://www.wurstclient.net/options-twitter/"));
		
		new WurstOptionsButton(54, 120, () -> "汉化地址",
            "§n§lGitHub",
			b -> os.openUri("https://github.com/axxecc/Wurst7-CN/"));
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderTitles(context);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
	}
	
	private void renderTitles(GuiGraphics context)
	{
		Font tr = minecraft.font;
		int middleX = width / 2;
		int y1 = 40;
		int y2 = height / 4 + 24 - 28;
		
		context.drawCenteredString(tr, "Wurst 选项", middleX, y1,
			CommonColors.WHITE);
		
		context.drawCenteredString(tr, "设置", middleX - 104, y2,
			WurstColors.VERY_LIGHT_GRAY);
		context.drawCenteredString(tr, "管理", middleX, y2,
			WurstColors.VERY_LIGHT_GRAY);
		context.drawCenteredString(tr, "链接", middleX + 104, y2,
			WurstColors.VERY_LIGHT_GRAY);
	}
	
	private void renderButtonTooltip(GuiGraphics context, int mouseX,
		int mouseY)
	{
		for(AbstractWidget button : Screens.getButtons(this))
		{
			if(!button.isHoveredOrFocused()
				|| !(button instanceof WurstOptionsButton))
				continue;
			
			WurstOptionsButton woButton = (WurstOptionsButton)button;
			
			if(woButton.tooltip.isEmpty())
				continue;
			
			context.setComponentTooltipForNextFrame(font, woButton.tooltip,
				mouseX, mouseY);
			break;
		}
	}
	
	private final class WurstOptionsButton extends Button
	{
		private final Supplier<String> messageSupplier;
		private final List<Component> tooltip;
		
		public WurstOptionsButton(int xOffset, int yOffset,
			Supplier<String> messageSupplier, String tooltip,
			OnPress pressAction)
		{
			super(WurstOptionsScreen.this.width / 2 + xOffset,
				WurstOptionsScreen.this.height / 4 - 16 + yOffset, 100, 20,
				Component.literal(messageSupplier.get()), pressAction,
				Button.DEFAULT_NARRATION);
			
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = ChatUtils.wrapText(tooltip, 200).split("\n");
				
				Component[] lines2 = new Component[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Component.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
			
			addRenderableWidget(this);
		}
		
		@Override
		public void onPress(InputWithModifiers context)
		{
			super.onPress(context);
			setMessage(Component.literal(messageSupplier.get()));
		}
	}
}
