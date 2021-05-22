package rea.dev.rmil.remote.items;

import rea.dev.rmil.remote.ItemOrchestrator;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class DistributedItem<T>{

    private final UUID itemID;
    private final Future<T> itemFuture;

    DistributedItem(UUID itemID, UUID nodeID, ItemOrchestrator orchestrator) {
        this.itemID = itemID;
        this.itemFuture =  new FutureTask<>(() -> orchestrator.getItem(nodeID, itemID));
    }

    public T get() {
        try {
            return itemFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public UUID getItemID() {
        return itemID;
    }


}
