/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.AutoBuildTemplate;
import net.wurstclient.util.BlockPlacer;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.json.JsonException;

public final class InstaBuildHack extends Hack
	implements UpdateListener, RightClickListener
{
	private final FileSetting templateSetting = new FileSetting("模板",
		"确定要构建的内容\n\n模板只是 JSON 文件, 请随意添加您自己的模板或编辑/删除默认模板\n\n如果您搞砸了, 只需按\"重置为默认值\"按钮或删除文件夹即可",
		"autobuild", path -> {});
	
	private final SliderSetting range = new SliderSetting("范围",
		"放置物块时要达到多远\n" + "推荐值: \nvanilla 为 6.0\n\" + \"NoCheat+ 为 4.25",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting useSavedBlocks = new CheckboxSetting(
		"使用已保存的块",
		"尝试放置保存在模板中的相同块\n\n如果模板未指定块类型, 则将从您持有的任何块构建它",
		false);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashMap<BlockPos, Item> remainingBlocks =
		new LinkedHashMap<>();
	
	public InstaBuildHack()
	{
		super("模板建造");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(useSavedBlocks);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		switch(status)
		{
			case NO_TEMPLATE:
			break;
			
			case LOADING:
			name += " [加载...]";
			break;
			
			case IDLE:
			name += " [" + template.getName() + "]";
			break;
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoBuildHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		
		remainingBlocks.clear();
		
		if(template == null)
			status = Status.NO_TEMPLATE;
		else
			status = Status.IDLE;
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(status != Status.IDLE)
			return;
		
		HitResult hitResult = MC.hitResult;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos =
			hitResultPos.relative(blockHitResult.getDirection());
		Direction direction = MC.player.getDirection();
		remainingBlocks = template.getBlocksToPlace(startPos, direction);
		
		buildInstantly();
	}
	
	@Override
	public void onUpdate()
	{
		switch(status)
		{
			case NO_TEMPLATE:
			loadSelectedTemplate();
			break;
			
			default:
			case LOADING:
			break;
			
			case IDLE:
			if(!template.isSelected(templateSetting))
				loadSelectedTemplate();
			break;
		}
	}
	
	private void buildInstantly()
	{
		Inventory inventory = MC.player.getInventory();
		int oldSlot = inventory.selected;
		
		for(Map.Entry<BlockPos, Item> entry : remainingBlocks.entrySet())
		{
			BlockPos pos = entry.getKey();
			Item item = entry.getValue();
			
			if(!BlockUtils.getState(pos).canBeReplaced())
				continue;
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > range.getValueSq())
				continue;
			
			if(useSavedBlocks.isChecked() && item != Items.AIR
				&& !MC.player.getMainHandItem().is(item))
				giveOrSelectItem(item);
			
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				SwingHand.OFF);
		}
		
		inventory.setSelectedHotbarSlot(oldSlot);
		remainingBlocks.clear();
	}
	
	private void giveOrSelectItem(Item item)
	{
		if(InventoryUtils.selectItem(item, 9))
			return;
		
		if(!MC.player.hasInfiniteMaterials())
			return;
		
		Inventory inventory = MC.player.getInventory();
		int slot = inventory.getFreeSlot();
		if(!Inventory.isHotbarSlot(slot))
			slot = inventory.selected;
		
		ItemStack stack = new ItemStack(item);
		InventoryUtils.setCreativeStack(slot, stack);
	}
	
	private void loadSelectedTemplate()
	{
		status = Status.LOADING;
		Path path = templateSetting.getSelectedFile();
		
		try
		{
			template = AutoBuildTemplate.load(path);
			status = Status.IDLE;
			
		}catch(IOException | JsonException e)
		{
			Path fileName = path.getFileName();
			ChatUtils.error("无法加载模板 '" + fileName);
			
			String simpleClassName = e.getClass().getSimpleName();
			String message = e.getMessage();
			ChatUtils.message(simpleClassName + ": " + message);
			
			e.printStackTrace();
			setEnabled(false);
		}
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE;
	}
}
