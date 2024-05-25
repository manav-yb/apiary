package org.dbos.apiary.benchmarks;

import com.mongodb.client.model.Indexes;
import org.dbos.apiary.client.ApiaryWorkerClient;
import org.dbos.apiary.mongo.MongoConnection;
import org.dbos.apiary.mongo.MongoContext;
import org.dbos.apiary.postgres.PostgresConnection;
import org.dbos.apiary.utilities.ApiaryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CarBookingBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(CarBookingBenchmark.class);
    private static final int threadPoolSize = 256;

    private static final int numCars = 100;
    private static final int numCustomers = 100;

    private static final int threadWarmupMs = 5000;  // First 5 seconds of request would be warm-up requests.
    private static final Collection<Long> searchTimes = new ConcurrentLinkedQueue<>();
    private static final Collection<Long> reserveTimes = new ConcurrentLinkedQueue<>();

    public static void benchmark(String dbAddr, Integer interval, Integer duration, int percentageSearch, int percentageReserve) throws SQLException, InterruptedException, IOException {
        assert (percentageSearch + percentageReserve == 100);
        PostgresConnection conn = new PostgresConnection("localhost", ApiaryConfig.postgresPort, "postgres", "dbos");
        conn.dropTable("FuncInvocations");
        conn.dropTable("CarsTable");
        conn.createTable("CarsTable", "CarID integer PRIMARY KEY NOT NULL, CarName VARCHAR(1000) NOT NULL, CarStatus integer NOT NULL");
        conn.createIndex("CREATE INDEX CarIndex ON CarsTable (CarID);");

        ThreadLocal<ApiaryWorkerClient> client = ThreadLocal.withInitial(() -> new ApiaryWorkerClient("localhost"));

        long loadStart = System.currentTimeMillis();
        for (int carNum = 0; carNum < numCars; carNum++) {
            int longitude = ThreadLocalRandom.current().nextInt(0, 90);
            int latitude = ThreadLocalRandom.current().nextInt(0, 90);
            client.get().executeFunction("RegisterCabLoc", carNum, "Car" + carNum, Integer.MAX_VALUE, longitude, latitude);
        }
        logger.info("Done Loading: {}", System.currentTimeMillis() - loadStart);

        AtomicInteger reservationIDs = new AtomicInteger(0);
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (duration * 1000 + threadWarmupMs);

        Runnable r = () -> {
            try {
                long t0 = System.nanoTime();
                int chooser = ThreadLocalRandom.current().nextInt(100);
                if (chooser < percentageSearch) {
                    int longitude = ThreadLocalRandom.current().nextInt(0, 90);
                    int latitude = ThreadLocalRandom.current().nextInt(0, 90);
                    client.get().executeFunction("RequestCab", longitude, latitude).getInt();
                    searchTimes.add(System.nanoTime() - t0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        while (System.currentTimeMillis() < endTime) {
            long t = System.nanoTime();
            if (System.currentTimeMillis() - startTime < threadWarmupMs) {
                searchTimes.clear();
                reserveTimes.clear();
            }
            threadPool.submit(r);
            while (System.nanoTime() - t < interval.longValue() * 1000) {
                // Busy-spin
            }
        }

        long elapsedTime = (System.currentTimeMillis() - startTime) - threadWarmupMs;

        List<Long> queryTimes = searchTimes.stream().map(i -> i / 1000).sorted().collect(Collectors.toList());
        int numQueries = queryTimes.size();
        if (numQueries > 0) {
            long average = queryTimes.stream().mapToLong(i -> i).sum() / numQueries;
            double throughput = (double) numQueries * 1000.0 / elapsedTime;
            long p50 = queryTimes.get(numQueries / 2);
            long p99 = queryTimes.get((numQueries * 99) / 100);
            logger.info("Searches: Duration: {} Interval: {}μs Queries: {} TPS: {} Average: {}μs p50: {}μs p99: {}μs", elapsedTime, interval, numQueries, String.format("%.03f", throughput), average, p50, p99);
        } else {
            logger.info("No searches");
        }

        queryTimes = reserveTimes.stream().map(i -> i / 1000).sorted().collect(Collectors.toList());
        numQueries = queryTimes.size();
        if (numQueries > 0) {
            long average = queryTimes.stream().mapToLong(i -> i).sum() / numQueries;
            double throughput = (double) numQueries * 1000.0 / elapsedTime;
            long p50 = queryTimes.get(numQueries / 2);
            long p99 = queryTimes.get((numQueries * 99) / 100);
            logger.info("Reservations: Duration: {} Interval: {}μs Queries: {} TPS: {} Average: {}μs p50: {}μs p99: {}μs", elapsedTime, interval, numQueries, String.format("%.03f", throughput), average, p50, p99);
        } else {
            logger.info("No reservations");
        }

        threadPool.shutdown();
        threadPool.awaitTermination(100000, TimeUnit.SECONDS);
        logger.info("All queries finished! {}", System.currentTimeMillis() - startTime);
    }
}
