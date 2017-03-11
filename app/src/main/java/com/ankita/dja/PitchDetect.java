package com.ankita.dja;

//Pitch detection is of interest whenever a single quasi­periodic(irregular period) sound source is to be studied

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;


import java.io.IOException;
import java.lang.*;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.shokai.firmata.ArduinoFirmata;
import org.shokai.firmata.ArduinoFirmataEventHandler;

import static android.R.attr.delay;


// Multithreading refers to two or more tasks executing concurrently within a single program.
// A thread is an independent path of execution within a program.
// Many threads can run concurrently within a program.
// Every thread in Java is created and controlled by the java.lang.Thread class.
// Looper is a class which is used to execute the Messages(Runnables) in a queue
// Android modifies the user interface and handles input events from one single thread, called the main thread.
// Android collects all events in this thread in a queue and processes this queue with an instance of the Looper class.
// Android supports the usage of the Thread class to perform asynchronous processing.
// Android also supplies the java.util.concurrent package to perform something in the background.
// For example, by using the ThreadPools and Executor classes.
// If you need to update the user interface from a new Thread, you need to synchronize with the main thread. Because of this restrictions, Android developer typically use Android specific code constructs.

// Android provides additional constructs to handle concurrently in comparison with standard Java.

// You can use the android.os.Handler class or the AsyncTasks classes.
// More sophisticated approaches are based on the Loader class, retained fragments and services.


////for pitch detection 2048 samples for buffersize is reasonable. Common - 1024, 2048
// Sample rate = when recording music or many types of acoustic events,
// audio waveforms are typically sampled at 44.1 kHz (CD), 48 kHz, 88.2 kHz, or 96 kHz.
//giving a 20 kHz maximum frequency. 20 kHz is the highest frequency generally audible by humans,
// so making 44.1 kHz the logical choice for most audio material.


// (samplerate    int audioBufferSize,        int bufferOverlap) ( samplerate size in overlap in samples)
//     throws javax.sound.sampled.LineUnavailableException

//bufferOverlap: no overlap

// You have to use runOnUiThread() when you want to update your UI from a Non-UI Thread.
// For eg- If you want to update your UI from a background Thread.
// You can also use Handler for the same thing.
// Runs the specified action on the UI thread.
// If the current thread is the UI thread, then the action is executed immediately.
// If the current thread is not the UI thread, the action is posted to the event queue of the UI thread.


//If you need to update the user interface from a new Thread, you need to synchronize with the main thread.
//used in this library


/*Activity.runOnUiThread() is a special case of more generic Handlers.
With Handler you can create your own event query within your own thread.
 Using Handlers instantiated with default constructor doesn't mean "code will run on UI thread" in general.
 By default, handlers binded to Thread from which they was instantiated from.

 To create Handler that is guaranteed to bind to UI (main) thread
 you should create Handler object binded to Main Looper like this:

        Handler mHandler = new Handler(Looper.getMainLooper());*/


//The amount of time allotted for processing is called the Buffer Size.
// Often times a smaller Buffer Size is desirable, however, not one that is too small. Here's why:
//If you have a very large Buffer Size, you will notice a lag between when you speak in to the Mic,
// and when the sound of your voice comes out of your speakers. While this can be very annoying,
// a large Buffer Size also makes recording audio less demanding on your computer.
// If you have a very small Buffer Size, you will notice little to no lag at all between speaking into the Mic
// and the audio coming out of the speakers. This makes recording and hearing your own singing much easier,
// however this can also place more strain on your computer, as it has very little time to process the audio.



public class PitchDetect extends AppCompatActivity implements MediaController.MediaPlayerControl {


    /////////////////////////////////////////////////////////////////////////
    String TAG = "AndroidFirmata";

    //   private Handler handler;
    private ArduinoFirmata arduino;
    private ToggleButton btnDigitalWrite;
    private Button btnCheck;

    //////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pitch_detect);

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22100, 1024, 0);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.MPM, 22100, 1024, new PitchDetectionHandler() {
            //http://miracle.otago.ac.nz/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf
            //A fast, accurate and robust method for finding the continuous pitch in monophonic musical sounds.
            //https://0110.be/releases/TarsosDSP/TarsosDSP-1.6/TarsosDSP-1.6-Documentation/be/hogent/tarsos/dsp/pitch/PitchProcessor.PitchEstimationAlgorithm.html

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();

                //////////////////////////////////////////////////////////////////////////

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TextView text = (TextView) findViewById(R.id.textView1);


                        processPitch(pitchInHz);


                    }
                });

            }
        }));

        new Thread(dispatcher, "Audio Dispatcher").start();


        ///////////////////////////////////////ARDUINO/////////////////////////////////////////////////

        this.btnDigitalWrite = (ToggleButton) findViewById(R.id.btn_digital_write);
        this.btnCheck = (Button) findViewById(R.id.btn_check);


        Log.v(TAG, "start");

        this.setTitle(this.getTitle() + " v" + ArduinoFirmata.VERSION);

        this.arduino = new ArduinoFirmata(this);
        final Activity self = this;
        arduino.setEventHandler(new ArduinoFirmataEventHandler() {
            public void onError(String errorMessage) {
                Log.e(TAG, errorMessage);
            }

            public void onClose() {
                Log.v(TAG, "arduino closed");
                self.finish();
            }
        });


        //to connect the arduino..... needs exception handling

        try {
            arduino.connect();
            Log.v(TAG, "Board Version : " + arduino.getBoardVersion());
            // Log.d(TAG, "No Connection");

        } catch (IOException e) {
            e.printStackTrace();

            // finish();         //so as the app wouldn't close itself
        } catch (InterruptedException e) {
            e.printStackTrace();
            //   finish();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////

        btnDigitalWrite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, final boolean isChecked) {

                Log.v(TAG, isChecked ? "LED on" : "LED off");
                arduino.digitalWrite(13, isChecked);


            }
        });
        // btnCheck.setOnClickListener();
    }


    @Override
    public void onBackPressed() {

        Intent intent_pitch = new Intent(PitchDetect.this, MainActivity.class);
        startActivity(intent_pitch);
        finish();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    public void processPitch(float pitchInHz) {

        TextView text = (TextView) findViewById(R.id.textView1);
        TextView noteText = (TextView) findViewById(R.id.textView2);
        text.setText("" + pitchInHz);

        if (pitchInHz == -1) {


        } else if (pitchInHz < 100) {
            noteText.setText("Y");
            // arduino.digitalWrite(13, true);
        } else if (pitchInHz > 100 && pitchInHz < 170) {
            noteText.setText("A");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 170 && pitchInHz < 230) {
            noteText.setText("B");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 230 && pitchInHz < 257) {
            noteText.setText("C");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 257 && pitchInHz < 270) {
            noteText.setText("D");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 270 && pitchInHz < 320) {
            noteText.setText("E");
        } else if (pitchInHz > 320 && pitchInHz < 470) {
            noteText.setText("F");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 470 && pitchInHz < 570) {
            noteText.setText("G");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 570 && pitchInHz < 650) {
            noteText.setText("H");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 650 && pitchInHz < 750) {
            noteText.setText("I");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 750 && pitchInHz < 900) {
            noteText.setText("J");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 900 && pitchInHz < 1100) {
            noteText.setText("J");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 1100 && pitchInHz < 1700) {
            noteText.setText("K");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 1700 && pitchInHz < 2500) {
            noteText.setText("L");
            //  arduino.digitalWrite(13, false);
        } else if (pitchInHz > 2500 && pitchInHz < 3300) {
            noteText.setText("M");
            //  arduino.digitalWrite(13, false);
        } else {
            noteText.setText("X");
            // arduino.digitalWrite(13, true);*/


        }

    }

    @Override
    public void start() {

        musicSrv.go();
    }

    @Override
    public void pause() {

        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);

    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}