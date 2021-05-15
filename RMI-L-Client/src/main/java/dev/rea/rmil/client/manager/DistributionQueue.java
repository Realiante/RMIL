package dev.rea.rmil.client.manager;

import rea.dev.rmil.remote.items.DistributedItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DistributionQueue<T> {

    private final Queue<DistributedItem<? super T>> inactiveQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, DistributedItem<? super T>> activeMap = new HashMap<>();

    public DistributionQueue() {
        //empty
    }

    public synchronized void put(DistributedItem<T> item) {
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
        return item;
    }

    public synchronized boolean removeCompleted(UUID itemID) {
        return activeMap.remove(itemID) != null;
    }
}
