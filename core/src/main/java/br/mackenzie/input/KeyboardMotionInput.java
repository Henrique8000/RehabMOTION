package br.mackenzie.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class KeyboardMotionInput extends AbstractMotionInput {

    private static final float CENTER = RAW_MAX / 2f;

    private static final float SIMULATED_REACH = 300f;
    private static final float SIMULATED_MIN = CENTER - SIMULATED_REACH;
    private static final float SIMULATED_MAX = CENTER + SIMULATED_REACH;

    private static final float TURN_SPEED = 550f;

    private static final float RETURN_SPEED = 2.5f;

    private float simulatedRaw = CENTER;

    public KeyboardMotionInput() {
        super(SIMULATED_MIN, SIMULATED_MAX);
    }

    @Override
    protected float readRawValue(float delta) {
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

        if (left && !right) {
            simulatedRaw -= TURN_SPEED * delta;
        } else if (right && !left) {
            simulatedRaw += TURN_SPEED * delta;
        } else {
            simulatedRaw += (CENTER - simulatedRaw) * Math.min(1f, RETURN_SPEED * delta);
        }

        simulatedRaw = clamp(simulatedRaw, SIMULATED_MIN, SIMULATED_MAX);
        return simulatedRaw;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getSourceName() {
        return "Teclado (simulação)";
    }

    @Override
    public String getStatus() {
        return "Use as setas ou A/D para simular o movimento";
    }

    @Override
    public void dispose() {
    }
}
