package main.java.device;

import main.java.device.vehicle.SpecialVehicle;
import main.java.utils.*;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class AccidentManager extends Device {
    private final List<Accident> accidents = new LinkedList<>();

    public AccidentManager(String id) {
        super(id);
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/+/alerts");

        new Thread(this).start();
    }


    @Override
    protected void handleMessage(MQTTMessage message) {
        Message payload = message.getPayload();
        if(payload.getType().equals("ACCIDENT")) {
            JSONObject msg = payload.getMsg();
            if(msg.getString("event").equals("OPEN")) {
                accidents.add(new Accident(msg.getString("id"), msg.getString("vehicle"), msg.getString("road-segment"), msg.getInt("position")));
                Logger.info(this.getId(), "Accident reported on segment " + msg.getString("road-segment") + " at position " + msg.getInt("position"));
                Logger.info(this.getId(), "Sending police car and ambulance to the accident location");
            }
        } else if(payload.getType().equals("SIMULATOR_STEP")) {
            // Check if the accidents are still active
            checkAccidents();
        }
    }

    private void checkAccidents() {
        for(Accident accident : accidents) {
            SpecialVehicle policeCar = accident.getPoliceCar();
            SpecialVehicle ambulance = accident.getAmbulance();

            // Check if the police car and ambulance have reached the accident location
            if(policeCar.reachedDestination() && ambulance.reachedDestination()) {
                Message message = Message.createAccident(accident.getId(), "CLOSE", accident.getVehicle(), accident.getSegment(), accident.getPosition());
                try {
                    this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + accident.getSegment() + "/alerts", message.toJson());
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }

                // Stop the vehicles
                policeCar.exitRoad();
                policeCar.stop();
                ambulance.exitRoad();
                ambulance.stop();

                accidents.remove(accident);
                Logger.info(this.getId(), "Accident " + accident.getId() + " has been resolved");
            }
        }
    }
}
