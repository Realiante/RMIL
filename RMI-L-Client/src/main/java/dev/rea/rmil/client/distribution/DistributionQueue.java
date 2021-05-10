package dev.rea.rmil.client.distribution;

import rea.dev.rmil.remote.items.DistributedItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class DistributionQueue<T> {

    private final Queue<DistributedItem<? super T>> inactiveQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, DistributedItem<? super T>> activeMap = new HashMap<>();
    private UUID functionID;

    protected DistributionQueue() {

    }

    public synchronized void put(DistributedItem<T> item) {
        onPut(item.getItemID());
        inactiveQueue.add(item);
    }

    public synchronized boolean hasNext() {
        if (inactiveQueue.isEmpty()) {
            return !activeMap.isEmpty();
        }
        return true;
    }

    public synchronized DistributedItem<? super T> getNext() {
        DistributedItem<? super T> item;
        if (inactiveQueue.isEmpty()) {
            var itemOpt = activeMap.values().stream().findAny();
            // should not throw anything since we check if an item is present with has next before getting a new item
            // this could be a source of unexpected behaviour, so an eye should be kept on it
            //todo: check for possible corner cases?
            item = itemOpt.orElseThrow();
        } else {
            item = inactiveQueue.poll();
            activeMap.put(item.getItemID(), item);
        }
        onGet(item.getItemID());
        return item;
    }

    public synchronized boolean removeCompleted(UUID itemID) {
        onRemove(itemID);
        return activeMap.remove(itemID) != null;
    }

    protected abstract void onGet(UUID itemID);

    protected abstract void onPut(UUID itemID);

    protected abstract void onRemove(UUID itemID);

    public UUID getFunctionID() {
        return functionID;
    }

    protected void setFunctionID(UUID functionID) {
        this.functionID = functionID;
    }
}
