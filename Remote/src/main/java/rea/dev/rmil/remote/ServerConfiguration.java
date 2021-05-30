package rea.dev.rmil.remote;

import java.util.UUID;

public final class ServerConfiguration {

    private final UUID serverID;
    private final int maxThreads;
    private final Priority priority;

    public ServerConfiguration(UUID serverID, int maxThreads, Priority priority) {
        this.serverID = serverID;
        this.maxThreads = maxThreads;
        this.priority = priority;
    }

    public UUID getServerID() {
        return serverID;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public Priority getPriority() {
        return priority;
    }

    public enum Priority {
        NORMAL, LOW
    }
}
