package com.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
@AllArgsConstructor
class TickTileInfo {
    private final int tick;
    private final WorldPoint worldPoint;
}