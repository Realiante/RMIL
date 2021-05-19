package rea.dev.rmil.remote;

@FunctionalInterface
public interface DistBiTask<T, A, R> extends BaseTask {

    R execute(T argument, A anotherArg);

}
