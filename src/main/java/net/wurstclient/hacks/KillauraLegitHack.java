/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.MouseUpdateListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

public final class KillauraLegitHack extends Hack implements UpdateListener,
	HandleInputListener, MouseUpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("范围", 4.25, 1, 4.25, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final SliderSetting speedRandMS =
		new SliderSetting("速度随机化",
			"通过改变攻击之间的延迟来帮助您绕过反作弊插件",
			100, 0, 1000, 50, ValueDisplay.INTEGER.withPrefix("\u00b1")
				.withSuffix("ms").withLabel(0, "关闭"));
	
	private final SliderSetting rotationSpeed =
		new SliderSetting("转头速度", 600, 10, 3600, 10,
			ValueDisplay.DEGREES.withSuffix("/s"));
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("优先权",
		"确定首先攻击哪个实体\n\u00a7l距离\u00a7r - 攻击最近的实体\n\u00a7l角度\u00a7r - 攻击需要最少视角旋转的实体\n\u00a7l角度+距离\u00a7r - 角度和距离的混合体\n\u00a7l血量\u00a7r - 攻击最弱的实体",
		Priority.values(), Priority.ANGLE);

	private final SliderSetting fov = new SliderSetting("视角范围",
		"视野 - 实体在被忽略之前可以离您的准星多远\n360° = 实体可以攻击您周围的所有实体",
		360, 30, 360, 10, ValueDisplay.DEGREES);
	
	private final SwingHandSetting swingHand =
		SwingHandSetting.withoutOffOption(
			SwingHandSetting.genericCombatDescription(this), SwingHand.CLIENT);
	
	private final CheckboxSetting damageIndicator = new CheckboxSetting(
		"伤害指示器",
		"在目标中渲染一个彩色框，与其剩余生命值成反比",
		true);
	
	// same filters as in Killaura, but with stricter defaults
	private final EntityFilterList entityFilters =
		new EntityFilterList(FilterPlayersSetting.genericCombat(false),
			FilterSleepingSetting.genericCombat(true),
			FilterFlyingSetting.genericCombat(0.5),
			FilterHostileSetting.genericCombat(false),
			FilterNeutralSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterPassiveSetting.genericCombat(false),
			FilterPassiveWaterSetting.genericCombat(false),
			FilterBabiesSetting.genericCombat(false),
			FilterBatsSetting.genericCombat(false),
			FilterSlimesSetting.genericCombat(false),
			FilterPetsSetting.genericCombat(false),
			FilterVillagersSetting.genericCombat(false),
			FilterZombieVillagersSetting.genericCombat(false),
			FilterGolemsSetting.genericCombat(false),
			FilterPiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterZombiePiglinsSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterEndermenSetting
				.genericCombat(AttackDetectingEntityFilter.Mode.OFF),
			FilterShulkersSetting.genericCombat(false),
			FilterAllaysSetting.genericCombat(false),
			FilterInvisibleSetting.genericCombat(true),
			FilterNamedSetting.genericCombat(false),
			FilterShulkerBulletSetting.genericCombat(false),
			FilterArmorStandsSetting.genericCombat(false),
			FilterCrystalsSetting.genericCombat(false));
	
	private Entity target;
	private float nextYaw;
	private float nextPitch;
	
	public KillauraLegitHack()
	{
		super("合法杀戮光环");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(speedRandMS);
		addSetting(rotationSpeed);
		addSetting(priority);
		addSetting(fov);
		addSetting(swingHand);
		addSetting(damageIndicator);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable other killauras
		WURST.getHax().aimAssistHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		speed.resetTimer(speedRandMS.getValue());
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(MouseUpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
		EVENTS.remove(MouseUpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		target = null;
	}
	
	@Override
	public void onUpdate()
	{
		target = null;
		
		// don't attack when a container/inventory screen is open
		if(MC.currentScreen instanceof HandledScreen)
			return;
		
		Stream<Entity> stream = EntityUtils.getAttackableEntities();
		double rangeSq = range.getValueSq();
		stream = stream.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
		
		if(fov.getValue() < 360.0)
			stream = stream.filter(e -> RotationUtils.getAngleToLookVec(
				e.getBoundingBox().getCenter()) <= fov.getValue() / 2.0);
		
		stream = entityFilters.applyTo(stream);
		
		target = stream.min(priority.getSelected().comparator).orElse(null);
		if(target == null)
			return;
		
		// check line of sight
		if(!BlockUtils.hasLineOfSight(target.getBoundingBox().getCenter()))
		{
			target = null;
			return;
		}
		
		// face entity
		WURST.getHax().autoSwordHack.setSlot(target);
		faceEntityClient(target);
	}
	
	@Override
	public void onHandleInput()
	{
		if(target == null)
			return;
		
		speed.updateTimer();
		if(!speed.isTimeToAttack())
			return;
		
		if(!RotationUtils.isFacingBox(target.getBoundingBox(),
			range.getValue()))
			return;
		
		// attack entity
		MC.interactionManager.attackEntity(MC.player, target);
		swingHand.swing(Hand.MAIN_HAND);
		speed.resetTimer(speedRandMS.getValue());
	}
	
	private boolean faceEntityClient(Entity entity)
	{
		// get needed rotation
		Box box = entity.getBoundingBox();
		Rotation needed = RotationUtils.getNeededRotations(box.getCenter());
		
		// turn towards center of boundingBox
		Rotation next = RotationUtils.slowlyTurnTowards(needed,
			rotationSpeed.getValueI() / 20F);
		nextYaw = next.yaw();
		nextPitch = next.pitch();
		
		// check if facing center
		if(RotationUtils.isAlreadyFacing(needed))
			return true;
		
		// if not facing center, check if facing anything in boundingBox
		return RotationUtils.isFacingBox(box, range.getValue());
	}
	
	@Override
	public void onMouseUpdate(MouseUpdateEvent event)
	{
		if(target == null || MC.player == null)
			return;
		
		int diffYaw = (int)(nextYaw - MC.player.getYaw());
		int diffPitch = (int)(nextPitch - MC.player.getPitch());
		if(MathHelper.abs(diffYaw) < 1 && MathHelper.abs(diffPitch) < 1)
			return;
		
		event.setDeltaX(event.getDefaultDeltaX() + diffYaw);
		event.setDeltaY(event.getDefaultDeltaY() + diffPitch);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(target == null || !damageIndicator.isChecked())
			return;
		
		float p = 1;
		if(target instanceof LivingEntity le)
			p = (le.getMaxHealth() - le.getHealth()) / le.getMaxHealth();
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		Box box = EntityUtils.getLerpedBox(target, partialTicks);
		if(p < 1)
			box = box.contract((1 - p) * 0.5 * box.getLengthX(),
				(1 - p) * 0.5 * box.getLengthY(),
				(1 - p) * 0.5 * box.getLengthZ());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
	
	private enum Priority
	{
		DISTANCE("距离", e -> MC.player.squaredDistanceTo(e)),
		
		ANGLE("角度",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		HEALTH("血量", e -> e instanceof LivingEntity
			? ((LivingEntity)e).getHealth() : Integer.MAX_VALUE);
		
		private final String name;
		private final Comparator<Entity> comparator;
		
		private Priority(String name, ToDoubleFunction<Entity> keyExtractor)
		{
			this.name = name;
			comparator = Comparator.comparingDouble(keyExtractor);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
