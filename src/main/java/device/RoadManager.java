package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RoadManager extends Device {
    ArrayList<SpeedLimit> speedLimits = new ArrayList<>();

    public RoadManager(String id) {
        super(id);
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/+/alerts");
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/+/info");
        new Thread(this).start();
    }

    @Override
    protected void handleMessage(MQTTMessage message) {
        String topic = message.getTopic();
        Message payload = message.getPayload();

        if(topic.endsWith("alerts")) {
            // Retransmit the alert to the info topic
            try {
                this.connection.publish(topic.replace("alerts", "info"), payload.toJson());
            } catch (MqttException e) {
                Logger.error(this.id, "An error occurred: " + e.getMessage());
            }
        } else if (topic.endsWith("info")) {
            handleStatus(payload);
        }
    }

    private void handleStatus(Message message) {
        if(!message.getType().equals("ROAD_STATUS")) {
            Logger.warn(this.id, "Received a message of type " + message.getType() + " on a status topic");
            return;
        }

        // Gett all the information from the message
        JSONObject msg = message.getMsg();
        String roadStatus = msg.getString("status");
        String roadSegment = msg.getString("road-segment");
        int startKm = msg.getInt("start-kp");
        int endKm = msg.getInt("end-kp");
        int speedLimit = msg.getInt("max-speed");

        Logger.debug(this.id, "Received status: " + roadStatus + " for road segment " + roadSegment + " with speed limit " + speedLimit);

        switch (roadStatus) {
            case "No_Manouvers":
            case "Collapsed":
                speedLimit = 20;
                break;
            case "Limited_Manouvers":
            case "Restricted_Manouvers":
                speedLimit -= 20;
                break;
            case "Free_Flow":
            case "Mostly_Free_Flow":
                speedLimit = 999;
                break;
            default:
                Logger.warn(this.id, "Received an unknown status: " + roadStatus);
        }

        // If speed limit is 999, remove the speed limit from the list with the corresponding road segment
        if(speedLimit == 999) removeSpeedLimit(roadSegment);
        // Else, apply the speed limit to the road segment
        else applySpeedLimit(roadSegment, startKm, endKm, speedLimit);
    }

    private void applySpeedLimit(String roadSegment, int startKm, int endKm, int speedLimit) {
        // Check if the speed limit is already in the list, if so update it
        for(SpeedLimit sl : speedLimits) {
            if(sl.getRoadSegment().equals(roadSegment)) {
                sl.updateSpeedLimit(speedLimit);
                return;
            }
        }

        // Otherwise, create a new speed limit
        createSpeedLimit(roadSegment, startKm, endKm, speedLimit);
        Logger.debug(this.id, "Created speed limit for road segment " + roadSegment + " with speed limit " + speedLimit);
    }

    private void createSpeedLimit(String roadSegment, int startKm, int endKm, int speedLimit) {
        SpeedLimit sl = new SpeedLimit("SL-" + roadSegment, speedLimit, roadSegment, startKm, endKm);
        speedLimits.add(sl);
        try {
            sl.init();
        } catch (MqttException e) {
            Logger.error(this.id, "An error occurred: " + e.getMessage());
        }
    }

    private void removeSpeedLimit(String roadSegment) {
        for(SpeedLimit sl : speedLimits) {
            if(sl.getRoadSegment().equals(roadSegment)) {
                sl.unregister();
                speedLimits.remove(sl);
                Logger.debug(this.id, "Removed speed limit for road segment " + roadSegment);
                return;
            }
        }
    }

    public static void main(String []args) {
        RoadManager manager = new RoadManager("RM-1");
        try {
            manager.init();
        } catch (MqttException e) {
            Logger.error(manager.id, "An error occurred: " + e.getMessage());
        }
    }
}
