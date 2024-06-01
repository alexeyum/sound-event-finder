package com.alexeyum.soundfinder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public final class WavReader {
    // Note: this is only a temporary method for POC application,
    // this should be redesigned later.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static ArrayList<Float> readAsFloatArray(InputStream fs) throws IOException, UnsupportedOperationException {

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
            throw new IOException("File is not in WAV format");
        }

        // check that file parameters are those that allowed
        if (bitsPerSample != 16) {
            throw new UnsupportedOperationException("Only 16 bps is supported.");
        }
        if (!subChunk2ID.equals("data")) {
            throw new UnsupportedOperationException("Only 'data' SubChunk2ID field value is supported.");
        }
        if (sampleRate < 24000 || sampleRate > 96000) {
            throw new UnsupportedOperationException("Only sample rates between 24k and 96k are supported.");
        }
        if (numChannels != 2) {
            throw new UnsupportedOperationException("Only stereo files are supported.");
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

    public static ArrayList<Float> stereoToMono(ArrayList<Float> stereo) throws IllegalArgumentException {
        if (stereo.size() % 2 != 0) {
            throw new IllegalArgumentException("Stereo data contains odd number of samples");
        }
        int mono_size = stereo.size() / 2;

        // average the channels
        ArrayList<Float> mono = new ArrayList<>();
        for (int i = 0; i < mono_size; i++) {
            mono.add((float) (0.5 * (stereo.get(2 * i) + stereo.get(2 * i + 1))));
        }

        return mono;
    }

    public static ArrayList<Float> normalizeTo01(ArrayList<Float> values, short bitsPerSample) throws IllegalArgumentException {
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

    private static ByteBuffer byteArrayToNumber(byte[] bytes, int numOfBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
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
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN);
        return buffer.getShort();
    }

    public static int readInt(InputStream fs) throws IOException {
        final int LEN = 4;
        byte[] byteArray = new byte[LEN];
        if (fs.read(byteArray, 0, LEN) == -1) {
            throw new EOFException();
        }
        ByteBuffer buffer = byteArrayToNumber(byteArray, LEN);
        return buffer.getInt();
    }

    public static float readFloat(InputStream fs, int bytesPerSample) throws IOException, UnsupportedOperationException {
        byte[] byteArray = new byte[bytesPerSample];
        if (fs.read(byteArray, 0, bytesPerSample) == -1) {
            throw new EOFException();
        }

        ByteBuffer buffer = byteArrayToNumber(byteArray, bytesPerSample);
        if (bytesPerSample == 2) {
            return (float) buffer.getShort();
        } else {
            throw new UnsupportedOperationException("Only 16-bit rate is implemented for now.");
        }
    }
}
