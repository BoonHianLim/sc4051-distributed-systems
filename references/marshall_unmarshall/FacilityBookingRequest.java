import java.io.Serializable;

public class FacilityBookingRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String user;
    private String facilityName;
    private String bookingTime;

    public FacilityBookingRequest(String user, String facilityName, String bookingTime) {
        this.user = user;
        this.facilityName = facilityName;
        this.bookingTime = bookingTime;
    }

    public String toString() {
        return "User: " + user + ", Facility: " + facilityName + ", Time: " + bookingTime;
    }
}
