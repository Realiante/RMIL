package dev.rea.rmil.container;

import dev.rea.rmil.container.remote.FunctionEngine;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class RmilContainer {

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        var address = "localhost";
        var name = String.format("//%s/engine",address);
        try {
            //todo: port and socket factories
            FunctionEngine engine = new FunctionEngine();
            Naming.rebind(name, engine);
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }

    }

}
