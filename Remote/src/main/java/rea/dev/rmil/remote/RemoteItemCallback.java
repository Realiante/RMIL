package rea.dev.rmil.remote;

import rea.dev.rmil.remote.items.DistributedItem;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.Optional;
import java.util.UUID;

public interface RemoteItemCallback extends Serializable, Remote {

    <T> DistributedItem<? super T> loadItem(UUID functionID, Class<? super T> classType);

    <T> DistributedItem<? super T> requestNewItem(UUID functionID, DistributedItem<? super T> returnValue);

    Optional<FunctionPackage> register();

}
