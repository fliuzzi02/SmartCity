package main.java.device.vehicle;

import main.java.utils.Accident;
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

import java.util.LinkedList;
import java.util.Queue;

import static java.lang.System.exit;
import static main.java.utils.GlobalVars.STEP_MS;

public class Vehicle extends Device {
    private final VehicleRole role;
    private final Navigator navigator;
    int actualSpeed;
    int cruiseSpeed;
    int speedLimit = 999;
    private boolean redLight = false;
    private String lastSegment = "";
    private int lastPosition = -1;
    //FIFO Queue of Strings
    private final Queue<Accident> accidentList = new LinkedList<>();

    Vehicle(String id, VehicleRole role, int initialSpeed, RoadPoint initialPosition) {
        super(id);
        this.role = role;
        this.navigator = new Navigator(id+"-navigator");
        this.navigator.setCurrentPosition(initialPosition);
        this.cruiseSpeed = initialSpeed;
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
    }

    /**
     * This method notifies that an accident has occurred at the position of the vehicle
     */
    public void notifyAccident(){
        // create UUID for accident
        String accidentID = java.util.UUID.randomUUID().toString();
        String segment = navigator.getCurrentPosition().getRoadSegment();
        int position = navigator.getCurrentPosition().getPosition();
        Accident accident = new Accident(accidentID, segment, position);
        accidentList.add(accident);

        Message message = Message.createAccident(accidentID, "OPEN", this.id, segment, position);
        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + segment + "/alerts", message.toJson());
        } catch (MqttException e) {
            Logger.error(this.id, "Error publishing ACCIDENT message: " + e.getMessage());
        }
    }

    /**
     * This method removes the oldest accident from the list and notifies that the accident has been resolved
     */
    public void removeAccident(){
        Accident oldestAccident = accidentList.poll();
        if(oldestAccident != null){
            Message message = Message.createAccident(oldestAccident.getId(), "CLOSED", this.id, oldestAccident.getSegment(), oldestAccident.getPosition());
            try {
                this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + oldestAccident.getSegment() + "/alerts", message.toJson());
            } catch (MqttException e) {
                Logger.error(this.id, "Error publishing ACCIDENT message: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onMessage(String topic, JSONObject payload) {
        Logger.info(this.id, "Received message from " + topic + ": " + payload.toString());
        Message message = new Message(payload);
        switch (message.getType()) {
            case "SIMULATOR_STEP":
                handleSimulationStep();
                break;
            case "TRAFFIC_SIGNAL":
                handleTrafficSignal(message);
                break;
            default:
                Logger.warn(this.id, "Unknown message type: " + message.getType());
                break;
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
                this.speedLimit = limit;
            }
        } else if (msg.getString("signal-type").equals("TRAFFIC-LIGHT")){
            // If the vehicle is in the range of the traffic light that is red (HLL) and within 50m, set the speed to 0
            int start = msg.getInt("starting-position");
            if(this.navigator.getCurrentPosition().getPosition() >= start-50 && this.navigator.getCurrentPosition().getPosition() <= start + 50){
                if(msg.getString("value").equals("HLL")) redLight = true;
                if(msg.getString("value").equals("LLH")) redLight = false;
            }
        }
    }

    private void handleSimulationStep(){
        // When receiving a step message, move vehicle and update position
        updateSpeed();
        this.navigator.move(STEP_MS, actualSpeed);

        // Logger.debug(this.id, "Moved to: " + this.navigator.getCurrentPosition());

        IRoadPoint position = this.navigator.getCurrentPosition();
        String roadSegment = position.getRoadSegment();
        int segmentPosition = position.getPosition();

        // If the vehicle is still in the same segment, just update the position using VEHILCE_IN
        if(roadSegment.equals(lastSegment)){
            Message message = Message.createTraffic(this.id, this.role.name(), "VEHICLE_IN", roadSegment, segmentPosition);

            try {
                this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + roadSegment + "/traffic", message.toJson());
            } catch (MqttException e) {
                Logger.error(this.id, "Error publishing VEHICLE_IN message: " + e.getMessage());
            }
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
        Logger.debug(this.id, "Entered road segment " + roadSegment);
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
        Logger.debug(this.id, "Exited road segment " + roadSegment);
    }

    /**
     * Updates the actual speed of the vehicle to the maximum of the speed limit and the cruise speed
     */
    void updateSpeed(){
        this.actualSpeed = Math.min(this.speedLimit, this.cruiseSpeed);
        if(redLight) this.actualSpeed = 0;
        Logger.debug(this.id, "Speed updated to " + this.actualSpeed + " km/h");
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
        Vehicle vehicle = new Vehicle("3240KKK", VehicleRole.PrivateUsage, 10, initialPosition);
        try {
            vehicle.init();

            // Set route
            Route route = new Route();
            route.addRouteFragment("R1s2", 0, 29);
            route.addRouteFragment("R1s2a", 29, 320);
            route.addRouteFragment("R5s1", 0, 300);
            vehicle.setRoute(route);

            // Set speed
            vehicle.setSpeed(50);

            // Start route
            vehicle.startRoute();

            // Wait for 10 seconds
            Thread.sleep(10000);
            vehicle.notifyAccident();
        } catch (MqttException | RoutingException e) {
            Logger.error(vehicle.id, "An error occurred: " + e.getMessage());
            exit(-1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class RoutingException extends Exception {
        public RoutingException(String message) {
            super(message);
        }
    }
}
