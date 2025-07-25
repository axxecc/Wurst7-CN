/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.wurstclient.WurstClient;
import net.wurstclient.util.json.JsonException;

public final class KeybindList
{
	public static final Set<Keybind> DEFAULT_KEYBINDS = createDefaultKeybinds();
	private final ArrayList<Keybind> keybinds = new ArrayList<>();
	
	private final KeybindsFile keybindsFile;
	private final Path profilesFolder =
		WurstClient.INSTANCE.getWurstFolder().resolve("keybinds");
	
	public KeybindList(Path keybindsFile)
	{
		this.keybindsFile = new KeybindsFile(keybindsFile);
		this.keybindsFile.load(this);
	}
	
	public String getCommands(String key)
	{
		for(Keybind keybind : keybinds)
		{
			if(!key.equals(keybind.getKey()))
				continue;
			
			return keybind.getCommands();
		}
		
		return null;
	}
	
	public List<Keybind> getAllKeybinds()
	{
		return Collections.unmodifiableList(keybinds);
	}
	
	public void add(String key, String commands)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		keybinds.add(new Keybind(key, commands));
		keybinds.sort(null);
		keybindsFile.save(this);
	}
	
	public void setKeybinds(Set<Keybind> keybinds)
	{
		this.keybinds.clear();
		this.keybinds.addAll(keybinds);
		this.keybinds.sort(null);
		keybindsFile.save(this);
	}
	
	public void remove(String key)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		keybindsFile.save(this);
	}
	
	public void removeAll()
	{
		keybinds.clear();
		keybindsFile.save(this);
	}
	
	public Path getProfilesFolder()
	{
		return profilesFolder;
	}
	
	public ArrayList<Path> listProfiles()
	{
		if(!Files.isDirectory(profilesFolder))
			return new ArrayList<>();
		
		try(Stream<Path> files = Files.list(profilesFolder))
		{
			return files.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".json"))
				.collect(Collectors.toCollection(ArrayList::new));
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void loadProfile(String fileName) throws IOException, JsonException
	{
		keybindsFile.loadProfile(this, profilesFolder.resolve(fileName));
	}
	
	public void saveProfile(String fileName) throws IOException, JsonException
	{
		keybindsFile.saveProfile(this, profilesFolder.resolve(fileName));
	}
	
	private static Set<Keybind> createDefaultKeybinds()
	{
		Set<Keybind> set = new LinkedHashSet<>();
		addKB(set, "b", "快速破坏;快速放置");
		addKB(set, "c", "黑暗视觉");
		addKB(set, "g", "飞行");
		addKB(set, "semicolon", "快速Nuker");
		addKB(set, "h", "say /home");
		addKB(set, "j", "水上行走");
		addKB(set, "k", "多重光环");
		addKB(set, "n", "挖掘光环");
		addKB(set, "r", "杀戮光环");
		addKB(set, "right.shift", "navigator");
		addKB(set, "right.control", "clickgui");
		addKB(set, "u", "灵魂出窍");
		addKB(set, "x", "x-ray");
		addKB(set, "y", "保持潜行");
		return Collections.unmodifiableSet(set);
	}
	
	private static void addKB(Set<Keybind> set, String key, String cmds)
	{
		set.add(new Keybind("key.keyboard." + key, cmds));
	}
}
