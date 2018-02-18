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

    private static final int PERMISSION_RECORD_AUDIO = 0;

    private AudioRecord audioRecord = null;

    private AudioTrack audioTrack = null;

    public float[] audioData;

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

    public void startRecording() {
        if (audioRecord != null) {
            Toast.makeText(this, "Do not spam the button", Toast.LENGTH_LONG).show();
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

                isRecordingComplete = false;

                while(!isPlaybackComplete){

                }

                audioRecord.startRecording();

                int shortsRead = 0;
                while (shortsRead < audioData.length) {
                    int numberOfIndexs = audioRecord.read(audioData, 0, audioData.length, AudioRecord.READ_BLOCKING);
                    shortsRead += numberOfIndexs;
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
                isRecordingComplete = true;

            }
        }).start();

        //Toast.makeText(this, "Playback!", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                isPlaybackComplete = false;

                while (!isRecordingComplete) {

                }

                audioTrack.play();

                int shortsRead = 0;
                while (shortsRead < audioData.length) {
                    int numberOfFloatsWritten = audioTrack.write(audioData, 0, audioData.length, AudioTrack.WRITE_BLOCKING);
                    shortsRead += numberOfFloatsWritten;
                }

                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;

                isPlaybackComplete = true;

            }



        }).start();

        //Toast.makeText(this, "All done!", Toast.LENGTH_SHORT).show();
    }

}
