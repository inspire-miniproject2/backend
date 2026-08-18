package com.gcivil.complaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gcivil.attachment.storage")
public class AttachmentStorageProperties {

    private String type = "local";
    private String localRoot = System.getProperty("java.io.tmpdir") + "/minwonon-attachments";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }
}
