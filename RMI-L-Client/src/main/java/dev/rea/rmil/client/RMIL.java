package dev.rea.rmil.client;

import dev.rea.rmil.client.grid.GridBuilder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public final class RMIL {

    private static final RmilGridManager gridManager = GridBuilder.buildGrid();

    private RMIL() {
        //static API class
    }

    public static void setMaxLocalTasks(int maxLocalTasks) {
        gridManager.setMaxLocalTasks(maxLocalTasks);
    }

    public static <T> Function<T, DistributedItem<T>> mapToGrid() {
        return gridManager.mapToGrid();
    }

    public static <T> Function<DistributedItem<T>, T> mapFromGrid() {
        return gridManager.mapFromGrid();
    }

    public static <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate) {
        return gridManager.gridPredicate(predicate);
    }

    public static <T, R> Function<DistributedItem<T>, DistributedItem<R>> gridFunction(Function<T, R> function) {
        return gridManager.gridFunction(function);
    }

    public static <T> ToIntFunction<DistributedItem<T>> gridToIntFunction(ToIntFunction<T> toIntFunction) {
        return gridManager.gridToIntFunction(toIntFunction);
    }

    public static <T> ToDoubleFunction<DistributedItem<T>> gridToDoubleFunction(ToDoubleFunction<T> toDoubleFunction) {
        return gridManager.gridToDoubleFunction(toDoubleFunction);
    }

    public static <T> ToLongFunction<DistributedItem<T>> gridToLongFunction(ToLongFunction<T> toFloatFunction) {
        return gridManager.gridToLongFunction(toFloatFunction);
    }

    public static <T> BinaryOperator<DistributedItem<T>> gridBinaryOperator(BinaryOperator<T> biOperator) {
        return gridManager.gridBinaryOperator(biOperator);
    }

    @SuppressWarnings("java:S1905")
    public static <T> Comparator<DistributedItem<T>> sortLocalFirst() {
        return ((Comparator<DistributedItem<T>>) (o1, o2) -> o1.sortingValue() - o2.sortingValue()).reversed();
    }

    public static <T> Comparator<DistributedItem<T>> sortRemoteFirst() {
        return Comparator.comparingInt(DistributedItem::sortingValue);
    }

    public static void setAwaitTimeout(long timeout, TimeUnit timeUnit) {
        gridManager.setAwaitTimeout(timeout, timeUnit);
    }

    public static void setRetry(int tries) {
        gridManager.setRetry(tries);
    }

    public static void addServer(String address) {
        Set<String> singleton = new HashSet<>();
        singleton.add(address);
        gridManager.addServers(singleton);
    }

    public static void addServers(Set<String> addresses) {
        gridManager.addServers(addresses);
    }

}
