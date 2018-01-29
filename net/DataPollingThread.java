package edu.vtekaev.posturecontrol.net;

/**
 * Created by Slava on 09.12.2017.
 */

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class DataPollingThread extends Thread {
    static final String TAG = "DataPolling";
    private byte[] currentResponse;
    private String address;
    private int port;
    private int delay;
    private byte[] pollRequest = {0x43, 0x44, 0x50};
    private byte dataSize;
    private int timeout = 1000;
    private boolean dataReady;
    private boolean isThreadAlive;
    private Handler handler;

    public DataPollingThread(String address, int port, int delay, byte dataSize) {
        this.address = address;
        this.port = port;
        this.delay = delay;
        currentResponse = new byte[dataSize];
        this.dataSize = dataSize;
        isThreadAlive = true;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void stopPolling() {
        isThreadAlive = false;
    }

    public byte[] getResponse() {
        dataReady = false;
        return currentResponse;
    }

    public boolean isDataReady() {
        return dataReady;
    }

    private boolean PollData(OutputStream writer, InputStream reader) {
        try {
            synchronized (this) {
                //Log.d(TAG, "Send request");
                writer.write(pollRequest);
                //Log.d(TAG, "Flush writer");
                writer.flush();
                // Log.d(TAG, "Read line");
                reader.read(currentResponse, 0, dataSize);
            }
            //Log.d(TAG, "Result: ");
        } catch (IOException e) {
            Log.e(TAG, "Poll data error: ", e);
            return false;
        }
        return true;
    }

    private Socket openSocket(String address, int port) {
        InetAddress serverAddress;
        Socket socket;
        try {
            serverAddress = InetAddress.getByName(address);
            socket = new Socket(serverAddress, port);
            socket.setSoTimeout(timeout);
        } catch (Exception e) {
            Log.e(TAG, "Create socket error: ", e);
            return null;
        }
        return socket;
    }

    private void closeConnection(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "close connection error: ", e);
        }
    }

    public boolean checkThreadAlive() {
        return isThreadAlive;
    }

    @Override
    public void run() {
        do {
            Log.d(TAG, "Trying to connect");
            //Open and connect socket
            Socket socket = openSocket(address, port);
            //If cant open socket close the task
            if (socket == null) {
                break;
            }
            //Get input and output streams
            InputStream reader;
            OutputStream writer;
            try {
                writer = socket.getOutputStream();
                reader = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Create writer/reader error: ", e);
                closeConnection(socket);
                //if there was some errors trying to reconnect
                continue;
            }

            Log.d(TAG, "connected");

            if (handler != null) {
                handler.sendEmptyMessage(-1);
            }

            //while poll data is ok
            while (PollData(writer, reader)) {
                //publish the progress after receiving data
                dataReady = true;
                synchronized (this) {
                    this.notify();
                }
                //wait for delay
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //close socket after data transfer ended
            closeConnection(socket);
            //send message that connection was lost
            if (handler != null) {
                handler.sendEmptyMessage(-2);
            }

            //if task is still not canceled trying to reconnect
        } while (isThreadAlive);
        Log.d(TAG, "Task is over");
        //send message that connection was lost
        if (handler != null) {
            handler.sendEmptyMessage(-2);
        }
    }
}
