package dev.rea.rmil.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class RmilServer {

    private static RmilServer instance;

    private final Server jettyServer;

    private RmilServer(int port) {
        this.jettyServer = new Server();
        var connector = new ServerConnector(jettyServer);
        connector.setPort(port);
        jettyServer.setConnectors(new Connector[]{connector});
    }

    public static RmilServer start(int port) {
        if (instance != null) {
            instance.destroy();
        }
        instance = new RmilServer(port);
        return instance;
    }

    public static RmilServer getInstance() {
        return instance;
    }

    public void destroy() {
        //todo: close the server and related resources
    }
}
