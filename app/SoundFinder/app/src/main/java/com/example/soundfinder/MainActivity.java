package com.example.soundfinder;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;


final class WavReader {
    public static float[] ReadAsFloatArray(String path) throws IOException {
        File file = new File(path);
        InputStream fileStream = new FileInputStream(file);

        float[] result = new float[]{};

        return result;
    }

    public static String readString(InputStream fs, int len) throws IOException {
        byte[] byteArray = new byte[len];
        int r = fs.read(byteArray, 0, len);
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
        int r = fs.read(byteArray, 0, LEN);
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN, LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public static int readInt(InputStream fs) throws IOException {
        final int LEN = 4;
        byte[] byteArray = new byte[LEN];
        int r = fs.read(byteArray, 0, LEN);
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN, LITTLE_ENDIAN);
        return buffer.getInt();
    }

    public static float readFloat(InputStream fs, int bytesPerSample) throws IOException {
        byte[] byteArray = new byte[bytesPerSample];
        int r = fs.read(byteArray, 0, bytesPerSample);
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
    TextView tvPredictionResult;
    FilePickerDialog fileDialog;

    Module soundClassifierModule;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFilePath = findViewById(R.id.tvFilePath);
        tvPredictionResult = findViewById(R.id.tvPredictionResult);

        try {
            soundClassifierModule = LiteModuleLoader.load(assetFilePath("classifier_20240213.pt"));
        } catch (IOException e) {
            Log.e(TAG, "Unable to load model", e);
        }

        setUpFileOpener();

        findViewById(R.id.buttonPredict).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Prepare the input tensor (N, 2)
                long[] shape = new long[]{1, 1, 384000};
                Tensor inputTensor = generateTensor(shape);

                // Get the output from the model
                float[] output = soundClassifierModule
                        .forward(IValue.from(inputTensor))
                        .toTensor()
                        .getDataAsFloatArray();

                // Get the output as a string
                String out = "";
                for (float l : output) {
                    out += String.valueOf(l);
                    out += "\n";
                }

                // Show the output
                tvPredictionResult.setText(out);
            }
        });
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

    // Generate a tensor of random numbers given the size of that tensor.
    public Tensor generateTensor(long[] Size) {
        // Create a random array of floats
        Random rand = new Random();
        int fullSize = (int)(Size[0]*Size[1]*Size[2]);
        float[] arr = new float[fullSize];
        for (int i = 0; i < fullSize; i++) {
            arr[i] = -10000 + rand.nextFloat() * (20000);
        }

        // Create the tensor and return it
        return Tensor.fromBlob(arr, Size);
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
                tvFilePath.setText(files[0]);
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