package edu.ucf.CD9;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ParentOrChildActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Checking for first time launch - before calling setContentView()
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = pref.edit();
        if (!pref.getBoolean("setup_needed", true)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        setContentView(R.layout.side_screen_parent_or_child);

        final Button btnParent = findViewById(R.id.btnParent);
        final Button btnChild = findViewById(R.id.btnChild);

        btnParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("ParentOrTeen", "Parent").apply();
                startActivity(new Intent(ParentOrChildActivity.this, WelcomeActivityParent.class));
                finish();
            }
        });

        btnChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("ParentOrTeen", "Child").apply();
                startActivity(new Intent(ParentOrChildActivity.this, WelcomeActivityChild.class));
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ParentOrChildActivity.this, Welcome.class));
        finish();
    }


}