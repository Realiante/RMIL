package dev.rea.rmil.client.items;

import dev.rea.rmil.client.DistributedItem;

import java.util.Objects;
import java.util.UUID;

public final class DistributedItemLocal<T> implements DistributedItem<T> {

    private final UUID itemID;
    private final T item;

    public DistributedItemLocal(UUID itemID, T item) {
        this.itemID = itemID;
        this.item = item;
    }

    @Override
    public T getItem() {
        return item;
    }

    @Override
    public UUID getItemID() {
        return itemID;
    }

    @Override
    public UUID getNodeID() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributedItemLocal<?> that = (DistributedItemLocal<?>) o;
        return itemID.equals(that.itemID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemID);
    }

    @Override
    public String toString() {
        return "Local Item{" + itemID +
                ", item=" + item +
                '}';
    }
}
