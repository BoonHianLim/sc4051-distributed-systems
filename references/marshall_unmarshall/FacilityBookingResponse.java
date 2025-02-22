import java.io.Serializable;

public class FacilityBookingResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;

    public FacilityBookingResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public String toString() {
        return "Success: " + success + ", Message: " + message;
    }
}

