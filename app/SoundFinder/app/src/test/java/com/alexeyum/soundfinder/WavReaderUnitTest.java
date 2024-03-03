package com.alexeyum.soundfinder;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class WavReaderUnitTest {
    protected void assertArrayListsEqual(ArrayList<Float> expected, ArrayList<Float> actual, double delta) {
        assertEquals(expected.size(), actual.size());
        for (int i : IntStream.range(0, expected.size()).boxed().toList()) {
            assertEquals(expected.get(i), actual.get(i), delta);
        }
    }

    @Test
    public void testReadFunctions() throws IOException {
        InputStream fs = this.getClass().getClassLoader().getResourceAsStream("100795-3-1-2.wav");

        assertEquals("RIFF", WavReader.readString(fs, 4));

        fs.skip(4);
        assertEquals("WAVE", WavReader.readString(fs, 4));

        fs.skip(10);
        assertEquals(2, WavReader.readShort(fs));

        assertEquals(44100, WavReader.readInt(fs));

        fs.skip(6);
        assertEquals(16, WavReader.readShort(fs));

        assertEquals("data", WavReader.readString(fs, 4));

        fs.skip(4);
        assertEquals(-39.0, WavReader.readFloat(fs, 2), 1e-6);
    }

    @Test
    public void testReadArray() throws IOException {
        InputStream fs = this.getClass().getClassLoader().getResourceAsStream("100795-3-1-2.wav");
        ArrayList<Float> values = WavReader.readAsFloatArray(fs);

        assertEquals(352800, values.size());

        assertEquals(-39.0, values.get(0), 1e-10);
        assertEquals(97.0, values.get(1), 1e-10);
        assertEquals(-49.0, values.get(2), 1e-10);
        assertEquals(79.0, values.get(3), 1e-10);
    }

    @Test
    public void testStereoToMono() throws IllegalArgumentException {
        ArrayList<Float> stereo1 = new ArrayList<>(Arrays.asList(10.0f, 20.0f, -5.0f, 15.0f));
        ArrayList<Float> mono1 = new ArrayList<>(Arrays.asList(15.0f, 5.0f));
        ArrayList<Float> monoConverted1 = WavReader.stereoToMono(stereo1);
        assertArrayListsEqual(mono1, monoConverted1, 1e-6);

        ArrayList<Float> stereo2 = new ArrayList<>(Arrays.asList(
                0.5f, -0.5f, -0.1f, -0.1f, 0.99f, -0.98f, -1.0f, 0.2f));
        ArrayList<Float> mono2 = new ArrayList<>(Arrays.asList(
                0.0f, -0.1f, 0.005f, -0.4f));
        ArrayList<Float> monoConverted2 = WavReader.stereoToMono(stereo2);
        assertArrayListsEqual(mono2, monoConverted2, 1e-6);
    }

    @Test
    public void testNormalize01() throws IllegalArgumentException {
        ArrayList<Float> raw = new ArrayList<>(Arrays.asList(
                0.0f, 6553.6f, -327.68f, -29491.2f));
        ArrayList<Float> expectedNormalized = new ArrayList<>(Arrays.asList(
                0.0f, 0.2f,    -0.01f,     -0.9f));
        ArrayList<Float> actualNormalized = WavReader.normalizeTo01(raw, (short)16);
        assertArrayListsEqual(expectedNormalized, actualNormalized, 1e-6);
    }
}