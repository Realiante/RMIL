package rea.dev.rmil.remote;

@FunctionalInterface
public interface DistBiFunction<T, A, R> extends BaseFunction {

    R execute(T argument, A anotherArg);

}
