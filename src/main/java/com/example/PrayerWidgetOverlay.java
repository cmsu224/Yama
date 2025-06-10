package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class PrayerWidgetOverlay extends Overlay {
    private final Client client;
    private final ExamplePlugin plugin;
    private final ExampleConfig config;

    // --- These are the correct widget IDs based on your example ---
    private static final int PRAYER_GROUP_ID = 541;
    private static final int PROTECT_FROM_MAGIC_CHILD_ID = 21;
    private static final int PROTECT_FROM_MISSILES_CHILD_ID = 22;

    @Inject
    private PrayerWidgetOverlay(Client client, ExamplePlugin plugin, ExampleConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
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

        Widget prayerWidget = null;
        // Look up the widget using the correct, verified IDs
        if (expectedPrayer == Prayer.PROTECT_FROM_MAGIC) {
            prayerWidget = client.getWidget(PRAYER_GROUP_ID, PROTECT_FROM_MAGIC_CHILD_ID);
        } else if (expectedPrayer == Prayer.PROTECT_FROM_MISSILES) {
            prayerWidget = client.getWidget(PRAYER_GROUP_ID, PROTECT_FROM_MISSILES_CHILD_ID);
        }

        // This combined check is the most reliable way to ensure the widget is on screen
        if (prayerWidget != null && !prayerWidget.isHidden()) {
            Rectangle bounds = prayerWidget.getBounds();

            // Don't draw if the bounds are invalid
            if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
                return null;
            }

            Color highlightColor = expectedPrayer == Prayer.PROTECT_FROM_MAGIC ? Color.CYAN : Color.GREEN;
            OverlayUtil.renderPolygon(graphics, bounds, highlightColor);
        }

        return null;
    }
}