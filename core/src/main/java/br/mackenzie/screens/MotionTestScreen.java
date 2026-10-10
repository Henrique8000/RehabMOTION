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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import br.mackenzie.Main;
import br.mackenzie.input.AbstractMotionInput;
import br.mackenzie.input.MotionInput;

import static br.mackenzie.screens.RiverArt.*;

public class MotionTestScreen extends ScreenAdapter {

    private final Main game;

    private final FitViewport viewport;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout = new GlyphLayout();

    private float time;
    private String message = "";
    private float messageTimer;

    public MotionTestScreen(Main game) {
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
        time += delta;
        messageTimer -= delta;

        handleKeys();

        MotionInput input = game.getInput();

        ScreenUtils.clear(WATER);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawRiver(shapes, time * 90f);
        drawBuoy(xFor(-1f));
        drawBuoy(xFor(1f));
        drawBoat(shapes, xFor(input.getPosition()), BOAT_Y + (float) Math.sin(time * 2.2f) * 3f);
        drawHudPanel();
        drawPositionBar(input.getPosition());
        if (input.isCalibrating()) {
            shapes.setColor(PANEL);
            shapes.rect(160f, 230f, 640f, 150f);
        }
        shapes.end();

        batch.begin();
        drawHudText(input);
        drawHelp();
        if (input.isCalibrating()) {
            drawCalibrationText(input);
        } else if (messageTimer > 0f) {
            drawCentered(bigFont, message, 300f, Color.WHITE);
        }
        batch.end();
    }

    private void handleKeys() {
        MotionInput input = game.getInput();

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            if (input.isCalibrating()) {
                if (input.finishCalibration()) {
                    showMessage(String.format(Locale.ROOT, "Calibrado: %.0f graus", input.getCalibratedRangeDegrees()));
                } else {
                    showMessage("Movimento pequeno demais. Tente de novo.");
                }
            } else {
                input.startCalibration();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            input.resetCalibration();
            showMessage("Calibração padrão");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            input.setInverted(!input.isInverted());
            showMessage(input.isInverted() ? "Sentido invertido" : "Sentido normal");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            game.switchInput();
            showMessage("Controle: " + game.getInput().getSourceName());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    private void showMessage(String text) {
        message = text;
        messageTimer = 2.5f;
    }

    private void drawBuoy(float x) {
        RiverArt.drawBuoy(shapes, x, 300f + (float) Math.sin(time * 3f + x) * 4f);
    }

    private void drawHudPanel() {
        shapes.setColor(PANEL);
        shapes.rect(0f, RIVER_TOP, WORLD_WIDTH, HUD_HEIGHT);
        shapes.rect(0f, 0f, WORLD_WIDTH, 34f);
    }

    private void drawPositionBar(float position) {
        float left = 600f, width = 320f, y = 520f;
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(left, y, width, 14f);
        shapes.setColor(Color.GRAY);
        shapes.rect(left + width / 2f - 1f, y - 4f, 2f, 22f);
        float markerX = left + (position + 1f) / 2f * width;
        shapes.setColor(OK);
        shapes.rect(markerX - 4f, y - 6f, 8f, 26f);
    }

    private void drawHudText(MotionInput input) {
        float x = 20f;
        float y = WORLD_HEIGHT - 16f;
        float line = 22f;

        font.setColor(input.isConnected() ? OK : WARNING);
        font.draw(batch, input.getSourceName() + "  |  " + input.getStatus(), x, y);

        font.setColor(Color.WHITE);
        font.draw(batch, String.format(Locale.ROOT, "Bruto: %4.0f     Ângulo: %5.1f graus     Posição: %+.2f",
            input.getRawValue(), input.getAngle(), input.getPosition()), x, y - line);

        font.draw(batch, String.format(Locale.ROOT, "Amplitude calibrada: %.0f graus%s",
            input.getCalibratedRangeDegrees(), calibrationRangeText(input)), x, y - 2 * line);

        font.draw(batch, "Sentido: " + (input.isInverted() ? "invertido" : "normal"), x, y - 3 * line);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "-1", 590f, 556f);
        font.draw(batch, "+1", 912f, 556f);
        font.draw(batch, "Posição", 735f, 510f);
    }

    private String calibrationRangeText(MotionInput input) {
        if (input instanceof AbstractMotionInput) {
            AbstractMotionInput base = (AbstractMotionInput) input;
            return String.format(Locale.ROOT, "  (bruto de %.0f a %.0f)", base.getCalibrationMin(), base.getCalibrationMax());
        }
        return "";
    }

    private void drawHelp() {
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "C calibrar    R resetar    I inverter sentido    TAB trocar Arduino/teclado    ESC sair",
            20f, 23f);
    }

    private void drawCalibrationText(MotionInput input) {
        drawCentered(bigFont, "CALIBRANDO", 360f, WARNING);
        drawCentered(font, "Mova até o extremo de um lado, depois até o outro.", 315f, Color.WHITE);
        drawCentered(font, "Quando terminar, aperte C.", 293f, Color.WHITE);
        if (input instanceof AbstractMotionInput) {
            float range = ((AbstractMotionInput) input).getRecordingRangeDegrees();
            drawCentered(font, String.format(Locale.ROOT, "Amplitude até agora: %.0f graus", range), 260f, OK);
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
