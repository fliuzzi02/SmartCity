package main.java.device.vehicle;

import main.java.device.Device;
import main.java.device.vehicle.navigation.components.Navigator;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;
import main.java.device.vehicle.navigation.interfaces.IRoadPoint;
import main.java.device.vehicle.navigation.interfaces.IRoute;
import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import static main.java.utils.GlobalVars.STEP_MS;

public class Vehicle extends Device {
    // TODO: To implement this class we need for it to include the component navigator
    private final VehicleRole role;
    private Navigator navigator;
    private int speed;

    private String lastSegment = "";
    private int lastPosition = -1;

    Vehicle(String id, VehicleRole role, int initialSpeed, RoadPoint initialPosition) {
        super(id);
        this.role = role;
        this.navigator = new Navigator(id+"-navigator");
        this.navigator.setCurrentPosition(initialPosition);
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
            handleSimulationStep(new Message(payload));
        }
    }

    /**
     * This method sets the speed of the vehicle
     * @param speed the speed of the vehicle in km/h
     */
    public void setSpeed(int speed){
        this.speed = speed;
    }

    public void setRoute(IRoute route) throws RoutingException{
        if(this.navigator.isRouting()) {
            throw new RoutingException("Vehicle is already routing, cannot set a new route");
        }
        this.navigator.setRoute(route);
        Logger.info(this.id, "Route set: " + route);
    }

    public void startRoute() throws RoutingException{
        if(this.navigator.getRoute() == null){
            throw new RoutingException("No route set");
        }
        this.navigator.startRouting();
    }

    private void handleSimulationStep(Message message){
        // When receiving a step message, move vehicle and update position
        if(message.getType().equals("SIMULATOR_STEP")){
            this.navigator.move(STEP_MS, speed);
            publishUpdate(this.navigator.getCurrentPosition());

            Logger.debug(this.id, "Moved to: " + this.navigator.getCurrentPosition());
        }
    }

    private void publishUpdate(IRoadPoint position){
        String roadSegment = position.getRoadSegment();
        int segmentPosition = position.getPosition();
        Message message;

        // If the vehicle is still in the same segment, just update the position
        if(roadSegment.equals(lastSegment)){
            // TODO: Is this correct? VEHICLE_UPDATE is not defined in the specifications of the project
            message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_UPDATE", roadSegment, segmentPosition);

            try {
                this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + roadSegment + "/traffic", message.toJson());
            } catch (MqttException e) {
                Logger.error(this.id, "Error publishing VEHICLE_UPDATE message: " + e.getMessage());
            }
        } else {
            // Publish vehicle entering new segment
            message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_IN", roadSegment, segmentPosition);

            try {
                this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + roadSegment + "/traffic", message.toJson());
            } catch (MqttException e) {
                Logger.error(this.id, "Error publishing VEHICLE_IN message: " + e.getMessage());
            }

            // Publish vehicle leaving previous segment
            if(!lastSegment.equals("")){
                message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_OUT", lastSegment, lastPosition);

                try {
                    this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + lastSegment + "/traffic", message.toJson());
                } catch (MqttException e) {
                    Logger.error(this.id, "Error publishing VEHICLE_IN message: " + e.getMessage());
                }
            }

            lastSegment = roadSegment;
            lastPosition = segmentPosition;
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
        RoadPoint initialPosition = new RoadPoint("R1s2", 0);
        Vehicle vehicle = new Vehicle("3240JXM", VehicleRole.PrivateUsage, 0, initialPosition);
        try {
            vehicle.init();

            // Set route
            Route route = new Route();
            route.addRouteFragment("R1s2", 0, 29);
            route.addRouteFragment("R1s2a", 29, 320);
            route.addRouteFragment("R5s1", 0, 300);
            vehicle.setRoute(route);

            // Set speed
            vehicle.setSpeed(10);

            // Start route
            vehicle.startRoute();
        } catch (MqttException | RoutingException e) {
            Logger.error(vehicle.id, "An error occurred: " + e.getMessage());
        }
    }

    private static class RoutingException extends Exception {
        public RoutingException(String message) {
            super(message);
        }
    }
}
