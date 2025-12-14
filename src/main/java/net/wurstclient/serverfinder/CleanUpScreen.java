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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class CleanUpScreen extends Screen
{
	private JoinMultiplayerScreen prevScreen;
	private Button cleanUpButton;
	
	private boolean removeAll;
	private boolean cleanupFailed = true;
	private boolean cleanupOutdated = true;
	private boolean cleanupRename = true;
	private boolean cleanupUnknown = true;
	private boolean cleanupGriefMe;
	
	public CleanUpScreen(JoinMultiplayerScreen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addRenderableWidget(new CleanUpButton(width / 2 - 100,
			height / 4 + 168 + 12, () -> "取消", "", b -> onClose()));
		
		addRenderableWidget(cleanUpButton = new CleanUpButton(width / 2 - 100,
			height / 4 + 144 + 12, () -> "清理",
			"使用上面指定的设置开始清理\n游戏可能看起来在几秒钟内没有响应",
			b -> cleanUp()));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 - 24 + 12,
				() -> "未知主机: " + removeOrKeep(cleanupUnknown),
				"不存在的服务器",
				b -> cleanupUnknown = !cleanupUnknown));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 0 + 12,
				() -> "过时的服务器: " + removeOrKeep(cleanupOutdated),
				"运行与您不同的 Minecraft 版本的服务器",
				b -> cleanupOutdated = !cleanupOutdated));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 24 + 12,
				() -> "Ping 失败: " + removeOrKeep(cleanupFailed),
				"上次 ping 失败的所有服务器\n在执行此作之前, 请确保最后一次 ping 已完成\n这意味着: 返回, 按下刷新按钮并等待所有服务器完成刷新",
				b -> cleanupFailed = !cleanupFailed));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 48 + 12,
				() -> "\"Grief me\"服务器: " + removeOrKeep(cleanupGriefMe),
				"用于删除所有名称以\"Grief me\"开头查找器找到的服务器",
				b -> cleanupGriefMe = !cleanupGriefMe));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 72 + 12,
				() -> "\u00a7c删除所有服务器: " + yesOrNo(removeAll),
				"这将彻底清除您的服务器列表\n\u00a7c请谨慎使用！",
				b -> removeAll = !removeAll));
		
		addRenderableWidget(
			new CleanUpButton(width / 2 - 100, height / 4 + 96 + 12,
				() -> "重命名所有服务器: " + yesOrNo(cleanupRename),
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
		for(int i = prevScreen.getServers().size() - 1; i >= 0; i--)
		{
			ServerData server = prevScreen.getServers().get(i);
			
			if(removeAll || shouldRemove(server))
				prevScreen.getServers().remove(server);
		}
		
		if(cleanupRename)
			for(int i = 0; i < prevScreen.getServers().size(); i++)
			{
				ServerData server = prevScreen.getServers().get(i);
				server.name = "Grief me #" + (i + 1);
			}
		
		saveServerList();
		minecraft.setScreen(prevScreen);
	}
	
	private boolean shouldRemove(ServerData server)
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
	
	private boolean isUnknownHost(ServerData server)
	{
		if(server.motd == null)
			return false;
		
		if(server.motd.getString() == null)
			return false;
		
		return server.motd.getString().equals("\u00a74无法解析主机名");
	}
	
	private boolean isSameProtocol(ServerData server)
	{
		return server.protocol == SharedConstants.getCurrentVersion()
			.protocolVersion();
	}
	
	private boolean isFailedPing(ServerData server)
	{
		return server.ping != -2L && server.ping < 0L;
	}
	
	private boolean isGriefMeServer(ServerData server)
	{
		return server.name != null && server.name.startsWith("Grief me");
	}
	
	private void saveServerList()
	{
		prevScreen.getServers().save();
		
		ServerSelectionList listWidget = prevScreen.serverSelectionList;
		listWidget.setSelected(null);
		listWidget.updateOnlineServers(prevScreen.getServers());
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			cleanUpButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			onClose();
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredString(font, "清理", width / 2, 20,
			CommonColors.WHITE);
		context.drawCenteredString(font,
			"请选择要删除的服务器:", width / 2, 36,
			CommonColors.LIGHT_GRAY);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		renderButtonTooltip(context, mouseX, mouseY);
	}
	
	private void renderButtonTooltip(GuiGraphics context, int mouseX,
		int mouseY)
	{
		for(AbstractWidget button : Screens.getButtons(this))
		{
			if(!button.isHoveredOrFocused()
				|| !(button instanceof CleanUpButton))
				continue;
			
			CleanUpButton cuButton = (CleanUpButton)button;
			
			if(cuButton.tooltip.isEmpty())
				continue;
			
			context.setComponentTooltipForNextFrame(font, cuButton.tooltip,
				mouseX, mouseY);
			break;
		}
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	private final class CleanUpButton extends Button
	{
		private final Supplier<String> messageSupplier;
		private final List<Component> tooltip;
		
		public CleanUpButton(int x, int y, Supplier<String> messageSupplier,
			String tooltip, OnPress pressAction)
		{
			super(x, y, 200, 20, Component.literal(messageSupplier.get()),
				pressAction, Button.DEFAULT_NARRATION);
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = tooltip.split("\n");
				
				Component[] lines2 = new Component[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = Component.literal(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(Component.literal(messageSupplier.get()));
		}
	}
}
