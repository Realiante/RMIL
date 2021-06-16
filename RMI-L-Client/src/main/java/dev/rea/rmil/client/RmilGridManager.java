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

    <T> ToIntFunction<DistributedItem<T>> gridToIntFunction(ToIntFunction<T> toIntFunction);

    <T> ToDoubleFunction<DistributedItem<T>> gridToDoubleFunction(ToDoubleFunction<T> toDoubleFunction);

    <T> ToLongFunction<DistributedItem<T>> gridToLongFunction(ToLongFunction<T> toFloatFunction);

    <T> BinaryOperator<DistributedItem<T>> gridBinaryOperator(BinaryOperator<T> biOperator);

    void setAwaitTimeout(long timeout, TimeUnit timeUnit);

    void setRetry(int tries);

    void addServers(Set<String> addresses);

}
