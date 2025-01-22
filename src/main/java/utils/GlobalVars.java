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
    public static final String AWS_ENDPOINT = "ampdveamdmijg-ats.iot.us-east-1.amazonaws.com";
    public static final String GLOBAL_CERT = "certs/global/99e3f3c36622033e6f14e23903f7bc75ed1770dcaf8f94f838a271f6beb94b5f-certificate.pem.crt";
    public static final String GLOBAL_KEY = "certs/global/99e3f3c36622033e6f14e23903f7bc75ed1770dcaf8f94f838a271f6beb94b5f-private.pem.key";
    public static final String IP_CERTIFICATE = "certs/infoPanel/cc0e82780227843436fbdb3f5887c5da076b4fdc92e56639d353106c1460677b-certificate.pem.crt";
    public static final String IP_KEY = "certs/infoPanel/cc0e82780227843436fbdb3f5887c5da076b4fdc92e56639d353106c1460677b-private.pem.key";
    public static final String SL_CERTIFICATE = "certs/speedLimit/9a92d1b63ddc4df2ee97df2fdd5898e92810ca23022cffd71f592dedf70646d3-certificate.pem.crt";
    public static final String SL_KEY = "certs/speedLimit/9a92d1b63ddc4df2ee97df2fdd5898e92810ca23022cffd71f592dedf70646d3-private.pem.key";
    public static final String VE_CERTIFICATE = "certs/vehiculo/adf5bfbf77e2b471bfcc6256f35de6d0f02cda66fcbd5805606d46a340eab408-certificate.pem.crt";
    public static final String VE_KEY = "certs/vehiculo/adf5bfbf77e2b471bfcc6256f35de6d0f02cda66fcbd5805606d46a340eab408-private.pem.key";

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
