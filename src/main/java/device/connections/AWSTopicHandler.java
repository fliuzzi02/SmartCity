package main.java.device.connections;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import main.java.device.Device;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.json.JSONObject;

public class AWSTopicHandler extends AWSIotTopic {
    private final String clientId;
    private final Device myDevice;
    public AWSTopicHandler(String topic, String clientId, Device myDevice) {
        super(topic, AWSIotQos.QOS0);
        this.clientId = clientId;
        this.myDevice = myDevice;
        Logger.debug(clientId, "Created topicHandler");
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        JSONObject payload = new JSONObject(message.getStringPayload());
        Message myMessage = new Message("COMMAND", payload);
        MQTTMessage mqttMessage = new MQTTMessage(message.getTopic(), myMessage);
        // Logger.debug(clientId, "Message received on topic: " + mqttMessage.getTopic() + " with payload: " + mqttMessage.getPayload().getMsg().toString());
        myDevice.onMessage(mqttMessage);
    }
}
