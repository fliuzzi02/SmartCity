package main.java.device;

import main.java.device.connections.AWSClient;
import main.java.device.connections.MQTTClient;
import main.java.utils.Logger;
import main.java.utils.MQTTMessage;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class represents a generic device
 */
public abstract class Device implements Runnable{
    protected String id;
    protected MQTTClient connection;
    protected AWSClient awsConnection;
    protected final Queue<MQTTMessage> pendingMessages = new LinkedList<>();
    protected boolean running = true;

    /**
     * Basic example constructor, it only connects to the server
     * @param id the device id
     */
    protected Device(String id) {
        this.id = id;
    }

    /**
     * @return the device id
     */
    public String getId() {
        return id;
    }

    /**
     * Connects the device to all its servers/brokers and performs all necessary actions to initialize the component, finally starts the thread
     */
    public abstract void init() throws MqttException;

    /**
     * This method is called when the thread is started, it loops indefinitely waiting for messages to be stored on the messages queue.
     * To manage them it will call the handleMessage method
     */
    @Override
    public void run() {
        while(this.running){
            synchronized (this.pendingMessages){
                while(this.pendingMessages.isEmpty()){
                    try {
                        this.pendingMessages.wait();
                    } catch (InterruptedException e) {
                        Logger.error(this.id, "An error occurred: " + e.getMessage());
                    }
                }
                handleMessage(this.pendingMessages.poll());
            }
        }
    }

    /**
     * This method is called by the thread where the device is running, it processes the message received from the queue of stored messages
     * @param message
     */
    protected abstract void handleMessage(MQTTMessage message);

    /**
     * Stops the current process
     */
    public void stop() {
        this.running = false;
    }

    protected void mqttConnect(String brokerAddress) throws MqttException {
        this.connection = new MQTTClient(this, brokerAddress);
    }

    protected void awsConnect(String clientEndpoint, String certificateFile, String privateKeyFile) {
        this.awsConnection = new AWSClient(this, clientEndpoint, certificateFile, privateKeyFile);
    }

    /**
     * This method is called when a message is received from any MQTT Broker
     * @param message the message received (containing the topic and the payload)
     */
    public void onMessage(MQTTMessage message) {
        synchronized (this.pendingMessages) {
            this.pendingMessages.add(message);
            this.pendingMessages.notifyAll();
        }
    }
}
