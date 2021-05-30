package dev.rea.rmil.client.grid;

import dev.rea.rmil.client.DistributedItem;
import dev.rea.rmil.client.RmilGridManager;
import dev.rea.rmil.client.grid.RmilGridManagerImpl.CheckFromLocal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import rea.dev.rmil.remote.ServerConfiguration;

import java.rmi.RemoteException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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

class RmilGridManagerImplCheck_NoGridTest {

    static RemoteServer remoteServer;
    static RemoteServer remoteServerLow;

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
        Assertions.assertDoesNotThrow(() -> {
            remoteServer = mock(RemoteServer.class, withSettings().useConstructor("test"));
            when(remoteServer.loadConfiguration()).thenReturn(
                    new ServerConfiguration(UUID.randomUUID(), 2, ServerConfiguration.Priority.NORMAL));
            doNothing().when(remoteServer).loadContainer(any());
            remoteServer.setConfiguration(remoteServer.loadConfiguration());

            remoteServerLow = mock(RemoteServer.class, withSettings().useConstructor("testLow"));
            when(remoteServerLow.loadConfiguration()).thenReturn(
                    new ServerConfiguration(UUID.randomUUID(), 1, ServerConfiguration.Priority.LOW));
            doCallRealMethod().when(remoteServerLow).setConfiguration(any());
            doNothing().when(remoteServerLow).loadContainer(any());
            remoteServerLow.setConfiguration(remoteServerLow.loadConfiguration());
        });
    }

    @Test
    @Timeout(value = 60)
    @SuppressWarnings("unchecked")
    void remotePredicateRemoteConditionReached() {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);
        AtomicBoolean pauseCondition = new AtomicBoolean(false);
        RmilGridManager gridManager = GridBuilder.buildGrid();
        gridManager.setMaxLocalTasks(1);

        Predicate<Integer> predicate = arg -> {
            await("Local-Waiting").untilTrue(pauseCondition);
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(3, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of());
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).then(invocation -> {
                    pauseCondition.set(true);
                    return predicate.test(((CheckFromLocal<Integer, Boolean>)
                            invocation.getArgument(0)).argumentPackage.getArgument());
                }
        );
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .map(dm.mapToGrid())
                    .filter(dm.gridPredicate(predicate))
                    .map(DistributedItem::getItem)
                    .collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(4, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of());
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).thenCallRealMethod();
        when(dm.checkWhenAvailableFromLocal(any())).thenCallRealMethod();
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .map(dm.mapToGrid())
                    .filter(dm.gridPredicate(predicate))
                    .map(DistributedItem::getItem).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest_RemoteExecutionMockOnly(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(0, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of(remoteServer, remoteServerLow));
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).thenCallRealMethod();
        when(dm.checkWhenAvailableFromLocal(any())).thenCallRealMethod();
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        try {
            when(dm.checkOnRemote(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .map(dm.mapToGrid())
                    .filter(dm.gridPredicate(predicate))
                    .map(DistributedItem::getItem).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(5, filteredSet.size());
        Assertions.assertEquals(Set.of(14, 55, 4, 15, 3), filteredSet);
    }

    @ParameterizedTest
    @MethodSource("sleepTimeSource")
    @Timeout(value = 60)
    void remotePredicateTest_CustomForkJoinPool(int sleepTime, TimeUnit timeUnit, int timeOut) {
        var testSet = Set.of(0, 14, 55, -2, 4, -717, 15, -11, 3, 1);

        Predicate<Integer> predicate = arg -> {
            try {
                timeUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return arg > 1;
        };

        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(4, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of());
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).thenCallRealMethod();
        when(dm.checkWhenAvailableFromLocal(any())).thenCallRealMethod();
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeOut), () -> {
            ForkJoinPool customPool = new ForkJoinPool(8);
            try {
                filteredSet.addAll(customPool.submit(() ->
                        //RMIL code in stream here
                        testSet.stream().parallel()
                                .map(dm.mapToGrid())
                                .filter(dm.gridPredicate(predicate))
                                .map(DistributedItem::getItem).collect(Collectors.toSet()))
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

        Predicate<Integer> predicate = arg -> arg > 1;
        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(4, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of(remoteServer, remoteServerLow));
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).thenCallRealMethod();
        when(dm.checkWhenAvailableFromLocal(any())).thenCallRealMethod();
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        try {
            when(dm.checkOnRemote(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            filteredSet.addAll(testSet.stream().parallel()
                    .map(dm.mapToGrid())
                    .filter(dm.gridPredicate(predicate))
                    .map(DistributedItem::getItem)
                    .collect(Collectors.toSet()));
        });

        Assertions.assertEquals(191000 - 1, filteredSet.size());
    }

    @Test
    @Timeout(value = 60)
    void remotePredicateTest_LargeSet_CustomForkJoinPool_NoWaitTime_RemoteExecutionMock() {
        var testSet = IntStream.rangeClosed(-9000, 191000)
                .boxed().collect(Collectors.toSet());

        Predicate<Integer> predicate = arg -> arg > 1;
        RmilGridManagerImpl dm = mock(RmilGridManagerImpl.class,
                withSettings().useConstructor(4, Set.of()));
        when(dm.getAvailableServers(anySet())).thenReturn(Set.of(remoteServer, remoteServerLow));
        when(dm.gridPredicate(predicate)).thenCallRealMethod();
        when(dm.registerMethod(any())).thenCallRealMethod();
        when(dm.sendFunctionPackage(any())).thenReturn(Set.of());
        when(dm.checkItemFromLocal(any())).thenCallRealMethod();
        when(dm.checkWhenAvailableFromLocal(any())).thenCallRealMethod();
        when(dm.mapToGrid()).thenCallRealMethod();
        when(dm.buildDistributedItem(any())).thenCallRealMethod();

        try {
            when(dm.checkOnRemote(any(), any(), any())).then(invocation ->
                    predicate.test(invocation.getArgument(2)));
        } catch (RemoteException e) {
            Assertions.fail(e);
        }

        Set<Integer> filteredSet = new HashSet<>();
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            ForkJoinPool customPool = new ForkJoinPool(24);
            try {
                filteredSet.addAll(customPool.submit(() ->
                        testSet.stream().parallel()
                                .map(dm.mapToGrid())
                                .filter(dm.gridPredicate(predicate))
                                .map(DistributedItem::getItem).collect(Collectors.toSet()))
                        .get());
            } catch (InterruptedException | ExecutionException e) {
                Assertions.fail(e);
            }
        });

        Assertions.assertEquals(191000 - 1, filteredSet.size());
    }

}
