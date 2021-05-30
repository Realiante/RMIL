package dev.rea.rmil.client.items;

import dev.rea.rmil.client.DistributedItem;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class DistributedItemFuture<T> implements DistributedItem<T> {

    private final UUID itemID;
    private final UUID nodeID;
    private final Future<T> itemFuture;

    public DistributedItemFuture(UUID itemID, UUID nodeID, ItemFetcher fetcher) {
        this.itemID = itemID;
        this.nodeID = nodeID;
        this.itemFuture = new FutureTask<>(() -> fetcher.getItem(nodeID, itemID));
    }

    @Override
    public T getItem() {
        try {
            return itemFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public UUID getItemID() {
        return itemID;
    }

    @Override
    public UUID getNodeID() {
        return nodeID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributedItemFuture<?> that = (DistributedItemFuture<?>) o;
        return itemID.equals(that.itemID) && Objects.equals(nodeID, that.nodeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemID, nodeID);
    }

    @Override
    public String toString() {
        return "Remote Item{" +
                "itemID=" + itemID +
                ", nodeID=" + nodeID +
                '}';
    }
}
