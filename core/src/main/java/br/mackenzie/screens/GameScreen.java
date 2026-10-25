package br.mackenzie.screens;

import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import br.mackenzie.Main;
import br.mackenzie.input.MotionInput;

import static br.mackenzie.screens.RiverArt.*;

public class GameScreen extends ScreenAdapter {

    private static final float SCROLL_SPEED = 170f;
    private static final float SPAWN_INTERVAL = 1.1f;
    private static final int START_LIVES = 3;
    private static final float INVULNERABLE_TIME = 1.5f;

    private static final float EDGE_ITEM_CHANCE = 0.65f;
    private static final float EDGE_THRESHOLD = 0.75f;
    private static final int ITEM_POINTS = 10;
    private static final int EDGE_ITEM_POINTS = 20;

    private static final float ITEM_RADIUS = 14f;

    private static final Color ROCK = new Color(0.45f, 0.45f, 0.48f, 1f);
    private static final Color ROCK_LIGHT = new Color(0.62f, 0.62f, 0.66f, 1f);
    private static final Color LOG = new Color(0.42f, 0.26f, 0.13f, 1f);
    private static final Color LOG_END = new Color(0.70f, 0.52f, 0.32f, 1f);
    private static final Color COIN = new Color(1f, 0.80f, 0.15f, 1f);
    private static final Color COIN_LIGHT = new Color(1f, 0.93f, 0.55f, 1f);
    private static final Color HEART = new Color(0.90f, 0.20f, 0.25f, 1f);

    private static class Obstacle {
        final boolean isLog;
        final Circle circle = new Circle();
        final Rectangle rect = new Rectangle();

        Obstacle(boolean isLog) {
            this.isLog = isLog;
        }

        float top() {
            return isLog ? rect.y + rect.height : circle.y + circle.radius;
        }

        void moveDown(float amount) {
            circle.y -= amount;
            rect.y -= amount;
        }

        boolean hits(Rectangle r) {
            return isLog ? rect.overlaps(r) : Intersector.overlaps(circle, r);
        }
    }

    private static class Item {
        final Circle circle;
        final boolean nearEdge;

        Item(float x, float y, boolean nearEdge) {
            this.circle = new Circle(x, y, ITEM_RADIUS);
            this.nearEdge = nearEdge;
        }
    }

    private static class Popup {
        final String text;
        final float x;
        float y;
        float life = 1f;

        Popup(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private final Main game;

    private final FitViewport viewport;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout = new GlyphLayout();

    private final Array<Obstacle> obstacles = new Array<>();
    private final Array<Item> items = new Array<>();
    private final Array<Popup> popups = new Array<>();

    private final Rectangle hullBox = new Rectangle();
    private final Rectangle sailBox = new Rectangle();

    private float elapsed;
    private float scroll;
    private float obstacleTimer;
    private float itemTimer = SPAWN_INTERVAL / 2f;
    private float invulnerableTimer;

    private int score;
    private int lives = START_LIVES;
    private int itemsCollected;
    private int edgeItemsCollected;
    private int itemsSpawned;
    private int hits;

    public GameScreen(Main game) {
        this.game = game;
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        bigFont = new BitmapFont();
        bigFont.getData().setScale(2f);
        bigFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 20f);

        handleKeys();

        MotionInput input = game.getInput();
        float boatX = xFor(input.getPosition());

        update(delta, boatX);

        draw(input, boatX);
    }

    private void update(float delta, float boatX) {
        elapsed += delta;
        scroll += SCROLL_SPEED * delta;
        invulnerableTimer -= delta;

        hullBox.set(boatX - 55f, BOAT_Y, 110f, 28f);
        sailBox.set(boatX, BOAT_Y + 28f, 55f, 95f);

        spawn(delta);

        float move = SCROLL_SPEED * delta;
        for (int i = obstacles.size - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.moveDown(move);
            if (o.top() < 0f) {
                obstacles.removeIndex(i);
            } else if (invulnerableTimer <= 0f && (o.hits(hullBox) || o.hits(sailBox))) {
                hit();
            }
        }

        for (int i = items.size - 1; i >= 0; i--) {
            Item item = items.get(i);
            item.circle.y -= move;
            if (item.circle.y + ITEM_RADIUS < 0f) {
                items.removeIndex(i);
            } else if (Intersector.overlaps(item.circle, hullBox) || Intersector.overlaps(item.circle, sailBox)) {
                collect(item);
                items.removeIndex(i);
            }
        }

        for (int i = popups.size - 1; i >= 0; i--) {
            Popup p = popups.get(i);
            p.y += 40f * delta;
            p.life -= delta;
            if (p.life <= 0f) {
                popups.removeIndex(i);
            }
        }
    }

    private void spawn(float delta) {
        obstacleTimer += delta;
        if (obstacleTimer >= SPAWN_INTERVAL) {
            obstacleTimer -= SPAWN_INTERVAL;
            spawnObstacle();
        }
        itemTimer += delta;
        if (itemTimer >= SPAWN_INTERVAL) {
            itemTimer -= SPAWN_INTERVAL;
            spawnItem();
        }
    }

    private void spawnObstacle() {
        boolean isLog = MathUtils.randomBoolean(0.35f);
        Obstacle o = new Obstacle(isLog);
        float x = xFor(MathUtils.random(-1f, 1f));
        if (isLog) {
            float width = MathUtils.random(120f, 170f);
            float height = 26f;
            x = clampToRiver(x, width / 2f);
            o.rect.set(x - width / 2f, RIVER_TOP, width, height);
        } else {
            float radius = MathUtils.random(24f, 36f);
            x = clampToRiver(x, radius);
            o.circle.set(x, RIVER_TOP + radius, radius);
        }
        obstacles.add(o);
    }

    private void spawnItem() {
        float position;
        if (MathUtils.randomBoolean(EDGE_ITEM_CHANCE)) {
            position = MathUtils.random(0.8f, 1f) * (MathUtils.randomBoolean() ? 1f : -1f);
        } else {
            position = MathUtils.random(-0.7f, 0.7f);
        }
        items.add(new Item(xFor(position), RIVER_TOP + ITEM_RADIUS, Math.abs(position) >= EDGE_THRESHOLD));
        itemsSpawned++;
    }

    private static float clampToRiver(float x, float halfWidth) {
        return MathUtils.clamp(x, BANK_WIDTH + halfWidth + 4f, WORLD_WIDTH - BANK_WIDTH - halfWidth - 4f);
    }

    private void hit() {
        lives--;
        hits++;
        invulnerableTimer = INVULNERABLE_TIME;
    }

    private void collect(Item item) {
        int points = item.nearEdge ? EDGE_ITEM_POINTS : ITEM_POINTS;
        score += points;
        itemsCollected++;
        if (item.nearEdge) {
            edgeItemsCollected++;
        }
        popups.add(new Popup("+" + points, item.circle.x, item.circle.y + 20f));
    }

    private void handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.changeScreen(new MotionTestScreen(game));
        }
    }

    private void draw(MotionInput input, float boatX) {
        ScreenUtils.clear(WATER);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawRiver(shapes, scroll);
        for (Item item : items) {
            drawCoin(item);
        }
        for (Obstacle o : obstacles) {
            drawObstacle(o);
        }
        boolean blinkOff = invulnerableTimer > 0f && ((int) (invulnerableTimer * 10f)) % 2 == 0;
        if (!blinkOff) {
            drawBoat(shapes, boatX, BOAT_Y + (float) Math.sin(elapsed * 2.2f) * 3f);
        }
        drawHudPanel();
        shapes.end();

        batch.begin();
        drawHudText(input);
        for (Popup p : popups) {
            font.setColor(1f, 1f, 1f, Math.max(p.life, 0f));
            layout.setText(font, p.text);
            font.draw(batch, layout, p.x - layout.width / 2f, p.y);
        }
        batch.end();
    }

    private void drawObstacle(Obstacle o) {
        if (o.isLog) {
            Rectangle r = o.rect;
            shapes.setColor(LOG);
            shapes.rect(r.x, r.y, r.width, r.height);
            shapes.setColor(LOG_END);
            shapes.circle(r.x, r.y + r.height / 2f, r.height / 2f);
            shapes.circle(r.x + r.width, r.y + r.height / 2f, r.height / 2f);
        } else {
            Circle c = o.circle;
            shapes.setColor(ROCK);
            shapes.circle(c.x, c.y, c.radius);
            shapes.setColor(ROCK_LIGHT);
            shapes.circle(c.x - c.radius * 0.3f, c.y + c.radius * 0.3f, c.radius * 0.35f);
        }
    }

    private void drawCoin(Item item) {
        Circle c = item.circle;
        if (item.nearEdge) {
            shapes.setColor(1f, 1f, 1f, 0.35f);
            shapes.circle(c.x, c.y, c.radius + 6f);
        }
        shapes.setColor(COIN);
        shapes.circle(c.x, c.y, c.radius);
        shapes.setColor(COIN_LIGHT);
        shapes.circle(c.x, c.y, c.radius * 0.55f);
    }

    private void drawHudPanel() {
        shapes.setColor(PANEL);
        shapes.rect(0f, RIVER_TOP, WORLD_WIDTH, HUD_HEIGHT);
        shapes.rect(0f, 0f, WORLD_WIDTH, 34f);

        for (int i = 0; i < START_LIVES; i++) {
            shapes.setColor(i < lives ? HEART : Color.DARK_GRAY);
            float x = 30f + i * 40f;
            float y = WORLD_HEIGHT - 95f;
            shapes.circle(x - 7f, y + 6f, 9f);
            shapes.circle(x + 7f, y + 6f, 9f);
            shapes.triangle(x - 16f, y + 3f, x + 16f, y + 3f, x, y - 16f);
        }
    }

    private void drawHudText(MotionInput input) {
        bigFont.setColor(COIN);
        bigFont.draw(batch, "Pontos: " + score, 20f, WORLD_HEIGHT - 16f);

        font.setColor(Color.WHITE);
        font.draw(batch, String.format(Locale.ROOT, "Moedas: %d   (margem: %d)", itemsCollected, edgeItemsCollected),
            200f, WORLD_HEIGHT - 85f);

        font.setColor(input.isConnected() ? OK : WARNING);
        font.draw(batch, input.getSourceName(), 600f, 505f);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "ESC voltar para a tela de teste", 20f, 23f);
    }

    private void drawCentered(BitmapFont f, String text, float y, Color color) {
        f.setColor(color);
        layout.setText(f, text);
        f.draw(batch, layout, (WORLD_WIDTH - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        bigFont.dispose();
    }
}
