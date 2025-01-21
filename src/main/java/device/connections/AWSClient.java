package main.java.device.connections;

import com.amazonaws.services.iot.client.*;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import main.java.device.Device;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.json.JSONObject;

public class AWSClient {
    private AWSIotMqttClient client;
    protected Device myDevice;
    private String clientId;
    private String clientEndpoint;
    private String certificateFile;
    private String privateKeyFile;

    public AWSClient(Device myDevice, String clientEndpoint, String certificateFile, String privateKeyFile) {
        this.myDevice = myDevice;
        this.clientId = myDevice.getId() + "-AWS";
        this.clientEndpoint = clientEndpoint;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
        KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
        this.client = new AWSIotMqttClient(clientEndpoint, this.clientId, pair.keyStore, pair.keyPassword);
        try {
            this.client.connect();
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error connecting to AWS: " + e.getMessage());
        }
    }

    public void subscribe(String topic) {
        AWSTopicHandler topicHandler = new AWSTopicHandler(topic, clientId, myDevice);
        try {
            this.client.subscribe(topicHandler);
            Logger.info(this.clientId, "Subscribed to: " + topic);
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error subscribing to topic: " + topic);
        }
    }

    public void unsubscribe(String topic) {
        try {
            this.client.unsubscribe(topic);
            Logger.info(this.clientId, "Unsubscribed from: " + topic);
        } catch (AWSIotException e) {
            Logger.error(this.clientId, "Error unsubscribing from topic: " + topic);
        }
    }

    public void publish(String topic, JSONObject payload) {
        try {
            AWSIotQos qos = AWSIotQos.QOS0;
            AWSIotMessage message = new AWSIotMessage(topic, qos, payload.toString());
            client.publish(message);
            Logger.info(clientId, "Published: " + payload);
        } catch (AWSIotException e) {
            Logger.error(clientId, "Error publishing message: " + e.getMessage());
        }
    }

    private class AWSTopicHandler extends AWSIotTopic{
        private String clientId;
        private Device myDevice;
        public AWSTopicHandler(String topic, String clientId, Device myDevice) {
            super(topic);
            this.clientId = clientId;
            this.myDevice = myDevice;
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            Message payload = new Message(new JSONObject(message.getStringPayload()));
            Logger.debug(clientId, "RECEIVED: " + payload);
            myDevice.onMessage(new MQTTMessage(message.getTopic(), payload));
        }
    }
}
