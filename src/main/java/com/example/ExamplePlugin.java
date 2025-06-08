package com.example;


import com.google.inject.Provides;

import java.util.*;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.AreaSoundEffectPlayed;

@Slf4j
@PluginDescriptor(
		name = "YAMA",
		description = "A dual-mode helper for boss mechanics.",
		tags = {"boss", "timer", "pvm", "helper", "example"}
)
public class ExamplePlugin extends Plugin {

	// --- Injections ---
	@Inject private Client client;
	@Inject private ExampleConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private ExampleOverlay tileOverlay;
	@Inject private BossTickCounterPanel counterPanel; // <-- ADD THIS LINE
	@Inject private PrayerWidgetOverlay prayerWidgetOverlay;
	@Inject private PrayerHelperOverlay prayerHelperOverlay;
	@Inject private ChatCommandManager chatCommandManager;
	@Inject private ChatMessageManager chatMessageManager;
	// Note: Inject other overlays (like counter panels) here if you use them.

	// --- Constants for Solver Mode ---
	private static final int METEOR_GRAPHICS_ID = 3262;
	private static final int SET_1_START_TICK = 1;
	private static final int SET_2_START_TICK = 19;
	private static final int SET_3_START_TICK = 42;

	@Getter private Prayer expectedPrayer = null;
	private int ticksUntilPrayerSwitch = -1;
	private int prayerOverlayMaxTicks = -1;

	// --- State for BOTH modes ---
	@Getter private final List<HighlightedTile> highlightedTiles = new ArrayList<>();

	private int nextStepDelayTicks = -1;
	// --- ADD A NEW MAP to store your parsed delay data ---
	private final Map<String, List<Integer>> sequenceDelays = new HashMap<>();

	// --- State for TICK_TIMER mode ---
	private boolean tickTimerActive = false;
	@Getter private int tickCounter = 0;
	private final List<TickTileInfo> tilesToHighlight = new ArrayList<>();
	private final Map<Integer, WorldPoint> instancedTileMap = new HashMap<>();

	// --- State for SEQUENCE_SOLVER mode ---
	private boolean solverActive = false;
	private int currentSet = 0;
	private String currentPattern = null;
	private int stepInSet = 0;
	private final List<WorldPoint> graphicsObjectsThisTick = new ArrayList<>();
	@Getter private int count3266 = 0;
	@Getter private int count3265 = 0;
	private int instancedRowY = -1;
	private int instancedStartX = -1;
	private int instancedEndX = -1;

	private static final WorldPoint METEOR_TRIGGER_1 = new WorldPoint(1511, 10082, 0);
	private static final WorldPoint HIGHLIGHT_TARGET_1 = new WorldPoint(1509, 10078, 0);
	private static final WorldPoint METEOR_TRIGGER_2 = new WorldPoint(1514, 10079, 0);
	private static final WorldPoint HIGHLIGHT_TARGET_2 = new WorldPoint(1510, 10077, 0);

	private int handsSpawnedCounter = 0;
	private boolean isReadyForSequence = false;

	// --- Getters for Overlays ---
	public boolean isTickTimerActive() { return tickTimerActive; }
	public boolean isSolverActive() { return solverActive; }
	public int getCurrentSet() { return currentSet; }
	public String getCurrentPattern() { return currentPattern; }
	public int getStepInSet() { return stepInSet; }

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(tileOverlay);
		overlayManager.add(counterPanel); // <-- ADD THIS LINE
		overlayManager.add(prayerHelperOverlay);
		//overlayManager.add(prayerWidgetOverlay);
		parseTileAndTickData();
		parseSequenceDelays();
		for (String sequenceKey : SequenceData.SEQUENCES.keySet()) {
			chatCommandManager.registerCommandAsync("!" + sequenceKey.toLowerCase(), this::onDebugSequenceCommand);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		resetAllModes();
		overlayManager.remove(tileOverlay);
		overlayManager.remove(counterPanel); // <-- ADD THIS LINE
		overlayManager.remove(prayerHelperOverlay);
		//overlayManager.remove(prayerWidgetOverlay);

		for (String sequenceKey : SequenceData.SEQUENCES.keySet()) {
			chatCommandManager.unregisterCommand("!" + sequenceKey.toLowerCase());
		}
	}

	private void onDebugSequenceCommand(ChatMessage chatMessage, String message) {
		String command = chatMessage.getMessage().split(" ")[0];
		String sequenceKey = command.substring(1).toUpperCase();
		if (!SequenceData.SEQUENCES.containsKey(sequenceKey)) {
			sendChatMessage("Sequence key '" + sequenceKey + "' not found.");
			return;
		}

		sendChatMessage("Manually starting sequence: " + sequenceKey);
		resetAllModes(); // This correctly sets tickCounter to 0 initially
		solverActive = true;

		this.currentPattern = String.valueOf(sequenceKey.charAt(0));
		this.currentSet = Character.getNumericValue(sequenceKey.charAt(1));
		this.stepInSet = 1;

		// --- THIS IS THE FIX ---
		// Synchronize the master tick counter with the debugged set.
		if (this.currentSet == 1) {
			this.tickCounter = SET_1_START_TICK;
		} else if (this.currentSet == 2) {
			this.tickCounter = SET_2_START_TICK;
		} else if (this.currentSet == 3) {
			this.tickCounter = SET_3_START_TICK;
		}

		// Trigger the first highlight and set the timer for the next step.
		this.nextStepDelayTicks = triggerSolverHighlight(sequenceKey, 0);
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ExampleConfig.class);
	}

	private void resetAllModes() {
		tickTimerActive = false;
		solverActive = false;
		tickCounter = 0;
		currentSet = 0;
		currentPattern = null;
		stepInSet = 0;
		highlightedTiles.clear();
		graphicsObjectsThisTick.clear();
		nextStepDelayTicks = -1;
		expectedPrayer = null;
		ticksUntilPrayerSwitch = -1;
		prayerOverlayMaxTicks = -1;
		handsSpawnedCounter = 0;
		isReadyForSequence = false;
	}

	// --- EVENT ROUTERS ---
	// These methods check the config and route events to the correct logic handler.

	@Subscribe
	public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed event) {
		// --- THIS IS THE MODIFIED LOGIC ---
		// Check if the helper is enabled first.
		if (!config.enablePrayerHelper()) {
			return;
		}

		// If it's linked, check that at least ONE mode is active before proceeding.
		if (config.linkPrayerHelperToSolver() && !isTickTimerActive() && !isSolverActive()) {
			return;
		}

		boolean soundRecognized = false;
		// Mage Attack Sound
		if (event.getSoundId() == 10263) {
			expectedPrayer = Prayer.PROTECT_FROM_MAGIC;
			soundRecognized = true;
		}
		// Range Attack Sound
		else if (event.getSoundId() == 10272) {
			expectedPrayer = Prayer.PROTECT_FROM_MISSILES;
			soundRecognized = true;
		}

		// If we recognized a sound, start both timers
		if (soundRecognized) {
			ticksUntilPrayerSwitch = 4;
			prayerOverlayMaxTicks = 25;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		boolean shouldCountHands = (config.mode() == ExampleConfig.OperatingMode.SEQUENCE_SOLVER) ||
				(config.mode() == ExampleConfig.OperatingMode.TICK_TIMER && config.useComplexTriggerForTimer());

		if (shouldCountHands) {
			// We only care about the hands if a main boss is present
			boolean leviathanPresent = client.getNpcs().stream().anyMatch(npc -> npc.getId() == config.npcId());
			if (leviathanPresent && event.getNpc().getId() == 14180) {
				handsSpawnedCounter++;
				sendChatMessage("Hand spawned. Total count: {}" + handsSpawnedCounter);
				if (handsSpawnedCounter >= 2) {
					isReadyForSequence = true;
					sendChatMessage("Two hands have spawned. System is ready for meteors.");
				}
			}
		}

		// The original Tick Timer trigger only runs if the new option is OFF
		if (config.mode() == ExampleConfig.OperatingMode.TICK_TIMER && !config.useComplexTriggerForTimer()) {
			handleTickTimerNpcSpawn(event);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
		GraphicsObject go = event.getGraphicsObject();

		// --- NEW, SIMPLER UNCONDITIONAL LOGIC ---
		if (go.getId() == METEOR_GRAPHICS_ID) {
			// Get the full WorldPoint of the spawned meteor inside the instance
			WorldPoint spawnedLocation = WorldPoint.fromLocalInstance(client, go.getLocation());
			WorldPoint targetToHighlight = null;

			// Directly compare the spawned location to your specified instance coordinates
			if (spawnedLocation.equals(METEOR_TRIGGER_1)) {
				targetToHighlight = HIGHLIGHT_TARGET_1;
			} else if (spawnedLocation.equals(METEOR_TRIGGER_2)) {
				targetToHighlight = HIGHLIGHT_TARGET_2;
			}

			if (targetToHighlight != null) {
				// If we have a match, the target coordinate is already correct.
				// No translation is needed. Just add the highlight.
				Collection<WorldPoint> instancedPoints = WorldPoint.toLocalInstance(client, targetToHighlight);
				for (WorldPoint instancedPoint : instancedPoints) {
					highlightedTiles.add(new HighlightedTile(instancedPoint));
				}
			}
		}


		if (go.getId() == METEOR_GRAPHICS_ID) {
			handleSolverGraphicsCreated(event);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// --- PRAYER HELPER LOGIC ---
		// First, handle the 25-tick max lifetime timer
		if (prayerOverlayMaxTicks > 0) {
			prayerOverlayMaxTicks--;
		}
		if (prayerOverlayMaxTicks == 0) {
			// Max time expired, turn off the overlay completely
			expectedPrayer = null;
			ticksUntilPrayerSwitch = -1;
			prayerOverlayMaxTicks = -1;
		}

		// Second, handle the 4-tick switch timer
		if (ticksUntilPrayerSwitch > 0) {
			ticksUntilPrayerSwitch--;
		}
		if (ticksUntilPrayerSwitch == 0) {
			// Switch timer expired, so flip the expected prayer
			if (expectedPrayer == Prayer.PROTECT_FROM_MAGIC) {
				expectedPrayer = Prayer.PROTECT_FROM_MISSILES;
			} else {
				expectedPrayer = Prayer.PROTECT_FROM_MAGIC;
			}

			// When the prayer flips, reset the max lifetime for the new overlay
			prayerOverlayMaxTicks = 25;

			ticksUntilPrayerSwitch = -1; // Stop this short timer
		}

		// Countdown highlights are shared across both modes
		highlightedTiles.forEach(HighlightedTile::tick);
		highlightedTiles.removeIf(tile -> tile.getTicksRemaining() < 1);

		if (config.mode() == ExampleConfig.OperatingMode.TICK_TIMER) {
			handleTickTimerGameTick();
		} else if (config.mode() == ExampleConfig.OperatingMode.SEQUENCE_SOLVER) {
			handleSolverGameTick();
		}
	}

	// --- LOGIC FOR TICK_TIMER MODE ---

	private void handleTickTimerNpcSpawn(NpcSpawned event) {
		if (event.getNpc().getId() == config.npcId()) {
			resetAllModes();
			tickTimerActive = true;
			tickCounter = config.startingTick();
		}
	}

	private void handleTickTimerGameTick() {
		// First, check if the timer should even be running.
		if (!tickTimerActive) {
			return;
		}

		// --- THIS IS THE NEW LOGIC ---
		// On every tick that the timer IS active, we check if the boss is still present.
		boolean leviathanPresent = client.getNpcs().stream().anyMatch(npc -> npc.getId() == config.npcId());
		if (!leviathanPresent) {
			// If the boss is gone, reset everything and stop.
			sendChatMessage("Yama not found. Resetting all modes.");
			resetAllModes();
			return;
		}

		// --- The rest of the original logic runs if the boss is still present ---
		tickCounter++;
		int cycleLength = config.cycleLength();
		if (cycleLength > 0 && tickCounter >= cycleLength) {
			tickCounter = 0;
		}

		if (tickCounter > 0) {
			for (TickTileInfo info : tilesToHighlight) {
				if (tickCounter == info.getTick()) {
					Collection<WorldPoint> instancedPoints = WorldPoint.toLocalInstance(client, info.getWorldPoint());
					instancedPoints.forEach(point -> highlightedTiles.add(new HighlightedTile(point)));
				}
			}
		}
	}

	// --- LOGIC FOR SEQUENCE_SOLVER MODE ---

	private void handleSolverGraphicsCreated(GraphicsObjectCreated event) {
		// This method is now called for any meteor spawn.
		// We first check if the system is ready (2 hands have spawned).
		if (!isReadyForSequence) {
			return;
		}

		// --- NEW, UNIFIED ACTIVATION LOGIC ---
		// Check if we should activate the TICK TIMER mode
		if (config.mode() == ExampleConfig.OperatingMode.TICK_TIMER && config.useComplexTriggerForTimer() && !tickTimerActive) {
			sendChatMessage("Complex condition met. Starting Tick Timer.");
			tickTimerActive = true;
			tickCounter = config.startingTick();
			return; // Activation is done, exit the method for this event.
		}

		// Check if we should activate the SEQUENCE SOLVER mode
		if (config.mode() == ExampleConfig.OperatingMode.SEQUENCE_SOLVER && !solverActive && config.startSolverOnMeteor()) {
			sendChatMessage("Complex condition met. Starting Sequence Solver.");
			solverActive = true;
			tickCounter = 1;
		}

		// If the solver is active, collect the meteor's location for processing in onGameTick
		if (solverActive) {
			graphicsObjectsThisTick.add(WorldPoint.fromLocal(client, event.getGraphicsObject().getLocation()));
		}
	}

	private void handleSolverGameTick() {
		// Master "OFF Switch" to reset everything if the boss disappears
		boolean leviathanPresent = client.getNpcs().stream().anyMatch(npc -> npc.getId() == config.npcId());
		if (!leviathanPresent) {
			if (solverActive || isReadyForSequence) {
				sendChatMessage("Leviathan not found. Resetting solver state.");
				resetAllModes();
			}
			return;
		}

		// Guard clause to ensure the solver has been activated
		if (!solverActive) return;

		// --- ALL OF THE ORIGINAL, CRITICAL LOGIC WILL NOW RUN FIRST ---

		// Master 70-Tick Cycle Management
		tickCounter++;
		if (tickCounter > 70) {
			tickCounter = 1;
			currentPattern = null;
		}

		// Determine which Set we are in based on the master timer
		int previousSet = currentSet;
		if (tickCounter >= SET_1_START_TICK && tickCounter < SET_2_START_TICK) {
			currentSet = 1;
		} else if (tickCounter >= SET_2_START_TICK && tickCounter < SET_3_START_TICK) {
			currentSet = 2;
		} else if (tickCounter >= SET_3_START_TICK) {
			currentSet = 3;
		}

		if (currentSet != previousSet) {
			currentPattern = null;
			stepInSet = 0;
		}

		// Sequence Advancement Logic (based on tick delays)
		if (nextStepDelayTicks > 0) {
			nextStepDelayTicks--;
		}
		if (nextStepDelayTicks == 0) {
			stepInSet++;
			String sequenceKey = currentPattern + currentSet;
			nextStepDelayTicks = triggerSolverHighlight(sequenceKey, stepInSet - 1);
		}

		// Sequence Initiation Logic (based on meteor events)
		if (!graphicsObjectsThisTick.isEmpty() && currentPattern == null) {
			if (graphicsObjectsThisTick.size() >= 2) {
				int firstX = graphicsObjectsThisTick.get(0).getX();
				boolean isVertical = graphicsObjectsThisTick.stream().allMatch(p -> p.getX() == firstX);
				currentPattern = isVertical ? "V" : "H";
				stepInSet = 1;
				String sequenceKey = currentPattern + currentSet;
				nextStepDelayTicks = triggerSolverHighlight(sequenceKey, 0);
			}
		}

		// --- THE NEW COUNTING LOGIC IS MOVED HERE, TO THE END ---
		// This is much safer and will not interfere with the timer.
		count3266 = 0;
		count3265 = 0;

		for (GraphicsObject go : client.getTopLevelWorldView().getGraphicsObjects()) {
			// Check if it's one of the wave objects
			if (go.getId() == 3266) {
				// Use our new helper function to see if it's on the correct row
				//if (isWaveOnTargetRow(go)) {
					count3266++;
				//}
			} else if (go.getId() == 3265) {
				//if (isWaveOnTargetRow(go)) {
					count3265++;
				//}
			}
			//sendChatMessage("Count 3266: " + count3266 + " Count 3265: " + count3265);
		}

		// --- Cleanup ---
		graphicsObjectsThisTick.clear();
	}

	private int triggerSolverHighlight(String sequenceKey, int index) {
		List<Integer> sequence = SequenceData.SEQUENCES.get(sequenceKey);
		if (sequence == null || index >= sequence.size()) {
			return -1; // End of sequence
		}

		int tileNumber = sequence.get(index);
		WorldPoint baseTileToHighlight = SequenceData.TILE_MAP.get(tileNumber);

		if (baseTileToHighlight != null) {
			Collection<WorldPoint> instancedPoints = WorldPoint.toLocalInstance(client, baseTileToHighlight);
			for (WorldPoint instancedPoint : instancedPoints) {
				final WorldPoint finalTile = instancedPoint;
				highlightedTiles.removeIf(t -> t.getWorldPoint().equals(finalTile));
				highlightedTiles.add(new HighlightedTile(finalTile));
			}
		}

		// --- Look up the corresponding delay ---
		List<Integer> delays = sequenceDelays.get(sequenceKey);
		if (delays != null && index < delays.size()) {
			return delays.get(index); // Return the specified delay
		}

		return 1; // Return the default delay of 1 tick if none is specified
	}

	private void parseSequenceDelays() {
		sequenceDelays.clear();
		String rawInput = config.sequenceDelays().trim();
		if (rawInput.isEmpty()) {
			return;
		}

		// This parser handles the {key,d1,d2,...},{key,d1,...} format
		rawInput = rawInput.substring(1, rawInput.length() - 1); // Remove outer braces
		String[] sequences = rawInput.split("\\},\\{");

		for(String seq : sequences) {
			String[] parts = seq.split(",");
			if (parts.length > 1) {
				String key = parts[0].trim().toUpperCase();
				List<Integer> delays = new ArrayList<>();
				for (int i = 1; i < parts.length; i++) {
					try {
						delays.add(Integer.parseInt(parts[i].trim()));
					} catch (NumberFormatException e) {
						sendChatMessage("Invalid delay value in sequence delay config: {}" + parts[i]);
					}
				}
				sequenceDelays.put(key, delays);
			}
		}
	}

	private void parseTileAndTickData() {
		tilesToHighlight.clear();
		String configData = config.tileAndTickData().trim();
		if (configData.isEmpty()) {
			return;
		}

		String[] pairs = configData.split(";");
		for (String pair : pairs) {
			String[] parts = pair.split(",");
			if (parts.length == 4) {
				try {
					int tick = Integer.parseInt(parts[0].trim());
					int x = Integer.parseInt(parts[1].trim());
					int y = Integer.parseInt(parts[2].trim());
					int plane = Integer.parseInt(parts[3].trim());
					tilesToHighlight.add(new TickTileInfo(tick, new WorldPoint(x, y, plane)));
				} catch (NumberFormatException e) {
					sendChatMessage("Invalid tile and tick data format: {}" + pair);
				}
			}
		}
	}

	private boolean isWaveOnTargetRow(GraphicsObject go) {
		// These are the main-world coordinates of the row you want to check
		final int targetY = 10070;
		final int targetPlane = 0;
		final int startX = 1490;
		final int endX = 1513;

		// Get the object's location in the current scene
		LocalPoint localPoint = go.getLocation();
		sendChatMessage("Original Loc: " + go.getLocation().getX() + ", " + go.getLocation().getY());
		if (localPoint == null) {
			return false;
		}

		// --- THIS IS THE FIX for older API versions ---
		// Manually calculate the base world coordinate from the local point.
		// This achieves the same result as the newer fromLocalToWorld() method.
		int worldX = client.getBaseX() + localPoint.getSceneX();
		int worldY = client.getBaseY() + localPoint.getSceneY();
		sendChatMessage("worldX: " + worldX + ", worldY" + worldY);
		// Now, we can do a simple comparison.
		return worldY == targetY &&
				client.getPlane() == targetPlane &&
				worldX >= startX &&
				worldX <= endX;
	}

	void sendChatMessage(String message) {
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).value(message).build());
	}
}