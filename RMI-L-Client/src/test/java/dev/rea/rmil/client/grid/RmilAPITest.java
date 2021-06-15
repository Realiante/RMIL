package dev.rea.rmil.client.grid;

import dev.rea.rmil.engine.EngineBinding;
import dev.rea.rmil.engine.backend.EngineBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import rea.dev.rmil.remote.ServerConfiguration;

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
    private static final int maxEngineThreads = Runtime.getRuntime().availableProcessors();
    private static final int parallelism = maxEngineThreads + 1;
    private static EngineBinding engineBinding;
    private static UUID serverID;


    @BeforeAll
    static void setup() {
        Assertions.assertDoesNotThrow(() -> {
            serverID = UUID.randomUUID();
            engineBinding = EngineBuilder.build(
                    new ServerConfiguration(serverID, maxEngineThreads, ServerConfiguration.Priority.NORMAL));
            //adds local test server as server
            addServer(null);
            setAwaitTimeout(30, TimeUnit.SECONDS);
            setRetry(0);
        });
    }

    @RepeatedTest(3)
    @Timeout(value = 60)
    void testChainPredicate() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-1000, 1000)
                    .boxed().collect(Collectors.toSet());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = new HashSet<>(customPool.submit(() -> testStream.stream().map(mapToGrid())
                        .filter(gridPredicate(num -> num >= -500))
                        .filter(gridPredicate(num -> num <= 500))
                        .filter(gridPredicate(num -> num > 0))
                        .filter(gridPredicate(num -> num < 100))
                        .map(mapFromGrid()).collect(Collectors.toSet())).get());
                Assertions.assertEquals(99, result.size());
            });
        });
    }

    @RepeatedTest(3)
    @Timeout(value = 60)
    void testChainParallelPredicate() {
        setMaxLocalTasks(0);
        Assertions.assertDoesNotThrow(() -> {
            var testStream = IntStream.rangeClosed(-1000, 1000)
                    .boxed().collect(Collectors.toSet());
            var customPool = new ForkJoinPool(parallelism);
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                Set<Integer> result = new HashSet<>(customPool.submit(() -> testStream.stream().parallel()
                        .map(mapToGrid())
                        .filter(gridPredicate(num -> num >= -500))
                        .filter(gridPredicate(num -> num <= 500))
                        .filter(gridPredicate(num -> num > 0))
                        .filter(gridPredicate(num -> num < 100))
                        .map(mapFromGrid()).collect(Collectors.toSet())).get());
                Assertions.assertEquals(99, result.size());
            });
        });
    }
}
