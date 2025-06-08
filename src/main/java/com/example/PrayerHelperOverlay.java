// Create this new file: PrayerHelperOverlay.java
package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;
import java.awt.Font;

class PrayerHelperOverlay extends OverlayPanel {
    private final Client client;
    private final ExamplePlugin plugin;
    private final ExampleConfig config;

    @Inject
    private PrayerHelperOverlay(Client client, ExamplePlugin plugin, ExampleConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);

        // --- THIS IS THE FIX ---
        // Part 1: Make the panel resizable
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // --- THIS IS THE MODIFIED LOGIC ---
        // 1. Is the feature enabled in the config?
        if (!config.enablePrayerHelper()) {
            return null;
        }
        // 2. If linked, is at least ONE mode active?
        if (config.linkPrayerHelperToSolver() && !plugin.isTickTimerActive() && !plugin.isSolverActive()) {
            return null;
        }

        // --- The rest of the method is unchanged ---
        Prayer expectedPrayer = plugin.getExpectedPrayer();
        if (expectedPrayer == null || client.isPrayerActive(expectedPrayer)) {
            return null;
        }

        if (expectedPrayer == Prayer.PROTECT_FROM_MAGIC) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("MAGE")
                    .color(Color.WHITE)
                    .build());
            panelComponent.setBackgroundColor(new Color(0, 78, 255, 150));
        } else if (expectedPrayer == Prayer.PROTECT_FROM_MISSILES) {
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("RANGE")
                    .color(Color.WHITE)
                    .build());
            panelComponent.setBackgroundColor(new Color(0, 255, 0, 150));
        }

        return super.render(graphics);
    }
}