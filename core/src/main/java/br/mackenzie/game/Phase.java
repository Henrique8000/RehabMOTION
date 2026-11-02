package br.mackenzie.game;

public enum Phase {

    ONE(  "Fase 1",  150f,  1.3f,   0.50f,  60f),
    TWO(  "Fase 2",  190f,  1.05f,  0.65f,  60f),
    THREE("Fase 3",  230f,  0.85f,  0.80f,  60f);

    public final String title;
    public final float scrollSpeed;
    public final float spawnInterval;
    public final float edgeItemChance;
    public final float duration;

    Phase(String title, float scrollSpeed, float spawnInterval, float edgeItemChance, float duration) {
        this.title = title;
        this.scrollSpeed = scrollSpeed;
        this.spawnInterval = spawnInterval;
        this.edgeItemChance = edgeItemChance;
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
