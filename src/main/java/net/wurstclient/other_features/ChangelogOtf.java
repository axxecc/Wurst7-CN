/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.Util;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;

@SearchTags({"change log", "wurst update", "release notes", "what's new",
	"what is new", "new features", "recently added features"})
@DontBlock
public final class ChangelogOtf extends OtherFeature
{
	public ChangelogOtf()
	{
		super("更改日志", "在浏览器中打开更改日志");
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "查看更改日志";
	}

	@Override
	public void doPrimaryAction()
	{
        String link = "https://www.wurstclient.net/download/";
		Util.getPlatform().openUri(link);
	}
}
