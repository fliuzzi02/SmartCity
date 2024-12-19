package main.java.device.vehicle.navigation.interfaces;

import java.util.UUID;

public interface IIdentifiable { 
	
	String ID = "id";
	
	String getId();
	
	static String getFreshId() {
		return UUID.randomUUID().toString().replace("-", ""); 
	}

}
