package main.java.utils;

import org.json.JSONObject;

public class Message {
    private String type;
    private long timestamp;

    // TODO: Maybe this should be a proper class
    private JSONObject msg;

    Message(String type, long timestamp, JSONObject msg){
        this.type = type;
        this.timestamp = timestamp;
        this.msg = msg;
    }

    Message(JSONObject payload){
        this.type = payload.getString("type");
        this.timestamp = payload.getLong("timestamp");
        this.msg = payload.getJSONObject("msg");
    }

    /**
     * This method returns the message as a JSON object ready to be sent
     * @return the message as a JSON object
     */
    public JSONObject toJson() {
        JSONObject payload = new JSONObject();
        payload.put("type", this.type);
        payload.put("id", "MSG_" + this.timestamp);
        payload.put("timestamp", this.timestamp);
        payload.put("msg", this.msg);
        return payload;
    }

    public static Message createStepMessage(int counter){
        JSONObject msg = new JSONObject();
        msg.put("simulator", "PTPaterna");
        msg.put("step", counter);

        return new Message("STEP", System.currentTimeMillis(), msg);
    }

    public static Message createRoadStatus(){
        JSONObject msg = new JSONObject();
        msg.put("road", "R1s1");
        msg.put("status", "CLOSED");

        return new Message("ROAD_STATUS", System.currentTimeMillis(), msg);
    }

}
