package dev.rea.rmil.client;

import dev.rea.rmil.client.grid.GridBuilder;
import rea.dev.rmil.remote.items.DistributedItem;

import java.util.function.Predicate;

public final class RMIL {

    private static final RmilGridManager gridManager = GridBuilder.buildGrid();

    private RMIL() {
        //static API class
    }

    public static void setMaxLocalTasks(int maxLocalTasks) {
        gridManager.setMaxLocalTasks(maxLocalTasks);
    }

    public static <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate) {
        return gridManager.gridPredicate(predicate);
    }

}
