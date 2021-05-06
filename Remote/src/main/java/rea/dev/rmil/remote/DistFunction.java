package rea.dev.rmil.remote;

import java.io.Serializable;

@FunctionalInterface
public interface DistFunction<T,R> extends Serializable {

     R execute(T argument);

}
