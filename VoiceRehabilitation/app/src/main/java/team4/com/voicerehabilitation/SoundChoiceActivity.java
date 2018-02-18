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

        Button btn_ee = (Button)findViewById(R.id.eeBtn);
        Button btn_i = (Button)findViewById(R.id.iBtn);
        Button btn_e = (Button)findViewById(R.id.eBtn);
        Button btn_ae = (Button)findViewById(R.id.aeBtn);
        Button btn_ah= (Button)findViewById(R.id.ahBtn);
        Button btn_aw = (Button)findViewById(R.id.awBtn);
        Button btn_û = (Button)findViewById(R.id.ûBtn);
        Button btn_oo = (Button)findViewById(R.id.ooBtn);
        Button btn_u = (Button)findViewById(R.id.uBtn);
        Button btn_er = (Button)findViewById(R.id.erBtn);
    }

    public void startAssessment(View view)
    {
        Intent intent = new Intent(this, AssessmentActivity.class);
        intent.putExtra("buttonId", view.getId());
        startActivity(intent);
    }
}
