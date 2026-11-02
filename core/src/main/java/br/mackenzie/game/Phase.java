package br.mackenzie.game;

import br.mackenzie.screens.Theme;

public enum Phase {

    ONE(  "Fase 1: Rio",               Theme.RIVER,   150f,  1.3f,   0.50f,  8,   60f),
    TWO(  "Fase 2: Floresta tropical", Theme.JUNGLE,  190f,  1.05f,  0.65f,  12,  60f),
    THREE("Fase 3",                    Theme.RIVER,   230f,  0.85f,  0.80f,  16,  60f);

    public final String title;
    public final Theme theme;
    public final float scrollSpeed;
    public final float spawnInterval;
    public final float edgeItemChance;
    public final int coinGoal;
    public final float duration;

    Phase(String title, Theme theme, float scrollSpeed, float spawnInterval,
          float edgeItemChance, int coinGoal, float duration) {
        this.title = title;
        this.theme = theme;
        this.scrollSpeed = scrollSpeed;
        this.spawnInterval = spawnInterval;
        this.edgeItemChance = edgeItemChance;
        this.coinGoal = coinGoal;
        this.duration = duration;
    }

    public int number() {
        return ordinal() + 1;
    }

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    public Phase next() {
        return isLast() ? null : values()[ordinal() + 1];
    }
}
