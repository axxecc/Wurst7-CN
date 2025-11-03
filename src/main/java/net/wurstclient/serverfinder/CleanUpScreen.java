/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.serverfinder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class CleanUpScreen extends Screen
{
	private MultiplayerScreen prevScreen;
	private ButtonWidget cleanUpButton;
	
	private boolean removeAll;
	private boolean cleanupFailed = true;
	private boolean cleanupOutdated = true;
	private boolean cleanupRename = true;
	private boolean cleanupUnknown = true;
	private boolean cleanupGriefMe;
	
	public CleanUpScreen(MultiplayerScreen prevScreen)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addDrawableChild(new CleanUpButton(width / 2 - 100,
			height / 4 + 168 + 12, () -> "取消", "", b -> close()));
		
		addDrawableChild(cleanUpButton = new CleanUpButton(width / 2 - 100,
			height / 4 + 144 + 12, () -> "清理",
			"使用上面指定的设置开始清理\n游戏可能看起来在几秒钟内没有响应",
			b -> cleanUp()));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 - 24 + 12,
				() -> "未知主机：" + removeOrKeep(cleanupUnknown),
				"不存在的服务器",
				b -> cleanupUnknown = !cleanupUnknown));
		
		addDrawableChild(new CleanUpButton(width / 2 - 100, height / 4 + 0 + 12,
			() -> "过时的服务器：" + removeOrKeep(cleanupOutdated),
			"运行与您不同的 Minecraft 版本的服务器",
			b -> cleanupOutdated = !cleanupOutdated));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 24 + 12,
				() -> "Ping 失败：" + removeOrKeep(cleanupFailed),
				"上次 ping 失败的所有服务器\n在执行此作之前，请确保最后一次 ping 已完成\n这意味着：返回，按下刷新按钮并等待所有服务器完成刷新",
				b -> cleanupFailed = !cleanupFailed));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 48 + 12,
				() -> "\"Grief me\"服务器：" + removeOrKeep(cleanupGriefMe),
				"用于删除所有名称以\"Grief me\"开头查找器找到的服务器",
				b -> cleanupGriefMe = !cleanupGriefMe));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 72 + 12,
				() -> "\u00a7c删除所有服务器：" + yesOrNo(removeAll),
				"这将彻底清除您的服务器列表\n\u00a7c请谨慎使用！",
				b -> removeAll = !removeAll));
		
		addDrawableChild(
			new CleanUpButton(width / 2 - 100, height / 4 + 96 + 12,
				() -> "重命名所有服务器：" + yesOrNo(cleanupRename),
				"将您的服务器重命名为\n\"Grief me #1\"\"Grief me #2\" 等等",
				b -> cleanupRename = !cleanupRename));
	}
	
	private String yesOrNo(boolean b)
	{
		return b ? "是" : "否";
	}
	
	private String removeOrKeep(boolean b)
	{
		return b ? "移除" : "保留";
	}
	
	private void cleanUp()
	{
		for(int i = prevScreen.getServerList().size() - 1; i >= 0; i--)
		{
			ServerInfo server = prevScreen.getServerList().get(i);
			
			if(removeAll || shouldRemove(server))
				prevScreen.getServerList().remove(server);
		}
		
		if(cleanupRename)
			for(int i = 0; i < prevScreen.getServerList().size(); i++)
			{
				ServerInfo server = prevScreen.getServerList().get(i);
				server.name = "Grief me #" + (i + 1);
			}
		
		saveServerList();
		client.setScreen(prevScreen);
	}
	
	private boolean shouldRemove(ServerInfo server)
	{
		if(server == null)
			return false;
		
		if(cleanupUnknown && isUnknownHost(server))
			return true;
		
		if(cleanupOutdated && !isSameProtocol(server))
			return true;
		
		if(cleanupFailed && isFailedPing(server))
			return true;
		
		if(cleanupGriefMe && isGriefMeServer(server))
			return true;
		
		return false;
	}
	
	private boolean isUnknownHost(ServerInfo server)
	{
		if(server.label == null)
			return false;
		
		if(server.label.getString() == null)
			return false;
		
		return server.label.getString()
			.equals("\u00a74无法解析主机名！");
	}
	
	private boolean isSameProtocol(ServerInfo server)
	{
		return server.protocolVersion == SharedConstants.getGameVersion()
			.protocolVersion();
	}
	
	private boolean isFailedPing(ServerInfo server)
	{
		return server.ping != -2L && server.ping < 0L;
	}
	
	private boolean isGriefMeServer(ServerInfo server)
	{
		return server.name != null && server.name.startsWith("Grief me");
	}
	
	private void saveServerList()
	{
		prevScreen.getServerList().saveFile();
		
		MultiplayerServerListWidget listWidget = prevScreen.serverListWidget;
		listWidget.setSelected(null);
		listWidget.setServers(prevScreen.getServerList());
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
	{
		if(context.key() == GLFW.GLFW_KEY_ENTER)
			cleanUpButton.onPress(context);
		
		return super.keyPressed(context);
	}
	
	@Override
	public boolean mouseClicked(Click context, boolean doubleClick)
	{
		if(context.button() == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			close();
			return true;
		}
		
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredTextWithShadow(textRenderer, "清理", width / 2,
			20, Colors.WHITE);
		context.drawCenteredTextWithShadow(textRenderer,
			"请选择要删除的服务器：", width / 2, 36,
			Colors.LIGHT_GRAY);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(DrawContext context, int mouseX,
		int mouseY)
	{
		for(ClickableWidget button : Screens.getButtons(this))
		{
			if(!button.isSelected() || !(button instanceof CleanUpButton))
				continue;
			
			CleanUpButton cuButton = (CleanUpButton)button;
			
			if(cuButton.tooltip.isEmpty())
				continue;
			
			context.drawTooltip(textRenderer, cuButton.tooltip, mouseX, mouseY);
			break;
		}
	}
	
	@Override
	public void close()
	{
		client.setScreen(prevScreen);
	}
	
	private final class CleanUpButton extends ButtonWidget
	{
		private final Supplier<String> messageSupplier;
		private final List<Text> tooltip;
		
		public CleanUpButton(int x, int y, Supplier<String> messageSupplier,
			String tooltip, PressAction pressAction)
		{
			super(x, y, 200, 20, Text.literal(messageSupplier.get()),
				pressAction, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = tooltip.split("\n");
				
				Text[] lines2 = new Text[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Text.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
		}
		
		@Override
		public void onPress(AbstractInput context)
		{
			super.onPress(context);
			setMessage(Text.literal(messageSupplier.get()));
		}
	}
}
