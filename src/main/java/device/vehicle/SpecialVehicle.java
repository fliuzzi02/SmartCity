package main.java.device.vehicle;

import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.utils.Logger;

public class SpecialVehicle extends Vehicle{
    SpecialVehicle(String id, VehicleRole role, int initialSpeed, RoadPoint initialPosition, String clientEndpoint, String certificateFile, String privateKeyFile) {
        super(id, role, initialSpeed, initialPosition, clientEndpoint, certificateFile, privateKeyFile);
    }

    @Override
    void updateSpeed() {
        this.actualSpeed = this.cruiseSpeed;
        Logger.info(this.id, "Special vehicle speed updated to " + this.actualSpeed + " km/h");
    }
}
