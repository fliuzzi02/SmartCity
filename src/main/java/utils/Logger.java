package main.java.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Logger class for logging messages with different levels of severity.
 */
public class Logger {
	// TODO: Add color to messages
	
	public static final int sender_spacing = 17;
	
	/**
	 * Method for logging info messages.
	 * @param sender - the name of the class or object that calls the method
	 * @param msg - the message to be logged
	 */
	public static void info(String sender, String msg) {
		System.out.println(putCurrentTimestamp() + " INFO  [" + formatWithSpaces(sender,sender_spacing) + "] " + msg);
	}

	/**
	 * Method for logging debug messages.
	 * @param sender
	 * @param msg
	 */
	public static void debug(String sender, String msg) {
		System.out.println(putCurrentTimestamp() + " DEBUG [" + formatWithSpaces(sender,sender_spacing) + "] " + msg);
	}

	/**
	 * Method for logging warning messages.
	 * @param sender - the name of the class or object that calls the method
	 * @param msg - the message to be logged
	 */
	public static void warn(String sender, String msg) {
		System.out.println(putCurrentTimestamp() + " WARN  [" + formatWithSpaces(sender,sender_spacing) + "] " + msg);
	}

	/**
	 * Method for logging error messages.
	 * @param sender - the name of the class or object that calls the method
	 * @param msg - the message to be logged
	 */
	public static void error(String sender, String msg) {
		System.out.println(putCurrentTimestamp() + " ERROR [" + formatWithSpaces(sender,sender_spacing) + "] " + msg);
	}

	/**
	 * Method for logging trace messages.
	 * @param sender - the name of the class or object that calls the method
	 * @param msg - the message to be logged
	 */
	public static void trace(String sender, String msg) {
		System.out.println(putCurrentTimestamp() + " TRACE [" + formatWithSpaces(sender,sender_spacing) + "] " + msg);
	}

	protected static String formatWithSpaces(String msg, int size) {
		if ( size <= msg.length() )
			return msg;

		int n = size - msg.length();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < n; i++) builder.append(" ");
		builder.append(msg);

		return builder.toString();
	}
	
	protected static String putCurrentTimestamp() {
		return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
	}
}
