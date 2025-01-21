package main.java.device;

import com.amazonaws.services.iot.client.*;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import main.java.utils.Logger;
import org.json.JSONObject;

import java.util.UUID;

public class AWSClient {
    private AWSIotMqttClient client;
    protected Device myDevice;
    private String clientId;
    private String clientEndpoint;
    private String certificateFile;
    private String privateKeyFile;

    AWSClient(Device myDevice, String clientEndpoint, String certificateFile, String privateKeyFile) {
        this.myDevice = myDevice;
        this.clientId = myDevice.id + "-AWS" + UUID.randomUUID().toString().substring(0, 5);
        this.clientEndpoint = clientEndpoint;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
        KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
        this.client = new AWSIotMqttClient(clientEndpoint, this.clientId, pair.keyStore, pair.keyPassword);
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
            JSONObject payload = new JSONObject(message.getStringPayload());
            Logger.debug(clientId, "RECEIVED: " + payload);
            myDevice.onMessage(message.getTopic(), payload);
        }
    }
}
