package com.gcivil.complaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gcivil.kafka.topics")
public class KafkaTopicProperties {

    private String complaintCreated;
    private String complaintStatusChanged;
    private String complaintResponseRegistered;

    public String getComplaintCreated() {
        return complaintCreated;
    }

    public void setComplaintCreated(String complaintCreated) {
        this.complaintCreated = complaintCreated;
    }

    public String getComplaintStatusChanged() {
        return complaintStatusChanged;
    }

    public void setComplaintStatusChanged(String complaintStatusChanged) {
        this.complaintStatusChanged = complaintStatusChanged;
    }

    public String getComplaintResponseRegistered() {
        return complaintResponseRegistered;
    }

    public void setComplaintResponseRegistered(String complaintResponseRegistered) {
        this.complaintResponseRegistered = complaintResponseRegistered;
    }
}
