package dev.rea.rmil.client.items;

import dev.rea.rmil.client.DistributedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Objects;
import java.util.UUID;

public final class DistributedItemFuture<T> implements DistributedItem<T> {

    private static final Logger logger = LoggerFactory.getLogger(DistributedItemFuture.class);

    private final UUID itemID;
    private final ItemFetcher fetcher;
    private UUID nodeID;
    private T nativeItem;

    public DistributedItemFuture(UUID itemID, T item, ItemFetcher fetcher) {
        this(itemID, null, item, fetcher);
    }

    public DistributedItemFuture(UUID itemID, UUID nodeID, T item, ItemFetcher fetcher) {
        this.itemID = itemID;
        this.nodeID = nodeID;
        this.nativeItem = item;
        this.fetcher = fetcher;
    }

    public DistributedItemFuture(UUID itemID, UUID nodeID, ItemFetcher fetcher) {
        this(itemID, nodeID, null, fetcher);
    }

    @Override
    public T getItem() {
        if (nodeID == null) {
            return nativeItem;
        }
        try {
            return fetcher.getItem(nodeID, itemID);
        } catch (RemoteException e) {
            logger.error("Remote exception while attempting to retrieve an item", e);
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
