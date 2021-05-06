package rea.dev.rmil.remote;

import java.io.Serializable;

@FunctionalInterface
public interface DistBiFunction<T, A, R> extends Serializable {

    R execute(T argument, A anotherArg);

}
