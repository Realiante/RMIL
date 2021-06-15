package dev.rea.rmil.client;

import java.util.UUID;

public interface DistributedItem<T> {

    T getItem();

    UUID getItemID();

    UUID getNodeID();

    void setNodeID(UUID id);

    int sortingValue();

}
