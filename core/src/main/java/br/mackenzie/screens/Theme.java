package br.mackenzie.screens;

import com.badlogic.gdx.graphics.Color;

public enum Theme {

    RIVER(
        rgb(0.16f, 0.45f, 0.70f), rgb(0.35f, 0.62f, 0.85f),
        rgb(0.30f, 0.58f, 0.28f), rgb(0.40f, 0.68f, 0.35f),
        rgb(0.45f, 0.45f, 0.48f), rgb(0.62f, 0.62f, 0.66f),
        rgb(0.42f, 0.26f, 0.13f), rgb(0.70f, 0.52f, 0.32f)),

    JUNGLE(
        rgb(0.10f, 0.38f, 0.33f), rgb(0.22f, 0.52f, 0.44f),
        rgb(0.10f, 0.33f, 0.12f), rgb(0.18f, 0.52f, 0.20f),
        rgb(0.35f, 0.38f, 0.32f), rgb(0.48f, 0.55f, 0.42f),
        rgb(0.33f, 0.22f, 0.10f), rgb(0.55f, 0.42f, 0.25f)),

    VOLCANO(
        rgb(0.80f, 0.25f, 0.05f), rgb(1.00f, 0.60f, 0.10f),
        rgb(0.17f, 0.14f, 0.14f), rgb(0.95f, 0.40f, 0.08f),
        rgb(0.20f, 0.18f, 0.18f), rgb(0.95f, 0.45f, 0.10f),
        rgb(0.15f, 0.12f, 0.10f), rgb(1.00f, 0.55f, 0.15f));

    public final Color water;
    public final Color wave;
    public final Color bank;
    public final Color bankDetail;
    public final Color rock;
    public final Color rockLight;
    public final Color log;
    public final Color logEnd;

    Theme(Color water, Color wave, Color bank, Color bankDetail,
          Color rock, Color rockLight, Color log, Color logEnd) {
        this.water = water;
        this.wave = wave;
        this.bank = bank;
        this.bankDetail = bankDetail;
        this.rock = rock;
        this.rockLight = rockLight;
        this.log = log;
        this.logEnd = logEnd;
    }

    private static Color rgb(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }
}
