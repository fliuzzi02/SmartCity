package main.java.utils;

import org.json.JSONObject;

/**
 * This class represents a message that can be sent through the MQTT broker, containing a type, a timestamp and the actual payload, called msg
 */
public class Message {
    private final String type;
    private final long timestamp;
    private final JSONObject msg;

    /**
     * This constructor creates a message with a type and a msg (The inner payload of the message)
     * @param type the type of the message
     * @param msg the payload of the message
     */
    public Message(String type, JSONObject msg){
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.msg = msg;
    }

    /**
     * This constructor creates a message from a JSON object of the received payload from the broker
     * @param payload the payload of the message
     */
    public Message(JSONObject payload){
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
     * This method returns the type of the message
     * @return the type of the message as a string
     */
    public String getType(){
        return this.type;
    }

    // TODO: At this point we should have one class for each type of message
    public JSONObject getMsg(){
        return this.msg;
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

        return new Message("STEP", msg);
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

        return new Message("ROAD_STATUS", msg);
    }

    /**
     * Creates a new traffic signal message
     * @param id the id of the signal
     * @param roadSegment the road segment where the signal is
     * @param signalType the type of the signal (TRAFFIC_LIGHT or SPEED_LIMIT)
     * @param startingPosition the starting position of the signal
     * @param endingPosition the ending position of the signal
     * @param value the value of the signal (can be String in case of TRAFFIC_LIGHT or int in case of SPEED_LIMIT)
     * @return the message containing the formatted information
     */
    public static Message createTrafficSignal(String id, String roadSegment, String signalType, int startingPosition, int endingPosition, Object value){
        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("rt", "traffic-signal");
        msg.put("signal-type", signalType);
        msg.put("road-segment", roadSegment);
        msg.put("starting-position", startingPosition);
        msg.put("ending-position", endingPosition);
        msg.put("value", value);

        return new Message("TRAFFIC_SIGNAL", msg);
    }

    /**
     * Creates a new traffic message that should be published by cars
     * @param id the id of the vehicle (its plate)
     * @param vehicleRole the role of the vehicle
     * @param action the action the vehicle is performing (VEHICLE_IN or VEHICLE_OUT)
     * @param roadSegment the road segment where the vehicle is
     * @param position the position of the vehicle in the road segment in meters
     * @return the message containing the formatted information
     */
    public static Message createTraffic(String id, String vehicleRole, String action, String roadSegment, int position) {
        JSONObject msg = new JSONObject();
        msg.put("vehicle-id", id);
        msg.put("vehicle-role", vehicleRole);
        msg.put("action", action);
        msg.put("road-segment", roadSegment);
        msg.put("position", position);

        return new Message("TRAFFIC", msg);
    }

    /**
     * Creates a new accident message
     * @param accidentId the id of the accident, should match in both OPEN and CLOSE event
     * @param event the event of the accident (OPEN or CLOSE)
     * @param vehicle the vehicle involved in the accident
     * @param roadSegment the road segment where the accident is
     * @param position the position of the accident in the road segment in meters
     * @return the message containing the formatted information
     */
    public static Message createAccident(String accidentId, String event, String vehicle, String roadSegment, int position){
        JSONObject msg = new JSONObject();
        msg.put("event", event);
        msg.put("id", accidentId);
        msg.put("rt", "accident");
        msg.put("vehicle", vehicle);
        msg.put("road-segment", roadSegment);
        msg.put("position", position);

        return new Message("ACCIDENT", msg);
    }
}
