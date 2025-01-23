package main.java.device.vehicle;

import main.java.device.Device;
import main.java.device.vehicle.navigation.components.Navigator;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;
import main.java.device.vehicle.navigation.interfaces.IRoadPoint;
import main.java.device.vehicle.navigation.interfaces.IRoute;
import main.java.utils.GlobalVars;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import main.java.utils.Message;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import static main.java.utils.GlobalVars.STEP_MS;
import static main.java.utils.GlobalVars.getRoadStatus;

public class Vehicle extends Device {
    private final VehicleRole role;
    private final Navigator navigator;
    int actualSpeed;
    int cruiseSpeed;
    int speedLimit = 999;
    private boolean redLight = false;
    private String lastSegment = "";
    private int lastPosition = -1;
    String clientEndpoint;
    String certificateFile;
    String privateKeyFile;

    public Vehicle(String id, VehicleRole role, int initialSpeed, RoadPoint initialPosition, String clientEndpoint, String certificateFile, String privateKeyFile) {
        super(id);
        this.role = role;
        this.navigator = new Navigator(id+"-navigator");
        this.navigator.setCurrentPosition(initialPosition);
        this.cruiseSpeed = initialSpeed;
        this.clientEndpoint = clientEndpoint;
        this.certificateFile = certificateFile;
        this.privateKeyFile = privateKeyFile;
    }

    @Override
    public void init() throws MqttException {
        this.mqttConnect(GlobalVars.BROKER_ADDRESS);
        this.awsConnect(clientEndpoint, certificateFile, privateKeyFile);
        this.connection.subscribe(GlobalVars.BASE_TOPIC + "/step");
        this.awsConnection.subscribe("vehicles/" + this.id + "/command");
        new Thread(this).start();
    }

    /**
     * This method notifies that an accident has occurred at the position of the vehicle
     */
    public void initiateAccident(){
        // create UUID for accident
        String accidentID = java.util.UUID.randomUUID().toString();
        String segment = navigator.getCurrentPosition().getRoadSegment();
        int position = navigator.getCurrentPosition().getPosition();

        Message message = Message.createAccident(accidentID, "OPEN", this.id, segment, position);
        try {
            this.connection.publish(GlobalVars.BASE_TOPIC + "/road/" + segment + "/alerts", message.toJson());
            this.awsConnection.publish("road/" + segment + "/alerts", message.toJson());
        } catch (MqttException e) {
            Logger.error(this.id, "Error publishing ACCIDENT message: " + e.getMessage());
        }

        // This vehicle was in an accident, so it should stop
        setSpeed(0);
        updateSpeed();
        Logger.info(this.id, "Accident initiated at " + segment + " position " + position);
    }

    @Override
    protected void handleMessage(MQTTMessage message) {
        String topic = message.getTopic();
        Message payload = message.getPayload();
        Logger.trace(this.id, "Received message from " + topic + ": " + payload.getMsg().toString());
        switch (payload.getType()) {
            case "SIMULATOR_STEP":
                handleSimulationStep();
                break;
            case "TRAFFIC_SIGNAL":
                handleTrafficSignal(payload);
                break;
            case "COMMAND":
                JSONObject msg = payload.getMsg();
                if(msg.getString("command").equals("ACCIDENT")) initiateAccident();
                if(msg.getString("command").equals("SET_SPEED")) setSpeed(msg.getInt("value"));
                break;
            case "ACCIDENT":
                handleAccident(payload);
                break;
            default:
                Logger.trace(this.id, "Unknown message type: " + payload.getType());
                break;
        }
    }

    private void handleAccident(Message payload) {
        JSONObject msg = payload.getMsg();
        String accidentID = msg.getString("id");
        String status = msg.getString("event");
        String segment = msg.getString("road-segment");
        int position = msg.getInt("position");

        if(status.equals("OPEN")){
            Logger.info(this.id, "Received accident alert: " + accidentID + " at " + segment + " position " + position);
        } else if(status.equals("CLOSED")){
            Logger.info(this.id, "Received accident resolved: " + accidentID + " at " + segment + " position " + position);
        }
    }

    /**
     * This method sets the cruise speed of the vehicle
     * @param speed the speed of the vehicle in km/h
     */
    public void setSpeed(int speed){
        this.cruiseSpeed = speed;
    }

    /**
     * This method sets the route of the vehicle
     * @param route the route to be set
     * @throws RoutingException if the vehicle is already routing
     */
    public void setRoute(IRoute route) throws RoutingException{
        if(this.navigator.isRouting()) {
            throw new RoutingException("Vehicle is already routing, cannot set a new route");
        }
        this.navigator.setRoute(route);
        Logger.info(this.id, "Route set: " + route);
    }

    /**
     * This method starts the routing of the vehicle
     * @throws RoutingException if the vehicle has no route set
     */
    public void startRoute() throws RoutingException{
        if(this.navigator.getRoute() == null){
            throw new RoutingException("No route set");
        }
        this.navigator.startRouting();
    }

    /**
     * This method checks if the vehicle has reached its destination
     * @return true if the vehicle has reached its destination, false otherwise
     */
    public boolean reachedDestination(){
        return this.navigator.getDestinationPoint().equals(this.navigator.getCurrentPosition());
    }

    /**
     * This method stops the routing and instructs the vehicle to exit the road
     */
    public void exitRoad() {
        IRoadPoint position = this.navigator.getCurrentPosition();
        this.navigator.stopRouting();

        // Notify that the vehicle is leaving the road
        handleExit(position);
    }

    private void handleTrafficSignal(Message message) {
        JSONObject msg = message.getMsg();

        if(msg.getString("signal-type").equals("SPEED_LIMIT")){
            // If the vehicle is in the range of the speed limit, set the speed limit accordingly
            int start = msg.getInt("starting-position");
            int end = msg.getInt("ending-position");
            int limit = msg.getInt("value");
            if(this.navigator.getCurrentPosition().getPosition() >= start && this.navigator.getCurrentPosition().getPosition() <= end){
                this.speedLimit = limit;
                Logger.info(this.id, "Recieved signal " + msg.getString("signal-type") + ": "+ limit + " km/h");
            }
        } else if (msg.getString("signal-type").equals("TRAFFIC_LIGHT")){
            // If the vehicle is in the range of the traffic light that is red (HLL) and within 50m, set the speed to 0
            int start = msg.getInt("starting-position");
            if(this.navigator.getCurrentPosition().getPosition() >= start-50 && this.navigator.getCurrentPosition().getPosition() <= start + 50){
                if(msg.getString("value").equals("HLL")) redLight = true;
                if(msg.getString("value").equals("LLH")) redLight = false;
                Logger.info(this.id, "Recieved signal " + msg.getString("signal-type") + ": "+ msg.getString("value"));
            }
        }
    }

    private void handleSimulationStep(){
        updateAWS();
        if(!this.navigator.isRouting() || (this.navigator.getDestinationPoint().equals(this.navigator.getCurrentPosition())))
            return;

        updateSpeed();
        this.navigator.move(STEP_MS, actualSpeed);


        Logger.info(this.id, "Moved to: " + this.navigator.getCurrentPosition());

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
            // Publish vehicle leaving previous segment
            if(!lastSegment.equals("")) handleExit(new RoadPoint(lastSegment, lastPosition));
            // Publish vehicle entering new segment
            handleEntrance(position);
        }

        lastSegment = roadSegment;
        lastPosition = segmentPosition;
    }

    private void handleEntrance(IRoadPoint position) {
        // Update current speed limit of the road segment
        JSONObject roadStatus = getRoadStatus(position.getRoadSegment());
        this.speedLimit = roadStatus.getInt("current-max-speed");

        // Subscribe to new segment
        try {
            this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/signals");
            this.connection.subscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/info");
            this.awsConnection.subscribe("road/" + position.getRoadSegment() + "/info");
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
        Logger.info(this.id, "Entered road segment " + roadSegment);
    }

    private void handleExit(IRoadPoint position) {
        // Unsubscribe from previous segment
        try {
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/signals");
            this.connection.unsubscribe(GlobalVars.BASE_TOPIC + "/road/" + position.getRoadSegment() + "/info");
            this.awsConnection.unsubscribe("road/" + position.getRoadSegment() + "/info");
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
        Logger.info(this.id, "Exited road segment " + roadSegment);
    }

    private void updateAWS(){
        JSONObject update = new JSONObject();
        update.put("current-position", this.navigator.getCurrentPosition().toString());
        update.put("destination", this.navigator.getDestinationPoint().toString());
        update.put("actual-speed", this.actualSpeed);
        update.put("cruise-speed", this.cruiseSpeed);

        this.awsConnection.publish("vehicles/"+this.id+"/status", update);
    }

    /**
     * Updates the actual speed of the vehicle to the maximum of the speed limit and the cruise speed
     */
    void updateSpeed(){
        int oldSpeed = this.actualSpeed;
        this.actualSpeed = Math.min(this.speedLimit, this.cruiseSpeed);
        if(redLight) this.actualSpeed = 0;
        if(oldSpeed != this.actualSpeed)
            Logger.debug(this.id, "Speed updated to " + this.actualSpeed + " km/h");
    }

    /**
     * This method returns the current position of the vehicle
     * @return the current position of the vehicle
     */
    public IRoadPoint getCurrentPosition(){
        return this.navigator.getCurrentPosition();
    }

    public enum VehicleRole {
        PrivateUsage{final String name = "PrivateUsage";},
        Bus{final String name = "Bus";},
        Taxi{final String name = "Taxi";},
        Ambulance{final String name = "Ambulance";},
        Police{final String name = "Police";},
        FireTruck{final String name = "Fire-Truck";}
    }

    public static void main(String []args){

        RoadPoint initialPosition = new RoadPoint("R1s1", 0);
        for(int i=0; i<2; i++) {
            try {
                // format int number to have 4 digits
                String vehicleID = String.format("%04d", i)+ "AAA";

                Vehicle vehicle = new Vehicle(vehicleID, VehicleRole.PrivateUsage, 10, initialPosition, GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
                vehicle.init();

                // Set route
                Route route = new Route();
                route.addRouteFragment("R1s1", 0, 29);
                route.addRouteFragment("R1s2a", 29, 320);
                route.addRouteFragment("R5s1", 0, 300);
                vehicle.setRoute(route);

                // Set speed
                vehicle.setSpeed(10);

                // Start route
                vehicle.startRoute();
            } catch (MqttException | RoutingException e) {
                e.printStackTrace();
            }
        }
    }

    private static class RoutingException extends Exception {
        public RoutingException(String message) {
            super(message);
        }
    }
}
