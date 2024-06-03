package com.alexeyum.soundfinder;

import static junit.framework.TestCase.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class MainActivityUnitTest {

    @Test
    public void testFormatEvents() {
        String jsonString = "{\"delayTime\":8350,\"executionTime\":1487,\"id\":\"sync-d55ddfdb-ce22-408e-93af-8d4e9ba5a586-e1\",\"output\":{\"events\":[[\"Music\",0,10],[\"Crow\",0,40],[\"Bird vocalization, bird call, bird song\",40,50],[\"Crow\",50,60],[\"Bird vocalization, bird call, bird song\",60,80],[\"Crow\",80,100]],\"time\":{\"extract_features\":0.026204347610473633,\"inference\":0.3831048011779785,\"load_model\":0.842029333114624}},\"status\":\"COMPLETED\"}";

        String res = MainActivity.formatEvents(jsonString);

        String expected =
                "0:00-0:10 - Music\n" +
                "0:00-0:40 - Crow\n" +
                "0:40-0:50 - Bird vocalization, bird call, bird song\n" +
                "0:50-1:00 - Crow\n" +
                "1:00-1:20 - Bird vocalization, bird call, bird song\n" +
                "1:20-1:40 - Crow\n";
        assertEquals(expected, res);
    }
}
