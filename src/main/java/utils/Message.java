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

    /**
     * Creates a Simulator step message
     * @param counter the step counter of the sim
     * @return the message
     */
    public static Message createStepMessage(int counter){
        JSONObject msg = new JSONObject();
        msg.put("simulator", "PTPaterna");
        msg.put("step", counter);

        return new Message("STEP", System.currentTimeMillis(), msg);
    }

    /**
     * Creates a new road Status message
     * @param road The name of the road, es. 'R1'
     * @param segment The name of the segment, es. 'R1s1'
     * @param len The length of the segment in km, es. 50
     * @param maxSpeed The maximum speed allowed in the segment, es. 40
     * @param capacity The maximum capacity of the segment, es. 8
     * @param status The status of the segment, es. 'No_Manouvers'
     * @return the message
     */
    public static Message createRoadStatus(String road, String segment, int len, int maxSpeed, int capacity, String status){
        JSONObject msg = new JSONObject();
        msg.put("code", segment);
        msg.put("rt", "road-segment");
        msg.put("road-segment", segment);
        msg.put("link", "/segment/" + segment);
        msg.put("road", road);
        msg.put("length", len);
        msg.put("start-kp", 0);
        msg.put("end-kp", len);
        msg.put("max-speed", maxSpeed);
        msg.put("current-max-speed", maxSpeed);
        msg.put("capacity", capacity);
        msg.put("num-vehicles", 0);
        msg.put("density", 0);
        msg.put("status", status);

        return new Message("ROAD_STATUS", System.currentTimeMillis(), msg);
    }
}
