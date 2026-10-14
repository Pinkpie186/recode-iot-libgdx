package br.edu.recode;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;

public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        String port = System.getProperty("recode.port", "").trim();
        boolean simulated = Boolean.parseBoolean(System.getProperty("recode.simulate", "false"));
        String preview = System.getProperty("recode.preview", "").trim().toLowerCase();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(960, 540);
        config.useVsync(true);
        ApplicationAdapter game;
        if ("rede".equals(preview)) {
            config.setTitle("ReCode - PREVIA Rede Instavel");
            game = new ScenarioPreviewGame("scenarios/rede_instavel.png", "Rede Instavel");
        } else if ("central".equals(preview)) {
            config.setTitle("ReCode - PREVIA Sistema Central");
            game = new ScenarioPreviewGame("scenarios/sistema_central.png", "Sistema Central");
        } else if (preview.isEmpty()) {
            config.setTitle(simulated ? "ReCode - SIMULACAO de desenvolvimento" : "ReCode - Prototipo do pedal");
            PedalSource source = simulated ? new SimulatedPedalSource() : new SerialPedalSource(port);
            game = new ReCodePedalGame(source, simulated);
        } else {
            throw new IllegalArgumentException("Use -Ppreview=rede ou -Ppreview=central");
        }
        new Lwjgl3Application(game, config);
    }
}
