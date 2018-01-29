package edu.vtekaev.posturecontrol.posture;

import android.os.Handler;

import edu.vtekaev.posturecontrol.math.Quaternion;
import edu.vtekaev.posturecontrol.net.DataPollingThread;

/**
 * Created by Slava on 03.12.2017.
 */

public class PostureMCU {
    private DataPollingThread dataPollingThread;
    private final byte mpuCount = 5;
    private final byte dataSize = mpuCount * 4 * 4;
    private final String address = "192.168.4.1";
    private final int port = 32015;
    private final int delay = 300;

    public PostureMCU() {
        dataPollingThread = null;
    }

    public byte getSensorsCount() {
        return mpuCount;
    }

    public void startDataPolling(Handler handler) {
        if (dataPollingThread != null && dataPollingThread.checkThreadAlive()) {
            stopDataPolling();
        }
        //start data polling
        dataPollingThread = new DataPollingThread(address, port, delay, dataSize);
        dataPollingThread.setHandler(handler);
        dataPollingThread.start();
    }

    public void stopDataPolling() {
        dataPollingThread.stopPolling();
        dataPollingThread.interrupt();
        dataPollingThread = null;
    }

    private void bytesToQuaternion(Quaternion q, byte[] buff, int offset) {
        if(q == null || buff == null) {
            return;
        }
        //convert byte raw data to floats and put them to quaternion
        int i = offset;
        q.w = Float.intBitsToFloat((buff[(i + 3)] & 0xff) << 24 | (buff[(i + 2)] & 0xff) << 16 | (buff[(i + 1)] & 0xff) << 8 | (buff[i] & 0xff));
        i = i + 4;
        q.x = Float.intBitsToFloat((buff[(i + 3)] & 0xff) << 24 | (buff[(i + 2)] & 0xff) << 16 | (buff[(i + 1)] & 0xff) << 8 | (buff[i] & 0xff));
        i = i + 4;
        q.y = Float.intBitsToFloat((buff[(i + 3)] & 0xff) << 24 | (buff[(i + 2)] & 0xff) << 16 | (buff[(i + 1)] & 0xff) << 8 | (buff[i] & 0xff));
        i = i + 4;
        q.z = Float.intBitsToFloat((buff[(i + 3)] & 0xff) << 24 | (buff[(i + 2)] & 0xff) << 16 | (buff[(i + 1)] & 0xff) << 8 | (buff[i] & 0xff));
    }

    public void getPosture(Posture posture) {
        //create tmp quaternion
        Quaternion q = new Quaternion();
         //if data is not ready
        if (!dataPollingThread.isDataReady()) {
            //wait for data
            synchronized (dataPollingThread) {
                try {
                    dataPollingThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (dataPollingThread) {
            //get data
            byte[] buffer = dataPollingThread.getResponse();
            //set quaternion for each sensor
            for (int i = 0; i < mpuCount; i++) {
                bytesToQuaternion(q, buffer, i * 16);
                posture.getOrientationSensor(i).setQuaternion(q);
            }
        }
    }
}
