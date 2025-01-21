package main.java.device;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

/**
 * This class represents a generic device
 */
public abstract class Device {
    protected String id;
    protected MQTTClient connection;
    protected AWSClient awsConnection;

    /**
     * Basic example constructor, it only connects to the server
     * @param id the device id
     */
    protected Device(String id) {
        this.id = id;
    }

    /**
     * Connects the device to all its servers/brokers and performs all necessary actions to initialize the component
     */
    public abstract void init() throws MqttException;

    protected void mqttConnect(String brokerAddress) throws MqttException {
        this.connection = new MQTTClient(this, brokerAddress);
    }

    protected void awsConnect(String clientEndpoint, String certificateFile, String privateKeyFile) {
        this.awsConnection = new AWSClient(this, clientEndpoint, certificateFile, privateKeyFile);
    }

    /**
     * This method is called when a message is received from any MQTT Broker
     * @param topic where the message was received
     * @param payload the message received
     */
    protected abstract void onMessage(String topic, JSONObject payload);
}
