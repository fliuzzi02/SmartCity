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
    private int actualSpeed;
    private int cruiseSpeed;
    private String lastSegment = "";
    private int lastPosition = -1;

    Vehicle(String id, VehicleRole role, int initialSpeed, RoadPoint initialPosition) {
        super(id);
        this.role = role;
        this.navigator = new Navigator(id+"-navigator");
        this.navigator.setCurrentPosition(initialPosition);
        this.cruiseSpeed = initialSpeed;
    }

    @Override
    public void init() throws MqttException {
        this.connect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());
        Message message = new Message(payload);
        switch (message.getType()) {
            case "SIMULATOR_STEP" -> handleSimulationStep();
            case "TRAFFIC_SIGNAL" -> handleTrafficSignal(message);
            default -> Logger.warn(this.id, "Unknown message type: " + message.getType());
        }
    }

    /**
     * This method sets the cruise speed of the vehicle
     * @param speed the speed of the vehicle in km/h
     */
    public void setSpeed(int speed){
        this.cruiseSpeed = speed;
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

    private void handleTrafficSignal(Message message) {
        JSONObject msg = message.getMsg();

        if(msg.getString("signal-type").equals("SPEED-LIMIT")){
            // If the vehicle is in the range of the speed limit, set the speed limit accordingly
            int start = msg.getInt("starting-position");
            int end = msg.getInt("ending-position");
            int limit = msg.getInt("value");
            if(this.navigator.getCurrentPosition().getPosition() >= start && this.navigator.getCurrentPosition().getPosition() <= end){
                updateSpeed(limit);
            }
        } else if (msg.getString("signal-type").equals("TRAFFIC-LIGHT")){
            // If the vehicle is in the range of the traffic light that is red (HLL) and within 50m, set the speed to 0
            int start = msg.getInt("starting-position");
            if(this.navigator.getCurrentPosition().getPosition() >= start-50 && this.navigator.getCurrentPosition().getPosition() <= start + 50){
                if(msg.getString("value").equals("HLL")) updateSpeed(0);
                if(msg.getString("value").equals("LLH")) updateSpeed(999);
            }
        }
    }

    private void handleSimulationStep(){
        // When receiving a step message, move vehicle and update position
        this.navigator.move(STEP_MS, actualSpeed);

        Logger.debug(this.id, "Moved to: " + this.navigator.getCurrentPosition());

        IRoadPoint position = this.navigator.getCurrentPosition();
        String roadSegment = position.getRoadSegment();
        int segmentPosition = position.getPosition();

        // If the vehicle is still in the same segment, just update the position
        if(roadSegment.equals(lastSegment)){
            // TODO: What to do here? Probably update the speed
        } else {
            // Publish vehicle entering new segment
            handleEntrance(position);

            // Publish vehicle leaving previous segment
            if(!lastSegment.equals("")) handleExit(new RoadPoint(lastSegment, lastPosition));
        }

        lastSegment = roadSegment;
        lastPosition = segmentPosition;
    }

    private void handleEntrance(IRoadPoint position) {
        // Subscribe to new segment
        try {
            this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/signals");
            this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/traffic");
            this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/alerts");
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        // Publish vehicle entering new segment
        String roadSegment = position.getRoadSegment();
        int segmentPosition = position.getPosition();
        Message message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_IN", roadSegment, segmentPosition);

        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + roadSegment + "/traffic", message.toJson());
        } catch (MqttException e) {
            Logger.error(this.id, "Error publishing VEHICLE_IN message: " + e.getMessage());
        }
    }

    private void handleExit(IRoadPoint position) {
        // Unsubscribe from previous segment
        try {
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/signals");
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/traffic");
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/alerts");
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        // Publish vehicle leaving previous segment
        String roadSegment = position.getRoadSegment();
        int segmentPosition = position.getPosition();
        Message message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_OUT", roadSegment, segmentPosition);

        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + roadSegment + "/traffic", message.toJson());
        } catch (MqttException e) {
            Logger.error(this.id, "Error publishing VEHICLE_OUT message: " + e.getMessage());
        }
    }

    /**
     * Updates the speed limit and the actual speed of the vehicle. It sets the speed upper bound to the new speed limit or the cruise speed, whichever is lower
     * @param speed the new upper limit of the speed
     */
    private void updateSpeed(int speed){
        if(speed <= this.actualSpeed){
            this.actualSpeed = speed;
        } else if(speed >= this.cruiseSpeed){
            this.actualSpeed = this.cruiseSpeed;
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
