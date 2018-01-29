package edu.vtekaev.posturecontrol.posture;

import android.os.Handler;

/**
 * Created by Slava on 27.01.2018.
 */

public class PostureAnalyzer {
    private PostureMCU postureMCU;
    private Posture posture;
    private Handler handler;
    private Analyzer analyzer;
    private boolean isAnalysisStarted;
    private boolean isDataPollingStarted;
    private boolean isStartPositionSet;

    public PostureAnalyzer(Handler handler) {
        posture = new Posture();
        postureMCU = new PostureMCU();
        analyzer = null;
        this.handler = handler;
        isDataPollingStarted = false;
        isAnalysisStarted = false;
        isStartPositionSet = false;
    }

    public boolean checkAnalysisStatus() {
        return isAnalysisStarted;
    }

    public void connect() {
        //if analyser has already connected, stop it
        if (analyzer != null && isDataPollingStarted) {
            disconnect();
        }
        //create new analyzer
        analyzer = new Analyzer();
        isDataPollingStarted = true;
        analyzer.start();
    }

    public void disconnect() {
        isDataPollingStarted = false;
        analyzer.interrupt();
        analyzer = null;
    }

    public void startAnalysis() {
        //if analyzer is not connected step out from func
        if (!isDataPollingStarted) {
            return;
        }
        //start analysis
        isAnalysisStarted = true;
    }

    public void stopAnalysis() {
        //stop analysis
        isAnalysisStarted = false;
        isStartPositionSet = false;
    }

    private class Analyzer extends Thread {
        @Override
        public void run() {
            //start data polling
            postureMCU.startDataPolling(handler);
            //while data polling is in progress
            while (isDataPollingStarted) {
                //load posture from mcu
                postureMCU.getPosture(posture);
                //if analysis is not started then continue
                if (!isAnalysisStarted) {
                    continue;
                }
                //is start position was not set
                if (!isStartPositionSet) {
                    //set start position
                    posture.setStartPosition();
                    isStartPositionSet = true;
                }
                //send deviation mask of sensors to ui thread
                handler.sendEmptyMessage(posture.getDeviatedSensors());
            }
            //stop data polling
            postureMCU.stopDataPolling();
        }
    }
}
