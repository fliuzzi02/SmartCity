package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.util.UUID;

import static java.lang.System.exit;

/**
 * This class represents the MQTT Connection to the broker
 */
public class MQTTClient implements MqttCallback {

    protected final Device myDevice;
    private final String address;
    private final String clientId;
    private final MqttClient client;

    MQTTClient(Device mydevice, String brokerAddress) throws MqttException {
        this.myDevice = mydevice;
        this.address = brokerAddress;
        // Client id is the device id plus a random short UUID
        this.clientId = myDevice.id + "-" + UUID.randomUUID().toString().substring(0, 5);
        this.client = new MqttClient(this.address, this.clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        this.client.setTimeToWait(1000);
        this.client.setCallback(this);
        this.client.connect(options);
        Logger.info(clientId, "Connected to broker at " + this.address);
    }

    MQTTClient(Device mydevice, String brokerAddress, String username, String password) throws MqttException {
        this.myDevice = mydevice;
        this.address = brokerAddress;
        // Client id is the device id plus a random short UUID
        this.clientId = myDevice.id + "-" + UUID.randomUUID().toString().substring(0, 5);
        this.client = new MqttClient(this.address, this.clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
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
        Logger.info(clientId, "Subscribed to " + topic);
    }

    /**
     * This method allows to publish a message to the specified topic
     * @param topic where to publish
     * @param payload the message to publish
     */
    public void publish(String topic, JSONObject payload) throws MqttException {
        this.client.publish(topic, payload.toString().getBytes(), 0, false);
        Logger.debug(clientId, "Published message to " + topic);
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
        Logger.debug(clientId, "Received message on " + s);
        JSONObject payload;
        try {
            payload = new JSONObject(new String(mqttMessage.getPayload()));
            this.myDevice.onMessage(s, payload);
        } catch (Exception e) {
            Logger.warn(clientId, "Error parsing message: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Logger.trace(clientId, "Message delivered");
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Logger.error(clientId, "Connection lost: " + throwable.getMessage());
    }

    public static void main(String[] args) {
        // Test the MQTTClient
        try {
            Device myDevice = new InfoPanel("test", "test", 0);
            MQTTClient client = new MQTTClient(myDevice, GlobalVars.BROKER_ADDRESS);
            Message message = Message.createTraffic("test", "sadnnead", "VEHICLE_IN", "ASDAS", 0);
            client.publish(GlobalVars.BASE_TOPIC + "/road/" + "ASDAS" + "/traffic", message.toJson());
            client.disconnect();
            exit(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
