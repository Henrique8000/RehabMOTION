package br.mackenzie.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public final class RiverArt {

    public static final float WORLD_WIDTH = 960f;
    public static final float WORLD_HEIGHT = 600f;

    public static final float HUD_HEIGHT = 130f;
    public static final float RIVER_TOP = WORLD_HEIGHT - HUD_HEIGHT;
    public static final float BANK_WIDTH = 80f;
    public static final float BOAT_Y = 110f;
    public static final float BOAT_HALF_WIDTH = 70f;

    public static final float BOAT_TRAVEL = WORLD_WIDTH / 2f - BANK_WIDTH - BOAT_HALF_WIDTH - 10f;

    public static final Color WATER = new Color(0.16f, 0.45f, 0.70f, 1f);
    public static final Color WAVE = new Color(0.35f, 0.62f, 0.85f, 1f);
    public static final Color BANK = new Color(0.30f, 0.58f, 0.28f, 1f);
    public static final Color HULL = new Color(0.55f, 0.33f, 0.17f, 1f);
    public static final Color SAIL = new Color(0.96f, 0.95f, 0.90f, 1f);
    public static final Color PANEL = new Color(0f, 0f, 0f, 0.65f);
    public static final Color OK = new Color(0.45f, 0.90f, 0.45f, 1f);
    public static final Color WARNING = new Color(1f, 0.75f, 0.30f, 1f);

    private RiverArt() {
    }

    public static float xFor(float position) {
        return WORLD_WIDTH / 2f + position * BOAT_TRAVEL;
    }

    public static void drawRiver(ShapeRenderer shapes, float scroll) {
        shapes.setColor(WAVE);
        float spacing = 70f;
        int firstRow = (int) (scroll / spacing);
        float offset = scroll % spacing;
        int row = 0;
        for (float y = RIVER_TOP - offset; y > -spacing; y -= spacing, row++) {
            float shift = ((firstRow + row) % 2 == 0) ? 0f : 70f;
            for (float x = BANK_WIDTH + 30f; x < WORLD_WIDTH - BANK_WIDTH - 40f - shift; x += 140f) {
                shapes.rect(x + shift, y, 40f, 4f);
            }
        }

        shapes.setColor(BANK);
        shapes.rect(0f, 0f, BANK_WIDTH, RIVER_TOP);
        shapes.rect(WORLD_WIDTH - BANK_WIDTH, 0f, BANK_WIDTH, RIVER_TOP);
    }

    public static void drawBuoy(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(Color.WHITE);
        shapes.circle(x, y, 16f);
        shapes.setColor(Color.RED);
        shapes.circle(x, y, 11f);
        shapes.setColor(Color.WHITE);
        shapes.circle(x, y, 5f);
    }

    public static void drawBoat(ShapeRenderer shapes, float x, float y) {
        shapes.setColor(HULL);
        shapes.rect(x - 50f, y, 100f, 28f);
        shapes.triangle(x - 50f, y, x - 50f, y + 28f, x - BOAT_HALF_WIDTH, y + 28f);
        shapes.triangle(x + 50f, y, x + 50f, y + 28f, x + BOAT_HALF_WIDTH, y + 28f);

        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(x - 3f, y + 28f, 6f, 95f);

        shapes.setColor(SAIL);
        shapes.triangle(x + 5f, y + 38f, x + 5f, y + 118f, x + 60f, y + 42f);
        shapes.setColor(Color.RED);
        shapes.triangle(x + 3f, y + 123f, x + 3f, y + 138f, x + 25f, y + 130f);
    }
}
