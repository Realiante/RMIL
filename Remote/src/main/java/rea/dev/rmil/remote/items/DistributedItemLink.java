package rea.dev.rmil.remote.items;

import rea.dev.rmil.remote.ItemProvider;

public class DistributedItemLink<T> extends DistributedItem<T> {

    private final ItemProvider<T> provider;

    public DistributedItemLink(ItemProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get() {
        return provider.get(getItemID()).orElseThrow();
    }

}
