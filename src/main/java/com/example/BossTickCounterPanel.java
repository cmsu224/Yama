package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class BossTickCounterPanel extends OverlayPanel {

    private final ExamplePlugin plugin;
    private final ExampleConfig config;

    @Inject
    private BossTickCounterPanel(ExamplePlugin plugin, ExampleConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT); // Position it in the top-left corner
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Only draw the panel if the boss has spawned and the counter is running
        if (!plugin.isBossSpawned()) {
            return null;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Boss Timer")
                .color(Color.CYAN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Tick:")
                .right(String.valueOf(plugin.getTickCounter()))
                .build());

        return super.render(graphics);
    }
}