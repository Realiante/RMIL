package rea.dev.rmil.remote.items;

//stub class, meant to notify the container that function was completed
public final class EndServiceItem<T> extends DistributedItem<T> {

    @Override
    public T get() {
        return null;
    }
}
