package com.example.soundfinder;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class WavReaderUnitTest {
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

        assertEquals(-15296.0, WavReader.readFloat(fs, 2), 1e-6);
    }

    @Test
    public void testReadArray() throws IOException {
        InputStream fs = this.getClass().getClassLoader().getResourceAsStream("100795-3-1-2.wav");
        ArrayList<Float> values = WavReader.ReadAsFloatArray(fs);
        assertEquals(-15296.0, values.get(0), 1e-10);
    }
}