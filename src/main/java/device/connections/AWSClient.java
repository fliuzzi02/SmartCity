package main.java.device.connections;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import main.java.device.Device;
import main.java.utils.Logger;
import org.json.JSONObject;

public class AWSClient {
    private AWSIotMqttClient client;
    protected Device myDevice;
    private String clientId;

    public AWSClient(Device myDevice, String clientEndpoint, String certificateFile, String privateKeyFile) {
        this.myDevice = myDevice;
        this.clientId = myDevice.getId() + "-AWS";
        KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
        this.client = new AWSIotMqttClient(clientEndpoint, this.clientId, pair.keyStore, pair.keyPassword);
        try {
            this.client.connect();
            Logger.debug(this.clientId, "Connected to AWS IoT");
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error connecting to AWS: " + e.getMessage());
        }
    }

    public void subscribe(String topic) {
        AWSTopicHandler topicHandler = new AWSTopicHandler(topic, clientId, myDevice);
        try {
            this.client.subscribe(topicHandler);
            Logger.debug(this.clientId, "Subscribed to: " + topic);
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error subscribing to topic: " + topic);
        }
    }

    public void unsubscribe(String topic) {
        try {
            this.client.unsubscribe(topic);
            Logger.trace(this.clientId, "Unsubscribed from: " + topic);
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error unsubscribing from topic: " + topic);
        }
    }

    public void publish(String topic, JSONObject payload) {
        try {
            AWSIotQos qos = AWSIotQos.QOS0;
            AWSIotMessage message = new AWSIotMessage(topic, qos, payload.toString());
            client.publish(message);
            Logger.trace(clientId, "Published: " + payload);
        } catch (AWSIotException e) {
            Logger.error(clientId, "Error publishing message: " + e.getMessage());
        }
    }
}
