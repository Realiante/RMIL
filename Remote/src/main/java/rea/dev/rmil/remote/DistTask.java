package rea.dev.rmil.remote;

@FunctionalInterface
public interface DistTask<T,R> extends BaseTask {

     R execute(T argument);

}
