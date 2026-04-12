/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.TextFieldSetting;

public final class ModelSettings
{
	public final EnumSetting<OpenAiModel> openAiModel =
		new EnumSetting<>("OpenAI 模型", "用于OpenAI API调用的模型",
			OpenAiModel.values(), OpenAiModel.GPT_4O_2024_08_06);
	
	public enum OpenAiModel
	{
		GPT_4O_2024_08_06("gpt-4o-2024-08-06", true),
		GPT_4O_2024_05_13("gpt-4o-2024-05-13", true),
		GPT_4O_MINI_2024_07_18("gpt-4o-mini-2024-07-18", true),
		GPT_4_TURBO_2024_04_09("gpt-4-turbo-2024-04-09", true),
		GPT_4_0125_PREVIEW("gpt-4-0125-preview", true),
		GPT_4_1106_PREVIEW("gpt-4-1106-preview", true),
		GPT_4_0613("gpt-4-0613", true),
		GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125", true),
		GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", true),
		GPT_3_5_TURBO_INSTRUCT("gpt-3.5-turbo-instruct", false),
		DAVINCI_002("davinci-002", false),
		BABBAGE_002("babbage-002", false);
		
		private final String name;
		private final boolean chat;
		
		private OpenAiModel(String name, boolean chat)
		{
			this.name = name;
			this.chat = chat;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public boolean isChatModel()
		{
			return chat;
		}
	}
	
	public final SliderSetting maxTokens = new SliderSetting("最大 tokens",
		"模型能生成的最大Token\n\n更高的值允许模型预测更长的聊天消息, 但也增加生成预测的时间\n\n默认值16在大多数用例中是合适的",
		16, 1, 100, 1, ValueDisplay.INTEGER);
	
	public final SliderSetting temperature = new SliderSetting("温度",
		"控制模特的创造力和随机性\n\n分值越高, 完成会更有创意, 有时甚至有些无厘头, 而分值越低,完成就会越无聊", 1, 0, 2,
		0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting topP = new SliderSetting("Top P",
		"温度的替代品, 通过只允许模型从最可能的标记中选择, 使模型不那么随机\n\n值为100%则禁用该功能, 允许模型从所有标记中选择", 1,
		0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	public final SliderSetting presencePenalty = new SliderSetting("同义词惩罚",
		"选择已出现在聊天记录中的标记会受到惩罚\n\n积极值鼓励模型使用同义词并讨论不同话题, 负值则鼓励模型反复重复同一个词", 0, -2, 2,
		0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting frequencyPenalty = new SliderSetting("频率惩罚",
		"类似于存在惩罚, 但基于Token在聊天记录中出现的频率\n\n正值鼓励模型使用同义词并讨论不同话题, 负值则鼓励模型重复已有的聊天消息",
		0, -2, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final EnumSetting<StopSequence> stopSequence = new EnumSetting<>(
		"停止序列",
		"控制自动补全如何检测聊天消息结束\n\n\u00a7l换行值是默认值, 大多数语言模型推荐\n\n\u00a7l下一条消息\u00a7r在某些代码优化的语言模型中表现更好, 这些模型倾向于在聊天消息中间插入换行",
		StopSequence.values(), StopSequence.LINE_BREAK);
	
	public enum StopSequence
	{
		LINE_BREAK("换行", "\n"),
		NEXT_MESSAGE("下一条消息", "\n<");
		
		private final String name;
		private final String sequence;
		
		private StopSequence(String name, String sequence)
		{
			this.name = name;
			this.sequence = sequence;
		}
		
		public String getSequence()
		{
			return sequence;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public final SliderSetting contextLength = new SliderSetting("上下文长度",
		"控制聊天记录中用于生成预测的消息数量\n\n更高的值提升预测质量, 但也增加生成预测所需的时间, 以及成本 (如OpenAI的API) 或RAM使用 (自架模型) 的使用",
		10, 0, 100, 1, ValueDisplay.INTEGER);
	
	public final CheckboxSetting filterServerMessages = new CheckboxSetting(
		"过滤服务器消息",
		"只向模型显示玩家创建的聊天消息\n\n这可以帮助你节省Token, 并在低上下文长度下获得更多收益, 但也意味着模型不会知道玩家加入、离开、死亡等事件",
		false);
	
	public final TextFieldSetting customModel = new TextFieldSetting("自定义模型",
		"如果设置好, 该模型将取代\"OpenAI 模型\"设置中指定的模型\n\n如果你有一个经过精细调校的 OpenAI 模型, 或者使用兼容 OpenAI 但提供不同模型的自定义端点, 请使用此模型",
		"");
	
	public final EnumSetting<CustomModelType> customModelType =
		new EnumSetting<>("自定义模型类型",
			"自定义模型是应该使用聊天端点还是传统端点\n\n如果\"自定义模型\"留空, 这个设置将被忽略",
			CustomModelType.values(), CustomModelType.CHAT);
	
	public enum CustomModelType
	{
		CHAT("聊天", true),
		LEGACY("传统", false);
		
		private final String name;
		private final boolean chat;
		
		private CustomModelType(String name, boolean chat)
		{
			this.name = name;
			this.chat = chat;
		}
		
		public boolean isChat()
		{
			return chat;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public final TextFieldSetting openaiChatEndpoint =
		new TextFieldSetting("OpenAI 聊天端点", "OpenAI 聊天完成 API 的端点",
			"https://api.openai.com/v1/chat/completions");
	
	public final TextFieldSetting openaiLegacyEndpoint =
		new TextFieldSetting("OpenAI 传统端点", "OpenAI 传统补全 API 的端点",
			"https://api.openai.com/v1/completions");
	
	private final List<Setting> settings =
		Collections.unmodifiableList(Arrays.asList(openAiModel, maxTokens,
			temperature, topP, presencePenalty, frequencyPenalty, stopSequence,
			contextLength, filterServerMessages, customModel, customModelType,
			openaiChatEndpoint, openaiLegacyEndpoint));
	
	public void forEach(Consumer<Setting> action)
	{
		settings.forEach(action);
	}
}
