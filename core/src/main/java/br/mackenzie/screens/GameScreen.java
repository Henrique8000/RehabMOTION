package br.mackenzie.screens;

import java.util.List;
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
import br.mackenzie.game.Phase;
import br.mackenzie.game.PhaseResult;
import br.mackenzie.input.MotionInput;

import static br.mackenzie.screens.RiverArt.*;

public class GameScreen extends ScreenAdapter {

    private static final float INTRO_TIME = 5f;
    private static final int START_LIVES = 3;
    private static final float INVULNERABLE_TIME = 1.5f;

    private static final float EDGE_THRESHOLD = 0.75f;
    private static final int ITEM_POINTS = 10;
    private static final int EDGE_ITEM_POINTS = 20;

    private static final float ITEM_RADIUS = 14f;

    private static final Color COIN = new Color(1f, 0.80f, 0.15f, 1f);
    private static final Color COIN_LIGHT = new Color(1f, 0.93f, 0.55f, 1f);
    private static final Color COIN_OUTLINE = new Color(0f, 0f, 0f, 0.5f);
    private static final Color HEART = new Color(0.90f, 0.20f, 0.25f, 1f);

    private enum State { INTRO, PLAYING, PAUSED, WON, LOST, COMPLETE }

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
    private final Phase phase;
    private final Theme theme;

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

    private State state = State.INTRO;
    private float introTimer = INTRO_TIME;
    private float elapsed;
    private float animTime;
    private float scroll;
    private float obstacleTimer;
    private float itemTimer;
    private float invulnerableTimer;
    private String lostReason = "";
    private String message = "";
    private float messageTimer;

    private int score;
    private int lives = START_LIVES;
    private int itemsCollected;
    private int edgeItemsCollected;
    private int itemsSpawned;
    private int hits;
    private float minPosition;
    private float maxPosition;

    public GameScreen(Main game, Phase phase) {
        this.game = game;
        this.phase = phase;
        this.theme = phase.theme;
        itemTimer = phase.spawnInterval / 2f;

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

        if (state == State.INTRO) {
            animTime += delta;
            scroll += phase.scrollSpeed * delta;
            introTimer -= delta;
            if (introTimer <= 0f) {
                state = State.PLAYING;
            }
        } else if (state == State.PLAYING) {
            update(delta, input.getPosition(), boatX);
        }

        draw(input, boatX);
    }

    private void update(float delta, float position, float boatX) {
        elapsed += delta;
        animTime += delta;
        scroll += phase.scrollSpeed * delta;
        invulnerableTimer -= delta;
        messageTimer -= delta;

        minPosition = Math.min(minPosition, position);
        maxPosition = Math.max(maxPosition, position);

        float s = BOAT_SCALE;
        hullBox.set(boatX - 55f * s, BOAT_Y, 110f * s, 28f * s);
        sailBox.set(boatX, BOAT_Y + 28f * s, 55f * s, 95f * s);

        spawn(delta);

        float move = phase.scrollSpeed * delta;
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

        if (lives <= 0) {
            lose("O barco afundou!");
        } else if (elapsed >= phase.duration) {
            if (itemsCollected >= phase.coinGoal) {
                win();
            } else {
                int missing = phase.coinGoal - itemsCollected;
                lose("Faltou " + missing + (missing == 1 ? " moeda!" : " moedas!"));
            }
        }
    }

    private void spawn(float delta) {
        if (elapsed > phase.duration - 3f) {
            return;
        }
        obstacleTimer += delta;
        if (obstacleTimer >= phase.spawnInterval) {
            obstacleTimer -= phase.spawnInterval;
            spawnObstacle();
        }
        itemTimer += delta;
        if (itemTimer >= phase.spawnInterval) {
            itemTimer -= phase.spawnInterval;
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
        if (MathUtils.randomBoolean(phase.edgeItemChance)) {
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
        if (itemsCollected == phase.coinGoal) {
            message = "Meta de moedas alcançada! Agora chegue ao fim.";
            messageTimer = 2.5f;
        }
    }

    private void win() {
        game.getResults().add(new PhaseResult(phase, score, itemsCollected, edgeItemsCollected, hits,
            usedAmplitudeFraction(), usedAmplitudeFraction() * game.getInput().getCalibratedRangeDegrees()));
        state = phase.isLast() ? State.COMPLETE : State.WON;
    }

    private void lose(String reason) {
        lostReason = reason;
        state = State.LOST;
    }

    private float usedAmplitudeFraction() {
        return (maxPosition - minPosition) / 2f;
    }

    private void handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            if (state == State.PLAYING) {
                state = State.PAUSED;
            } else if (state == State.PAUSED) {
                state = State.PLAYING;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (state) {
                case INTRO:
                    state = State.PLAYING;
                    break;
                case WON:
                    game.changeScreen(new GameScreen(game, phase.next()));
                    break;
                case LOST:
                    game.changeScreen(new GameScreen(game, phase));
                    break;
                case COMPLETE:
                    game.getResults().clear();
                    game.changeScreen(new GameScreen(game, Phase.ONE));
                    break;
                default:
                    break;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.changeScreen(new MotionTestScreen(game));
        }
    }

    private void draw(MotionInput input, float boatX) {
        ScreenUtils.clear(theme.water);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawRiver(shapes, scroll, theme);
        for (Item item : items) {
            drawCoin(item);
        }
        for (Obstacle o : obstacles) {
            drawObstacle(o);
        }
        boolean blinkOff = invulnerableTimer > 0f && ((int) (invulnerableTimer * 10f)) % 2 == 0;
        if (!blinkOff) {
            drawBoat(shapes, boatX, BOAT_Y + (float) Math.sin(animTime * 2.2f) * 3f);
        }
        drawHudPanel();
        if (state == State.COMPLETE) {
            shapes.setColor(PANEL);
            shapes.rect(150f, 90f, 660f, 360f);
        } else if (state != State.PLAYING) {
            shapes.setColor(PANEL);
            shapes.rect(180f, 150f, 600f, 280f);
        }
        shapes.end();

        batch.begin();
        drawHudText(input);
        for (Popup p : popups) {
            font.setColor(1f, 1f, 1f, Math.max(p.life, 0f));
            layout.setText(font, p.text);
            font.draw(batch, layout, p.x - layout.width / 2f, p.y);
        }
        if (state == State.PLAYING && messageTimer > 0f) {
            drawCentered(font, message, 420f, OK);
        }
        drawOverlayText();
        batch.end();
    }

    private void drawObstacle(Obstacle o) {
        if (o.isLog) {
            Rectangle r = o.rect;
            shapes.setColor(theme.log);
            shapes.rect(r.x, r.y, r.width, r.height);
            shapes.setColor(theme.logEnd);
            shapes.circle(r.x, r.y + r.height / 2f, r.height / 2f);
            shapes.circle(r.x + r.width, r.y + r.height / 2f, r.height / 2f);
        } else {
            Circle c = o.circle;
            shapes.setColor(theme.rock);
            shapes.circle(c.x, c.y, c.radius);
            shapes.setColor(theme.rockLight);
            shapes.circle(c.x - c.radius * 0.3f, c.y + c.radius * 0.3f, c.radius * 0.35f);
        }
    }

    private void drawCoin(Item item) {
        Circle c = item.circle;
        if (item.nearEdge) {
            shapes.setColor(1f, 1f, 1f, 0.35f);
            shapes.circle(c.x, c.y, c.radius + 7f);
        }
        shapes.setColor(COIN_OUTLINE);
        shapes.circle(c.x, c.y, c.radius + 3f);
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
            float y = WORLD_HEIGHT - 105f;
            shapes.circle(x - 7f, y + 6f, 9f);
            shapes.circle(x + 7f, y + 6f, 9f);
            shapes.triangle(x - 16f, y + 3f, x + 16f, y + 3f, x, y - 16f);
        }

        float left = 600f, width = 320f, y = 520f;
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(left, y, width, 14f);
        shapes.setColor(OK);
        shapes.rect(left, y, width * Math.min(elapsed / phase.duration, 1f), 14f);
    }

    private void drawHudText(MotionInput input) {
        float x = 20f;

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, phase.title, x, WORLD_HEIGHT - 12f);

        bigFont.setColor(COIN);
        bigFont.draw(batch, "Pontos: " + score, x, WORLD_HEIGHT - 32f);

        boolean goalReached = itemsCollected >= phase.coinGoal;
        bigFont.setColor(goalReached ? OK : Color.WHITE);
        bigFont.draw(batch, "Moedas: " + itemsCollected + " / " + phase.coinGoal, 200f, WORLD_HEIGHT - 85f);

        font.setColor(Color.LIGHT_GRAY);
        int remaining = (int) Math.ceil(Math.max(phase.duration - elapsed, 0f));
        font.draw(batch, "Tempo: " + remaining + " s", 600f, 556f);

        font.setColor(input.isConnected() ? OK : WARNING);
        font.draw(batch, input.getSourceName(), 600f, 505f);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "P pausar    ESC voltar para a tela de teste", 20f, 23f);
    }

    private void drawOverlayText() {
        switch (state) {
            case INTRO:
                drawIntro();
                break;
            case PAUSED:
                drawCentered(bigFont, "PAUSADO", 320f, Color.WHITE);
                drawCentered(font, "Aperte P para continuar", 270f, Color.LIGHT_GRAY);
                break;
            case WON:
                drawCentered(bigFont, "Fase concluída!", 400f, OK);
                drawResults();
                drawCentered(font, "ENTER próxima fase    ESC tela de teste", 175f, Color.LIGHT_GRAY);
                break;
            case LOST:
                drawCentered(bigFont, lostReason, 400f, WARNING);
                drawResults();
                drawCentered(font, "ENTER tentar de novo    ESC tela de teste", 175f, Color.LIGHT_GRAY);
                break;
            case COMPLETE:
                drawSummary();
                break;
            default:
                break;
        }
    }

    private void drawIntro() {
        drawCentered(bigFont, phase.title, 395f, Color.WHITE);
        drawCentered(font, String.format(Locale.ROOT, "Objetivo: chegue ao fim do rio (%.0f s) e pegue %d moedas.",
            phase.duration, phase.coinGoal), 340f, COIN);
        drawCentered(font, "Desvie das pedras e dos troncos: você tem " + START_LIVES + " vidas.", 312f, Color.WHITE);
        drawCentered(font, "Moedas com brilho, perto das margens, valem o dobro.", 288f, Color.WHITE);
        drawCentered(font, "Chegar ao fim sem a meta de moedas não vence a fase.", 264f, WARNING);
        drawCentered(bigFont, "Começa em " + (int) Math.ceil(Math.max(introTimer, 0f)), 215f, OK);
        drawCentered(font, "ENTER começar agora", 175f, Color.LIGHT_GRAY);
    }

    private void drawResults() {
        MotionInput input = game.getInput();
        float fraction = usedAmplitudeFraction();

        drawCentered(font, "Pontos: " + score, 345f, Color.WHITE);
        drawCentered(font, String.format(Locale.ROOT, "Moedas: %d de %d (meta: %d)   na margem: %d",
            itemsCollected, itemsSpawned, phase.coinGoal, edgeItemsCollected), 320f, Color.WHITE);
        drawCentered(font, "Batidas: " + hits, 295f, Color.WHITE);
        drawCentered(font, String.format(Locale.ROOT, "Amplitude usada: %.0f%% (%.0f de %.0f graus)",
            fraction * 100f, fraction * input.getCalibratedRangeDegrees(), input.getCalibratedRangeDegrees()),
            270f, Color.WHITE);
    }

    private void drawSummary() {
        drawCentered(bigFont, "Você completou o rio!", 425f, OK);
        drawCentered(font, "Evolução nas fases", 380f, Color.LIGHT_GRAY);

        float[] columns = {180f, 380f, 470f, 590f, 670f};
        float y = 350f;
        drawRow(columns, y, Color.LIGHT_GRAY, "Fase", "Pontos", "Moedas", "Batidas", "Amplitude");

        List<PhaseResult> results = game.getResults();
        int total = 0;
        for (PhaseResult r : results) {
            y -= 28f;
            total += r.score;
            drawRow(columns, y, Color.WHITE,
                r.phase.title,
                String.valueOf(r.score),
                r.coins + " (" + r.edgeCoins + ")",
                String.valueOf(r.hits),
                String.format(Locale.ROOT, "%.0f%%  %.0f graus", r.amplitudeFraction * 100f, r.amplitudeDegrees));
        }

        drawCentered(bigFont, "Total: " + total + " pontos", 205f, COIN);
        drawCentered(font, "Moedas: total (na margem)", 160f, Color.GRAY);
        drawCentered(font, "ENTER jogar de novo    ESC tela de teste", 120f, Color.LIGHT_GRAY);
    }

    private void drawRow(float[] columns, float y, Color color, String... cells) {
        font.setColor(color);
        for (int i = 0; i < cells.length; i++) {
            font.draw(batch, cells[i], columns[i], y);
        }
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
