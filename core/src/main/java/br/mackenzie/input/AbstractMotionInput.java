package br.mackenzie.input;

public abstract class AbstractMotionInput implements MotionInput {

    public static final float RAW_MAX = 1023f;

    public static final float POT_TOTAL_ANGLE = 270f;

    private static final float SMOOTHING_SPEED = 15f;

    private static final float MIN_CALIBRATION_RANGE = 40f;

    private final float defaultMin;
    private final float defaultMax;

    private float smoothedRaw = RAW_MAX / 2f;
    private boolean hasValue = false;

    private float calibrationMin;
    private float calibrationMax;

    private boolean calibrating = false;
    private float recordedMin;
    private float recordedMax;

    private boolean inverted = false;

    protected AbstractMotionInput(float defaultMin, float defaultMax) {
        this.defaultMin = defaultMin;
        this.defaultMax = defaultMax;
        this.calibrationMin = defaultMin;
        this.calibrationMax = defaultMax;
    }

    protected abstract float readRawValue(float delta);

    @Override
    public final void update(float delta) {
        float raw = readRawValue(delta);
        if (Float.isNaN(raw)) {
            return;
        }

        if (!hasValue) {
            smoothedRaw = raw;
            hasValue = true;
        } else {
            float amount = Math.min(1f, delta * SMOOTHING_SPEED);
            smoothedRaw += (raw - smoothedRaw) * amount;
        }

        if (calibrating) {
            recordedMin = Math.min(recordedMin, smoothedRaw);
            recordedMax = Math.max(recordedMax, smoothedRaw);
        }
    }

    @Override
    public float getPosition() {
        float range = calibrationMax - calibrationMin;
        if (range < 1f) {
            return 0f;
        }
        float t = (smoothedRaw - calibrationMin) / range;
        float position = clamp(t * 2f - 1f, -1f, 1f);
        return inverted ? -position : position;
    }

    @Override
    public float getRawValue() {
        return smoothedRaw;
    }

    @Override
    public float getAngle() {
        return rawToDegrees(smoothedRaw);
    }

    @Override
    public void startCalibration() {
        calibrating = true;
        recordedMin = smoothedRaw;
        recordedMax = smoothedRaw;
    }

    @Override
    public boolean finishCalibration() {
        if (!calibrating) {
            return false;
        }
        calibrating = false;
        if (recordedMax - recordedMin < MIN_CALIBRATION_RANGE) {
            return false;
        }
        calibrationMin = recordedMin;
        calibrationMax = recordedMax;
        return true;
    }

    @Override
    public boolean isCalibrating() {
        return calibrating;
    }

    @Override
    public void resetCalibration() {
        calibrating = false;
        calibrationMin = defaultMin;
        calibrationMax = defaultMax;
    }

    @Override
    public float getCalibratedRangeDegrees() {
        return rawToDegrees(calibrationMax - calibrationMin);
    }

    public float getRecordingRangeDegrees() {
        return calibrating ? rawToDegrees(recordedMax - recordedMin) : 0f;
    }

    public float getCalibrationMin() {
        return calibrationMin;
    }

    public float getCalibrationMax() {
        return calibrationMax;
    }

    @Override
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public boolean isInverted() {
        return inverted;
    }

    public static float rawToDegrees(float raw) {
        return raw * POT_TOTAL_ANGLE / RAW_MAX;
    }

    protected static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
