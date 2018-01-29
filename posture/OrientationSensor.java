package edu.vtekaev.posturecontrol.posture;

import edu.vtekaev.posturecontrol.math.Quaternion;

/**
 * Created by Slava on 28.01.2018.
 */

public class OrientationSensor {
    private Quaternion startPosition;
    private Quaternion currentPosition;
    public float euler[];
    private int mainEulerAngle;

    public OrientationSensor() {
        startPosition = new Quaternion();
        currentPosition = new Quaternion();
        euler = new float[3];
        mainEulerAngle = 1;
    }

    public boolean isDeviated() {
        return Math.abs(euler[mainEulerAngle]) > 0.15;
    }

    public void setMainEulerAngle(int angle) {
        mainEulerAngle = angle;
    }

    public void setStartPosition() {
        startPosition.copy(currentPosition);
        startPosition.conjugate();
    }

    public void setQuaternion(Quaternion q) {
        if (q == null) {
            return;
        }
        currentPosition.copy(q);
    }

    public void calcEulerAngles() {
        currentPosition.product(startPosition);
        currentPosition.getEuler(euler);
    }
}
