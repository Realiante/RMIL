package dev.rea.rmil.client;

public final class RmilConfig {

    public final int port; // default: 51199
    private int queueCheckPause; //default: 2 ms
    private int sendFunctionTries; //default: 2 tries
    private int maxLocalTasks; //default: available processors count
    private boolean forceCleanup; //default: false
    private boolean recheckAvailableProcessors; //default: true

    public RmilConfig(int port) {
        this.port = port;
        this.queueCheckPause = 2;
        this.sendFunctionTries = 2;
        this.maxLocalTasks = Runtime.getRuntime().availableProcessors();
        this.forceCleanup = false;
        this.recheckAvailableProcessors = true;
    }

    public RmilConfig() {
        this(51199);
    }

    public int getQueueCheckPause() {
        return queueCheckPause;
    }

    public void setQueueCheckPause(int queueCheckPause) {
        this.queueCheckPause = queueCheckPause;
    }

    public int getSendFunctionTries() {
        return sendFunctionTries;
    }

    public void setSendFunctionTries(int sendFunctionTries) {
        this.sendFunctionTries = sendFunctionTries;
    }

    public boolean isForceCleanup() {
        return forceCleanup;
    }

    public void setForceCleanup(boolean forceCleanup) {
        this.forceCleanup = forceCleanup;
    }

    public int getMaxLocalTasks() {
        return maxLocalTasks;
    }

    public void setMaxLocalTasks(int maxLocalTasks) {
        this.maxLocalTasks = maxLocalTasks;
    }

    public boolean isRecheckAvailableProcessors() {
        return recheckAvailableProcessors;
    }

    public void setRecheckAvailableProcessors(boolean recheckAvailableProcessors) {
        this.recheckAvailableProcessors = recheckAvailableProcessors;
    }


}
