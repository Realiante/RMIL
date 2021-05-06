package dev.rea.rmil.client.manager;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteServer {

    private final String address;
    private final AtomicInteger maxTasks;

    public RemoteServer(String address, int maxTasks) {
        this.address = address;
        this.maxTasks = new AtomicInteger(maxTasks);
    }

    public String getAddress() {
        return address;
    }

    public AtomicInteger getMaxTasks() {
        return maxTasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteServer that = (RemoteServer) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
