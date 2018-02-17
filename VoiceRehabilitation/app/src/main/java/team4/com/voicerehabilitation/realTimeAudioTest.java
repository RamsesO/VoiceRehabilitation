package team4.com.voicerehabilitation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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


    private static final int PERMISSION_RECORD_AUDIO = 0;

    private AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
    private AudioTrack audioTrack = new AudioTrack.Builder()
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
                            new String[] { Manifest.permission.RECORD_AUDIO },
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

                if(audioRecord.getState() != AudioRecord.RECORDSTATE_RECORDING){
                    Toast.makeText(realTimeAudioTest.this, "Not recording anything", Toast.LENGTH_SHORT).show();
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

    public void startRecording(){
        switch(audioRecord.getState()){
            case AudioRecord.RECORDSTATE_RECORDING:
                Toast.makeText(this, "Already running!", Toast.LENGTH_LONG).show();
                break;
            case AudioRecord.RECORDSTATE_STOPPED:
                playAudio();
                break;
            case AudioRecord.READ_BLOCKING:
                playAudio();
                break;
            case AudioRecord.ERROR:
                Toast.makeText(this, "An error has occurred!", Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void playAudio(){
        Toast.makeText(this, "Starting Up!", Toast.LENGTH_LONG).show();
        this.audioData =  new float[BUFFER_SIZE];
        int read;
        this.audioRecord.startRecording();
        int systemTime;

        read = this.audioRecord.read(audioData, 0, 0, AudioRecord.READ_BLOCKING);
        if (read == AudioRecord.ERROR || read == AudioRecord.ERROR_DEAD_OBJECT || read == AudioRecord.ERROR_BAD_VALUE || read == AudioRecord.ERROR_INVALID_OPERATION){
            Toast.makeText(this, "Error Code in Audio Recording " + read, Toast.LENGTH_LONG).show();
        }


        this.audioRecord.stop();
        this.audioRecord.release();
        this.audioTrack.write(this.audioData, 0, 0, AudioTrack.WRITE_BLOCKING);
        this.audioTrack.play();
    }
}
