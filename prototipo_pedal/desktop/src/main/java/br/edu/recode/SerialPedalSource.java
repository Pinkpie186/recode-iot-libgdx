package br.edu.recode;

import com.fazecast.jSerialComm.SerialPort;
import java.util.concurrent.ConcurrentLinkedQueue;

final class SerialPedalSource implements PedalSource {
    private final ConcurrentLinkedQueue<PedalFrame> frames = new ConcurrentLinkedQueue<>();
    private final String portName;
    private volatile boolean running;
    private volatile String error = "";
    private volatile long lastFrameNanos = -1;
    private SerialPort port;
    private Thread reader;

    SerialPedalSource(String portName) {
        this.portName = portName;
    }

    @Override public void start() {
        if (portName.isEmpty()) {
            error = "Informe a porta serial com -Pport=COM3";
            return;
        }
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(115200);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        if (!port.openPort()) {
            error = "Nao foi possivel abrir " + portName;
            return;
        }
        running = true;
        reader = new Thread(this::readLoop, "recode-forearm-serial");
        reader.setDaemon(true);
        reader.start();
    }

    @Override public PedalFrame poll() { return frames.poll(); }
    @Override public long lastFrameNanos() { return lastFrameNanos; }
    @Override public String error() { return error; }

    private void readLoop() {
        byte[] buffer = new byte[128];
        StringBuilder line = new StringBuilder(256);
        try {
            while (running) {
                int count = port.readBytes(buffer, buffer.length);
                if (count < 0) throw new IllegalStateException("Falha de leitura serial");
                for (int i = 0; i < count; i++) {
                    char c = (char) (buffer[i] & 0xff);
                    if (c == '\n') {
                        try {
                            PedalFrame frame = PedalFrame.parse(line.toString().trim());
                            frames.add(frame);
                            lastFrameNanos = System.nanoTime();
                            error = "";
                        } catch (RuntimeException ex) {
                            // Linhas incompletas sao ignoradas; incompatibilidade fica visivel.
                            String reason = ex.getMessage();
                            if (reason != null && (reason.startsWith("Firmware do sensor") ||
                                    reason.startsWith("Controle incompativel") ||
                                    reason.startsWith("MPU6050 nao") ||
                                    reason.startsWith("Mantenha o antebraco"))) error = reason;
                        }
                        line.setLength(0);
                    } else if (line.length() < 256 && c >= 32 && c < 127) {
                        line.append(c);
                    } else if (line.length() >= 256) {
                        line.setLength(0);
                    }
                }
            }
        } catch (RuntimeException ex) {
            error = ex.getMessage() == null ? "Erro serial" : ex.getMessage();
        } finally {
            running = false;
            if (port != null) port.closePort();
        }
    }

    @Override public void close() {
        running = false;
        if (port != null) port.closePort();
        if (reader != null) reader.interrupt();
    }
}
