package edu.upb.desktop.model;

public class BackendStatusModel {
    private final String backendUrl;
    private final String status;
    private final boolean inBalancing;
    private final boolean inVerification;
    private final int failCount;
    private final String lastError;
    private final String databaseStatus;
    private final String diskStatus;
    private final String lastCheck;

    public BackendStatusModel(String backendUrl, String status, boolean inBalancing, boolean inVerification,
            int failCount, String lastError, String databaseStatus, String diskStatus, String lastCheck) {
        this.backendUrl = backendUrl;
        this.status = status;
        this.inBalancing = inBalancing;
        this.inVerification = inVerification;
        this.failCount = failCount;
        this.lastError = lastError;
        this.databaseStatus = databaseStatus;
        this.diskStatus = diskStatus;
        this.lastCheck = lastCheck;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getStatus() {
        return status;
    }

    public boolean isInBalancing() {
        return inBalancing;
    }

    public boolean isInVerification() {
        return inVerification;
    }

    public int getFailCount() {
        return failCount;
    }

    public String getLastError() {
        return lastError;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public String getDiskStatus() {
        return diskStatus;
    }

    public String getLastCheck() {
        return lastCheck;
    }
}
