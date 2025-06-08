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
			keyName = "useComplexTriggerForTimer",
			name = "Use Solver Start Condition",
			description = "Starts the Tick Timer using the same conditions as the Solver (Leviathan + 2 Hands + Meteor).",
			position = 2
	)
	default boolean useComplexTriggerForTimer() {
		return false;
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

	@ConfigItem(
			keyName = "sequenceDelays",
			name = "Sequence Tick Delays",
			description = "Define custom delays between sequence steps. Format: {key,delay1,delay2,...},{key,delay1,...}",
			position = 6
	)
	default String sequenceDelays() {
		return "{V1,4,4,4,4,2},{H1,4,4,4,4,2},{V2,4,4,3,2},{H2,4,4,3,2},{V3,4,4,4,4},{H3,4,4,4,4}"; // e.g., "{V1,2,3,0,2},{H1,4,4,4,4}"
	}

	@ConfigItem(
			keyName = "enablePrayerHelper",
			name = "Enable Prayer Helper",
			description = "Shows an overlay for the next expected prayer based on boss sounds.",
			position = 10
	)
	default boolean enablePrayerHelper() {
		return true; // Disabled by default
	}

	@ConfigItem(
			keyName = "linkPrayerHelperToSolver",
			name = "Link Prayer Helper to Solver",
			description = "If enabled, the prayer helper will only be active when the Sequence Solver is running.",
			position = 11
	)
	default boolean linkPrayerHelperToSolver() {
		return false;
	}

	@Range(
			min = 8,
			max = 24
	)
	@ConfigItem(
			keyName = "prayerHelperFontSize",
			name = "Prayer Helper Font Size",
			description = "Adjusts the size of the text in the Prayer Helper overlay.",
			position = 12
	)
	default int prayerHelperFontSize() {
		return 16; // Default font size
	}
}
