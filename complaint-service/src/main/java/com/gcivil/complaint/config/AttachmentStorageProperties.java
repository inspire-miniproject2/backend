package com.gcivil.complaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gcivil.attachment.storage")
public class AttachmentStorageProperties {

    private String type = "local";
    private String localRoot = System.getProperty("java.io.tmpdir") + "/minwonon-attachments";
    private String awsRegion = "ap-northeast-2";
    private String s3Bucket;
    private String s3Prefix = "complaints/";

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

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3Prefix() {
        return s3Prefix;
    }

    public void setS3Prefix(String s3Prefix) {
        this.s3Prefix = s3Prefix;
    }

    public String buildStoredPath(String complaintNo, String storedFilename) {
        String normalizedPrefix = normalizePrefix();
        return normalizedPrefix + complaintNo + "/" + storedFilename;
    }

    private String normalizePrefix() {
        String prefix = s3Prefix;
        if (prefix == null || prefix.isBlank()) {
            prefix = "complaints/";
        }
        prefix = prefix.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }
}
