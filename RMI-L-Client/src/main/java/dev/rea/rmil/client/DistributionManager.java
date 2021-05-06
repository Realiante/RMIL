package dev.rea.rmil.client;

import java.util.function.Predicate;

public interface DistributionManager {

    <T> Predicate<? super T> filterTask(Predicate<? super T> predicate);

}
