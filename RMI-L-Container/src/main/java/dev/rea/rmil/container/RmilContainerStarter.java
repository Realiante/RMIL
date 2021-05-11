package dev.rea.rmil.container;

import dev.rea.rmil.container.remote.FunctionEngine;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class RmilContainerStarter {

    private static int maxTasks;

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Expected 2-3 arguments (address(String), port(Int), max tasks(optional Int)) but received " + args.length);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        var address = args[0];
        var port = args[1];
        var name = String.format("//%s:%s/engine", address, port);
        maxTasks = Runtime.getRuntime().availableProcessors();
        if (args.length > 2)
            maxTasks = Integer.parseInt(args[2]);

        try {
            //todo: port and socket factories
            FunctionEngine engine = new FunctionEngine(maxTasks);
            Naming.rebind(name, engine);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

}
