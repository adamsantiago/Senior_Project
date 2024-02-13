package com.example.textrecognizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ButtonPage extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_page);
        //
        Button mailButton = findViewById(R.id.mail_button);
        Button stateButton = findViewById(R.id.stateId_button);
        Button driverButton = findViewById(R.id.driversId_button);
        Button manualButton = findViewById(R.id.manual_button);

        mailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ButtonPage.this, MainActivity.class);
                startActivity(intent);
            }
        });

        driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ButtonPage.this, MainActivity.class);
                startActivity(intent);
            }
        });

        stateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ButtonPage.this, MainActivity.class);
                startActivity(intent);
            }
        });

        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ButtonPage.this, ManualInput.class);
                startActivity(intent);
            }
        });
    }
}
