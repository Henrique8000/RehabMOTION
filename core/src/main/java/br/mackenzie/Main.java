package br.mackenzie;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

import br.mackenzie.input.MotionInput;
import br.mackenzie.input.MotionInputs;
import br.mackenzie.screens.MotionTestScreen;

public class Main extends Game {

    private final String portName;
    private MotionInput input;

    public Main(String portName) {
        this.portName = portName;
    }

    public Main() {
        this(null);
    }

    @Override
    public void create() {
        input = MotionInputs.createDefault(portName);
        setScreen(new MotionTestScreen(this));
    }

    @Override
    public void render() {
        input.update(Gdx.graphics.getDeltaTime());
        super.render();
    }

    public MotionInput getInput() {
        return input;
    }

    public void switchInput() {
        MotionInput old = input;
        input = MotionInputs.createOther(old, portName);
        old.dispose();
    }

    public String getPortName() {
        return portName;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (input != null) {
            input.dispose();
        }
    }
}
