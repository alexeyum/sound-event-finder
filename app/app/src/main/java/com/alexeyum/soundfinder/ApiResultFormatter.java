package com.alexeyum.soundfinder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class ApiResultFormatter {
    public static String convertSecondsToTime(int seconds) {
        return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60);
    }

    public static String formatEvents(JSONObject json) throws JSONException {
        StringBuilder result = new StringBuilder();
        JSONArray events = json.getJSONObject("output").getJSONArray("events");
        for (int i = 0; i < events.length(); i++) {
            JSONArray eventData = events.getJSONArray(i);
            String eventName = eventData.getString(0);
            int fromSec = eventData.getInt(1);
            int toSec = eventData.getInt(2);
            String fmtEvent = convertSecondsToTime(fromSec) + "-" + convertSecondsToTime(toSec) +
                    " - " + eventName + "\n";
            result.append(fmtEvent);
        }
        return result.toString();
    }

    public static String formatExecutionInfo(JSONObject json) throws JSONException {
        StringBuilder result = new StringBuilder();

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

        result.append(String.format(Locale.getDefault(),
                "Finding events: %.3f s.\n",
                jsonTime.getDouble("find_events")));

        return result.toString();
    }
}
