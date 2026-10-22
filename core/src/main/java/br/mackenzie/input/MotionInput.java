package br.mackenzie.input;

public interface MotionInput {

    void update(float delta);

    float getPosition();

    float getRawValue();

    float getAngle();

    boolean isConnected();

    String getSourceName();

    String getStatus();

    void startCalibration();

    boolean finishCalibration();

    boolean isCalibrating();

    void resetCalibration();

    float getCalibratedRangeDegrees();

    void setInverted(boolean inverted);

    boolean isInverted();

    void dispose();
}
