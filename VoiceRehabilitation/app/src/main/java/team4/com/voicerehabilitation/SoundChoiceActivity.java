package team4.com.voicerehabilitation;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SoundChoiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_choice);

        setTitle("Practicing Vowels");
    }

    public void startAssessment(View view)
    {
        Intent intent = new Intent(this, AssessmentActivity.class);
        intent.putExtra("buttonId", view.getId());
        startActivity(intent);
    }
}
