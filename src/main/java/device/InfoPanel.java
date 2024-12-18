package main.java.device;

import main.java.utils.ConnOptions;
import main.java.utils.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

/**
 * This class represents an InfoPanel device
 * It has three functions, each one has a different purpose
 * 1. Traffic congestion (ON: Free_Flow/Mostly_Free_Flow, BLINK: Limited_Manouvers, ON: No_Manouvers/Collapsed
 * 2. Accidents: (BLINK: Accident, OFF: No_Accident)
 * 3. Reserved Circulation: (ON: Special vehicle >200m, BLINK: Special Vehicle <200m, OFF: No special vehicle)
 * Subscribes to topic .../road/<roadSegment>/traffic
 */
public class InfoPanel extends Device{
    String roadSegment;
    FunctionStatus TrafficCongestion;
    FunctionStatus Accidents;
    FunctionStatus ReservedCirculation;

    InfoPanel(String id, String roadSegment) {
        super(id);
        this.roadSegment = roadSegment;
        this.TrafficCongestion = FunctionStatus.OFF;
        this.Accidents = FunctionStatus.OFF;
        this.ReservedCirculation = FunctionStatus.OFF;
    }

    @Override
    protected void connect(String brokerAddress) throws MqttException {
        super.connect(brokerAddress);
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());

        if (topic.contains("traffic")){
            updateTrafficCongestion(payload);
        }
    }

    private void updateTrafficCongestion(JSONObject payload){
        JSONObject msg = payload.getJSONObject("msg");
        String status = msg.getString("status");

        switch (status) {
            case "Free_Flow", "Mostly_Free_Flow" -> this.TrafficCongestion = FunctionStatus.OFF;
            case "Limited_Manouvers" -> this.TrafficCongestion = FunctionStatus.BLINK;
            case "No_Manouvers", "Collapsed" -> this.TrafficCongestion = FunctionStatus.ON;
        }

        Logger.info(this.id, "Traffic congestion status: " + this.TrafficCongestion);
    }

    public static void main(String[] args) {
        InfoPanel panel = new InfoPanel("Panel1", "R1s1");
        try {
            panel.connect(ConnOptions.BROKER_ADDRESS);
            panel.connection.subscribe(ConnOptions.BASE_TOPIC + "/road/" + panel.roadSegment + "/traffic");
        } catch (MqttException e) {
            Logger.error(panel.id, "An error occurred: " + e.getMessage());
        }
    }

    private enum FunctionStatus{
        ON, OFF, BLINK
    }
}
