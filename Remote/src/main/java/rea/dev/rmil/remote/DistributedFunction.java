package rea.dev.rmil.remote;

import java.rmi.Remote;
import java.util.UUID;

public interface DistributedFunction extends Remote {

    <T> DistributedItem<T> applyFunction(UUID fID, DistributedItem<T> item);

    <T, R> DistributedItem<T> applyBiFunction(UUID fID, DistributedItem<T> returnTypeItem, DistributedItem<R> modifierItem);
}
