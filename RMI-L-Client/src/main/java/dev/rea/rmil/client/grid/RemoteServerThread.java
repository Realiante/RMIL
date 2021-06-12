package dev.rea.rmil.client.grid;

import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.ServerConfiguration;

import java.util.Objects;
import java.util.UUID;

class RemoteServerThread implements RemoteThread {

    private final RemoteServer parent;

    public RemoteServerThread(RemoteServer parent) {
        this.parent = parent;
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
        return Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }
}
