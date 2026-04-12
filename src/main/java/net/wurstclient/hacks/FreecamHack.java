/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.events.VisGraphListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.freecam.FreecamInitialPosSetting;
import net.wurstclient.hacks.freecam.FreecamInputSetting;
import net.wurstclient.hacks.freecam.FreecamInputSetting.ApplyInputTo;
import net.wurstclient.hacks.freecam.FreecamInteractionSetting;
import net.wurstclient.hacks.freecam.FreecamInteractionSetting.InteractFrom;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@DontSaveState
@SearchTags({"灵魂出窍", "FreeCamera"})
public final class FreecamHack extends Hack
	implements UpdateListener, VisGraphListener,
	CameraTransformViewBobbingListener, RenderListener, MouseScrollListener
{
	private final FreecamInputSetting applyInputTo = new FreecamInputSetting();

	private final FreecamInteractionSetting interactFrom =
		new FreecamInteractionSetting();

	private final SliderSetting horizontalSpeed =
		new SliderSetting("水平速度",
			"使用灵魂出窍时的水平摄像机移动速度\n\n当\"§l应用输入§r\"设置为§l玩家§r时, 不生效", 1, 0.05, 10,
			0.05, ValueDisplay.DECIMAL);

	private final SliderSetting verticalSpeed = new SliderSetting(
		"垂直速度", "使用灵魂出窍时的垂直摄像机移动速度(相对于水平)\n\n当\"§l应用输入§r\"设置为§l玩家§r时, 不生效", 1,
		0.05, 5, 0.05,
		v -> ValueDisplay.DECIMAL.getValueString(getActualVerticalSpeed()));

	private final CheckboxSetting scrollToChangeSpeed =
		new CheckboxSetting("滚动改变速度",
			"当你滚动鼠标滚轮时, 会改变摄像机的移动速度\n\n当\"§l应用输入§r\"设置为§l玩家§r时, 不生效", true);

	private final CheckboxSetting renderSpeed =
		new CheckboxSetting("列表中显示速度",
			"在HackList中显示你当前的水平和垂直摄像机移动速度", true);

	private final FreecamInitialPosSetting initialPos =
		new FreecamInitialPosSetting();
	
	private final CheckboxSetting tracer = new CheckboxSetting("追踪",
		"它会为你角色的实际位置画出一条线", false);
	
	private final ColorSetting color =
		new ColorSetting("追踪颜色", Color.WHITE);
	
	private final CheckboxSetting hideHand = new CheckboxSetting("隐藏手",
		"使用灵魂出窍时, 可以隐藏你的手和手中物品", true);

	private final CheckboxSetting disableOnDamage =
		new CheckboxSetting("Disable on damage",
			"description.wurst.setting.freecam.disable_on_damage", true);

	private final CheckboxSetting reloadChunks =
		new CheckboxSetting("Reload chunks",
			"description.wurst.setting.freecam.reload_chunks", true);

	private Vec3 camPos;
	private Vec3 prevCamPos;
	private float camYaw;
	private float camPitch;
	private float lastHealth;
	
	public FreecamHack()
	{
		super("灵魂出窍");
		setCategory(Category.RENDER);
		addSetting(applyInputTo);
		addSetting(interactFrom);
		addSetting(horizontalSpeed);
		addSetting(verticalSpeed);
		addSetting(scrollToChangeSpeed);
		addSetting(renderSpeed);
		addSetting(initialPos);
		addSetting(tracer);
		addSetting(color);
		addSetting(hideHand);
		addSetting(disableOnDamage);
		addSetting(reloadChunks);
	}

	@Override
	public String getRenderName()
	{
		if(!renderSpeed.isChecked())
			return getName();

		return getName() + " [" + horizontalSpeed.getValueString() + ", "
			+ verticalSpeed.getValueString() + "]";
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(VisGraphListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(MouseScrollListener.class, this);
		
		lastHealth = Float.MIN_VALUE;
		camPos = RotationUtils.getEyesPos()
			.add(initialPos.getSelected().getOffset());
		prevCamPos = camPos;
		camYaw = MC.player.getYRot();
		camPitch = MC.player.getXRot();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(VisGraphListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(MouseScrollListener.class, this);
		
		if(reloadChunks.isChecked())
			MC.levelRenderer.allChanged();
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;

		// Check for damage
		float currentHealth = player.getHealth();
		if(disableOnDamage.isChecked() && currentHealth < lastHealth)
		{
			setEnabled(false);
			return;
		}
		lastHealth = currentHealth;

		if(!isMovingCamera() || MC.screen != null)
		{
			prevCamPos = camPos;
			return;
		}

		// Get movement vector (x=left, y=forward)
		Vec2 moveVector = player.input.getMoveVector();
		
		// Convert to world coordinates
		double yawRad = MC.gameRenderer.getMainCamera().yRot() * Mth.DEG_TO_RAD;
		double sinYaw = Mth.sin(yawRad);
		double cosYaw = Mth.cos(yawRad);
		double offsetX = moveVector.x * cosYaw - moveVector.y * sinYaw;
		double offsetZ = moveVector.x * sinYaw + moveVector.y * cosYaw;
		
		// Calculate vertical offset
		double offsetY = 0;
		double vSpeed = getActualVerticalSpeed();
		if(IKeyMapping.get(MC.options.keyJump).isActuallyDown())
			offsetY += vSpeed;
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
			offsetY -= vSpeed;
		
		// Apply to camera
		Vec3 offsetVec = new Vec3(offsetX, 0, offsetZ)
			.scale(horizontalSpeed.getValueF()).add(0, offsetY, 0);
		prevCamPos = camPos;
		camPos = camPos.add(offsetVec);
	}

	private double getActualVerticalSpeed()
	{
		return Mth.clamp(horizontalSpeed.getValue() * verticalSpeed.getValue(),
			0.05, 10);
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!isControllingScrollEvents())
			return;

		if(amount > 0)
			horizontalSpeed.increaseValue();
		else if(amount < 0)
			horizontalSpeed.decreaseValue();
	}

	public boolean isControllingScrollEvents()
	{
		return isMovingCamera() && scrollToChangeSpeed.isChecked()
			&& MC.screen == null
			&& !WURST.getOtfs().zoomOtf.isControllingScrollEvents();
	}

	public boolean isMovingCamera()
	{
		return isEnabled() && applyInputTo.getSelected() == ApplyInputTo.CAMERA;
	}
	
	public boolean isClickingFromCamera()
	{
		return isEnabled() && interactFrom.getSelected() == InteractFrom.CAMERA;
	}

	@Override
	public void onVisGraph(VisGraphEvent event)
	{
		event.cancel();
	}

	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!tracer.isChecked())
			return;
		
		int colorI = color.getColorI(0x80);
		
		// Box
		double extraSize = 0.05;
		AABB rawBox = EntityUtils.getLerpedBox(MC.player, partialTicks);
		AABB box = rawBox.move(0, extraSize, 0).inflate(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		// Line
		RenderUtils.drawTracer(matrixStack, partialTicks, rawBox.getCenter(),
			colorI, false);
	}

	public boolean shouldHideHand()
	{
		return isEnabled() && hideHand.isChecked();
	}

	public Vec3 getCamPos(float partialTicks)
	{
		return Mth.lerp(partialTicks, prevCamPos, camPos);
	}

	public void turn(double deltaYaw, double deltaPitch)
	{
		// This needs to be consistent with Entity.turn()
		camYaw += (float)(deltaYaw * 0.15);
		camPitch += (float)(deltaPitch * 0.15);
		camPitch = Mth.clamp(camPitch, -90, 90);
	}

	public float getCamYaw()
	{
		return camYaw;
	}

	public float getCamPitch()
	{
		return camPitch;
	}

	public Vec3 getScaledCamDir(double scale)
	{
		return Vec3.directionFromRotation(camPitch, camYaw).scale(scale);
	}
}
