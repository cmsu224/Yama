package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("example")
public interface ExampleConfig extends Config
{
	// This enum will be our master switch
	enum OperatingMode {
		TICK_TIMER,
		SEQUENCE_SOLVER
	}

	@ConfigItem(
			keyName = "mode",
			name = "Operating Mode",
			description = "Choose the plugin's functionality.",
			position = 0
	)
	default OperatingMode mode() {
		return OperatingMode.TICK_TIMER;
	}

	// --- SETTINGS FOR TICK_TIMER MODE ---

	@ConfigItem(
			keyName = "npcId",
			name = "Boss NPC ID",
			description = "The ID of the boss to track.",
			position = 1
	)
	default int npcId() {
		return 0;
	}

	@ConfigItem(
			keyName = "startingTick",
			name = "Starting Tick",
			description = "The tick number the counter will start from when the boss spawns.",
			position = 2
	)
	@Range(min = -1000, max = 1000)
	default int startingTick() {
		return 1;
	}

	@ConfigItem(
			keyName = "cycleLength",
			name = "Repetition Cycle Ticks",
			description = "The number of ticks before the pattern repeats. Set to 0 to disable.",
			position = 3
	)
	default int cycleLength() {
		return 0;
	}

	@ConfigItem(
			keyName = "tileAndTickData",
			name = "Tile and Tick Data (Tick Timer Mode)",
			description = "Data for tick-based highlights. Format: tick,x,y,plane;...",
			position = 4
	)
	default String tileAndTickData() {
		return "";
	}

	// --- SETTINGS FOR SEQUENCE_SOLVER MODE ---

	@ConfigItem(
			keyName = "startSolverOnMeteor",
			name = "Start Solver on First Meteor",
			description = "Starts the sequence when the first meteor graphics object appears.",
			position = 5
	)
	default boolean startSolverOnMeteor() {
		return true;
	}
}
