package main.java.device;

public class Accident {
    String accidentId;
    String accidentSegment;
    int accidentPosition;

    public Accident(String accidentId, String accidentSegment, int accidentPosition) {
        this.accidentId = accidentId;
        this.accidentSegment = accidentSegment;
        this.accidentPosition = accidentPosition;
    }

    public String getId() {
        return accidentId;
    }

    public String getSegment() {
        return accidentSegment;
    }

    public int getPosition() {
        return accidentPosition;
    }
}
