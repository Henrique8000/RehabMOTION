/*
 * RehabMOTION
 *
 * Henrique Flávio Guimarães - RA 10427920
 * Lucas Zeferino Baracat - RA 10396267
 */
package br.mackenzie.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import br.mackenzie.Main;

public class Lwjgl3Launcher {

    public static void main(String[] args) {
        String portName = args.length > 0 ? args[0] : null;

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("RehabMOTION");
        configuration.setWindowedMode(960, 600);
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        new Lwjgl3Application(new Main(portName), configuration);
    }
}
