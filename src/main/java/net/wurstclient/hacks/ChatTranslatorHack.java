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
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.chattranslator.FilterOwnMessagesSetting;
import net.wurstclient.hacks.chattranslator.GoogleTranslate;
import net.wurstclient.hacks.chattranslator.LanguageSetting;
import net.wurstclient.hacks.chattranslator.LanguageSetting.Language;
import net.wurstclient.hacks.chattranslator.WhatToTranslateSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;

@SearchTags({"聊天翻译", "ChatTranslate"})
public final class ChatTranslatorHack extends Hack
	implements ChatInputListener, ChatOutputListener
{
	private final WhatToTranslateSetting whatToTranslate =
		new WhatToTranslateSetting();
	
	private final LanguageSetting playerLanguage =
		LanguageSetting.withoutAutoDetect("你的语言",
			"您可以使用和理解的主要语言\n\n您收到的消息将始终翻译成这种语言 (如果启用)\n\n当\"检测发送语言\"关闭时, 所有发送的消息都被认为是这种语言",
			Language.ENGLISH);
	
	private final LanguageSetting otherLanguage =
		LanguageSetting.withoutAutoDetect("其他语言",
			"服务器上其他玩家使用的主要语言\n\n您发送的消息将始终被翻译成此语言 (如果启用)\n\n当\"检测收到的语言\"关闭时, 所有收到的消息都被认为是这种语言",
			Language.CHINESE_SIMPLIFIED);
	
	private final CheckboxSetting autoDetectReceived =
		new CheckboxSetting("检测接收的语言",
			"自动检测收到的消息的语言\n\n当其他玩家使用多种不同语言时很有用\n\n如果每个人都使用同一种语言, 关闭此功能可以提高准确性",
			true);
	
	private final CheckboxSetting autoDetectSent = new CheckboxSetting(
		"检测发送的语言",
		"自动检测已发送消息的语言\n\n如果您使用多种不同语言, 此功能非常有用\n\n如果您始终使用同一种语言, 关闭此功能可以提高准确性", true);
	
	private final FilterOwnMessagesSetting filterOwnMessages =
		new FilterOwnMessagesSetting();
	
	public ChatTranslatorHack()
	{
		super("聊天翻译");
		setCategory(Category.CHAT);
		addSetting(whatToTranslate);
		addSetting(playerLanguage);
		addSetting(otherLanguage);
		addSetting(autoDetectReceived);
		addSetting(autoDetectSent);
		addSetting(filterOwnMessages);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
		EVENTS.add(ChatOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		EVENTS.remove(ChatOutputListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		if(!whatToTranslate.includesReceived())
			return;
		
		String message = event.getComponent().getString();
		Language fromLang = autoDetectReceived.isChecked()
			? Language.AUTO_DETECT : otherLanguage.getSelected();
		Language toLang = playerLanguage.getSelected();
		
		if(message.startsWith(ChatUtils.WURST_PREFIX)
			|| message.startsWith(toLang.getPrefix()))
			return;
		
		if(filterOwnMessages.isChecked()
			&& filterOwnMessages.isOwnMessage(message))
			return;
		
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> showTranslated(message, fromLang, toLang));
	}
	
	private void showTranslated(String message, Language fromLang,
		Language toLang)
	{
		String translated = GoogleTranslate.translate(message,
			fromLang.getValue(), toLang.getValue());
		
		if(translated != null)
			MC.gui.getChat().addMessage(toLang.prefixText(translated));
	}
	
	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		if(!whatToTranslate.includesSent())
			return;
		
		String message = event.getMessage();
		Language fromLang = autoDetectSent.isChecked() ? Language.AUTO_DETECT
			: playerLanguage.getSelected();
		Language toLang = otherLanguage.getSelected();
		
		if(message.startsWith("/") || message.startsWith("."))
			return;
		
		event.cancel();
		
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> sendTranslated(message, fromLang, toLang));
	}
	
	private void sendTranslated(String message, Language fromLang,
		Language toLang)
	{
		String translated = GoogleTranslate.translate(message,
			fromLang.getValue(), toLang.getValue());
		
		if(translated == null)
			translated = message;
		
		MC.getConnection().sendChat(translated);
	}
}
