package main.java.scenarios;

import main.java.device.InfoPanel;
import main.java.device.RoadManager;
import main.java.device.vehicle.SpecialVehicle;
import main.java.device.vehicle.Vehicle;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;
import main.java.utils.GlobalVars;

import java.util.LinkedList;
import java.util.Queue;

public class Main {
    public static void main(String[] args) throws Exception{
        // congestionScenario();
        infoPanelTesting();
    }

    /**
     * Scenario where congestion is incremented and then decremented
     */
    private static void congestionScenario() throws Exception {
        // Create manager
        RoadManager manager = new RoadManager("RM-1");
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-R5s1", "R5s1", 290, GlobalVars.AWS_ENDPOINT, GlobalVars.IP_CERTIFICATE, GlobalVars.IP_KEY);

        Queue<Vehicle> vehicles = new LinkedList<>();

        manager.init();
        infoPanel.init();

        // Increment congestion
        for(int i = 0; i < 30; i++) {
            // Format number in four digits
            String vehicleId = String.format("%04d", i) + "AAA";
            RoadPoint start = new RoadPoint("R5s1", 0);
            Route route = new Route();
            route.addRouteFragment("R5s1", 0, 580);
            Vehicle vehicle = new Vehicle(vehicleId, Vehicle.VehicleRole.PrivateUsage, 100, start, GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
            vehicle.setRoute(route);
            vehicle.startRoute();
            vehicle.init();
            vehicles.add(vehicle);
            Thread.sleep(250);
        }

        while(!vehicles.isEmpty()) {
            Vehicle vehicle = vehicles.poll();
            while(!vehicle.reachedDestination()) {
                Thread.sleep(1000);
            }
            vehicle.exitRoad();
            vehicle.stop();
            Thread.sleep(1000);
        }
    }

    /**
     * Scenario where an ambulance is sent down the road and the info panel shows its passing by
     * @throws Exception
     */
    private static void infoPanelTesting() throws Exception {
        InfoPanel infoPanel = new InfoPanel("IP-R5s1", "R5s1", 290, GlobalVars.AWS_ENDPOINT, GlobalVars.GLOBAL_CERT, GlobalVars.GLOBAL_KEY);
        infoPanel.init();

        // Test f3 function by sending Ambulance
        SpecialVehicle ambulance = new SpecialVehicle("8924KNX", Vehicle.VehicleRole.Ambulance, 100, new RoadPoint("R5s1", 0), GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);

        Route route = new Route();
        route.addRouteFragment("R5s1", 0, 580);
        ambulance.setRoute(route);
        ambulance.startRoute();
        ambulance.init();
        while(!ambulance.reachedDestination()) {
            Thread.sleep(1000);
        }
        ambulance.exitRoad();
        ambulance.stop();
    }
}
