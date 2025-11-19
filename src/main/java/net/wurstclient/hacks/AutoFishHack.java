/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofish.AutoFishDebugDraw;
import net.wurstclient.hacks.autofish.AutoFishRodSelector;
import net.wurstclient.hacks.autofish.FishingSpotManager;
import net.wurstclient.hacks.autofish.ShallowWaterWarningCheckbox;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"AutoFishing", "auto fishing", "AutoFisher", "auto fisher",
	"AFKFishBot", "afk fish bot", "AFKFishingBot", "afk fishing bot",
	"AFKFisherBot", "afk fisher bot"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final EnumSetting<AutoFishHack.BiteMode> biteMode =
		new EnumSetting<>("咬钩模式",
					"\u00a7l声音\u00a7r 模式通过聆听咬钩声来检测咬钩\n\n\u00a7l实体\u00a7r 模式通过检查鱼钩的实体更新数据包来检测咬钩",
			AutoFishHack.BiteMode.values(), AutoFishHack.BiteMode.SOUND);
	
	private final SliderSetting validRange = new SliderSetting("有效范围",
		"任何超出此范围的咬合都将被忽略\n\n如果未检测到咬钩, 则增加您的范围；如果将其他人的咬钩为您的咬钩, 则减少范围\n\n当\"咬钩模式\"设置为\u00a7l实体\u00a7r时, 此设置无效",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private final SliderSetting catchDelay = new SliderSetting("捕获延迟",
		"自动鱼咬钩后要等多久才会收线", 0, 0, 60,
		1, ValueDisplay.INTEGER.withSuffix(" Tick"));
	
	private final SliderSetting retryDelay = new SliderSetting("重试延迟",
		"如果抛竿或收竿失败, 自动钓鱼将等待以下时间后再尝试重试",
		15, 0, 100, 1,
		ValueDisplay.INTEGER.withSuffix(" Tick"));
	
	private final SliderSetting patience = new SliderSetting("耐心",
		"如果自动钓鱼没有钓到鱼, 它会等待多久才收线",
		60, 10, 120, 1, ValueDisplay.INTEGER.withSuffix("S"));
	
	private final ShallowWaterWarningCheckbox shallowWaterWarning =
		new ShallowWaterWarningCheckbox();
	
	private final FishingSpotManager fishingSpots = new FishingSpotManager();
	private final AutoFishDebugDraw debugDraw =
		new AutoFishDebugDraw(validRange, fishingSpots);
	private final AutoFishRodSelector rodSelector =
		new AutoFishRodSelector(this);
	
	private int castRodTimer;
	private int reelInTimer;
	private boolean biteDetected;
	
	public AutoFishHack()
	{
		super("自动钓鱼");
		setCategory(Category.OTHER);
		addSetting(biteMode);
		addSetting(validRange);
		addSetting(catchDelay);
		addSetting(retryDelay);
		addSetting(patience);
		debugDraw.getSettings().forEach(this::addSetting);
		rodSelector.getSettings().forEach(this::addSetting);
		addSetting(shallowWaterWarning);
		fishingSpots.getSettings().forEach(this::addSetting);
	}
	
	@Override
	public String getRenderName()
	{
		if(rodSelector.isOutOfRods())
			return getName() + " [钓竿用完了]";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		castRodTimer = 0;
		reelInTimer = 0;
		biteDetected = false;
		rodSelector.reset();
		debugDraw.reset();
		fishingSpots.reset();
		shallowWaterWarning.reset();
		
		WURST.getHax().antiAfkHack.setEnabled(false);
		WURST.getHax().aimAssistHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// update timers
		if(castRodTimer > 0)
			castRodTimer--;
		if(reelInTimer > 0)
			reelInTimer--;
		
		// update inventory
		if(!rodSelector.update())
			return;
		
		// if not fishing, cast rod
		if(!isFishing())
		{
			if(castRodTimer > 0)
				return;
			
			reelInTimer = 20 * patience.getValueI();
			if(!fishingSpots.onCast())
				return;
			
			MC.startUseItem();
			castRodTimer = retryDelay.getValueI();
			return;
		}
		
		// if a bite was detected, check water type and reel in
		if(biteDetected)
		{
			shallowWaterWarning.checkWaterType();
			reelInTimer = catchDelay.getValueI();
			fishingSpots.onBite(MC.player.fishing);
			biteDetected = false;
			
			// also reel in if an entity was hooked
		}else if(MC.player.fishing.getHookedIn() != null)
			reelInTimer = catchDelay.getValueI();
		
		// otherwise, reel in when the timer runs out
		if(reelInTimer == 0)
		{
			MC.startUseItem();
			reelInTimer = retryDelay.getValueI();
			castRodTimer = retryDelay.getValueI();
		}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		switch(biteMode.getSelected())
		{
			case SOUND -> processSoundUpdate(event);
			case ENTITY -> processEntityUpdate(event);
		}
	}
	
	private void processSoundUpdate(PacketInputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof ClientboundSoundPacket sound))
			return;
		
		// check sound type
		if(!SoundEvents.FISHING_BOBBER_SPLASH.equals(sound.getSound().value()))
			return;
		
		// check if player is fishing
		if(!isFishing())
			return;
		
		// register sound position
		debugDraw.updateSoundPos(sound);
		
		// check sound position (Chebyshev distance)
		Vec3 bobber = MC.player.fishing.position();
		double dx = Math.abs(sound.getX() - bobber.x());
		double dz = Math.abs(sound.getZ() - bobber.z());
		if(Math.max(dx, dz) > validRange.getValue())
			return;
		
		biteDetected = true;
	}
	
	private void processEntityUpdate(PacketInputEvent event)
	{
		// check packet type
		if(!(event
			.getPacket() instanceof ClientboundSetEntityDataPacket update))
			return;
		
		// check if the entity is a bobber
		if(!(MC.level.getEntity(update.id()) instanceof FishingHook bobber))
			return;
		
		// check if it's our bobber
		if(bobber != MC.player.fishing)
			return;
		
		// check if player is fishing
		if(!isFishing())
			return;
		
		biteDetected = true;
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		debugDraw.render(matrixStack, partialTicks);
	}
	
	private boolean isFishing()
	{
		LocalPlayer player = MC.player;
		return player != null && player.fishing != null
			&& !player.fishing.isRemoved()
			&& player.getMainHandItem().is(Items.FISHING_ROD);
	}
	
	private enum BiteMode
	{
		SOUND("声音"),
		ENTITY("实体");
		
		private final String name;
		
		private BiteMode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
