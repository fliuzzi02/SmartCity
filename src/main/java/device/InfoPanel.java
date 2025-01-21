package main.java.device;

import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.Math.abs;

/**
 * This class represents an InfoPanel device
 * It has three functions, each one has a different purpose
 * 1. Traffic congestion (ON: Free_Flow/Mostly_Free_Flow, BLINK: Limited_Manouvers, ON: No_Manouvers/Collapsed
 * 2. Accidents: (BLINK: Accident, OFF: No_Accident)
 * 3. Reserved Circulation: (ON: Special vehicle >200m, BLINK: Special Vehicle <200m, OFF: No special vehicle)
 * Subscribes to topic .../road/<roadSegment>/traffic
 */
public class InfoPanel extends Device{
    // TODO: Add AWS IoT Device Shadow functionality
    final String roadSegment;
    final int position;
    FunctionStatus trafficStatus;
    FunctionStatus accidentStatus;
    ArrayList<String> accidentsIds = new ArrayList<>();
    FunctionStatus circulationStatus;

    /**
     * Constructor of the Information Panel
     * @param id id of the panel
     * @param roadSegment name of the segment where the panel is located
     * @param position position in meters at which the panel is located
     */
    InfoPanel(String id, String roadSegment, int position) {
        super(id);
        this.roadSegment = roadSegment;
        this.trafficStatus = FunctionStatus.OFF;
        this.accidentStatus = FunctionStatus.OFF;
        this.circulationStatus = FunctionStatus.OFF;
        this.position = position;
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + this.roadSegment + "/info");
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + this.roadSegment + "/alerts");
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + this.roadSegment + "/traffic");
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");

        this.awsConnect("ampdveamdmijg-ats.iot.us-east-1.amazonaws.com",
                "certs/99e3f3c36622033e6f14e23903f7bc75ed1770dcaf8f94f838a271f6beb94b5f-certificate.pem.crt",
                "certs/99e3f3c36622033e6f14e23903f7bc75ed1770dcaf8f94f838a271f6beb94b5f-private.pem.key");

        new Thread(this).start();
    }

    @Override
    protected void handleMessage(MQTTMessage message){
        String topic = message.getTopic();
        JSONObject payload = message.getPayload().toJson();
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());

        if (topic.endsWith("info")){
            updateTrafficCongestion(payload);
        } else if (topic.endsWith("alerts")){
            updateAccident(payload);
        } else if (topic.endsWith("traffic")){
            updateCirculation(payload);
        } else if (topic.endsWith("step")){
            JSONObject update = new JSONObject();
            update.put("f1", this.trafficStatus.name());
            update.put("f2", this.accidentStatus.name());
            update.put("f3", this.circulationStatus.name());
            this.awsConnection.publish("status", update);
        }
    }

    private void updateTrafficCongestion(JSONObject payload){
        String type = payload.getString("type");
        if (type.equals("ROAD_STATUS")) {
            JSONObject msg = payload.getJSONObject("msg");
            String status = msg.getString("status");

            switch (status) {
                case "Free_Flow":
                case "Mostly_Free_Flow":
                    this.trafficStatus = FunctionStatus.OFF;
                    break;
                case "Limited_Manouvers":
                    this.trafficStatus = FunctionStatus.BLINK;
                    break;
                case "No_Manouvers":
                case "Collapsed":
                    this.trafficStatus = FunctionStatus.ON;
                    break;
            }

            Logger.info(this.id, "Traffic congestion status: " + this.trafficStatus);
        }
    }

    private void updateAccident(JSONObject payload){
        String type = payload.getString("type");
        if(type.equals("ACCIDENT")){
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

    private void updateCirculation(JSONObject payload){
        String type = payload.getString("type");
        if (type.equals("TRAFFIC")){
            JSONObject msg = payload.getJSONObject("msg");
            String action = msg.getString("action");
            String vehicle = msg.getString("vehicle-role");
            int pos = msg.getInt("position");

            circulationStatus = FunctionStatus.OFF;

            if(vehicle.equals("Ambulance") || vehicle.equals("Police")){
                if (action.equals("VEHICLE_IN")){
                    if( abs(this.position - pos) <= 200) {
                        circulationStatus = FunctionStatus.BLINK;
                    } else if ( abs(this.position - pos) > 200) {
                        circulationStatus = FunctionStatus.ON;
                    }
                }
            }

            Logger.info(this.id, "Reserved circulation status: " + this.circulationStatus);
        }
    }

    public static void main(String[] args) {
        InfoPanel panel = new InfoPanel(args[0], args[1], Integer.parseInt(args[2]));
        try {
            panel.init();
        } catch (MqttException e) {
            Logger.error(panel.id, "An error occurred: " + e.getMessage());
        }
    }

    private enum FunctionStatus{
        ON, OFF, BLINK
    }
}
