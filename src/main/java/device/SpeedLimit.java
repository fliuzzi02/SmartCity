package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * This class represents a speed limit device that can be placed on a road segment
 */
public class SpeedLimit extends Device {
    private int speedLimit;
    private final String roadSegment;
    private final int initialPosition;
    private final int finalPosition;
    private String clientEndpoint;
    private String certificateFile;
    private String privateKeyFile;

    SpeedLimit(String id, int speedLimit, String roadSegment, int initialPosition, int finalPosition) {
        super(id);
        this.speedLimit = speedLimit;
        this.roadSegment = roadSegment;
        this.initialPosition = initialPosition;
        this.finalPosition = finalPosition;
    }

    SpeedLimit(String id, int speedLimit, String roadSegment, int initialPosition, int finalPosition, String clientEndpoint, String certificateFile, String privateKeyFile) {
        super(id);
        this.speedLimit = speedLimit;
        this.roadSegment = roadSegment;
        this.initialPosition = initialPosition;
        this.finalPosition = finalPosition;
        this.clientEndpoint = clientEndpoint;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");

        this.awsConnect(this.clientEndpoint,
                this.certificateFile,
                this.privateKeyFile);

        new Thread(this).start();
    }

    @Override
    protected void handleMessage(MQTTMessage message) {
        Message response = Message.createTrafficSignal(this.id, this.roadSegment, "SPEED_LIMIT", this.initialPosition, this.finalPosition, this.speedLimit);
        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + this.roadSegment + "/signals", response.toJson());
            this.awsConnection.publish("speedLimit/" + this.roadSegment + "/status", response.toJson());
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
