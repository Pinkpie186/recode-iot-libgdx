package br.edu.recode;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;
import com.fazecast.jSerialComm.SerialPort;

public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        String port = System.getProperty("recode.port", "").trim();
        boolean keyboard = Boolean.parseBoolean(System.getProperty("recode.keyboard", "false"));
        for (String argument : args) {
            if ("--keyboard".equals(argument)) keyboard = true;
            else if ("--sensor".equals(argument)) keyboard = false;
            else if (argument.startsWith("--port=")) port = argument.substring(7).trim();
        }
        if (!keyboard && port.isEmpty()) port = detectEsp32Port();
        boolean simulated = keyboard || Boolean.parseBoolean(System.getProperty("recode.simulate", "false"));
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
            PedalSource source = keyboard ? new KeyboardPedalSource()
                    : simulated ? new SimulatedPedalSource() : new SerialPedalSource(port);
            if ("campaign".equals(phase)) {
                config.setTitle(keyboard ? "ReCode - Campanha (teclado)"
                        : simulated ? "ReCode - Campanha (simulacao)" : "ReCode - Campanha");
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

    private static String detectEsp32Port() {
        String candidate = "";
        int candidates = 0;
        for (SerialPort serial : SerialPort.getCommPorts()) {
            String description = (serial.getDescriptivePortName() + " " +
                    serial.getPortDescription()).toLowerCase();
            if (description.contains("bluetooth") || description.contains("bthenum"))
                continue;
            if (description.contains("ch910") || description.contains("usb-serial") ||
                    description.contains("usb serial") || description.contains("usb-enhanced"))
                return serial.getSystemPortName();
            candidate = serial.getSystemPortName();
            candidates++;
        }
        return candidates == 1 ? candidate : "";
    }
}
