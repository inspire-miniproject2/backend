package com.gcivil.complaint.event;

public final class ComplaintEventTypes {
    public static final String VERSION = "v1";
    public static final String PRODUCER = "complaint-service";
    public static final String COMPLAINT_CREATED = "ComplaintCreated";
    public static final String COMPLAINT_STATUS_CHANGED = "ComplaintStatusChanged";
    public static final String COMPLAINT_RESPONSE_REGISTERED = "ComplaintResponseRegistered";

    private ComplaintEventTypes() {
    }
}
