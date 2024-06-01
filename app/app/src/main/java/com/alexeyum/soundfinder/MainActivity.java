package com.alexeyum.soundfinder;

import static java.lang.Math.min;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView tvFilePath;
    TextView tvPredictionResult;
    TextView tvPercentageBars;
    TextView tvClassesNames;
    TextView tvInfo;
    FilePickerDialog fileDialog;

    // file path for processing
    String filePath;

    // torch module
    Module soundClassifierModule;

    static final short BITS_PER_SAMPLE = 16;
    static final int MODEL_AUDIO_LENGTH = 384000;

    static final String[] SOUND_CLASSES = {
        "air_conditioner",
        "car_horn",
        "children_playing",
        "dog_bark",
        "drilling",
        "engine_idling",
        "gun_shot",
        "jackhammer",
        "siren",
        "street_music"
    };
    static final int MAX_PERCENTAGE_BARS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFilePath = findViewById(R.id.tvFilePath);
        tvPredictionResult = findViewById(R.id.tvPredictionResult);
        tvPercentageBars = findViewById(R.id.tvPercentageBars);
        tvClassesNames = findViewById(R.id.tvClassesNames);
        tvInfo = findViewById(R.id.tvInfo);

        soundClassifierModule = LiteModuleLoader.loadModuleFromAsset(getAssets(), "classifier_20240213.pt");

        setUpFileOpener();

        findViewById(R.id.buttonPredict).setOnClickListener(view -> runPrediction());
    }

    public void runPrediction() {
        long start;

        start = System.currentTimeMillis();
        Tensor inputTensor;
        try {
            inputTensor = loadAndPrepareWav(filePath);
        } catch (NullPointerException e) {
            String infoText = "File not found";
            tvInfo.setText(infoText);
            return;
        } catch (IOException e) {
            String infoText = "Error reading file:\n" + e.getMessage();
            tvInfo.setText(infoText);
            return;
        } catch (UnsupportedOperationException e) {
            String infoText = "Unsupported sound file parameters:\n" + e.getMessage();
            tvInfo.setText(infoText);
            return;
        }
        long elapsedPreprocessing = System.currentTimeMillis() - start;

        // Get the output from the model
        start = System.currentTimeMillis();
        float[] output = soundClassifierModule
                .forward(IValue.from(inputTensor))
                .toTensor()
                .getDataAsFloatArray();
        long elapsedInference = System.currentTimeMillis() - start;

        String infoText = String.format(
                "Preprocessing time: %s\nInference time: %s\n",
                elapsedTimeFormat(elapsedPreprocessing),
                elapsedTimeFormat(elapsedInference));
        tvInfo.setText(infoText);
        Log.i(TAG, "Made prediction\n" + infoText);

        showResults(output);
    }

    protected String elapsedTimeFormat(long milliseconds) {
        return String.format(Locale.getDefault(), "%d.%d", milliseconds / 1000, milliseconds % 1000);
    }

    protected void showResults(float[] predictions) {
        List<String> classesListCopy = Arrays.asList(SOUND_CLASSES);
        ArrayList<String> sortedClasses = new ArrayList<>(classesListCopy);

        // Note: comparator is in reverse
        sortedClasses.sort((left, right) ->
                Float.compare(predictions[classesListCopy.indexOf(right)],
                              predictions[classesListCopy.indexOf(left)]));

        Float[] predictionsObject = new Float[predictions.length];
        for (int i = 0; i < predictions.length; i++) {
            predictionsObject[i] = predictions[i];
        }
        Arrays.sort(predictionsObject, Collections.reverseOrder());

        StringBuilder classesText = new StringBuilder();
        for (String c : sortedClasses) {
            classesText.append(c).append("\n");
        }
        tvClassesNames.setText(classesText.toString());

        StringBuilder barsText = new StringBuilder();
        for (float prob : predictionsObject) {
            barsText.append(percentageBars(prob, MAX_PERCENTAGE_BARS)).append("\n");
        }
        tvPercentageBars.setText(barsText.toString());

        StringBuilder valuesText = new StringBuilder();
        for (float prob : predictionsObject) {
            valuesText.append(String.format(Locale.getDefault(),"%.2f", prob * 100)).append("%\n");
        }
        tvPredictionResult.setText(valuesText.toString());
    }

    @SuppressWarnings("SameParameterValue") // max_bars parameter always constant, but good to have for future
    protected String percentageBars(float ratio, int max_bars) {
        StringBuilder bars = new StringBuilder();
        for (int i = 0; i < (int)(ratio * max_bars); i++) {
            bars.append("|");
        }
        return bars.toString();
    }

    protected Tensor loadAndPrepareWav(String filePath) throws  IOException, NullPointerException {
        InputStream fs = Files.newInputStream(Paths.get(filePath));
        ArrayList<Float> rawData = WavReader.readAsFloatArray(fs);
        ArrayList<Float> prepData = WavReader.normalizeTo01(WavReader.stereoToMono(rawData), BITS_PER_SAMPLE);

        int realSize = min(prepData.size(), MODEL_AUDIO_LENGTH);

        // copy to float[] and pad with zeros
        float[] blobData = new float[MODEL_AUDIO_LENGTH];
        for (int i = 0; i < MODEL_AUDIO_LENGTH; i++) {
            if (i < realSize) {
                blobData[i] = prepData.get(i);
            } else {
                blobData[i] = 0;
            }
        }

        long[] shape = new long[]{1, 1, MODEL_AUDIO_LENGTH};
        return Tensor.fromBlob(blobData, shape);
    }

    protected void setUpFileOpener() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"wav"};
        properties.show_hidden_files = false;

        fileDialog = new FilePickerDialog(MainActivity.this, properties);
        fileDialog.setTitle("Select a WAV file");

        fileDialog.setDialogSelectionListener(files -> {
            filePath = files[0];
            tvFilePath.setText(filePath);
        });

        findViewById(R.id.buttonOpen).setOnClickListener(view -> fileDialog.show());
    }
}