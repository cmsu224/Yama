package com.example;

import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

public class SequenceData {
    // The 9 tiles of the glyph + 2 outside tiles
    public static final Map<Integer, WorldPoint> TILE_MAP = Map.ofEntries(
            Map.entry(1, new WorldPoint(1502, 10074, 0)),
            Map.entry(2, new WorldPoint(1503, 10074, 0)),
            Map.entry(3, new WorldPoint(1504, 10074, 0)),
            Map.entry(4, new WorldPoint(1502, 10073, 0)),
            Map.entry(5, new WorldPoint(1503, 10073, 0)),
            Map.entry(6, new WorldPoint(1504, 10073, 0)),
            Map.entry(7, new WorldPoint(1502, 10072, 0)),
            Map.entry(8, new WorldPoint(1503, 10072, 0)),
            Map.entry(9, new WorldPoint(1504, 10072, 0)),
            Map.entry(10, new WorldPoint(1506, 10071, 0)),
            Map.entry(11, new WorldPoint(1506, 10072, 0))
    );

    // Your solved sequences
    public static final Map<String, List<Integer>> SEQUENCES = Map.of(
            "V1", List.of(4, 2, 5, 1, 2, 6, 7, 5),
            "V2", List.of(4, 2, 5, 9, 7, 1, 6, 10, 11, 5),
            "V3", List.of(4, 2, 5, 1, 2, 5),
            "H1", List.of(8, 6, 5, 9, 2, 6, 7, 5),
            "H2", List.of(2, 4, 5, 9, 7, 1, 6, 7, 5),
            "H3", List.of(8, 6, 5, 9, 2, 5)
            // TODO: Add the rest of your solved sequences here as you figure them out
    );

    // Your solved sequences. You can easily update these later.
    /*public static final Map<String, List<Integer>> SEQUENCES = Map.of(
            // Vertical Starts
            "V1", List.of(4, 2, 5, 1, 2),
            "W1a_V", List.of(6, 7), // Wave 1a after a V-start
            "W1b_V", List.of(6, 7), // Wave 1b after a V-start
            "V2", List.of(4, 2, 5), // V2 is interrupted by W2
            "W2a_V", List.of(9, 7, 1), // W2a after a V-start (includes a pause)
            "W2b_V", List.of(9, 7, 1), // W2b after a V-start
            "V3", List.of(4, 2, 5, 1, 2),

            // Horizontal Starts
            "H1", List.of(8, 6, 5, 9, 2),
            "W1a_H", List.of(6, 7), // Wave 1a after an H-start
            "W1b_H", List.of(6, 7), // Wave 1b after an H-start
            "H2", List.of(2, 4, 5), // H2 is interrupted by W2
            "W2b_H", List.of(9, 7, 1),
            "W3a_H_after_W2b", List.of(6, 7), // Example of a sub-sub-sequence
            "H3", List.of(8, 6, 5, 9, 2)
    );*/
}