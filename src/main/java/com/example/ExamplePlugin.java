package com.example;


import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
		name = "Boss Helper Example",
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
	// Note: Inject other overlays (like counter panels) here if you use them.

	// --- Constants for Solver Mode ---
	private static final int METEOR_GRAPHICS_ID = 3262;
	private static final int SET_1_START_TICK = 1;
	private static final int SET_2_START_TICK = 28;
	private static final int SET_3_START_TICK = 56;

	// --- State for BOTH modes ---
	@Getter private final List<HighlightedTile> highlightedTiles = new ArrayList<>();

	// --- State for TICK_TIMER mode ---
	private boolean tickTimerActive = false;
	@Getter private int tickCounter = 0;
	private final List<TickTileInfo> tilesToHighlight = new ArrayList<>();

	// --- State for SEQUENCE_SOLVER mode ---
	private boolean solverActive = false;
	private int currentSet = 0;
	private String currentPattern = null;
	private int stepInSet = 0;
	private final List<WorldPoint> graphicsObjectsThisTick = new ArrayList<>();

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
		parseTileAndTickData();
	}

	@Override
	protected void shutDown() throws Exception {
		resetAllModes();
		overlayManager.remove(tileOverlay);
		overlayManager.remove(counterPanel); // <-- ADD THIS LINE
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
	}

	// --- EVENT ROUTERS ---
	// These methods check the config and route events to the correct logic handler.

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		if (config.mode() == ExampleConfig.OperatingMode.TICK_TIMER) {
			handleTickTimerNpcSpawn(event);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
		if (config.mode() == ExampleConfig.OperatingMode.SEQUENCE_SOLVER) {
			handleSolverGraphicsCreated(event);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Countdown highlights are shared across both modes
		highlightedTiles.forEach(HighlightedTile::tick);
		highlightedTiles.removeIf(tile -> tile.getTicksRemaining() < 1);

		// Route to the correct logic for this tick
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
		if (!tickTimerActive) return;
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
		GraphicsObject graphicsObject = event.getGraphicsObject();
		if (graphicsObject.getId() == METEOR_GRAPHICS_ID) {
			// Activate the solver on the first meteor if the setting is enabled
			if (!solverActive && config.startSolverOnMeteor()) {
				resetAllModes();
				solverActive = true;
				tickCounter = 1; // Start the master 70-tick timer
			}
			if (solverActive) {
				LocalPoint lp = graphicsObject.getLocation();
				graphicsObjectsThisTick.add(WorldPoint.fromLocal(client, lp));
			}
		}
	}

	private void handleSolverGameTick() {
		if (!solverActive) return;

		// Advance the master timer and handle full cycle reset
		tickCounter++;
		if (tickCounter > 70) {
			tickCounter = 1;
			currentSet = 0;
			currentPattern = null;
		}

		// Determine which set (1, 2, or 3) we are in based on the master timer
		int previousSet = currentSet;
		if (tickCounter >= SET_1_START_TICK && tickCounter < SET_2_START_TICK) {
			currentSet = 1;
		} else if (tickCounter >= SET_2_START_TICK && tickCounter < SET_3_START_TICK) {
			currentSet = 2;
		} else if (tickCounter >= SET_3_START_TICK) {
			currentSet = 3;
		}

		// If we just entered a new set, reset its specific state
		if (currentSet != previousSet) {
			currentPattern = null;
			stepInSet = 0;
		}

		// If any meteors were detected in the last tick, process them
		if (!graphicsObjectsThisTick.isEmpty()) {
			if (currentPattern == null) { // This is the first meteor spawn of a new set
				if (graphicsObjectsThisTick.size() >= 2) {
					int firstX = graphicsObjectsThisTick.get(0).getX();
					boolean isVertical = graphicsObjectsThisTick.stream().allMatch(p -> p.getX() == firstX);
					currentPattern = isVertical ? "V" : "H";
					stepInSet = 1;
				}
			} else { // This is a subsequent diagonal spawn in the current set
				stepInSet++;
			}

			// Construct the sequence key (e.g., "V1", "H2") and trigger the highlight
			String sequenceKey = currentPattern + currentSet;
			triggerSolverHighlight(sequenceKey, stepInSet - 1); // Use 0-based index for lists
		}
		graphicsObjectsThisTick.clear();
	}

	private void triggerSolverHighlight(String sequenceKey, int index) {
		List<Integer> sequence = SequenceData.SEQUENCES.get(sequenceKey);
		if (sequence != null && index < sequence.size()) {
			int tileNumber = sequence.get(index);
			// 1. Get the base world point from our data file
			WorldPoint baseTileToHighlight = SequenceData.TILE_MAP.get(tileNumber);

			if (baseTileToHighlight != null) {
				// --- THIS IS THE FIX ---
				// 2. Translate the base point into the point(s) that exist in our current instance
				Collection<WorldPoint> instancedPoints = WorldPoint.toLocalInstance(client, baseTileToHighlight);

				// 3. Add a highlight for each translated point found
				for (WorldPoint instancedPoint : instancedPoints) {
					// Prevent highlighting the same tile back-to-back instantly
					highlightedTiles.removeIf(t -> t.getWorldPoint().equals(instancedPoint));
					highlightedTiles.add(new HighlightedTile(instancedPoint));
				}
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
					log.warn("Invalid tile and tick data format: {}", pair);
				}
			}
		}
	}
}