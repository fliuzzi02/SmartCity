package main.java.device.connections;

import main.java.device.Device;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

/**
 * This class represents the MQTT Connection to the broker
 */
public class MQTTClient implements MqttCallback {

    protected final Device myDevice;
    private final String address;
    private final String clientId;
    private final MqttClient client;

    public MQTTClient(Device mydevice, String brokerAddress) throws MqttException {
        this.myDevice = mydevice;
        this.address = brokerAddress;
        // Client id is the device id plus a random short UUID
        this.clientId = myDevice.getId() + "-MQTT";
        this.client = new MqttClient(this.address, this.clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        this.client.setCallback(this);
        this.client.connect(options);
        Logger.info(clientId, "Connected to broker at " + this.address);
    }

    /**
     * This method allows the client to subscribe to the specified topic
     * @param topic where to subscribe
     */
    public void subscribe(String topic) throws MqttException {
        this.client.subscribe(topic);
        Logger.trace(clientId, "Subscribed to " + topic);
    }

    /**
     * This method allows the client to unsubscribe from the specified topic
     * @param topic where to unsubscribe
     */
    public void unsubscribe(String topic) throws MqttException {
        this.client.unsubscribe(topic);
        Logger.trace(clientId, "Unsubscribed from " + topic);
    }

    /**
     * This method allows to publish a message to the specified topic
     * @param topic where to publish
     * @param payload the message to publish
     */
    public void publish(String topic, JSONObject payload) throws MqttException {
        this.client.publish(topic, payload.toString().getBytes(), 0, false);
        Logger.trace(clientId, "Published message to " + topic);
    }

    protected void disconnect() throws MqttException {
        this.client.disconnect();
        Logger.info(clientId, "Disconnected from broker");
    }

    protected void reconnect() throws MqttException {
        this.client.reconnect();
        Logger.info(clientId, "Reconnected to broker");
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) {
        // Logger.debug(clientId, "Received message on " + s);
        JSONObject payload;
        try {
            payload = new JSONObject(new String(mqttMessage.getPayload()));
        } catch (Exception e) {
            Logger.warn(clientId, "Error parsing message: " + e.getMessage());
            return;
        }
        MQTTMessage message = new MQTTMessage(s, new Message(payload));
        myDevice.onMessage(message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Logger.trace(clientId, "Message delivered");
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Logger.error(clientId, "Connection lost: " + throwable.getMessage());
    }
}
