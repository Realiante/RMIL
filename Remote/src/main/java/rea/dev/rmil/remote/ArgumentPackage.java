package rea.dev.rmil.remote;

import java.util.Objects;
import java.util.UUID;

public final class ArgumentPackage<T> {

    private final T argument;
    private final UUID itemID;

    public ArgumentPackage(T argument, UUID itemID) {
        this.argument = argument;
        this.itemID = itemID;
    }

    public T getArgument() {
        return argument;
    }

    public UUID getItemID() {
        return itemID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgumentPackage<?> that = (ArgumentPackage<?>) o;
        return itemID.equals(that.itemID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemID);
    }
}
