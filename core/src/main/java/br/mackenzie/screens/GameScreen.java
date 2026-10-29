package br.mackenzie.screens;

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

    private static final Color ROCK = new Color(0.45f, 0.45f, 0.48f, 1f);
    private static final Color ROCK_LIGHT = new Color(0.62f, 0.62f, 0.66f, 1f);
    private static final Color LOG = new Color(0.42f, 0.26f, 0.13f, 1f);
    private static final Color LOG_END = new Color(0.70f, 0.52f, 0.32f, 1f);
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

    private final Main game;

    private final FitViewport viewport;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout = new GlyphLayout();

    private final Array<Obstacle> obstacles = new Array<>();

    private final Rectangle hullBox = new Rectangle();
    private final Rectangle sailBox = new Rectangle();

    private float elapsed;
    private float scroll;
    private float obstacleTimer;
    private float invulnerableTimer;

    private int lives = START_LIVES;
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
    }

    private void spawn(float delta) {
        obstacleTimer += delta;
        if (obstacleTimer >= SPAWN_INTERVAL) {
            obstacleTimer -= SPAWN_INTERVAL;
            spawnObstacle();
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

    private static float clampToRiver(float x, float halfWidth) {
        return MathUtils.clamp(x, BANK_WIDTH + halfWidth + 4f, WORLD_WIDTH - BANK_WIDTH - halfWidth - 4f);
    }

    private void hit() {
        lives--;
        hits++;
        invulnerableTimer = INVULNERABLE_TIME;
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