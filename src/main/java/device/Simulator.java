package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

public class Simulator {
    public static void main(String[] args) throws MqttException {
        MQTTClient client = new MQTTClient(new Device("Simulator") {
            @Override
            public void init() throws MqttException {

            }

            @Override
            protected void onMessage(String topic, JSONObject payload) {

            }
        }, GlobalVars.BROKER_ADDRESS);
        int counter = 1;
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // TODO: Create message class
            JSONObject payload = Message.createStepMessage(counter);
            client.publish(GlobalVars.BASE_TOPIC + "/step", payload);
            counter++;
        }
    }
}
