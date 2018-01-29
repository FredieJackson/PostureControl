package edu.vtekaev.posturecontrol.posture;

import android.util.Log;

/**
 * Created by Slava on 28.01.2018.
 */

public class Posture {
    private final byte sensorCount = 5;
    private OrientationSensor orientationSensors[];
    public OrientationSensor leftShoulder;
    public OrientationSensor rightShoulder;
    public OrientationSensor upperBack;
    public OrientationSensor midBack;
    public OrientationSensor lowerBack;

    public Posture() {
        //initializing sensor objects
        orientationSensors = new OrientationSensor[sensorCount];
        for (int i = 0; i < sensorCount; i++) {
            orientationSensors[i] = new OrientationSensor();
        }
        //links for comfortable using
        leftShoulder = orientationSensors[3];
        rightShoulder = orientationSensors[2];
        upperBack = orientationSensors[0];
        midBack = orientationSensors[1];
        lowerBack = orientationSensors[4];

        //change main angle of shoulders' sensors
        leftShoulder.setMainEulerAngle(2);
        rightShoulder.setMainEulerAngle(2);
    }

    public OrientationSensor getOrientationSensor(int sensor_num) {
        if (sensor_num < 0 || sensor_num > sensorCount - 1) {
            return null;
        }
        return orientationSensors[sensor_num];
    }

    public void setStartPosition() {
        for (int i = 0; i < sensorCount; i++) {
            orientationSensors[i].setStartPosition();
        }
    }

    public void calcEuler() {
        //calculate euler angles from quaternion for each sensor
        for (int i = 0; i < sensorCount; i++) {
            orientationSensors[i].calcEulerAngles();
            Log.d("SO", "imu: " + i + ". " +
                    orientationSensors[i].euler[0] + " : " +
                    orientationSensors[i].euler[1] + " : " +
                    orientationSensors[i].euler[2]);
        }
    }

    public byte getDeviatedSensors() {
        //calculate sensors' angles
        calcEuler();
        byte result = 0;
        //for each sensor check deviation and form bit mask
        for (int i = 0; i < sensorCount; i++) {
            if (orientationSensors[i].isDeviated()) {
                //if i sensor was deviated set i bit as 1
                result |= 1 << i;
            }
        }
        //return bit mask
        return result;
    }
}
