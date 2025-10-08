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
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"name tags"})
public final class NameTagsHack extends Hack
{
	private final SliderSetting scale =
		new SliderSetting("尺寸", "名称标签应该有多大", 1, 0.05,
			5, 0.05, SliderSetting.ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting unlimitedRange =
		new CheckboxSetting("无限范围",
			"移除了名称标签的 64 格距离限制", true);
	
	private final CheckboxSetting seeThrough = new CheckboxSetting(
		"透明模式",
		"在透明文本图层上呈现名称标签。这使得它们在墙后更容易阅读，但会导致水和其他透明物体出现一些图形故障",
		false);
	
	private final CheckboxSetting forceMobNametags = new CheckboxSetting(
		"总是显示命名的生物", "显示命名生物的名称标签，即使你没有直视它们",
		true);
	
	private final CheckboxSetting forcePlayerNametags =
		new CheckboxSetting("始终显示玩家姓名",
			"显示您自己的名称标签以及通常由记分板团队设置禁用的任何玩家名称",
			false);
	
	public NameTagsHack()
	{
		super("名称标签");
		setCategory(Category.RENDER);
		addSetting(scale);
		addSetting(unlimitedRange);
		addSetting(seeThrough);
		addSetting(forceMobNametags);
		addSetting(forcePlayerNametags);
	}
	
	public float getScale()
	{
		return scale.getValueF();
	}
	
	public boolean isUnlimitedRange()
	{
		return isEnabled() && unlimitedRange.isChecked();
	}
	
	public boolean isSeeThrough()
	{
		return isEnabled() && seeThrough.isChecked();
	}
	
	public boolean shouldForceMobNametags()
	{
		return isEnabled() && forceMobNametags.isChecked();
	}
	
	public boolean shouldForcePlayerNametags()
	{
		return isEnabled() && forcePlayerNametags.isChecked();
	}
	
	// See EntityRenderCommandQueueImpl, EntityRendererMixin,
	// LivingEntityRendererMixin, MobEntityRendererMixin
}
