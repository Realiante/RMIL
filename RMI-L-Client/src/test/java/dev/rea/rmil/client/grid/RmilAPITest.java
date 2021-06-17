package dev.rea.rmil.client.grid;

import dev.rea.rmil.engine.EngineBinding;
import dev.rea.rmil.engine.backend.EngineBuilder;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.ServerConfiguration;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.rea.rmil.client.RMIL.*;

class RmilAPITest {
    private static final Logger logger = LoggerFactory.getLogger(RmilAPITest.class);

    private static final int maxEngineThreads = Runtime.getRuntime().availableProcessors();
    private static final int parallelism = maxEngineThreads + 60;
    private static EngineBinding engineBinding;
    private static RemoteEngine engineStub;
    private static UUID serverID;

    @BeforeAll
    static void setup() {
        Assertions.assertDoesNotThrow(() -> {
            serverID = UUID.randomUUID();
            engineBinding = EngineBuilder.build(
                    new ServerConfiguration(serverID, maxEngineThreads, ServerConfiguration.Priority.NORMAL));
            engineStub = engineBinding.getStub();

            //adds local test server as server
            addServer(null);
            setAwaitTimeout(30, TimeUnit.SECONDS);
            setRetry(0);
        });
        logger.info("Starting tests with parallelism=" + parallelism);
    }

    @AfterAll
    static void cleanUp() {
        try {
            Assertions.assertFalse(engineStub.removeItem(UUID.randomUUID()));
            engineBinding.unbind();
        } catch (NotBoundException | RemoteException notBoundException) {
            Assertions.fail(notBoundException);
        }
    }

    @BeforeEach
    void maxTaskSetup() {
        setMaxLocalTasks(1);
    }

    @Test
    @Timeout(value = 30)
    void unconnectedChainTest() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-3, 3)
                    .boxed().collect(Collectors.toSet());
            Assertions.assertEquals(7, testStream.size());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = customPool.submit(() -> testStream.stream().parallel()
                        .map(mapToGrid())
                        .filter(gridPredicate(new TestPredicate(0)))
                        .map(mapFromGrid())
                        .map(integer -> integer + 1)
                        .map(mapToGrid())
                        .map(gridFunction(new TestMap()))
                        .map(mapFromGrid()).collect(Collectors.toSet())
                ).get();
                Assertions.assertEquals(Set.of(2, 3, 4, 5), result);
            });
        });
    }

    @RepeatedTest(2)
    @Timeout(value = 30)
    void testPredicate() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-10, 10)
                    .boxed().collect(Collectors.toSet());
            Assertions.assertEquals(21, testStream.size());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = customPool.submit(() -> testStream.stream().parallel()
                        .map(mapToGrid())
                        .filter(gridPredicate(new TestPredicate(0)))
                        .map(mapFromGrid()).collect(Collectors.toSet())).get();
                Assertions.assertEquals(11, result.size());
            });
        });
    }

    @RepeatedTest(2)
    @Timeout(value = 30)
    void testChainPredicate() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-1000, 1000)
                    .boxed().collect(Collectors.toSet());
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = testStream.stream()
                        .map(mapToGrid())
                        .filter(gridPredicate(new TestPredicate(0)))
                        .filter(gridPredicate(new TestPredicate(1)))
                        .filter(gridPredicate(new TestPredicate(2)))
                        .map(mapFromGrid()).collect(Collectors.toSet());
                Assertions.assertEquals(49, result.size());
            });
        });
    }

    @RepeatedTest(2)
    @Timeout(value = 120)
    void testChainParallelPredicate() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-100, 100)
                    .boxed().collect(Collectors.toSet());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = new HashSet<>(customPool.submit(() -> testStream.stream().parallel()
                        .map(mapToGrid())
                        .filter(gridPredicate(new TestPredicate(0)))
                        .filter(gridPredicate(new TestPredicate(1)))
                        .filter(gridPredicate(new TestPredicate(2)))
                        .filter(gridPredicate(new TestPredicate(3)))
                        .map(mapFromGrid()).collect(Collectors.toSet())).get());
                Assertions.assertEquals(9, result.size());
            });
        });
    }


    @RepeatedTest(2)
    @Timeout(value = 30)
    void testFunction() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-5, 2)
                    .boxed().collect(Collectors.toSet());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Double> result = customPool.submit(() ->
                        testStream.stream().parallel().map(mapToGrid())
                                .map(gridToDoubleFunction(new TestToDouble()))
                                .map(mapFromGrid())
                                .collect(Collectors.toSet())).get();
                Assertions.assertEquals(8, result.size());
                Assertions.assertEquals(Set.of(-7.5, -6.0, -4.5, -3.0, -1.5, 0.0, 1.5, 3.0), result);
            });
        });
    }
}
