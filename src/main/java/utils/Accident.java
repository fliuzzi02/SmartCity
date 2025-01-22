package main.java.utils;

import main.java.device.vehicle.SpecialVehicle;
import main.java.device.vehicle.Vehicle;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;

public class Accident {
    String accidentId;
    String vehicleId;
    String accidentSegment;
    int accidentPosition;
    SpecialVehicle policeCar;
    SpecialVehicle ambulance;

    /**
     * Constructor for the Accident class, creates the accident and dispatches the police car and ambulance to the accident location
     * @param accidentId Unique identifier for the accident
     * @param accidentSegment Segment of the road where the accident is located
     * @param accidentPosition Position of the accident in the segment
     */
    public Accident(String accidentId, String vehicleId, String accidentSegment, int accidentPosition) {
        this.accidentId = accidentId;
        this.vehicleId = vehicleId;
        this.accidentSegment = accidentSegment;
        this.accidentPosition = accidentPosition;
        this.policeCar = dispatchVehicle(Vehicle.VehicleRole.Police);
        this.ambulance = dispatchVehicle(Vehicle.VehicleRole.Ambulance);
    }

    public String getId() {
        return accidentId;
    }

    public String getVehicle() {
        return vehicleId;
    }

    public String getSegment() {
        return accidentSegment;
    }

    public int getPosition() {
        return accidentPosition;
    }

    public SpecialVehicle getPoliceCar() {
        return this.policeCar;
    }

    public SpecialVehicle getAmbulance() {
        return this.ambulance;
    }

    private SpecialVehicle dispatchVehicle(Vehicle.VehicleRole role) {
        // Generate random plate of 4numbers and 3 letters
        String plate = String.format("%04d", (int)(Math.random() * 10000)) + (char)(Math.random() * 26 + 'A') + (char)(Math.random() * 26 + 'A') + (char)(Math.random() * 26 + 'A');
        RoadPoint start = new RoadPoint(this.accidentSegment, 0);
        SpecialVehicle vehicle = new SpecialVehicle(plate, role, 80, start, GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);

        Route route = new Route();
        // Route the vehicle to the accident location
        route.addRouteFragment(this.accidentSegment, 0, this.accidentPosition);
        // Route the vehicle to the back to the beginning of the road
        route.addRouteFragment(this.accidentSegment, this.accidentPosition, 0);
        try {
            vehicle.setRoute(route);
            vehicle.startRoute();
            vehicle.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return vehicle;
    }
}
