package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Varbits; // Using the compatible Varbits API
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class PrayerWidgetOverlay extends Overlay {
    private final Client client;
    private final ExamplePlugin plugin;
    private final ExampleConfig config;

    private static final int PRAYER_GROUP_ID = 541;
    private static final int PROTECT_FROM_MAGIC_CHILD_ID = 27;
    private static final int PROTECT_FROM_MISSILES_CHILD_ID = 28;

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
        if (!config.enablePrayerHelper() || (config.linkPrayerHelperToSolver() && !plugin.isSolverActive())) {
            return null;
        }

        Prayer expectedPrayer = plugin.getExpectedPrayer();
        if (expectedPrayer == null || client.isPrayerActive(expectedPrayer)) {
            return null;
        }

        Widget prayerWidget = null;
        if (expectedPrayer == Prayer.PROTECT_FROM_MAGIC) {
            prayerWidget = client.getWidget(PRAYER_GROUP_ID, PROTECT_FROM_MAGIC_CHILD_ID);
        } else if (expectedPrayer == Prayer.PROTECT_FROM_MISSILES) {
            prayerWidget = client.getWidget(PRAYER_GROUP_ID, PROTECT_FROM_MISSILES_CHILD_ID);
        }

        if (prayerWidget != null && !prayerWidget.isHidden()) {
            Rectangle bounds = prayerWidget.getBounds();
            Color highlightColor = expectedPrayer == Prayer.PROTECT_FROM_MAGIC ? Color.CYAN : Color.GREEN;
            OverlayUtil.renderPolygon(graphics, bounds, highlightColor);
        }

        return null;
    }
}