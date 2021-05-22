package dev.rea.rmil.client;

import rea.dev.rmil.remote.items.DistributedItem;

import java.util.function.Predicate;

public interface RmilGridManager {

    void setMaxLocalTasks(int max);

    <T> Predicate<? super T> filterTask(Predicate<? super T> predicate);

    <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate);


}
