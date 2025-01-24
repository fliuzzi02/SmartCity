package main.java.scenarios;

import main.java.device.AccidentManager;
import main.java.device.InfoPanel;
import main.java.device.RoadManager;
import main.java.device.vehicle.SpecialVehicle;
import main.java.device.vehicle.Vehicle;
import main.java.device.vehicle.navigation.components.RoadPoint;
import main.java.device.vehicle.navigation.components.Route;
import main.java.utils.GlobalVars;
import main.java.utils.Logger;

import java.util.LinkedList;
import java.util.Queue;

public class Main {
    public static void main(String[] args) throws Exception{
        if (args.length == 0) {
            printUsage();
            return;
        }

        switch (args[0]) {
            case "--congestion":
                if (args.length != 5) {
                    printUsage();
                    return;
                }
                int numberOfCars = Integer.parseInt(args[1]);
                String roadSegment = args[2];
                int startingPoint = Integer.parseInt(args[3]);
                int endPoint = Integer.parseInt(args[4]);
                congestionScenario(numberOfCars, roadSegment, startingPoint, endPoint);
                break;
            case "--infoPanelTesting":
                infoPanelTesting();
                break;
            case "--accident":
                accidentScenario();
                break;
            case "--createManagers":
                managersCreator();
                break;
            case "--createInfoPanel":
                if (args.length != 3) {
                    printUsage();
                    return;
                }
                String panelRoadSegment = args[1];
                int position = Integer.parseInt(args[2]);
                infoPanelCreator(panelRoadSegment, position);
                break;
            case "--createVehicle":
                if (args.length != 5) {
                    printUsage();
                    return;
                }
                String vehicleId = args[1];
                Vehicle.VehicleRole role = Vehicle.VehicleRole.valueOf(args[2]);
                int speed = Integer.parseInt(args[3]);
                String codedPath = args[4];
                vehicleCreator(vehicleId, role, speed, codedPath);
                break;
            default:
                printUsage();
        }
    }

    /**
     * Scenario where congestion is incremented and then decremented
     * At first, 30 vehicles are sent down the road, one by one every 1 second at an initial speed of 60km/h
     * When reaching the end of the road, the vehicles will stop
     * After all vehicles are created, they will exit the road one by one after they reach the end.
     */
    private static void congestionScenario(int numberOfCars, String roadSegment, int startingPoint, int endPoint) throws Exception {
        Logger.warn("Main", "Starting congestion scenario with " + numberOfCars + " vehicles");
        // Create managers
        managersCreator();
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-"+roadSegment, roadSegment, (endPoint+startingPoint)/2, GlobalVars.AWS_ENDPOINT, GlobalVars.IP_CERTIFICATE, GlobalVars.IP_KEY);
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

        Logger.warn("Main", "All vehicles have been created, slowly exiting the road");
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
     * Scenario where an ambulance is sent down the road and the info panel shows it's passing by
     * @throws Exception
     */
    private static void infoPanelTesting() throws Exception {
        // Create managers
        managersCreator();
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-R5s1", "R5s1", 290, GlobalVars.AWS_ENDPOINT, GlobalVars.IP_CERTIFICATE, GlobalVars.IP_KEY);
        infoPanel.init();

        // Test f3 function by sending Ambulance
        Logger.warn("Main", "Starting scenario where an ambulance is sent down the road and the info panel shows its passing by");
        SpecialVehicle ambulance = new SpecialVehicle("8924KNX", Vehicle.VehicleRole.Ambulance, 100, new RoadPoint("R5s1", 0), GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);

        Route route = new Route();
        route.addRouteFragment("R5s1", 0, 580);
        ambulance.setRoute(route);
        ambulance.startRoute();
        ambulance.init();
        while (!ambulance.reachedDestination()) {
            Thread.sleep(1000);
        }
        ambulance.exitRoad();
        ambulance.stop();
        Logger.warn("Main", "Ambulance has exited the road, scenario finished");
    }

    /**
     * Scenario where a car has an accident and an ambulance is sent to the location
     * @throws Exception
     */
    private static void accidentScenario() throws Exception {
        // Create managers
        managersCreator();
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-R5s1", "R5s1", 290, GlobalVars.AWS_ENDPOINT, GlobalVars.IP_CERTIFICATE, GlobalVars.IP_KEY);
        infoPanel.init();

        // Test f2 function by simulating accident
        Logger.warn("Main", "Starting scenario where a car has an accident and an ambulance is sent to the location");
        Vehicle vehicle = new Vehicle("1234AAA", Vehicle.VehicleRole.PrivateUsage, 50, new RoadPoint("R5s1", 0), GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
        Route route2 = new Route();
        route2.addRouteFragment("R5s1", 0, 580);
        vehicle.setRoute(route2);
        vehicle.startRoute();
        vehicle.init();
        while(290 - (vehicle.getCurrentPosition().getPosition()) > 50)
            Thread.sleep(1000);
        vehicle.initiateAccident();
        Thread.sleep(20000);
        vehicle.exitRoad();
        vehicle.stop();
    }

    /**
     * Create a road manager and an accident manager
     * @throws Exception if the managers cannot be created
     */
    private static void managersCreator() throws Exception {
        // Create road manager
        RoadManager manager = new RoadManager("RM-1", GlobalVars.AWS_ENDPOINT, GlobalVars.MA_CERTIFICATE, GlobalVars.MA_KEY);
        manager.init();
        // Create accident manager
        AccidentManager accidentManager = new AccidentManager("AM-1", GlobalVars.AWS_ENDPOINT, GlobalVars.MA_CERTIFICATE, GlobalVars.MA_KEY);
        accidentManager.init();
    }

    /**
     * Create an info panel placed at the given position
     * @param roadSegment the name of the road segment the info panel is placed on
     * @param position the position of the info panel
     * @throws Exception if the info panel cannot be created
     */
    private static void infoPanelCreator(String roadSegment, int position) throws Exception {
        // Create info panel
        InfoPanel infoPanel = new InfoPanel("IP-"+roadSegment, roadSegment, position, GlobalVars.AWS_ENDPOINT, GlobalVars.IP_CERTIFICATE, GlobalVars.IP_KEY);
        infoPanel.init();
    }

    /**
     * Create a vehicle with the given parameters
     * @param vehicleId the plate of the vehicle
     * @param role the role of the vehicle
     * @param speed the speed of the vehicle
     * @param codedPath the path the vehicle will take, it has to be coded in the following way: R5s1,0,580; or R5s1,0,580;R5s2,0,580;... etc
     * @throws Exception if the vehicle cannot be created
     */
    private static void vehicleCreator(String vehicleId, Vehicle.VehicleRole role, int speed, String codedPath) throws Exception {
        // The coded path is something like R5s1,0,580; or R5s1,0,580;R5s2,0,580;... etc
        String[] pathFragments = codedPath.split(";");
        Route route = new Route();
        for (String fragment : pathFragments) {
            String[] fragmentParts = fragment.split(",");
            route.addRouteFragment(fragmentParts[0], Integer.parseInt(fragmentParts[1]), Integer.parseInt(fragmentParts[2]));
        }
        Vehicle vehicle = new Vehicle(vehicleId, role, speed, new RoadPoint(pathFragments[0].split(",")[0], 0), GlobalVars.AWS_ENDPOINT, GlobalVars.VE_CERTIFICATE, GlobalVars.VE_KEY);
        vehicle.setRoute(route);
        vehicle.startRoute();
        vehicle.init();
        while (!vehicle.reachedDestination()) {
            Thread.sleep(1000);
        }
        vehicle.exitRoad();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("\tTo run the various scenarios:");
        System.out.println("\t\t--congestion <numberOfCars> <roadSegment> <startingPoint> <endPoint>");
        System.out.println("\t\t--infoPanelTesting");
        System.out.println("\t\t--accident");
        System.out.println("\t To create various components:");
        System.out.println("\t\t--createManagers");
        System.out.println("\t\t--createInfoPanel <roadSegment> <position>");
        System.out.println("\t\t--createVehicle <vehicleId> <role> <speed> <codedPath>");
        System.out.println("\tNotes:");
        System.out.println("\t\t- The codedPath for the vehicle has to be in the following format: R5s1,0,580; or R5s1,0,580;R5s2,0,580;... etc");
        System.out.println("\t\t- The role of the vehicle has to be one of the following: PrivateUsage, Bus, Police, Taxi, Ambulance");
        System.out.println("\t\t- There is no need to use apices to write the arguments, just write them separated by spaces in the correct order");
    }
}
