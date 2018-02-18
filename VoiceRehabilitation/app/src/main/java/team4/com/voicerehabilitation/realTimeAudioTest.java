package team4.com.voicerehabilitation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sonarsource.bdd.dbjh.*;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayList;

/*
See https://gist.github.com/kmark/d8b1b01fb0d2febf5770
For reference code thank you whoever you are!
 */
public class realTimeAudioTest extends AppCompatActivity {

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_FLOAT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    private static final int RECORD_TIME = 1;

    private boolean isRecordingComplete = false;
    private boolean isPlaybackComplete = true;
    private boolean isStopButtonPressed = false;

    private static final int PERMISSION_RECORD_AUDIO = 0;

    private AudioRecord audioRecord = null;

    private AudioTrack audioTrack = null;

    public float[] audioData;

    public float[] magnitudes;
    public ArrayList<Integer> peakIndexes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_audio_test);
        findViewById(R.id.start_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(realTimeAudioTest.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request permission
                    ActivityCompat.requestPermissions(realTimeAudioTest.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_RECORD_AUDIO);
                    return;
                }
                startRecording();
            }
        });

        findViewById(R.id.start_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        findViewById(R.id.stop_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioRecord == null) {
                    Toast.makeText(realTimeAudioTest.this, "Not started", Toast.LENGTH_LONG).show();
                } else {
                    if (audioRecord.getState() != AudioRecord.RECORDSTATE_RECORDING) {
                        Toast.makeText(realTimeAudioTest.this, "Not recording anything", Toast.LENGTH_SHORT).show();
                    }
                }
                isStopButtonPressed = true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    startRecording();
                } else {
                    // Permission denied
                    Toast.makeText(this, "\uD83D\uDE41", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void startRecording(){
        FormantExtractor extractor = new FormantExtractor();
        Log.d("", "Hi and stuff!!");

            if(this.audioRecord != null){
                Toast.makeText(this, "Dont spam the button!", Toast.LENGTH_LONG).show();
            } else {
                playAudio();
            }

        }


    public void playAudio() {
        //this.isRecordingComplete = false;
        this.audioData = new float[SAMPLE_RATE * RECORD_TIME];

        this.audioRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(BUFFER_SIZE)
                .build();

        this.audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        Toast.makeText(this, "Starting Recording!", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

//                isRecordingComplete = false;

//                while(!isPlaybackComplete){
//
//                }

                audioRecord.startRecording();
                for( int i =0; i < Integer.MAX_VALUE && !isStopButtonPressed; i++) {
                    int shortsRead = 0;
                    while (shortsRead < audioData.length) {
                        int numberOfIndexs = audioRecord.read(audioData, 0, audioData.length, AudioRecord.READ_NON_BLOCKING);
                        shortsRead += numberOfIndexs;
                    }
                    generateGraphData(audioData);
                }
                float max = Float.MIN_VALUE;
                for (int i = 0; i < audioData.length; i++){
                    if (audioData[i] > max){
                        max = audioData[i];
                    }
                }


                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
//                isRecordingComplete = true;

            }
        }).start();

        //Toast.makeText(this, "Playback!", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

//                isPlaybackComplete = false;

//                while (!isRecordingComplete) {
//
//                }

                audioTrack.play();

                for(int i = 0; i < Integer.MAX_VALUE && !isStopButtonPressed; i++) {
                    int shortsRead = 0;
                    while (shortsRead < audioData.length) {
                        int numberOfFloatsWritten = audioTrack.write(audioData, 0, audioData.length, AudioTrack.WRITE_NON_BLOCKING);
                        shortsRead += numberOfFloatsWritten;
                    }
                }
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
                isStopButtonPressed = false;
//                isPlaybackComplete = true;

            }



        }).start();

        //Toast.makeText(this, "All done!", Toast.LENGTH_SHORT).show();
    }

    public void generateGraphData(final float[] audioData){
        new Thread(new Runnable() {
            @Override
            public void run() {

                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                float[] magnitude = calculateFFT(audioData);
                ArrayList<Integer> peakIndex = calculatePeaks(magnitude, 500);
                magnitudes = magnitude;
                peakIndexes = peakIndex;
            }
        }).start();
    }

    public float[] calculateFFT(float[] audioData){

        FloatFFT_1D fft = new FloatFFT_1D(audioData.length);
        fft.realForward(audioData);

        float[] magnitudes = new float[audioData.length/2];
        for (int frequencyBin = 0; frequencyBin < audioData.length/2; frequencyBin++){
            float real = audioData[frequencyBin*2];
            float imaginary = audioData[2*frequencyBin+1];
            float magnitude = (float)Math.sqrt(real * real + imaginary * imaginary);
            magnitudes[frequencyBin] = magnitude;
        }
        return magnitudes;
    }

    public ArrayList<Integer> calculatePeaks(float[] magnitudes, int minimumDistance){
        ArrayList<Integer> peakIndexes = new ArrayList<>();
        int frequencyBin = 0;
        float max = magnitudes[0];
        int lastPeakIndex = 0;
        while(frequencyBin < magnitudes.length - 1){
            while(frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] >= max){
                frequencyBin++;
                max = magnitudes[frequencyBin];
            }
            if(!peakIndexes.isEmpty() && frequencyBin - lastPeakIndex < minimumDistance){
                if(magnitudes[lastPeakIndex] > magnitudes[frequencyBin]){
                    //Do not do anything, old peak is better
                } else {
                    //new peak is higher so replace the old close by one
                    peakIndexes.remove(peakIndexes.size() - 1);
                    peakIndexes.add(frequencyBin);
                    lastPeakIndex = frequencyBin;
                    //do not change peakIndex
                }
            } else {
                //Add a new peak not near any others
                peakIndexes.add(frequencyBin);
                lastPeakIndex = frequencyBin;
            }

            while(frequencyBin < magnitudes.length - 1 && magnitudes[frequencyBin + 1] <= max){
                frequencyBin++;
                max = magnitudes[frequencyBin];
            }
        }
        return peakIndexes;
    }

    //FormantExtractor formantExtractor = new FormantExtractor();
//        float[] data = new float[100000];
//        double value = 0;
//        for(int i = 0; i < data.length; i++){
//            data[i] = (float) Math.sin(2* Math.PI * 120 *  value);
//            value += 0.0001;
//        }
//        FloatFFT_1D fft = new FloatFFT_1D(data.length);
//        fft.realForward(data);
//        float max = Float.MIN_VALUE;
//        int index = 0;
//        float[] magnitudes = new float[data.length/2];
//        for (int i = 0; i < data.length/2; i++){
//            float real = data[i*2];
//            float imag = data[2*i+1];
//            float magnitude = (float)Math.sqrt(real * real + imag * imag);
//            magnitudes[i] = magnitude;
//        }
//        for(int i =0; i< data.length/2; i++){
//            if(magnitudes[i] > max){
//                max = magnitudes[i];
//                index = i;
//            }
//        }

}
