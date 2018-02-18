package team4.com.voicerehabilitation;

import com.sonarsource.bdd.dbjh.*;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;





public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }

    public void goToHomeScreen(View view){
        Intent intent = new Intent(this, homepage.class);
        startActivity(intent);
    }
    
}
