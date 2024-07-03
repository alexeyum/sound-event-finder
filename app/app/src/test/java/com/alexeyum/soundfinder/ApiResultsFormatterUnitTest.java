package com.alexeyum.soundfinder;

import static junit.framework.TestCase.assertEquals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class ApiResultsFormatterUnitTest {
    @Test
    public void testConvertSecondsToTime() {
        assertEquals("0:00", ApiResultFormatter.convertSecondsToTime(0));
        assertEquals("0:59", ApiResultFormatter.convertSecondsToTime(59));
        assertEquals("1:00", ApiResultFormatter.convertSecondsToTime(60));
        assertEquals("1:01", ApiResultFormatter.convertSecondsToTime(61));
        assertEquals("2:00", ApiResultFormatter.convertSecondsToTime(120));
        assertEquals("10:15", ApiResultFormatter.convertSecondsToTime(615));
    }

    @Test
    public void testFormatEventsSimple() throws Exception {
        String jsonResponse = new JSONObject()
                .put("output", new JSONObject()
                        .put("events", new JSONArray()
                                .put(new JSONArray()
                                        .put("Event1").put(0).put(60))
                                .put(new JSONArray()
                                        .put("Event2").put(61).put(120))))
                .toString();

        String expectedOutput = "0:00-1:00 - Event1\n1:01-2:00 - Event2\n";

        JSONObject json = new JSONObject(jsonResponse);
        String actualOutput = ApiResultFormatter.formatEvents(json);

        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testFormatEventsReal() throws JSONException {
        String jsonString = "{\"delayTime\":8350,\"executionTime\":1487,\"id\":\"sync-d55ddfdb-ce22-408e-93af-8d4e9ba5a586-e1\",\"output\":{\"events\":[[\"Music\",0,10],[\"Crow\",0,40],[\"Bird vocalization, bird call, bird song\",40,50],[\"Crow\",50,60],[\"Bird vocalization, bird call, bird song\",60,80],[\"Crow\",80,100]],\"time\":{\"extract_features\":0.026204347610473633,\"inference\":0.3831048011779785,\"load_model\":0.842029333114624}},\"status\":\"COMPLETED\"}";

        JSONObject json = new JSONObject(jsonString);
        String res = ApiResultFormatter.formatEvents(json);

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
