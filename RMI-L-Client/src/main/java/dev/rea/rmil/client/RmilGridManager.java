package dev.rea.rmil.client;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RmilGridManager {

    void setMaxLocalTasks(int max);

    <T> Function<T, DistributedItem<T>> mapToGrid();

    <T> Function<DistributedItem<T>, T> mapFromGrid();

    <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate);

    void setAwaitTimeout(long timeout, TimeUnit timeUnit);

    void setRetry(int tries);

}
