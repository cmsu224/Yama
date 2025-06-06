package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("example")
public interface ExampleConfig extends Config
{
	@ConfigItem(
			keyName = "npcId",
			name = "Boss NPC ID",
			description = "The ID of the boss to track."
	)
	default int npcId() {
		return 0;
	}

	@ConfigItem(
			keyName = "tileAndTickData",
			name = "Tile and Tick Data",
			description = "Data for tiles to highlight. Format: tick,x,y,plane;tick,x,y,plane"
	)
	default String tileAndTickData() {
		return "";
	}

	@ConfigItem(
			keyName = "cycleLength",
			name = "Repetition Cycle Ticks",
			description = "The number of ticks before the pattern repeats. Set to 0 to disable repetition."
	)
	default int cycleLength() {
		return 0; // Default is 0, meaning the cycle is off.
	}

	@Range(
			min = -1000,
			max = 1000
	)
	@ConfigItem(
			keyName = "startingTick",
			name = "Starting Tick",
			description = "The tick number the counter will start from when the boss spawns. Can be negative."
	)
	default int startingTick() {
		return 1;
	}
}
