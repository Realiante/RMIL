package dev.rea.rmil.client.items;

import dev.rea.rmil.client.DistributedItem;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class DistributedItemFuture<T> implements DistributedItem<T> {

    private final UUID itemID;
    private final Future<T> itemFuture;
    private UUID nodeID;
    private T nativeItem;

    public DistributedItemFuture(UUID itemID, T item, ItemFetcher fetcher) {
        this(itemID, null, item, fetcher);
    }

    public DistributedItemFuture(UUID itemID, UUID nodeID, T item, ItemFetcher fetcher) {
        this.itemID = itemID;
        this.nodeID = nodeID;
        this.itemFuture = new FutureTask<>(() -> fetcher.getItem(nodeID, itemID));
        this.nativeItem = item;
    }

    @Override
    public T getItem() {
        if (nodeID == null) {
            return nativeItem;
        }

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
    public void setNodeID(UUID nodeID) {
        if (this.nodeID == null && nodeID != null) {
            //this native item is no longer the latest version of the item
            nativeItem = null;
            this.nodeID = nodeID;
        } else if (this.nodeID != null && nodeID != null) {
            this.nodeID = nodeID;
        } else if (this.nodeID != null) {
            throw new IllegalArgumentException("Attempted to pass a null nodeID to a remote distributed future");
        }
    }

    @Override
    public int sortingValue() {
        if (nodeID == null) {
            return 0;
        } else {
            return 1;
        }
    }

    public void updateItem(T localItem) {
        if (localItem == null) {
            throw new NullPointerException();
        }
        this.nativeItem = localItem;
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
