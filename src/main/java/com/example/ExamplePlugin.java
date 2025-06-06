package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import java.util.Collection;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import java.util.ArrayList;
import java.util.List;

@Slf4j

@PluginDescriptor(
		name = "Yama Tick Timer",
		description = "Displays a tick timer and highlights tiles when a specific boss spawns.",
		tags = {"boss", "timer", "tile", "yama"}
)
public class ExamplePlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ExampleOverlay tileOverlay;

	@Inject
	private BossTickCounterPanel counterPanel;

	private int tickCounter = 0;
	private boolean bossSpawned = false;
	private final List<TickTileInfo> tilesToHighlight = new ArrayList<>();

	@Getter
	private final List<HighlightedTile> highlightedTiles = new ArrayList<>();

	// 2. ADD PUBLIC GETTERS FOR THE OVERLAY TO USE
	public boolean isBossSpawned() {
		return bossSpawned;
	}

	public int getTickCounter() {
		return tickCounter;
	}

	@Override
	protected void startUp() throws Exception {
		log.info("Boss Tick Timer started!");
		parseTileAndTickData();

		// 3. ADD BOTH OVERLAYS TO THE MANAGER
		overlayManager.add(tileOverlay);
		overlayManager.add(counterPanel);
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Boss Tick Timer stopped!");

		// 4. REMOVE BOTH OVERLAYS ON SHUTDOWN
		overlayManager.remove(tileOverlay);
		overlayManager.remove(counterPanel);

		highlightedTiles.clear();
		tilesToHighlight.clear();
		bossSpawned = false;
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

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned) {
		NPC npc = npcSpawned.getNpc();
		if (npc.getId() == config.npcId()) {
			bossSpawned = true;
			tickCounter = config.startingTick();; // Reset counter on spawn
			highlightedTiles.clear();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned) {
	   if (npcDespawned.getNpc().getId() == config.npcId()) {
	       bossSpawned = false;
	   }
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!bossSpawned) {
			return;
		}

		// The counter still increments on every tick
		tickCounter++;

		// Get the cycle length from the config
		int cycleLength = config.cycleLength();
		if (cycleLength > 0 && tickCounter >= cycleLength) {
			tickCounter = 0;
		}

		highlightedTiles.forEach(HighlightedTile::tick);
		highlightedTiles.removeIf(tile -> tile.getTicksRemaining() < 1);

		// Only check for and create NEW highlights if the tick counter is in the positive.
		if (tickCounter > 0) {
			for (TickTileInfo info : tilesToHighlight) {
				if (tickCounter == info.getTick()) {
					Collection<WorldPoint> instancedPoints = WorldPoint.toLocalInstance(client, info.getWorldPoint());
					for (WorldPoint instancedPoint : instancedPoints) {
						highlightedTiles.add(new HighlightedTile(instancedPoint));
					}
				}
			}
		}
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}