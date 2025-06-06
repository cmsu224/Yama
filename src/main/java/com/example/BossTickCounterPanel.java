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
        // The panel's content will change based on the selected mode
        if (config.mode() == ExampleConfig.OperatingMode.TICK_TIMER) {
            renderTickTimerInfo();
        } else if (config.mode() == ExampleConfig.OperatingMode.SEQUENCE_SOLVER) {
            renderSequenceSolverInfo();
        }

        return super.render(graphics);
    }

    private void renderTickTimerInfo() {
        if (!plugin.isTickTimerActive()) {
            return; // Don't draw if the timer isn't running
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Boss Timer")
                .color(Color.CYAN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Tick:")
                .right(String.valueOf(plugin.getTickCounter()))
                .build());
    }

    private void renderSequenceSolverInfo() {
        if (!plugin.isSolverActive()) {
            return; // Don't draw if the solver isn't running
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Sequence Solver")
                .color(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Cycle Tick:")
                .right(String.valueOf(plugin.getTickCounter()))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Set:")
                .right(String.valueOf(plugin.getCurrentSet()))
                .build());

        String pattern = plugin.getCurrentPattern() != null ? plugin.getCurrentPattern() + plugin.getCurrentSet() : "Detecting...";
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Pattern:")
                .right(pattern)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Step:")
                .right(String.valueOf(plugin.getStepInSet()))
                .build());
    }
}