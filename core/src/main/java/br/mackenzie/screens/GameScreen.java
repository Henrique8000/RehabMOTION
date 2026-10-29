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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import br.mackenzie.Main;
import br.mackenzie.input.MotionInput;

import static br.mackenzie.screens.RiverArt.*;

public class GameScreen extends ScreenAdapter {

    private static final float SCROLL_SPEED = 170f;

    private final Main game;

    private final FitViewport viewport;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout = new GlyphLayout();

    private float elapsed;
    private float scroll;

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

        update(delta);

        draw(input, boatX);
    }

    private void update(float delta) {
        elapsed += delta;
        scroll += SCROLL_SPEED * delta;
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
        drawBoat(shapes, boatX, BOAT_Y + (float) Math.sin(elapsed * 2.2f) * 3f);
        drawHudPanel();
        shapes.end();

        batch.begin();
        drawHudText(input);
        batch.end();
    }

    private void drawHudPanel() {
        shapes.setColor(PANEL);
        shapes.rect(0f, RIVER_TOP, WORLD_WIDTH, HUD_HEIGHT);
        shapes.rect(0f, 0f, WORLD_WIDTH, 34f);
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
