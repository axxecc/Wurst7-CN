/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.OptionInstance;
import net.minecraft.util.Mth;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.ISimpleOption;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"Fullbrightness", "full brightness", "Fulbrightness",
	"ful brightness", "NightVision", "night vision", "FullLightness",
	"FulLightness", "full lightness", "FullGamma", "full gamma"})
public final class FullbrightHack extends Hack implements UpdateListener
{
	private final EnumSetting<Method> method = new EnumSetting<>("模式",
		"\u00a7l伽马覆写\u00a7r 将亮度滑块设置为 100% 以上\n\n\u00a7l夜视效果\u00a7r 通过应用夜视效果来工作",
		Method.values(), Method.GAMMA);
	
	private final CheckboxSetting fade = new CheckboxSetting("淡出",
		"在光明与黑暗之间慢慢消失", true);
	
	private final SliderSetting defaultGamma = new SliderSetting(
		"默认亮度",
		"当您关闭黑暗视觉时, 它会将亮度滑块设置回此值",
		0.5, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private boolean wasGammaChanged;
	private float nightVisionStrength;
	
	public FullbrightHack()
	{
		super("黑暗视觉");
		setCategory(Category.RENDER);
		addSetting(method);
		addSetting(fade);
		addSetting(defaultGamma);
		
		checkGammaOnStartup();
		EVENTS.add(UpdateListener.class, this);
	}
	
	private void checkGammaOnStartup()
	{
		EVENTS.add(UpdateListener.class, new UpdateListener()
		{
			@Override
			public void onUpdate()
			{
				double gamma = MC.options.gamma().get();
				System.out.println("Brightness started at " + gamma);
				
				if(gamma > 1)
					wasGammaChanged = true;
				else
					defaultGamma.setValue(gamma);
				
				EVENTS.remove(UpdateListener.class, this);
			}
		});
	}
	
	@Override
	public void onUpdate()
	{
		updateGamma();
		updateNightVision();
	}
	
	private void updateGamma()
	{
		if(isChangingGamma())
		{
			setGamma(16);
			return;
		}
		
		if(wasGammaChanged)
			resetGamma(defaultGamma.getValue());
	}
	
	private void setGamma(double target)
	{
		wasGammaChanged = true;
		
		OptionInstance<Double> gammaOption = MC.options.gamma();
		ISimpleOption<Double> gammaOption2 = ISimpleOption.get(gammaOption);
		double oldGammaValue = gammaOption.get();
		
		if(!fade.isChecked() || Math.abs(oldGammaValue - target) <= 0.5)
		{
			gammaOption2.forceSetValue(target);
			return;
		}
		
		if(oldGammaValue < target)
			gammaOption2.forceSetValue(oldGammaValue + 0.5);
		else
			gammaOption2.forceSetValue(oldGammaValue - 0.5);
	}
	
	private void resetGamma(double target)
	{
		OptionInstance<Double> gammaOption = MC.options.gamma();
		ISimpleOption<Double> gammaOption2 = ISimpleOption.get(gammaOption);
		double oldGammaValue = gammaOption.get();
		
		if(!fade.isChecked() || Math.abs(oldGammaValue - target) <= 0.5)
		{
			gammaOption2.forceSetValue(target);
			wasGammaChanged = false;
			return;
		}
		
		if(oldGammaValue < target)
			gammaOption2.forceSetValue(oldGammaValue + 0.5);
		else
			gammaOption2.forceSetValue(oldGammaValue - 0.5);
	}
	
	private void updateNightVision()
	{
		boolean shouldGiveNightVision =
			isEnabled() && method.getSelected() == Method.NIGHT_VISION;
		
		if(fade.isChecked())
		{
			if(shouldGiveNightVision)
				nightVisionStrength += 0.03125;
			else
				nightVisionStrength -= 0.03125;
			
			nightVisionStrength = Mth.clamp(nightVisionStrength, 0, 1);
			
		}else if(shouldGiveNightVision)
			nightVisionStrength = 1;
		else
			nightVisionStrength = 0;
	}
	
	public boolean isNightVisionActive()
	{
		return nightVisionStrength > 0;
	}
	
	public float getNightVisionStrength()
	{
		return nightVisionStrength;
	}
	
	public boolean isChangingGamma()
	{
		return isEnabled() && method.getSelected() == Method.GAMMA;
	}
	
	/**
	 * Returns the value of Fullbright's "Default brightness" slider. Used by
	 * {@link XRayHack} to restore the gamma value when X-Ray is turned off.
	 */
	public double getDefaultGamma()
	{
		return defaultGamma.getValue();
	}
	
	private static enum Method
	{
		GAMMA("伽马覆写"),
		NIGHT_VISION("夜视效果");
		
		private final String name;
		
		private Method(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	// See ClientPlayerEntityMixin.hasStatusEffect()
}
