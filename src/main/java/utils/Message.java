package main.java.utils;

import org.json.JSONObject;

/**
 * {
 * "msg":"<mensaje-en-JSON>",
 * "id": "MSG_1639124586136",
 * "type": "<tipo-mensaje>",
 * "timestamp": 1639124586136
 * }
 */
public class Message {
    public static JSONObject createStepMessage(int counter){
        long millis = System.currentTimeMillis();
        JSONObject payload = new JSONObject();
        payload.put("type", "SIMULATOR_STEP");
        payload.put("timestamp", millis);
        payload.put("id", "MSG_"+millis);
        JSONObject msg = new JSONObject();
        msg.put("simulator", "PTPaterna");
        msg.put("step", counter);
        payload.put("msg", msg);
        return payload;
    }
}
