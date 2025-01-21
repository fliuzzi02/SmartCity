package main.java.utils;

/**
 * This class encapsulates a message received from the MQTT broker
 */
public class MQTTMessage {
    private final String topic;
    private final Message payload;

    public MQTTMessage (String topic, Message payload) {
        this.topic = topic;
        this.payload = payload;
    }

    public String getTopic() {
        return this.topic;
    }

    public Message getPayload() {
        return this.payload;
    }
}
