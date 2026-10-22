package br.mackenzie.input;

public final class MotionInputs {

    private MotionInputs() {
    }

    public static MotionInput createDefault(String portName) {
        if (portName != null || ArduinoMotionInput.isArduinoLikelyConnected()) {
            return new ArduinoMotionInput(portName);
        }
        return new KeyboardMotionInput();
    }

    public static MotionInput createOther(MotionInput current, String portName) {
        if (current instanceof ArduinoMotionInput) {
            return new KeyboardMotionInput();
        }
        return new ArduinoMotionInput(portName);
    }
}
