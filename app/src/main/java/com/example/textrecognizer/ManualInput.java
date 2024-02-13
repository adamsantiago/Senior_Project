package com.example.textrecognizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.textrecognizer.databinding.ManualInputBinding;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.HashMap;

public class ManualInput extends AppCompatActivity {

    private ManualInputBinding binding;

    private static String[] splitNewlines(String inputString){
        String[] returnArray = inputString.split("\n");
        return returnArray;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manual_input);
        //
        EditText nameEditText = findViewById(R.id.nameInput);
        EditText addressEditText = findViewById(R.id.addressInput);
        EditText cityEditText = findViewById(R.id.cityInput);

        Button saveButton = findViewById(R.id.goToAppButton);
        //New code to transfer to manual input below by Santiago
        Intent intent   = getIntent();
        String savedText = intent.getStringExtra("savedText");

        String[] splitText = splitNewlines(savedText);
        nameEditText.setText(splitText[0], TextView.BufferType.EDITABLE);
        addressEditText.setText(splitText[1], TextView.BufferType.EDITABLE);
        cityEditText.setText(splitText[2], TextView.BufferType.EDITABLE);



        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable nameText = nameEditText.getText();
                Editable addressText = addressEditText.getText();
                Editable cityText = cityEditText.getText();

                //For storing text from input, need to implement data transfer between pages
                Editable[] list = {nameText, addressText, cityText};

                Intent intent = new Intent(ManualInput.this, HomePage.class);
                startActivity(intent);
            }
        });
    }



}
