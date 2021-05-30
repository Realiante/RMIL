package dev.rea.rmil.client.grid;

import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.ServerConfiguration;

import java.util.Objects;
import java.util.UUID;

class RemoteServerThread implements RemoteThread {

    private final RemoteServer parent;
    private final int num;

    public RemoteServerThread(RemoteServer parent, int num) {
        this.parent = parent;
        this.num = num;
    }

    @Override
    public String getAddress() {
        return parent.getAddress();
    }

    @Override
    public UUID getID() {
        return parent.getID();
    }

    @Override
    public RemoteEngine getExecutorContainer() {
        return parent.getExecutorContainer();
    }

    @Override
    public ServerConfiguration.Priority getPriority() {
        return parent.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteServerThread)) return false;
        RemoteServerThread that = (RemoteServerThread) o;
        return num == that.num && parent.equals(that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, num);
    }
}
