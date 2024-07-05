package com.alexeyum.soundfinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        tvStatus.setText(getString(R.string.status, text));
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

    private Request buildApiRequest(String fileBase64) throws JSONException {
        String apiInput = new JSONObject()
            .put("input", new JSONObject()
                    .put("audio", new JSONObject()
                            .put("format", "mp3")
                            .put("base64", fileBase64)))
            .toString();

        RequestBody body = RequestBody.create(apiInput, MediaType.get("application/json"));
        return new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + BuildConfig.API_KEY)
                .post(body)
                .build();
    }

    public void runPrediction() {
        setResultsText("");
        setInfoText("");

        if (filePath == null) {
            displayError(
                "Error: no file provided",
                "Select a file before running a prediction");
            return;
        }

        try {
            String fileBase64 = readFileAsBase64(filePath);

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(300, TimeUnit.SECONDS)
                    .build();
            Request request = buildApiRequest(fileBase64);

            Log.i(TAG, "Sending API request");
            client.newCall(request).enqueue(new APICallHandler());

            setStatus("running prediction");

        } catch (IOException e) {
            displayError("Error: failed to read file", e.toString());
        } catch (JSONException e) {
            displayError("Error: failed to build JSON", e.toString());
        }
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

                JSONObject json = new JSONObject(responseStr);
                String strEvents = ApiResultFormatter.formatEvents(json);
                runOnUiThread(() -> setResultsText(strEvents));
                String strInfo = ApiResultFormatter.formatExecutionInfo(json);
                runOnUiThread(() -> setInfoText(strInfo));
            } catch (IOException e) {
                String errorDescription =
                        "Response message: " + response.message() + "\n" +
                        "Exception message: " + e.getMessage();
                displayError("Failed to run prediction", errorDescription);
                runOnUiThread(() -> setStatus("failed"));
            } catch (JSONException e) {
                String errorDescription =
                        "Response message: " + response.message() + "\n" +
                        "Exception message: " + e.getMessage();
                displayError("Failed to parse results", errorDescription);
                runOnUiThread(() -> setStatus("failed"));
            }

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

    public static String readFileAsBase64(String path) throws IOException {
        File file = new File(path);
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
}