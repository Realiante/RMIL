package dev.rea.rmil.client.items;

import java.rmi.RemoteException;
import java.util.UUID;

public interface ItemFetcher {

    <R> R getItem(UUID nodeID, UUID itemID) throws RemoteException;

}
