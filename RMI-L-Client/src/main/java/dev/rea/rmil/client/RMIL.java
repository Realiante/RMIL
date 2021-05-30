package dev.rea.rmil.client;

import dev.rea.rmil.client.grid.GridBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

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

    public static void setAwaitTimeout(long timeout, TimeUnit timeUnit) {
        gridManager.setAwaitTimeout(timeout, timeUnit);
    }

    public static void setRetry(int tries) {
        gridManager.setRetry(tries);
    }

}
