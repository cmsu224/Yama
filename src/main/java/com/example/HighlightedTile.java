package com.example;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

class HighlightedTile {
    @Getter
    private final WorldPoint worldPoint;

    @Getter
    private int ticksRemaining;

    HighlightedTile(WorldPoint worldPoint) {
        this.worldPoint = worldPoint;
        this.ticksRemaining = 3; // Start the countdown from 3
    }

    void tick() {
        this.ticksRemaining--;
    }
}
