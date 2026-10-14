package br.edu.recode;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;

public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        String port = System.getProperty("recode.port", "").trim();
        boolean simulated = Boolean.parseBoolean(System.getProperty("recode.simulate", "false"));
        String phase = System.getProperty("recode.phase", "campaign").trim();
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
            PedalSource source = simulated ? new SimulatedPedalSource() : new SerialPedalSource(port);
            if ("campaign".equals(phase)) {
                config.setTitle(simulated ? "ReCode - Campanha (simulacao)" : "ReCode - Campanha");
                game = new CampaignGame(source, simulated);
            } else if ("1".equals(phase)) {
                config.setTitle(simulated ? "ReCode - Circuitos Basicos (simulacao)" : "ReCode - Circuitos Basicos");
                game = new ReCodePedalGame(source, simulated);
            } else if ("2".equals(phase)) {
                config.setTitle(simulated ? "ReCode - Rede Instavel (simulacao)" : "ReCode - Rede Instavel");
                game = new PhaseTwoGame(source, simulated);
            } else if ("3".equals(phase)) {
                config.setTitle(simulated ? "ReCode - Sistema Central (simulacao)" : "ReCode - Sistema Central");
                game = new PhaseThreeGame(source, simulated);
            } else {
                throw new IllegalArgumentException("Use -Pphase=campaign, 1, 2 ou 3");
            }
        } else {
            throw new IllegalArgumentException("Use -Ppreview=rede ou -Ppreview=central");
        }
        new Lwjgl3Application(game, config);
    }
}
