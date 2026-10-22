package br.mackenzie.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fazecast.jSerialComm.SerialPort;

public class ArduinoMotionInput extends AbstractMotionInput {

    public static final int BAUD_RATE = 9600;

    private static final long DATA_TIMEOUT_MS = 1000;

    private static final long NO_DATA_SWITCH_MS = 5000;

    private static final long RETRY_DELAY_MS = 2000;

    private static final String[] ARDUINO_HINTS = {
        "arduino", "ch340", "ch341", "ch910", "cp210", "ftdi", "usb serial", "usb-serial",
        "ttyacm", "ttyusb", "usbmodem", "usbserial", "wchusbserial"
    };

    private static final String[] IGNORED_HINTS = {"bluetooth", "debug-console", "wlan"};

    private final String requestedPortName;
    private final Thread readerThread;

    private volatile boolean running = true;
    private volatile int lastValue = -1;
    private volatile long lastDataTime = 0;
    private volatile String status = "Procurando Arduino...";

    private SerialPort port;
    private long portOpenedAt;
    private int nextCandidate = 0;
    private final StringBuilder lineBuffer = new StringBuilder();

    public ArduinoMotionInput(String portName) {
        super(0f, RAW_MAX);
        this.requestedPortName = portName;
        this.readerThread = new Thread(this::readLoop, "leitor-arduino");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    @Override
    protected float readRawValue(float delta) {
        if (!isConnected()) {
            return Float.NaN;
        }
        return lastValue;
    }

    @Override
    public boolean isConnected() {
        return lastValue >= 0 && System.currentTimeMillis() - lastDataTime < DATA_TIMEOUT_MS;
    }

    @Override
    public String getSourceName() {
        return "Arduino";
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void dispose() {
        running = false;
        readerThread.interrupt();
        try {
            readerThread.join(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void readLoop() {
        byte[] buffer = new byte[256];
        try {
            while (running) {
                if (port == null) {
                    port = openNextPort();
                    if (port == null) {
                        sleep(RETRY_DELAY_MS);
                    }
                    continue;
                }

                int count = port.readBytes(buffer, buffer.length);
                if (count < 0) {
                    closePort("Arduino desconectado. Tentando reconectar...");
                    sleep(RETRY_DELAY_MS);
                    continue;
                }
                for (int i = 0; i < count; i++) {
                    handleByte((char) buffer[i]);
                }

                long lastUseful = Math.max(portOpenedAt, lastDataTime);
                if (System.currentTimeMillis() - lastUseful > NO_DATA_SWITCH_MS) {
                    closePort("Sem dados em " + port.getSystemPortName() + ". Tentando de novo...");
                }
            }
        } catch (Throwable error) {
            status = "Erro na porta serial: " + error.getMessage();
        } finally {
            if (port != null) {
                port.closePort();
                port = null;
            }
        }
    }

    private void handleByte(char c) {
        if (c == '\n') {
            parseLine(lineBuffer.toString().trim());
            lineBuffer.setLength(0);
        } else if (c != '\r' && lineBuffer.length() < 32) {
            lineBuffer.append(c);
        }
    }

    private void parseLine(String line) {
        try {
            int value = Integer.parseInt(line);
            if (value >= 0 && value <= RAW_MAX) {
                lastValue = value;
                lastDataTime = System.currentTimeMillis();
                status = "Conectado em " + port.getSystemPortName();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private SerialPort openNextPort() {
        List<SerialPort> candidates = findCandidatePorts(requestedPortName);
        if (candidates.isEmpty()) {
            status = requestedPortName != null
                ? "Porta " + requestedPortName + " não encontrada. Verifique o cabo."
                : "Nenhum Arduino encontrado. Verifique o cabo USB.";
            return null;
        }

        for (int tries = 0; tries < candidates.size(); tries++) {
            SerialPort candidate = candidates.get(nextCandidate % candidates.size());
            nextCandidate++;

            candidate.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
            if (candidate.openPort()) {
                portOpenedAt = System.currentTimeMillis();
                lineBuffer.setLength(0);
                status = "Abrindo " + candidate.getSystemPortName() + "...";
                return candidate;
            }
        }
        status = "Não foi possível abrir a porta. Cabo ligado? Monitor Serial do Arduino fechado?";
        return null;
    }

    private void closePort(String message) {
        if (port != null) {
            port.closePort();
            port = null;
        }
        status = message;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    static List<SerialPort> findCandidatePorts(String portName) {
        List<SerialPort> likely = new ArrayList<>();
        List<SerialPort> others = new ArrayList<>();

        for (SerialPort p : SerialPort.getCommPorts()) {
            if (portName != null) {
                if (portName.equalsIgnoreCase(p.getSystemPortName())
                        || portName.equalsIgnoreCase(p.getSystemPortPath())) {
                    likely.add(p);
                }
                continue;
            }
            String text = describe(p);
            if (containsAny(text, IGNORED_HINTS)) {
                continue;
            }
            if (containsAny(text, ARDUINO_HINTS)) {
                likely.add(p);
            } else {
                others.add(p);
            }
        }

        if (portName != null && likely.isEmpty()) {
            try {
                likely.add(SerialPort.getCommPort(portName));
            } catch (Exception ignored) {
            }
        }

        likely.addAll(others);
        return likely;
    }

    public static boolean isArduinoLikelyConnected() {
        try {
            for (SerialPort p : SerialPort.getCommPorts()) {
                String text = describe(p);
                if (containsAny(text, ARDUINO_HINTS) && !containsAny(text, IGNORED_HINTS)) {
                    return true;
                }
            }
        } catch (Throwable error) {
        }
        return false;
    }

    private static String describe(SerialPort p) {
        return (p.getSystemPortPath() + " " + p.getDescriptivePortName() + " " + p.getPortDescription())
            .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
