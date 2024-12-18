package main.java.example;

import main.java.utils.Logger;

public class Example {
    public static void main(String[] args) {
        Logger.info("Example", "Hello, World! Provided arguments: " + String.join(", ", args));
    }
}
