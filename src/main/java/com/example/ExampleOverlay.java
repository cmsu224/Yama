package com.example;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import java.awt.*;
import net.runelite.api.Point; // This is the correct import

public class ExampleOverlay extends Overlay {
    private final Client client;
    private final ExamplePlugin plugin;
    private final ExampleConfig config;

    @Inject
    private ExampleOverlay(Client client, ExamplePlugin plugin, ExampleConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        for (HighlightedTile highlightedTile : plugin.getHighlightedTiles()) {
            if (highlightedTile.getWorldPoint().getPlane() != client.getPlane()) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, highlightedTile.getWorldPoint());
            if (localPoint == null) {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
            if (poly == null) {
                continue;
            }

            // --- START OF MODIFICATION ---

            // 1. Determine the color based on the ticks remaining
            Color highlightColor;
            switch (highlightedTile.getTicksRemaining()) {
                case 3:
                    highlightColor = new Color(255, 0, 0, 100); // Red
                    break;
                case 2:
                    highlightColor = new Color(255, 255, 0, 100); // Yellow
                    break;
                case 1:
                    highlightColor = new Color(0, 255, 0, 100); // Green
                    break;
                default:
                    // Fallback, should not happen with current logic
                    continue;
            }

            // 2. Render the polygon with the dynamic color
            OverlayUtil.renderPolygon(graphics, poly, highlightColor);

            // --- END OF MODIFICATION ---


            // The logic for rendering the text is still the same
            String text = String.valueOf(highlightedTile.getTicksRemaining());
            Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 0);

            if (textLocation != null) {
                OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.WHITE);
            }
        }
        return null;
    }
}