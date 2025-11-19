/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;

@SearchTags({"Server Finder"})
@DontBlock
public final class ServerFinderOtf extends OtherFeature
{
	public ServerFinderOtf()
	{
		super("服务器查找器",
			"让您快速轻松地找到容易出现故障的Minecraft服务器, 要使用它, 请在服务器选择屏幕上按下\"服务器查找器\"按钮");
	}
}
