package dev.rea.rmil.client.grid;

import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.ServerConfiguration;

import java.util.UUID;

public interface RemoteThread {

    String getAddress();

    UUID getID();

    RemoteEngine getExecutorContainer();

    ServerConfiguration.Priority getPriority();

}
