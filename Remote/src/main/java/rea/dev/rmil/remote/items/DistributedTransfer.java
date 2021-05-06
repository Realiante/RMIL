package rea.dev.rmil.remote.items;

public class DistributedTransfer<T> extends DistributedItem<T> {

    private T item;

    public DistributedTransfer(T item) {
        this.item = item;
    }

    public void override(T item) {
        this.item = item;
    }

    @Override
    public T get() {
        return item;
    }


}
