package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

public class SpeedLimit extends Device {
    private int speedLimit;
    private final String roadSegment;
    private final int initialPosition;
    private final int finalPosition;

    SpeedLimit(String id, int speedLimit, String roadSegment, int initialPosition, int finalPosition) {
        super(id);
        this.speedLimit = speedLimit;
        this.roadSegment = roadSegment;
        this.initialPosition = initialPosition;
        this.finalPosition = finalPosition;
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Message message = Message.createTrafficSignal(this.id, this.roadSegment, "SPEED_LIMIT", this.initialPosition, this.finalPosition, this.speedLimit);
        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + this.roadSegment + "/signals", message.toJson());
        } catch (MqttException e) {
            Logger.warn(this.id, "An error occurred: " + e.getMessage());
        }
    }

    /**
     * This method updates the speed limit of the road segment
     * @param newSpeedLimit the new speed limit
     */
    protected void updateSpeedLimit(int newSpeedLimit) {
        this.speedLimit = newSpeedLimit;
    }

    /**
     * This method returns the road segment where the speed limit is applied
     * @return the road segment
     */
    protected String getRoadSegment() {
        return this.roadSegment;
    }

    protected void unregister() {
        try {
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/step");
        } catch (MqttException e) {
            Logger.error(this.id, "An error occurred: " + e.getMessage());
        }
    }
}
