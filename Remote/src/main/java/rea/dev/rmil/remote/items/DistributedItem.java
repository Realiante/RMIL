package rea.dev.rmil.remote.items;

import java.io.Serializable;
import java.util.UUID;

public abstract class DistributedItem<T> implements Serializable {

    private static final long serialVersionUID = 13534L;
    private final UUID itemID;

    DistributedItem() {
        this.itemID = UUID.randomUUID();
    }

    public abstract T get();

    public UUID getItemID() {
        return itemID;
    }
}
