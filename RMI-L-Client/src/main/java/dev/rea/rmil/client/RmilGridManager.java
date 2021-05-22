package dev.rea.rmil.client;

import java.util.function.Predicate;

public interface RmilGridManager {

    <T> Predicate<? super T> filterTask(Predicate<? super T> predicate);

}
