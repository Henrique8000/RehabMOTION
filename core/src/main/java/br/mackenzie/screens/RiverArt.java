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
    public static final float BOAT_SCALE = 0.8f;
    public static final float BOAT_HALF_WIDTH = 70f * BOAT_SCALE;

    public static final float BOAT_TRAVEL = WORLD_WIDTH / 2f - BANK_WIDTH - BOAT_HALF_WIDTH - 10f;

    public static final Color WATER = Theme.RIVER.water;
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
        drawRiver(shapes, scroll, Theme.RIVER);
    }

    public static void drawRiver(ShapeRenderer shapes, float scroll, Theme theme) {
        shapes.setColor(theme.wave);
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

        shapes.setColor(theme.bank);
        shapes.rect(0f, 0f, BANK_WIDTH, RIVER_TOP);
        shapes.rect(WORLD_WIDTH - BANK_WIDTH, 0f, BANK_WIDTH, RIVER_TOP);

        if (theme != Theme.RIVER) {
            drawBankDecor(shapes, scroll, theme);
        }
    }

    private static void drawBankDecor(ShapeRenderer shapes, float scroll, Theme theme) {
        float spacing = 110f;
        int firstSlot = (int) (scroll / spacing);
        float offset = scroll % spacing;
        int slot = 0;
        for (float y = RIVER_TOP - offset; y > -spacing; y -= spacing, slot++) {
            int n = firstSlot + slot;
            if (theme == Theme.JUNGLE) {
                drawJungleSlot(shapes, BANK_WIDTH / 2f, y, n, theme, -1f);
                drawJungleSlot(shapes, WORLD_WIDTH - BANK_WIDTH / 2f, y + 55f, n + 1, theme, 1f);
            } else if (theme == Theme.VOLCANO) {
                drawCrack(shapes, BANK_WIDTH / 2f, y, n, theme);
                drawCrack(shapes, WORLD_WIDTH - BANK_WIDTH / 2f, y + 55f, n + 1, theme);
            }
        }

        if (theme == Theme.VOLCANO) {
            shapes.setColor(theme.wave);
            shapes.rect(BANK_WIDTH - 5f, 0f, 5f, RIVER_TOP);
            shapes.rect(WORLD_WIDTH - BANK_WIDTH, 0f, 5f, RIVER_TOP);
        }
    }

    private static void drawJungleSlot(ShapeRenderer shapes, float x, float y, int n, Theme theme, float side) {
        float towardRiver = -side;
        if (n % 2 == 0) {
            shapes.setColor(theme.log);
            shapes.rect(x - 4f, y - 20f, 8f, 50f);
            shapes.setColor(theme.bankDetail);
            float top = y + 30f;
            shapes.triangle(x, top, x - 38f, top - 6f, x - 14f, top + 10f);
            shapes.triangle(x, top, x + 38f, top - 6f, x + 14f, top + 10f);
            shapes.triangle(x, top, x - 22f, top + 26f, x + 4f, top + 12f);
            shapes.triangle(x, top, x + 22f, top + 26f, x - 4f, top + 12f);
        } else {
            shapes.setColor(theme.bankDetail);
            shapes.circle(x - 14f, y, 16f);
            shapes.circle(x + 12f, y + 4f, 18f);
            shapes.circle(x, y + 16f, 15f);
            shapes.setColor(n % 3 == 0 ? Color.MAGENTA : Color.YELLOW);
            shapes.circle(x + 6f, y + 10f, 4f);
        }
        shapes.setColor(theme.bankDetail);
        float edge = x + towardRiver * BANK_WIDTH / 2f;
        shapes.triangle(edge, y - 12f, edge, y + 12f, edge + towardRiver * 22f, y - 4f);
    }

    private static void drawCrack(ShapeRenderer shapes, float x, float y, int n, Theme theme) {
        float flip = (n % 2 == 0) ? 1f : -1f;
        shapes.setColor(theme.bankDetail);
        shapes.rectLine(x - 18f * flip, y + 30f, x + 6f * flip, y + 12f, 4f);
        shapes.rectLine(x + 6f * flip, y + 12f, x - 8f * flip, y - 8f, 4f);
        shapes.rectLine(x - 8f * flip, y - 8f, x + 16f * flip, y - 28f, 3f);
        if (n % 3 == 0) {
            shapes.setColor(theme.wave);
            shapes.circle(x + 14f * flip, y + 26f, 5f);
        }
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
        float s = BOAT_SCALE;

        shapes.setColor(HULL);
        shapes.rect(x - 50f * s, y, 100f * s, 28f * s);
        shapes.triangle(x - 50f * s, y, x - 50f * s, y + 28f * s, x - BOAT_HALF_WIDTH, y + 28f * s);
        shapes.triangle(x + 50f * s, y, x + 50f * s, y + 28f * s, x + BOAT_HALF_WIDTH, y + 28f * s);

        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(x - 3f * s, y + 28f * s, 6f * s, 95f * s);

        shapes.setColor(SAIL);
        shapes.triangle(x + 5f * s, y + 38f * s, x + 5f * s, y + 118f * s, x + 60f * s, y + 42f * s);
        shapes.setColor(Color.RED);
        shapes.triangle(x + 3f * s, y + 123f * s, x + 3f * s, y + 138f * s, x + 25f * s, y + 130f * s);
    }
}
