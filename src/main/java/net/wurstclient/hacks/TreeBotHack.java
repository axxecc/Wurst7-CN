/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.treebot.Tree;
import net.wurstclient.hacks.treebot.TreeBotUtils;
import net.wurstclient.settings.FacingSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;

@SearchTags({"tree bot"})
@DontSaveState
public final class TreeBotHack extends Hack
	implements UpdateListener, RenderListener
{
	private final SliderSetting range = new SliderSetting("范围",
		"自动砍树可以到达多远来破坏方块", 4.5, 1, 6, 0.05,
		ValueDisplay.DECIMAL);
	
	private final FacingSetting facing = FacingSetting.withoutPacketSpam(
		"自动砍树在破坏原木和树叶时应如何面向它们\n\n\u00a7l关闭\u00a7r - 完全不面向方块，会被反作弊插件检测\n\n\u00a7l服务器\u00a7r - 在服务器端面向方块，同时仍允许您在客户端自由移动镜头\n\n\u00a7l客户端\u00a7r - 通过在客户端移动镜头面向方块。这是最合法的选项");
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.SERVER);
	
	private TreeFinder treeFinder;
	private AngleFinder angleFinder;
	private TreeBotPathProcessor processor;
	private Tree tree;
	
	private BlockPos currentBlock;
	private final OverlayRenderer overlay = new OverlayRenderer();
	
	public TreeBotHack()
	{
		super("自动砍树");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(facing);
		addSetting(swingHand);
	}
	
	@Override
	public String getRenderName()
	{
		if(treeFinder != null && !treeFinder.isDone() && !treeFinder.isFailed())
			return getName() + " [搜索中]";
		
		if(processor != null && !processor.isDone())
			return getName() + " [前往中]";
		
		if(tree != null && !tree.getLogs().isEmpty())
			return getName() + " [砍树中]";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		treeFinder = new TreeFinder();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		PathProcessor.releaseControls();
		treeFinder = null;
		angleFinder = null;
		processor = null;
		tree = null;
		
		if(currentBlock != null)
		{
			MC.interactionManager.breakingBlock = true;
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		overlay.resetProgress();
	}
	
	@Override
	public void onUpdate()
	{
		if(treeFinder != null)
		{
			goToTree();
			return;
		}
		
		if(tree == null)
		{
			treeFinder = new TreeFinder();
			return;
		}
		
		tree.getLogs().removeIf(Predicate.not(TreeBotUtils::isLog));
		
		if(tree.getLogs().isEmpty())
		{
			tree = null;
			return;
		}
		
		if(angleFinder != null)
		{
			goToAngle();
			return;
		}
		
		if(breakBlocks(tree.getLogs()))
			return;
		
		if(angleFinder == null)
			angleFinder = new AngleFinder();
	}
	
	private void goToTree()
	{
		// find path
		if(!treeFinder.isDoneOrFailed())
		{
			PathProcessor.lockControls();
			treeFinder.findPath();
			return;
		}
		
		// process path
		if(processor != null && !processor.isDone())
		{
			processor.goToGoal();
			return;
		}
		
		PathProcessor.releaseControls();
		treeFinder = null;
	}
	
	private void goToAngle()
	{
		// find path
		if(!angleFinder.isDone() && !angleFinder.isFailed())
		{
			PathProcessor.lockControls();
			angleFinder.findPath();
			return;
		}
		
		// process path
		if(processor != null && !processor.isDone())
		{
			processor.goToGoal();
			return;
		}
		
		PathProcessor.releaseControls();
		angleFinder = null;
	}
	
	private boolean breakBlocks(ArrayList<BlockPos> blocks)
	{
		for(BlockPos pos : blocks)
			if(breakBlock(pos))
			{
				currentBlock = pos;
				return true;
			}
		
		return false;
	}
	
	private boolean breakBlock(BlockPos pos)
	{
		BlockBreakingParams params = BlockBreaker.getBlockBreakingParams(pos);
		if(params == null || !params.lineOfSight()
			|| params.distanceSq() > range.getValueSq())
			return false;
		
		// select tool
		WURST.getHax().autoToolHack.equipBestTool(pos, false, true, 0);
		
		// face block
		facing.getSelected().face(params.hitVec());
		
		// damage block and swing hand
		if(MC.interactionManager.updateBlockBreakingProgress(pos,
			params.side()))
			swingHand.swing(Hand.MAIN_HAND);
		
		// update progress
		overlay.updateProgress();
		
		return true;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		
		if(treeFinder != null)
			treeFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
				pathCmd.isDepthTest());
		
		if(angleFinder != null)
			angleFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
				pathCmd.isDepthTest());
		
		if(tree != null)
			tree.draw(matrixStack);
		
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
	
	private ArrayList<BlockPos> getNeighbors(BlockPos pos)
	{
		return BlockUtils
			.getAllInBoxStream(pos.add(-1, -1, -1), pos.add(1, 1, 1))
			.filter(TreeBotUtils::isLog)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private abstract class TreeBotPathFinder extends PathFinder
	{
		public TreeBotPathFinder(BlockPos goal)
		{
			super(goal);
		}
		
		public TreeBotPathFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		public void findPath()
		{
			think();
			
			if(isDoneOrFailed())
			{
				// set processor
				formatPath();
				processor = new TreeBotPathProcessor(this);
			}
		}
		
		public boolean isDoneOrFailed()
		{
			return isDone() || isFailed();
		}
		
		public abstract void reset();
	}
	
	private class TreeBotPathProcessor
	{
		private final TreeBotPathFinder pathFinder;
		private final PathProcessor processor;
		
		public TreeBotPathProcessor(TreeBotPathFinder pathFinder)
		{
			this.pathFinder = pathFinder;
			processor = pathFinder.getProcessor();
		}
		
		public void goToGoal()
		{
			if(!pathFinder.isPathStillValid(processor.getIndex())
				|| processor.getTicksOffPath() > 20)
			{
				pathFinder.reset();
				return;
			}
			
			if(processor.canBreakBlocks() && breakBlocks(getLeavesOnPath()))
				return;
			
			processor.process();
		}
		
		private ArrayList<BlockPos> getLeavesOnPath()
		{
			List<PathPos> path = pathFinder.getPath();
			path = path.subList(processor.getIndex(), path.size());
			
			return path.stream().flatMap(pos -> Stream.of(pos, pos.up()))
				.distinct().filter(TreeBotUtils::isLeaves)
				.collect(Collectors.toCollection(ArrayList::new));
		}
		
		public final boolean isDone()
		{
			return processor.isDone();
		}
	}
	
	private class TreeFinder extends TreeBotPathFinder
	{
		public TreeFinder()
		{
			super(BlockPos.ofFloored(WurstClient.MC.player.getPos()));
		}
		
		public TreeFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		@Override
		protected boolean isMineable(BlockPos pos)
		{
			return TreeBotUtils.isLeaves(pos);
		}
		
		@Override
		protected boolean checkDone()
		{
			return done = isNextToTreeStump(current);
		}
		
		private boolean isNextToTreeStump(PathPos pos)
		{
			return isTreeStump(pos.north()) || isTreeStump(pos.east())
				|| isTreeStump(pos.south()) || isTreeStump(pos.west());
		}
		
		private boolean isTreeStump(BlockPos pos)
		{
			if(!TreeBotUtils.isLog(pos))
				return false;
			
			if(TreeBotUtils.isLog(pos.down()))
				return false;
			
			analyzeTree(pos);
			
			// ignore large trees (for now)
			if(tree.getLogs().size() > 6)
				return false;
			
			return true;
		}
		
		private void analyzeTree(BlockPos stump)
		{
			ArrayList<BlockPos> logs = new ArrayList<>(Arrays.asList(stump));
			ArrayDeque<BlockPos> queue = new ArrayDeque<>(Arrays.asList(stump));
			
			for(int i = 0; i < 1024; i++)
			{
				if(queue.isEmpty())
					break;
				
				BlockPos current = queue.pollFirst();
				
				for(BlockPos next : getNeighbors(current))
				{
					if(logs.contains(next))
						continue;
					
					logs.add(next);
					queue.add(next);
				}
			}
			
			tree = new Tree(stump, logs);
		}
		
		@Override
		public void reset()
		{
			treeFinder = new TreeFinder(treeFinder);
		}
	}
	
	private class AngleFinder extends TreeBotPathFinder
	{
		public AngleFinder()
		{
			super(BlockPos.ofFloored(WurstClient.MC.player.getPos()));
			setThinkSpeed(512);
			setThinkTime(1);
		}
		
		public AngleFinder(TreeBotPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		@Override
		protected boolean isMineable(BlockPos pos)
		{
			return TreeBotUtils.isLeaves(pos);
		}
		
		@Override
		protected boolean checkDone()
		{
			return done = hasAngle(current);
		}
		
		private boolean hasAngle(PathPos pos)
		{
			double rangeSq = range.getValueSq();
			ClientPlayerEntity player = WurstClient.MC.player;
			Vec3d eyes = Vec3d.ofBottomCenter(pos).add(0,
				player.getEyeHeight(player.getPose()), 0);
			
			for(BlockPos log : tree.getLogs())
			{
				BlockBreakingParams params =
					BlockBreaker.getBlockBreakingParams(eyes, log);
				
				if(params != null && params.lineOfSight()
					&& params.distanceSq() <= rangeSq)
					return true;
			}
			
			return false;
		}
		
		@Override
		public void reset()
		{
			angleFinder = new AngleFinder(angleFinder);
		}
	}
}
