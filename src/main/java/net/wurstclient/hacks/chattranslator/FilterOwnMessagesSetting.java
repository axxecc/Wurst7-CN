/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chattranslator;

import java.util.regex.Pattern;

import net.wurstclient.WurstClient;
import net.wurstclient.settings.CheckboxSetting;

public class FilterOwnMessagesSetting extends CheckboxSetting
{
	private Pattern ownMessagePattern;
	private String lastUsername;
	
	public FilterOwnMessagesSetting()
	{
		super("过滤自己的消息",
			"它不会翻译看起来像是你发送的消息\n\n它会根据常见的聊天格式如\"<name>\",\"[name]\"或\"name：\"来检测你的消息, 这在某些服务器上可能无法正常工作",
			true);
	}
	
	public boolean isOwnMessage(String message)
	{
		updateOwnMessagePattern();
		return ownMessagePattern.matcher(message).find();
	}
	
	private void updateOwnMessagePattern()
	{
		String username = WurstClient.MC.getUser().getName();
		if(username.equals(lastUsername))
			return;
		
		String rankPattern = "(?:\\[[^\\]]+\\] ?){0,2}";
		String namePattern = Pattern.quote(username);
		String regex = "^" + rankPattern + "[<\\[]?" + namePattern + "[>\\]:]";
		
		ownMessagePattern = Pattern.compile(regex);
		lastUsername = username;
	}
}
