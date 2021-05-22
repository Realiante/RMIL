package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.DistributionTactic;
import dev.rea.rmil.client.RmilConfig;
import dev.rea.rmil.client.RmilGridManager;
import dev.rea.rmil.client.manager.RmilGridManagerImpl.FunctionTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.rmi.RemoteException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.*;

class RmilGridManagerTest {

    static RemoteServer remoteServer1;
    static RemoteServer remoteServer2;
    static RemoteServer remoteServer3;

    static Stream<Arguments> sleepTimeSource() {
        return Stream.of(
                of(1, TimeUnit.NANOSECONDS, 10),
                of(2, TimeUnit.NANOSECONDS, 10),
                of(5, TimeUnit.NANOSECONDS, 10),
                of(10, TimeUnit.NANOSECONDS, 10),
                of(100, TimeUnit.NANOSECONDS, 10),
                of(1, TimeUnit.MICROSECONDS, 10),
                of(5, TimeUnit.MICROSECONDS, 10),
                of(10, TimeUnit.MICROSECONDS, 10),
                of(1, TimeUnit.MILLISECONDS, 10),
                of(5, TimeUnit.MILLISECONDS, 10),
                of(10, TimeUnit.MILLISECONDS, 10),
                of(100, TimeUnit.MILLISECONDS, 20),
                of(1, TimeUnit.SECONDS, 40),
                of(3, TimeUnit.SECONDS, 55)
        );
    }

    @BeforeAll
    static void setup() {
        remoteServer1 = mock(RemoteServer.class, withSettings().useConstructor("test1", 1));
        when(remoteServer1.attemptLoad()).thenReturn(true);

        remoteServer2 = mock(RemoteServer.class, withSettings().useConstructor("test2", 2));
        when(remoteServer2.attemptLoad()).thenReturn(true);

        remoteServer3 = mock(RemoteServer.class, withSettings().useConstructor("test3", 3));
        when(remoteServer3.attemptLoad()).thenReturn(true);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void localOnlyPredicateTest(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManager dm = new RmilGridManagerImpl(new RmilConfig(), DistributionTactic.LOCAL_ONLY);
        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .filter(dm.filterTask(predicate)).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @Test
    @Timeout(value = 60)
    @SuppressWarnings("unchecked")
    void remotePredicateRemoteConditionReached() {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        AtomicBoolean pauseCondition = new AtomicBoolean(false);
        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(1);

        Predicate<Integer> predicate = arg -> {
            await("Local-Waiting").untilTrue(pauseCondition);
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of());
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).then(invocation -> {
                    pauseCondition.set(true);
                    return predicate.test(((FunctionTask<Integer, Boolean>) invocation.getArgument(0)).argument);
                }
        );

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .filter(dm.filterTask(predicate)).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of());
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).thenCallRealMethod();
        when(dm.executeTaskServerUnavailable(any())).thenCallRealMethod();

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .filter(dm.filterTask(predicate)).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest_RemoteExecutionMock(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of(remoteServer1, remoteServer2, remoteServer3));
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).thenCallRealMethod();
        when(dm.executeTaskServerUnavailable(any())).thenCallRealMethod();
        try {
            when(dm.executeTaskOnServer(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .filter(dm.filterTask(predicate)).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest_CustomForkJoinPool(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        AtomicBoolean pauseCondition = new AtomicBoolean(false);
        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of());
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).thenCallRealMethod();
        when(dm.executeTaskServerUnavailable(any())).thenCallRealMethod();

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            ForkJoinPool customPool = new ForkJoinPool(8);
            try {
                filteredSet.addAll(customPool.submit(() ->
                        testSet.stream().parallel().filter(dm.filterTask(predicate)).collect(Collectors.toSet()))
                        .get());
            } catch (InterruptedException | ExecutionException e) {
                Assertions.fail(e);
            }
        });


        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @Test
    @Timeout(value = 60)
    void remotePredicateTest_LargeSet_NoWaitTime_RemoteExecutionMock() {
        var testSet = IntStream.rangeClosed(-9000, 191000)
                .boxed().collect(Collectors.toSet());

        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(4);

        Predicate<Integer> predicate = arg -> arg > 1;
        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of(remoteServer1, remoteServer2, remoteServer3));
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).thenCallRealMethod();
        when(dm.executeTaskServerUnavailable(any())).thenCallRealMethod();
        try {
            when(dm.executeTaskOnServer(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .filter(dm.filterTask(predicate)).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(191000 - 1, filteredSet.size());
    }

    @Test
    @Timeout(value = 60)
    void remotePredicateTest_LargeSet_CustomForkJoinPool_NoWaitTime_RemoteExecutionMock() {
        var testSet = IntStream.rangeClosed(-9000, 191000)
                .boxed().collect(Collectors.toSet());

        RmilConfig config = new RmilConfig();
        config.setMaxLocalTasks(4);

        Predicate<Integer> predicate = arg -> arg > 1;
        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor("test", 1, config, DistributionTactic.STANDARD));
        when(dm.getAvailableServers()).thenReturn(Set.of(remoteServer1, remoteServer2, remoteServer3));
        when(dm.filterTask(predicate)).thenCallRealMethod();
        when(dm.registerFunctionTask(any())).thenCallRealMethod();
        when(dm.sendFunctionAndReturnUnavailable(any())).thenReturn(Set.of());
        when(dm.executeFunctionTask(any())).thenCallRealMethod();
        when(dm.executeTaskServerUnavailable(any())).thenCallRealMethod();
        try {
            when(dm.executeTaskOnServer(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            ForkJoinPool customPool = new ForkJoinPool(24);
            try {
                filteredSet.addAll(customPool.submit(() ->
                        testSet.stream().parallel().filter(dm.filterTask(predicate)).collect(Collectors.toSet()))
                        .get());
            } catch (InterruptedException | ExecutionException e) {
                Assertions.fail(e);
            }
        });

        Assertions.assertEquals(191000 - 1, filteredSet.size());
    }
}
