package gtfsparsingzip.data;

public class CalendarDate {
    private String serviceId;
    private String date;
    private int exceptionType;

    public CalendarDate(String serviceId, String date, int exceptionType) {
        this.serviceId = serviceId;
        this.date = date;
        this.exceptionType = exceptionType;
    }

    public String getServiceId() { return serviceId; }
    public String getDate() { return date; }
    public int getExceptionType() { return exceptionType; }
}
