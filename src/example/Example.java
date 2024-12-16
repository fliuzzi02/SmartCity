package example;

import utils.Logger;

public class Example {
    public static void main(String[] args) {
        Logger.info("Example", "Hello, World! Provided arguments: " + String.join(", ", args));
    }
}
