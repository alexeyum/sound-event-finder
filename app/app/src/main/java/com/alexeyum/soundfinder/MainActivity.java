package com.alexeyum.soundfinder;

import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    static final String API_URL = "https://api.runpod.ai/v2/mu386vlbjedica/runsync";

    // ui elements
    TextView tvFilePath;
    TextView tvStatus;
    FilePickerDialog fileDialog;
    ViewPagerAdapter pagerAdapter;

    // file path for processing
    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFilePath = findViewById(R.id.tvFilePath);
        tvStatus = findViewById(R.id.tvStatus);

        setupTabs();

        setupFileOpener();

        findViewById(R.id.buttonPredict).setOnClickListener(view -> runPrediction());
    }

    protected void setupTabs() {
        ViewPager2 viewPager = findViewById(R.id.pager);
        pagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        TabLayout textTabs = findViewById(R.id.textTabs);
        new TabLayoutMediator(textTabs, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText(R.string.resultsTabName);
                    } else if (position == 1) {
                        tab.setText(R.string.infoTabName);
                    }
                }
        ).attach();

        viewPager.setOffscreenPageLimit(2);
    }

    protected void setupFileOpener() {
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

        findViewById(R.id.tvFilePath).setOnClickListener(view -> fileDialog.show());
    }

    public void setInfoText(String text) {
        InfoFragment infoFragment = (InfoFragment) pagerAdapter.getFragment(1);
        infoFragment.updateInfoText(text);
    }

    public void setResultsText(String text) {
        ResultsFragment infoFragment = (ResultsFragment) pagerAdapter.getFragment(0);
        infoFragment.updateResultsText(text);
    }

    public void setStatus(String text) {
        tvStatus.setText("Status: " + text);
    }

    public void displayError(String shortText, String longText) {
        setInfoText(longText);

        Snackbar snackbar = Snackbar.make(
                findViewById(R.id.constraintLayout),
                shortText,
                Snackbar.LENGTH_LONG);

        snackbar.setAction("Details", v -> {
            ViewPager2 viewPager = findViewById(R.id.pager);
            viewPager.setCurrentItem(1, true);
        });

        snackbar.show();
    }

    public void runPrediction() {
        setResultsText("");
        setInfoText("");

        String fileBase64;
        if (filePath == null) {
            displayError(
                "Error: no file provided",
                "Select a file before running a prediction");
        }
        try {
            fileBase64 = readFileAsBase64(filePath);
        } catch (IOException e) {
            displayError("Error: failed to read file", e.toString());
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
            displayError("Error: failed to build JSON", e.toString());
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        RequestBody body = RequestBody.create(apiInput, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.API_KEY)
                .post(body)
                .build();

        Log.i(TAG, "Starting API request");

        client.newCall(request).enqueue(new APICallHandler());

        Log.i(TAG, "API request sent");
        setStatus("running prediction");
    }

    protected class APICallHandler implements Callback {
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
            String responseStr;
            try {
                if (!response.isSuccessful()) {
                    throw new IOException("Response wasn't successful");
                } else if (response.body() == null) {
                    throw new IOException("Response body is null");
                }
                responseStr = response.body().string();
            } catch (IOException e) {
                String errorDescription =
                        "Response message: " + response.message() + "\n" +
                                "Exception message: " + e.getMessage();
                displayError("Failed to run prediction", errorDescription);
                runOnUiThread(() -> setStatus("failed"));
                return;
            }

            // TODO: fix duplicate json parsing?
            String strEvents = formatEvents(responseStr);
            runOnUiThread(() -> setResultsText(strEvents));
            String strInfo = formatInfo(responseStr);
            runOnUiThread(() -> setInfoText(strInfo));

            runOnUiThread(() -> setStatus("finished"));
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            String errorDescription;
            if (e.getMessage() != null) {
                errorDescription = "Exception message:" + e.getMessage();
            } else {
                errorDescription = "Unknown failure reason";
            }
            displayError("Failed to run prediction", errorDescription);
        }
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
            // TODO: better error handling?
            return "Error: malformed response JSON\n" + e.getMessage();
        }

        return result.toString();
    }

    static protected String secondsToTime(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    static protected String formatInfo(String jsonString) {
        StringBuilder result = new StringBuilder();
        try {
            JSONObject json = new JSONObject(jsonString);


            result.append(String.format(Locale.getDefault(),
                    "Delay: %.3f s.\n",
                    json.getDouble("delayTime") / 1000));

            result.append(String.format(Locale.getDefault(),
                    "Execution: %.3f s.\n",
                    json.getDouble("executionTime") / 1000));

            JSONObject jsonTime = json.getJSONObject("output").getJSONObject("time");

            result.append(String.format(Locale.getDefault(),
                    "Model loading: %.3f s.\n",
                    jsonTime.getDouble("load_model")));

            result.append(String.format(Locale.getDefault(),
                    "Feature extraction: %.3f s.\n",
                    jsonTime.getDouble("extract_features")));

            result.append(String.format(Locale.getDefault(),
                    "Inference: %.3f s.\n",
                    jsonTime.getDouble("inference")));


        } catch (JSONException e) {
            // TODO: better error handling?
            return "Error: malformed response JSON\n" + e.getMessage();
        }

        return result.toString();
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
}