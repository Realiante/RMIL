package dev.rea.rmil.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class RmilHttpsServer {

    private static RmilHttpsServer instance;

    private final Server jettyServer;

    //todo: switch to ssl (https)
    private RmilHttpsServer(int port) {
        //todo: read from properties?
        var maxThreads = 12;
        var minThreads = 1;
        var idleTimeout = 300;
        var threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        this.jettyServer = new Server(threadPool);
        var connector = new ServerConnector(jettyServer);
        connector.setPort(port);
        jettyServer.setConnectors(new Connector[]{connector});
    }

    public static RmilHttpsServer start(int port) {
        if (instance != null) {
            instance.destroy();
        }
        instance = new RmilHttpsServer(port);
        return instance;
    }

    public static RmilHttpsServer getInstance() {
        return instance;
    }

    public void destroy() {
        //todo: close the server and related resources
    }
}

//todo: add server functionality!!
