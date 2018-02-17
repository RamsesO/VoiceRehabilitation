package team4.com.voicerehabilitation;

import android.graphics.PixelFormat;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    VideoView playVideo;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        playVideo = findViewById(R.id.videoView1);
        uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a);
        playVideo.setVideoURI(uri);

        Button playButton = findViewById(R.id.button1);
        playButton.setOnClickListener(new View.OnClickListener(){
            @Override
                    public void onClick(View v){
                playVideo.start();
            }
        });


    }
}
