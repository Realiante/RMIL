package rea.dev.rmil.remote;

@FunctionalInterface
public interface DistFunction<T,R> extends BaseFunction {

     R execute(T argument);

}
