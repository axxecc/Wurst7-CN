/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class ParkourHack extends Hack implements UpdateListener
{
	private final SliderSetting minDepth = new SliderSetting("最小深度",
		"如果坑不是至少这么深，则不会跳过坑\n增加以阻止跑酷跳下楼梯\n减少以使跑酷在地毯边缘跳跃",
		0.5, 0.05, 10, 0.05, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private final SliderSetting edgeDistance =
		new SliderSetting("边距",
			"跑酷可以让您在跳跃之前到达边缘的距离",
			0.001, 0.001, 0.25, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private final CheckboxSetting sneak = new CheckboxSetting(
		"潜行时跳跃",
		"即使在您潜行时也能保持跑酷活动\n使用此选项时，您可能希望增加\u00a7l边距\u00a7r滑块",
		false);
	
	public ParkourHack()
	{
		super("简单跑酷");
		setCategory(Category.MOVEMENT);
		addSetting(minDepth);
		addSetting(edgeDistance);
		addSetting(sneak);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().safeWalkHack.setEnabled(false);
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
		if(!MC.player.onGround() || MC.options.keyJump.isDown())
			return;
		
		if(!sneak.isChecked()
			&& (MC.player.isShiftKeyDown() || MC.options.keyShift.isDown()))
			return;
		
		AABB box = MC.player.getBoundingBox();
		AABB adjustedBox = box.expandTowards(0, -minDepth.getValue(), 0)
			.inflate(-edgeDistance.getValue(), 0, -edgeDistance.getValue());
		
		if(!MC.level.noCollision(MC.player, adjustedBox))
			return;
		
		MC.player.jumpFromGround();
	}
}
