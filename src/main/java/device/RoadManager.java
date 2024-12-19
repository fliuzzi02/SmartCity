package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

public class RoadManager extends Device {

    RoadManager(String id) {
        super(id);
    }

    @Override
    public void init() throws MqttException {
        this.connect(GlobalVars.BROKER_ADDRESS, GlobalVars.USERNAME, GlobalVars.PASSWORD);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/+/alerts");
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        // TODO: Verify that the manager only has to retransmit the same message from alerts to info??
        String segment = payload.getJSONObject("msg").getString("road-segment");
        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + segment + "/info", payload);
        } catch (MqttException e) {
            Logger.error(this.id, "Could not publish message on topic: " + topic);
        }
    }

    public static void main(String []args) {
        RoadManager manager = new RoadManager(args[0]);
        try {
            manager.init();
        } catch (MqttException e) {
            Logger.error(manager.id, "An error occurred: " + e.getMessage());
        }
    }
}
