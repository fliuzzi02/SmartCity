package main.java.device;

import main.java.utils.ConnOptions;
import main.java.utils.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

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
    FunctionStatus trafficStatus;
    FunctionStatus accidentStatus;
    ArrayList<String> accidentsIds = new ArrayList<>();
    FunctionStatus circulationStatus;

    InfoPanel(String id, String roadSegment) {
        super(id);
        this.roadSegment = roadSegment;
        this.trafficStatus = FunctionStatus.OFF;
        this.accidentStatus = FunctionStatus.OFF;
        this.circulationStatus = FunctionStatus.OFF;
    }

    @Override
    protected void connect(String brokerAddress) throws MqttException {
        super.connect(brokerAddress);
    }

    @Override
    protected void connect(String brokerAddress, String username, String password) throws MqttException {
        super.connect(brokerAddress, username, password);
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());

        if (topic.contains("info")){
            updateTrafficCongestion(payload);
        } else if (topic.contains("alerts")){
            updateAccident(payload);
        }
    }

    private void updateTrafficCongestion(JSONObject payload){
        JSONObject msg = payload.getJSONObject("msg");
        String status = msg.getString("status");

        switch (status) {
            case "Free_Flow", "Mostly_Free_Flow" -> this.trafficStatus = FunctionStatus.OFF;
            case "Limited_Manouvers" -> this.trafficStatus = FunctionStatus.BLINK;
            case "No_Manouvers", "Collapsed" -> this.trafficStatus = FunctionStatus.ON;
        }

        Logger.info(this.id, "Traffic congestion status: " + this.trafficStatus);
    }

    private void updateAccident(JSONObject payload){
        String type = payload.getString("type");
        if(Objects.equals(type, "ACCIDENT")){
            JSONObject msg = payload.getJSONObject("msg");
            String event = msg.getString("event");
            String accidentId = msg.getString("id");

            // If event is OPEN, a new accident occurred, add it to active accident list
            // If event is CLOSE, remove it from active list, if list is empty, turn off signal
            if (event.equals("OPEN")){
                if (!accidentsIds.contains(accidentId)) {
                    accidentsIds.add(accidentId);
                    Logger.info(this.id, "New accident added: " + accidentId);
                }
                accidentStatus = FunctionStatus.BLINK;
            } else if(event.equals("CLOSE")){
                // Remove element with same string
                accidentsIds.remove(accidentId);
                Logger.info(this.id, "Accident removed: " + accidentId);
                if (accidentsIds.size() == 0)
                    accidentStatus = FunctionStatus.OFF;
            }

            Logger.info(this.id, "Accident status: " + this.accidentStatus);
        }
    }

    public static void main(String[] args) {
        InfoPanel panel = new InfoPanel("Panel1", "R1s1");
        try {
            panel.connect(ConnOptions.BROKER_ADDRESS, ConnOptions.USERNAME, ConnOptions.PASSWORD);
            panel.connection.subscribe(ConnOptions.BASE_TOPIC + "/road/" + panel.roadSegment + "/info");
            panel.connection.subscribe(ConnOptions.BASE_TOPIC + "/road/" + panel.roadSegment + "/alerts");
        } catch (MqttException e) {
            Logger.error(panel.id, "An error occurred: " + e.getMessage());
        }
    }

    private enum FunctionStatus{
        ON, OFF, BLINK
    }
}
