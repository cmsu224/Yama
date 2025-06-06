package com.example;

public class SequenceStep {
    final int tileNumber;
    final int delayTicks; // Ticks to wait AFTER this highlight appears

    public SequenceStep(int tileNumber, int delayTicks) {
        this.tileNumber = tileNumber;
        this.delayTicks = delayTicks;
    }

    public int getTileNumber() {
        return tileNumber;
    }

    public int getDelayTicks() {
        return delayTicks;
    }
}