package rea.dev.rmil.remote;

import java.io.Serializable;
import java.util.function.Function;

public interface DistributedMethod extends Serializable {

    @FunctionalInterface
    interface DistFunction<T, R> extends DistributedMethod, Function<T, R>, Serializable {
    }

    @FunctionalInterface
    interface DistCheck<T, R> extends DistributedMethod, Serializable {
        R check(T argument);
    }

    @FunctionalInterface
    interface DistBiCheck<T, A, R> extends DistributedMethod, Serializable {
        R check(T argument, A anotherArg);
    }

}
