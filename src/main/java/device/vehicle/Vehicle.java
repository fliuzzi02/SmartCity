package main.java.device.vehicle;

import main.java.device.Device;
import main.java.device.vehicle.navigation.components.Navigator;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;
import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import static main.java.utils.GlobalVars.STEP_MS;

public class Vehicle extends Device {
    // TODO: To implement this class we need for it to include the component navigator
    private final VehicleRole role;
    private Navigator navigator;
    private int speed;

    Vehicle(String id, VehicleRole role, int initialSpeed, String initialSegment, int initialPosition) {
        super(id);
        this.role = role;
        this.navigator = new Navigator(id+"-navigator");
        this.navigator.setCurrentPosition(new RoadPoint(initialSegment, initialPosition));
        this.speed = initialSpeed;
    }

    @Override
    public void init() throws MqttException {
        this.connect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());
        if (topic.endsWith("step")){
            handleSimulationStep(payload);
        }
    }

    // TODO: This is testing
    public void setRoute(){
        Route route = new Route();
        route.addRouteFragment("R1s1", 0, 29);
        route.addRouteFragment("R1s2a", 29, 320);
        route.addRouteFragment("R5s1", 0, 300);

        this.navigator.setRoute(route);
        Logger.info(this.id, "Route set: " + route);
    }

    private void handleSimulationStep(JSONObject payload){
        if(payload.getString("type").equals("SIMULATOR_STEP")){
            this.navigator.move(STEP_MS, speed);
        }
    }

    public enum VehicleRole {
        PrivateUsage{final String name = "PrivateUsage";},
        Bus{final String name = "Bus";},
        Taxi{final String name = "Taxi";},
        Ambulance{final String name = "Ambulance";},
        Police{final String name = "Police";},
        FireTruck{final String name = "Fire-Truck";};
    }

    public static void main(String []args){
        Vehicle vehicle = new Vehicle("vehicle1", VehicleRole.PrivateUsage, 0, "R1s1", 0);
        try {
            vehicle.init();
        } catch (MqttException e) {
            Logger.error(vehicle.id, "An error occurred: " + e.getMessage());
        }
    }
}
