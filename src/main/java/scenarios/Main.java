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

// TODO: Change the certificates once the policies are updated
public class Main {
    public static void main(String[] args) throws Exception{
        // congestionScenario(10, "R5s1", 0, 580);
        // infoPanelTesting();
    }

    /**
     * Scenario where congestion is incremented and then decremented
     * At first, 30 vehicles are sent down the road, one by one every 1 second at an initial speed of 60km/h
     * When reaching the end of the road, the vehicles will stop
     * After all vehicles are created, they will exit the road one by one after they reach the end.
     */
    private static void congestionScenario(int numberOfCars, String roadSegment, int startingPoint, int endPoint) throws Exception {
        // Create manager
        RoadManager manager = new RoadManager("RM-1");
        manager.init();
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-"+roadSegment, roadSegment, (endPoint+startingPoint)/2, GlobalVars.AWS_ENDPOINT, GlobalVars.GLOBAL_CERT, GlobalVars.GLOBAL_KEY);
        infoPanel.init();

        Queue<Vehicle> vehicles = new LinkedList<>();

        // Increment congestion
        for(int i = 0; i < numberOfCars; i++) {
            // Format number in four digits
            String vehicleId = String.format("%04d", i) + "AAA";
            RoadPoint start = new RoadPoint(roadSegment, 0);
            Route route = new Route();
            route.addRouteFragment(roadSegment, startingPoint, endPoint);
            Vehicle vehicle = new Vehicle(vehicleId, Vehicle.VehicleRole.PrivateUsage, 60, start, GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
            vehicle.setRoute(route);
            vehicle.startRoute();
            vehicle.init();
            vehicles.add(vehicle);
            Thread.sleep(1000);
        }

        while(!vehicles.isEmpty()) {
            Vehicle vehicle = vehicles.poll();
            while(!vehicle.reachedDestination()) {
                Thread.sleep(100);
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
        // Create manager
        RoadManager manager = new RoadManager("RM-1");
        manager.init();
        // Create info panel
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

        // Test f2 function by simulating accident
        Vehicle vehicle = new Vehicle("1234AAA", Vehicle.VehicleRole.PrivateUsage, 100, new RoadPoint("R5s1", 0), GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
        Route route2 = new Route();
        route2.addRouteFragment("R5s1", 0, 580);
        vehicle.setRoute(route2);
        vehicle.startRoute();
        vehicle.init();
        Thread.sleep(5000);
        vehicle.notifyAccident();
        Thread.sleep(10000);
        vehicle.removeAccident();
        vehicle.exitRoad();
        vehicle.stop();
    }
}
