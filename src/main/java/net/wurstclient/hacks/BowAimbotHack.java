/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"bow aimbot"})
public final class BowAimbotHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private final EnumSetting<Priority> priority = new EnumSetting<>("优先权",
		"确定首先攻击哪个实体\n\u00a7l距离\u00a7r - 攻击最近的实体\n\u00a7l角度\u00a7r - 攻击需要最少视角旋转的实体\n\u00a7l角度+距离\u00a7r - 角度和距离的混合体\n\u00a7l血量\u00a7r - 攻击最弱的实体",
		Priority.values(), Priority.ANGLE_DIST);
	
	private final SliderSetting predictMovement = new SliderSetting(
		"预测移动",
		"控制弓箭自瞄的运动预测算法的强度",
		0.2, 0, 2, 0.01, ValueDisplay.PERCENTAGE);
	
	private final EntityFilterList entityFilters =
		EntityFilterList.genericCombat();
	
	private final ColorSetting color = new ColorSetting("ESP 颜色",
		"弓箭自瞄在目标周围绘制的框的颜色", Color.RED);
	
	private Entity target;
	private float velocity;
	
	public BowAimbotHack()
	{
		super("弓箭自瞄");
		
		setCategory(Category.COMBAT);
		addSetting(priority);
		addSetting(predictMovement);
		
		entityFilters.forEach(this::addSetting);
		
		addSetting(color);
	}
	
	@Override
	protected void onEnable()
	{
		// disable conflicting hacks
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		
		// register event listeners
		EVENTS.add(GUIRenderListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(GUIRenderListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		// check if item is ranged weapon
		ItemStack stack = MC.player.getInventory().getSelectedStack();
		Item item = stack.getItem();
		if(!(item instanceof BowItem || item instanceof CrossbowItem))
		{
			target = null;
			return;
		}
		
		// check if using bow
		if(item instanceof BowItem && !MC.options.useKey.isPressed()
			&& !player.isUsingItem())
		{
			target = null;
			return;
		}
		
		// check if crossbow is loaded
		if(item instanceof CrossbowItem && !CrossbowItem.isCharged(stack))
		{
			target = null;
			return;
		}
		
		// set target
		if(filterEntities(Stream.of(target)) == null)
			target = filterEntities(StreamSupport
				.stream(MC.world.getEntities().spliterator(), true));
		
		if(target == null)
			return;
		
		// set velocity
		velocity = (72000 - player.getItemUseTimeLeft()) / 20F;
		velocity = (velocity * velocity + velocity * 2) / 3;
		if(velocity > 1)
			velocity = 1;
		
		// set position to aim at
		double d = RotationUtils.getEyesPos().distanceTo(
			target.getBoundingBox().getCenter()) * predictMovement.getValue();
		double posX = target.getX() + (target.getX() - target.lastRenderX) * d
			- player.getX();
		double posY = target.getY() + (target.getY() - target.lastRenderY) * d
			+ target.getHeight() * 0.5 - player.getY()
			- player.getEyeHeight(player.getPose());
		double posZ = target.getZ() + (target.getZ() - target.lastRenderZ) * d
			- player.getZ();
		
		// set yaw
		float neededYaw = (float)Math.toDegrees(Math.atan2(posZ, posX)) - 90;
		MC.player.setYaw(
			RotationUtils.limitAngleChange(MC.player.getYaw(), neededYaw));
		
		// calculate needed pitch
		double hDistance = Math.sqrt(posX * posX + posZ * posZ);
		double hDistanceSq = hDistance * hDistance;
		float g = 0.006F;
		float velocitySq = velocity * velocity;
		float velocityPow4 = velocitySq * velocitySq;
		float neededPitch = (float)-Math.toDegrees(Math.atan((velocitySq - Math
			.sqrt(velocityPow4 - g * (g * hDistanceSq + 2 * posY * velocitySq)))
			/ (g * hDistance)));
		
		// set pitch
		if(Float.isNaN(neededPitch))
			WURST.getRotationFaker()
				.faceVectorClient(target.getBoundingBox().getCenter());
		else
			MC.player.setPitch(neededPitch);
	}
	
	private Entity filterEntities(Stream<Entity> s)
	{
		Stream<Entity> stream = s.filter(EntityUtils.IS_ATTACKABLE);
		stream = entityFilters.applyTo(stream);
		
		return stream.min(priority.getSelected().comparator).orElse(null);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(target == null)
			return;
		
		Box box = EntityUtils.getLerpedBox(target, partialTicks)
			.offset(0, 0.05, 0).expand(0.05);
		
		int quadColor = color.getColorI(0.5F * velocity);
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		
		int lineColor = color.getColorI(0.25F * velocity);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		if(target == null)
			return;
		
		String message;
		if(velocity < 1)
			message = "锁定：" + (int)(velocity * 100) + "%";
		else
			message = "目标已锁定";
		
		TextRenderer tr = MC.textRenderer;
		int msgWidth = tr.getWidth(message);
		
		int msgX1 = context.getScaledWindowWidth() / 2 - msgWidth / 2;
		int msgX2 = msgX1 + msgWidth + 3;
		int msgY1 = context.getScaledWindowHeight() / 2 + 1;
		int msgY2 = msgY1 + 10;
		
		// background
		context.fill(msgX1, msgY1, msgX2, msgY2, 0x80000000);
		
		// text
		context.drawText(tr, message, msgX1 + 2, msgY1 + 1, 0xFFFFFFFF, false);
	}
	
	private enum Priority
	{
		DISTANCE("距离", e -> MC.player.squaredDistanceTo(e)),
		
		ANGLE("角度",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		ANGLE_DIST("角度+距离",
			e -> Math
				.pow(RotationUtils
					.getAngleToLookVec(e.getBoundingBox().getCenter()), 2)
				+ MC.player.squaredDistanceTo(e)),
		
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
