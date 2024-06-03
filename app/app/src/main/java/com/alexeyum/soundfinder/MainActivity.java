package com.alexeyum.soundfinder;

import static java.lang.Math.min;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView tvFilePath;
    TextView tvInfo;
    FilePickerDialog fileDialog;

    // file path for processing
    String filePath;

    static final String API_URL = "https://api.runpod.ai/v2/mu386vlbjedica/runsync";
    public static final MediaType JSON = MediaType.get("application/json");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFilePath = findViewById(R.id.tvFilePath);
        tvInfo = findViewById(R.id.tvInfo);

        setUpFileOpener();

        findViewById(R.id.buttonPredict).setOnClickListener(view -> runPrediction());
    }

    public void runPrediction() {
        long start;

        String fileBase64;
        try {
            fileBase64 = readFileAsBase64(filePath);
        } catch (IOException e) {
            // TODO: print error to info field
            e.printStackTrace();
            return;
        }

        String apiInput;
        try {
            apiInput = new JSONObject()
                .put("input", new JSONObject()
                    .put("audio", new JSONObject()
                        .put("base64", fileBase64)))
                .toString();
        } catch (JSONException e) {
            // TODO: print error to info field
            e.printStackTrace();
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        RequestBody body = RequestBody.create(apiInput, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.API_KEY)
                .post(body)
                .build();

        tvInfo.setText("Running prediction...");
        Log.i(TAG, "Starting API call");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String strFormatted = formatEvents(response.body().string());
                    runOnUiThread(() -> tvInfo.setText(strFormatted));
                } else {
                    Log.e(TAG,"API call failed (onResponse)");
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"API call failed (onFailure)");
                Log.e(TAG,e.getMessage());
            }
        });

//        start = System.currentTimeMillis();
//        float[] output = soundClassifierModule
//                .forward(IValue.from(inputTensor))
//                .toTensor()
//                .getDataAsFloatArray();
//        long elapsedInference = System.currentTimeMillis() - start;
//
//        String infoText = String.format(
//                "Preprocessing time: %s\nInference time: %s\n",
//                elapsedTimeFormat(elapsedPreprocessing),
//                elapsedTimeFormat(elapsedInference));
//        tvInfo.setText(infoText);
//        Log.i(TAG, "Made prediction\n" + infoText);
//
//        showResults(output);
    }

    static protected String formatEvents(String jsonString) {
        StringBuilder result = new StringBuilder();
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray events = json.getJSONObject("output").getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONArray eventData = events.getJSONArray(i);
                String eventName = eventData.getString(0);
                int fromSec = eventData.getInt(1);
                int toSec = eventData.getInt(2);
                String fmtEvent = secondsToTime(fromSec) + "-" + secondsToTime(toSec) +
                                  " - " + eventName + "\n";
                result.append(fmtEvent);
            }
        } catch (JSONException e) {
            // TODO: better error handling
            return "Error";
        }

        return result.toString();
    }

    static protected String secondsToTime(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    static protected String readFileAsBase64(String path) throws IOException {
        File file = new File(path);
        int size = (int)file.length();

        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(Files.newInputStream(file.toPath()));
        int bytes_read = buf.read(bytes, 0, bytes.length);
        buf.close();

        if (bytes_read < bytes.length) {
            throw new IOException("Failed to read full file");
        }

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    protected String elapsedTimeFormat(long milliseconds) {
        return String.format(Locale.getDefault(), "%d.%d", milliseconds / 1000, milliseconds % 1000);
    }

    protected void setUpFileOpener() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"mp3"};
        properties.show_hidden_files = false;

        fileDialog = new FilePickerDialog(MainActivity.this, properties);
        fileDialog.setTitle("Select an mp3 file");

        fileDialog.setDialogSelectionListener(files -> {
            filePath = files[0];
            tvFilePath.setText(filePath);
        });

        findViewById(R.id.buttonOpen).setOnClickListener(view -> fileDialog.show());
    }
}