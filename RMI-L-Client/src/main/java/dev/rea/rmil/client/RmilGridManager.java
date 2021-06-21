package dev.rea.rmil.client;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public interface RmilGridManager {

    void setMaxLocalTasks(int max);

    <T> Function<T, DistributedItem<T>> mapToGrid();

    <T> Function<DistributedItem<T>, T> mapFromGrid();

    <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate);

    <T, R> Function<DistributedItem<T>, DistributedItem<R>> gridFunction(Function<T, R> function);

    <T> Function<DistributedItem<T>, DistributedItem<Integer>> gridToIntFunction(ToIntFunction<T> toIntFunction);

    <T> Function<DistributedItem<T>, DistributedItem<Double>> gridToDoubleFunction(ToDoubleFunction<T> toDoubleFunction);

    <T> Function<DistributedItem<T>, DistributedItem<Long>> gridToLongFunction(ToLongFunction<T> toFloatFunction);

    <T> BinaryOperator<DistributedItem<T>> gridBinaryOperator(BinaryOperator<T> biOperator);

    void setAwaitTimeout(long timeout, TimeUnit timeUnit);

    void setRetry(int tries);

    void addServers(Set<String> addresses);

}
