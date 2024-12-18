package main.java.device;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

/**
 * This class represents a generic device
 */
public abstract class Device {
    String id;
    MQTTClient connection;

    /**
     * Basic example constructor, it only connects to the server
     * @param id the device id
     */
    Device(String id) {
        this.id = id;
    }

    protected void connect(String brokerAddress) throws MqttException {
        this.connection = new MQTTClient(this, brokerAddress);
    }

    /**
     * This method is called when a message is received
     * @param topic where the message was received
     * @param payload the message received
     */
    protected abstract void onMessage(String topic, JSONObject payload);
}
