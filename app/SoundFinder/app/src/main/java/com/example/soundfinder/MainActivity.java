package com.example.soundfinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.pytorch.Module;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    TextView tv;
    FilePickerDialog fileDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.textView);

        setUpFileOpener();
    }

    protected void setUpFileOpener() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        //If you want to view files of all extensions then pass null to properties.extensions
        properties.extensions = null;
        //If you want to view files with specific type of extensions the pass string array to properties.extensions
//        properties.extensions = new String[]{"zip","jpg","mp3","csv"};
        properties.show_hidden_files = false;

        fileDialog = new FilePickerDialog(MainActivity.this, properties);
        fileDialog.setTitle("Select a File");

        fileDialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                tv.setText(files[0]);
            }
        });

        findViewById(R.id.buttonOpen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileDialog.show();
            }
        });
    }
}