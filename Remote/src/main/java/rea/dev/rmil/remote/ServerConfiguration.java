package rea.dev.rmil.remote;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class ServerConfiguration implements Serializable {
    static final long serialVersionUID = 2323415151251L;

    private final UUID serverID;
    private final int maxThreads;
    private final Priority priority;

    public ServerConfiguration(UUID serverID, int maxThreads, Priority priority) {
        this.serverID = Objects.requireNonNull(serverID);
        this.maxThreads = maxThreads;
        this.priority = Objects.requireNonNull(priority);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerConfiguration)) return false;
        ServerConfiguration that = (ServerConfiguration) o;
        return maxThreads == that.maxThreads && serverID.equals(that.serverID) && priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverID, maxThreads, priority);
    }

    public enum Priority implements Serializable {
        NORMAL, LOW;
        static final long serialVersionUID = 2323415151251886L;
    }
}
