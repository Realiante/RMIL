package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.DistributionManager;
import dev.rea.rmil.client.DistributionTactic;
import dev.rea.rmil.client.RmilConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

public class DistributedManagerTest {

    @Test
    void localOnlyPredicateTest() {
        Predicate<Integer> predicate = arg -> arg > 1;
        DistributionManager dm = new DistributionManagerImpl("test", 0,
                new RmilConfig(), DistributionTactic.LOCAL_ONLY);
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        var filteredSet = testSet.stream().filter(dm.filterTask(predicate)).collect(Collectors.toSet());

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @Test
    void remotePredicateConditionReachedTest() {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        AtomicBoolean pauseCondition = new AtomicBoolean(false);
        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(2);

        Predicate<Integer> predicate = arg -> {
            await("Local-Waiting").untilTrue(pauseCondition);
            return arg > 1;
        };

        DistributionManagerImpl dm = mock(DistributionManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of());
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.executeFunctionTask(any(), any())).then(invocation -> {
                    pauseCondition.set(true);
                    return predicate.test(invocation.getArgument(1));
                }
        );

        var filteredSet = testSet.stream().parallel().filter(dm.filterTask(predicate)).collect(Collectors.toSet());

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }


}
