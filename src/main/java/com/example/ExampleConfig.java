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
		return 14176;
	}

	@ConfigItem(
			keyName = "useComplexTriggerForTimer",
			name = "Use Solver Start Condition",
			description = "Starts the Tick Timer using the same conditions as the Solver (Leviathan + 2 Hands + Meteor).",
			position = 2
	)
	default boolean useComplexTriggerForTimer() {
		return true;
	}

	@ConfigItem(
			keyName = "startingTick",
			name = "Starting Tick",
			description = "The tick number the counter will start from when the boss spawns.",
			position = 2
	)
	@Range(min = -1000, max = 1000)
	default int startingTick() {
		return -18;
	}

	@ConfigItem(
			keyName = "cycleLength",
			name = "Repetition Cycle Ticks",
			description = "The number of ticks before the pattern repeats. Set to 0 to disable.",
			position = 3
	)
	default int cycleLength() {
		return 70;
	}

	@ConfigItem(
			keyName = "tileAndTickData",
			name = "Tile and Tick Data (Tick Timer Mode)",
			description = "Data for tick-based highlights. Format: tick,x,y,plane;...",
			position = 4
	)
	default String tileAndTickData() {
		return "1,1511,10079,0;\n" +
				"3,1509,10077,0;\n" +
				"9,1509,10079,0;\n" +
				"10,1511,10077,0;\n" +
				"12,1510,10078,0;\n" +
				"14,1508,10082,0;\n" +
				"16,1510,10082,0;\n" +
				"17,1504,10082,0;\n" +
				"19,1496,10082,0;\n" +
				"21,1506,10080,0;\n" +
				"23,1504,10078,0;\n" +
				"24,1503,10073,0;\n" +
				"64,1506,10073,0;\n" +
				"65,1508,10075,0;\n" +
				"67,1511,10079,0;";
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
		return "{V1,2,3,3,2,2,1,4},{V2,2,3,1,1,1,3,1,2},{V3,3,3,3,3,4},{H1,2,3,3,3,2,1,4},{H2,2,3,2,1,2,4,1,4},{H3,2,3,3,3,4}"; // e.g., "{V1,2,3,0,2},{H1,4,4,4,4}"
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
		return true;
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
		return 24; // Default font size
	}
}
