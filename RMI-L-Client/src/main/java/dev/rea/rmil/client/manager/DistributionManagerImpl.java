package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.DistributionManager;
import dev.rea.rmil.client.DistributionTactic;
import rea.dev.rmil.remote.DistFunction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;


public class DistributionManagerImpl implements DistributionManager {

    private final DistributionTactic distTactic;
    private final int maxLocalTasks;
    private AtomicInteger localCounter;
    private RemoteManager remoteManager;

    public DistributionManagerImpl(DistributionTactic tactic, int maxLocalTasks) {
        this.distTactic = tactic;
        this.maxLocalTasks = maxLocalTasks;
        if (distTactic != DistributionTactic.LOCAL_ONLY) {
            remoteManager = new RemoteManager();
        }
    }

    @SuppressWarnings("java:S128") //suppressing sonarlint warning
    public <T> Predicate<? super T> filterTask(Predicate<? super T> predicate) {
        return (Predicate<T>) argument -> {
            DistFunction<? super T, Boolean> ttDistFunction = predicate::test;
            switch (distTactic) {
                case STANDARD:
                    if (localCounter.get() <= maxLocalTasks) {
                        localCounter.incrementAndGet();
                        var result = ttDistFunction.execute(argument);
                        localCounter.decrementAndGet();
                        return result;
                    }
                    //this non terminated switch case is intentional.
                    //if you need to start a new task over local limit, start it as remote instead.
                case REMOTE_ONLY:
                    //todo: send function to server
                    //todo: queue up an item
                case LOCAL_ONLY:
            }
            return ttDistFunction.execute(argument);
        };
    }



}
