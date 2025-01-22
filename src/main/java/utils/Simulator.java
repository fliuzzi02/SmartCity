package main.java.utils;

import main.java.device.Device;
import main.java.device.connections.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Simulator {
    public static void main(String[] args) throws MqttException {
        MQTTClient client = new MQTTClient(new Device("Simulator") {
            @Override
            public void init() throws MqttException {

            }

            @Override
            protected void handleMessage(MQTTMessage message) {

            }
        }, GlobalVars.BROKER_ADDRESS);

        int count = 0;
        while(true){
            client.publish(GlobalVars.BASE_TOPIC + "/step", Message.createStepMessage(count).toJson());
            Logger.info("Simulator", "Sent step message: " + count);
            count++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
