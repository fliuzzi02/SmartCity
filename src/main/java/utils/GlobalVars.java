package main.java.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GlobalVars {
    public static final String BROKER_ADDRESS = "tcp://tambori.dsic.upv.es:10083";
    // public static final String BROKER_ADDRESS = "tcp://localhost:1883";
    public static final String BASE_TOPIC = "es/upv/pros/tatami/smartcities/traffic/PTPaterna";
    public static final String USERNAME = "client1";
    public static final String PASSWORD = "test";
    public static final int STEP_MS = 3000;

    public static JSONObject getRoadStatus(String roadSegment){
        JSONObject result = new JSONObject();
        try {
            URL url = new URL("http://tambori.dsic.upv.es:10082/segment/" + roadSegment);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            conn.disconnect();

            result = new JSONObject(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
