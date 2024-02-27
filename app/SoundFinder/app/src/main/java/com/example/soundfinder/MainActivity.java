package com.example.soundfinder;

import static java.lang.Math.min;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;


final class WavReader {
    public static ArrayList<Float> ReadAsFloatArray(InputStream fs) throws IOException {

        String chunkID = readString(fs, 4);
        fs.skip(4);
        String format = readString(fs, 4);
        fs.skip(10);
        short numChannels = readShort(fs);
        int sampleRate = readInt(fs);
        fs.skip(6);
        short bitsPerSample = readShort(fs);
        String subChunk2ID = readString(fs, 4);
        fs.skip(4);

        if (!chunkID.equals("RIFF") || !format.equals("WAVE")) {
            throw new IOException("Incorrect file format");
        }

        if (bitsPerSample != 16 || !subChunk2ID.equals("data") || sampleRate != 44100) {
            throw new UnsupportedOperationException("Unsupported file data.");
        }

        int bytesPerSample = bitsPerSample / 8;
        ArrayList<Float> result = new ArrayList<>();

        while (true) {
            try {
                float val = readFloat(fs, bytesPerSample);
                result.add(val);
            } catch (EOFException e) {
                // file finished
                break;
            }
        }

        return result;
    }

    public static ArrayList<Float> StereoToMono(ArrayList<Float> stereo) throws IllegalArgumentException {
        if (stereo.size() % 2 != 0) {
            throw new IllegalArgumentException("Stereo data contains odd number of samples");
        }
        int mono_size = stereo.size() / 2;

        ArrayList<Float> mono = new ArrayList<>();
        for (int i = 0; i < mono_size; i++) {
            mono.add((float) (0.5 * (stereo.get(2 * i) + stereo.get(2 * i + 1))));
        }
        return mono;
    }

    public static ArrayList<Float> NormalizeTo01(ArrayList<Float> values, short bitsPerSample)  throws IllegalArgumentException {
        float maxVal;
        if (bitsPerSample == 8) {
            maxVal = 255;
        } else if (bitsPerSample == 16) {
            maxVal = 32768;
        } else {
            throw new IllegalArgumentException("Only 8-bit and 16-bit sampling rates are implemented");
        }

        ArrayList<Float> newValues = new ArrayList<>();
        for (float v : values) {
            newValues.add(v / maxVal);
        }

        return newValues;
    }

    public static String readString(InputStream fs, int len) throws IOException {
        byte[] byteArray = new byte[len];
        if (fs.read(byteArray, 0, len) == -1) {
            throw new EOFException();
        }
        return new String(byteArray);
    }

    public static ByteBuffer byteArrayToNumber(byte[] bytes, int numOfBytes, ByteOrder order){
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        buffer.order(order);
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }

    public static short readShort(InputStream fs) throws IOException {
        final int LEN = 2;
        byte[] byteArray = new byte[LEN];
        if (fs.read(byteArray, 0, LEN) == -1) {
            throw new EOFException();
        }
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN, LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public static int readInt(InputStream fs) throws IOException {
        final int LEN = 4;
        byte[] byteArray = new byte[LEN];
        if (fs.read(byteArray, 0, LEN) == -1) {
            throw new EOFException();
        }
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN, LITTLE_ENDIAN);
        return buffer.getInt();
    }

    public static float readFloat(InputStream fs, int bytesPerSample) throws IOException {
        byte[] byteArray = new byte[bytesPerSample];
        if (fs.read(byteArray, 0, bytesPerSample) == -1) {
            throw new EOFException();
        }
        ByteBuffer buffer = byteArrayToNumber(byteArray, bytesPerSample, LITTLE_ENDIAN);
        if (bytesPerSample == 2) {
            return (float) buffer.getShort();
        } else {
            throw new UnsupportedOperationException("Only 16-bit rate is implemented for now.");
        }
    }
}

public class MainActivity extends AppCompatActivity {

    TextView tvFilePath;
    String filePath;
    TextView tvPredictionResult;
    TextView tvPercentageBars;
    TextView tvClassesNames;
    TextView tvInfo;
    FilePickerDialog fileDialog;

    Module soundClassifierModule;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFilePath = findViewById(R.id.tvFilePath);
        tvPredictionResult = findViewById(R.id.tvPredictionResult);
        tvPercentageBars = findViewById(R.id.tvPercentageBars);
        tvClassesNames = findViewById(R.id.tvClassesNames);
        tvInfo = findViewById(R.id.tvInfo);

        try {
            soundClassifierModule = LiteModuleLoader.load(assetFilePath("classifier_20240213.pt"));
        } catch (IOException e) {
            Log.e(TAG, "Unable to load model", e);
        }

        setUpFileOpener();

        findViewById(R.id.buttonPredict).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long start, elapsed;

                Tensor inputTensor;
                try {
                    start = System.currentTimeMillis();
                    inputTensor = loadAndPrepareWav(filePath);
                    elapsed = System.currentTimeMillis() - start;
                } catch (IOException e) {
                    System.out.println("Error reading file");
                    System.out.println(e.getMessage());
                    return;
                }

                String infoText = "";
                infoText += "Preprocessing time: " + elapsedTimeFormat(elapsed) + "\n";

                // Get the output from the model
                start = System.currentTimeMillis();
                float[] output = soundClassifierModule
                        .forward(IValue.from(inputTensor))
                        .toTensor()
                        .getDataAsFloatArray();
                elapsed = System.currentTimeMillis() - start;

                infoText += "Inference time: " + elapsedTimeFormat(elapsed) + "\n";
                tvInfo.setText(infoText);

                makePrettyOutput(output);
            }
        });
    }

    protected String elapsedTimeFormat(long milliseconds) {
        return String.format("%d.%d", milliseconds / 1000, milliseconds % 1000);
    }

    public static final String[] SOUND_CLASSES = {
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

    protected void makePrettyOutput(float[] predictions) {
        List<String> classesListCopy = Arrays.asList(SOUND_CLASSES);
        ArrayList<String> sortedClasses = new ArrayList<>(classesListCopy);

        // Note: comparator is in reverse
        Collections.sort(sortedClasses, (left, right) ->
                Float.compare(predictions[classesListCopy.indexOf(right)],
                              predictions[classesListCopy.indexOf(left)]));

        Float[] predsObject = new Float[predictions.length];
        for (int i = 0; i < predictions.length; i++) {
            predsObject[i] = predictions[i];
        }
        Arrays.sort(predsObject, Collections.reverseOrder());

        String classesText = "";
        for (String c : sortedClasses) {
            classesText += c + "\n";
        }
        tvClassesNames.setText(classesText);

        String barsText = "";
        for (float prob : predsObject) {
            barsText += percentageBars(prob, 10);
            barsText += "\n";
        }
        tvPercentageBars.setText(barsText);

        String valuesText = "";
        for (float prob : predsObject) {
            valuesText += String.format("%.2f", prob * 100) + "%\n";
        }
        tvPredictionResult.setText(valuesText);
    }

    protected String percentageBars(float ratio, int max_bars) {
        String bars = "";
        for (int i = 0; i < (int)(ratio * max_bars); i++) {
            bars += "|";
        }
        return bars;
    }

    // Given the name of the pytorch model, get the path for that model
    public String assetFilePath(String assetName) throws IOException {
        File file = new File(this.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = this.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    public Tensor loadAndPrepareWav(String filePath) throws  IOException {
        InputStream fs = new FileInputStream(filePath);
        ArrayList<Float> rawData = WavReader.ReadAsFloatArray(fs);
        // TODO: remove hard-coded data
        ArrayList<Float> prepData = WavReader.NormalizeTo01(WavReader.StereoToMono(rawData), (short) 16);

        int expectedSize = 384000;
        int realSize = min(prepData.size(), 384000);

        float[] blobData = new float[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            if (i < realSize) {
                blobData[i] = prepData.get(i);
            } else {
                blobData[i] = 0;
            }
        }

        long[] shape = new long[]{1, 1, expectedSize};
        return Tensor.fromBlob(blobData, shape);
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
        properties.extensions = new String[]{"wav"};
        properties.show_hidden_files = false;

        fileDialog = new FilePickerDialog(MainActivity.this, properties);
        fileDialog.setTitle("Select a File");

        fileDialog.setDialogSelectionListener(files -> {
            filePath = files[0];
            tvFilePath.setText(filePath);
        });

        findViewById(R.id.buttonOpen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileDialog.show();
            }
        });
    }
}