package edu.vtekaev.posturecontrol;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import edu.vtekaev.posturecontrol.posture.PostureAnalyzer;

public class MainActivity extends AppCompatActivity {
    private PostureAnalyzer postureAnalyzer;
    private final byte sensorsCount = 5;
    private ImageView sensorViews[];
    private Button connectBtn;
    private Button analysisBtn;
    private TextView connectedText;
    private TextView disconnectedText;
    private Vibrator vibrator;

    private void initializeSensorViews() {
        sensorViews = new ImageView[sensorsCount];
        sensorViews[0] = findViewById(R.id.mpuUBView);
        sensorViews[1] = findViewById(R.id.mpuMBView);
        sensorViews[2] = findViewById(R.id.mpuRSView);
        sensorViews[3] = findViewById(R.id.mpuLSView);
        sensorViews[4] = findViewById(R.id.mpuLBView);
    }

    private void initializeUIElements() {
        connectBtn = findViewById(R.id.connectBtn);
        analysisBtn = findViewById(R.id.analysisBtn);
        connectedText = findViewById(R.id.connectedText);
        disconnectedText = findViewById(R.id.disconnectedText);
        initializeSensorViews();
    }

    private void showInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Attention!")
                .setMessage("Connect to wifi before using connect button\nname: PostureControl\npass: 123445678")
                .setCancelable(false)
                .setNegativeButton("ОК",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        postureAnalyzer = new PostureAnalyzer(new AnalysisHandler());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        initializeUIElements();
        showInfoDialog();
    }

    public void onAnalysisBtnClick(View view) {
        //if analysis is not working
        if (!postureAnalyzer.checkAnalysisStatus()) {
            postureAnalyzer.startAnalysis();
            analysisBtn.setText("STOP ANALYSIS");
        } else {
            postureAnalyzer.stopAnalysis();
            analysisBtn.setText("START ANALYSIS");

        }
        //reset posture view
        setSensorsView(0);
    }

    public void onConnectBtnClick(View view) {
        postureAnalyzer.connect();
    }


    private void setSensorsView(int mask) {
        for (int i = 0; i < sensorsCount; i++) {
            //if i bit is 1 (means sensor was deviated)
            if (((mask >> i) & 1) == 1) {
                //show deviated sensor in red color
                sensorViews[i].setImageResource(R.drawable.dot_r);
                //vibrate
                vibrator.vibrate(300);

            } else {
                //show sensor in green color
                sensorViews[i].setImageResource(R.drawable.dot_g);
            }
        }
    }

    public class AnalysisHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("MAIN", "Handle message: " + msg.what);
            //if there is info about sensors
            if (msg.what >= 0) {
                //update posture view
                setSensorsView(msg.what);
            } else if (msg.what == -1) {
                //if connected successfully
                connectBtn.setEnabled(false);
                connectBtn.setVisibility(View.INVISIBLE);

                analysisBtn.setVisibility(View.VISIBLE);
                analysisBtn.setEnabled(true);

                disconnectedText.setVisibility(View.INVISIBLE);
                connectedText.setVisibility(View.VISIBLE);
            } else if (msg.what == -2) {
                //if disconnected
                connectBtn.setEnabled(true);
                connectBtn.setVisibility(View.VISIBLE);

                analysisBtn.setVisibility(View.INVISIBLE);
                analysisBtn.setEnabled(false);

                disconnectedText.setVisibility(View.VISIBLE);
                connectedText.setVisibility(View.INVISIBLE);
            }
        }
    }
}
