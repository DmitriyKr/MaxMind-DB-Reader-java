package com.maxmind.db;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class MultiThreadedTest {

    @Test
    public void multipleMmapOpens() throws InterruptedException,
            ExecutionException {
        Callable<JsonNode> task = new Callable<JsonNode>() {
            @Override
            public JsonNode call() throws UnknownHostException, IOException,
                    URISyntaxException {
                URI file = ReaderTest.class.getResource(
                        "/maxmind-db/test-data/MaxMind-DB-test-decoder.mmdb")
                        .toURI();
                final Reader reader = new Reader(new File(file));
                return reader.get(InetAddress.getByName("::1.1.1.0"));
            }
        };
        MultiThreadedTest.runThreads(task);
    }

    @Test
    public void streamThreadTest() throws IOException, InterruptedException,
            ExecutionException {
        final Reader reader = new Reader(ReaderTest.class.getResource(
                "/maxmind-db/test-data/MaxMind-DB-test-decoder.mmdb")
                .openStream());
        MultiThreadedTest.threadTest(reader);
    }

    @Test
    public void mmapThreadTest() throws IOException, InterruptedException,
            ExecutionException, URISyntaxException {
        URI file = ReaderTest.class.getResource(
                "/maxmind-db/test-data/MaxMind-DB-test-decoder.mmdb").toURI();
        final Reader reader = new Reader(new File(file));
        MultiThreadedTest.threadTest(reader);
    }

    private static void threadTest(final Reader reader) throws InterruptedException,
            ExecutionException {
        Callable<JsonNode> task = new Callable<JsonNode>() {
            @Override
            public JsonNode call() throws UnknownHostException, IOException {
                return reader.get(InetAddress.getByName("::1.1.1.0"));
            }
        };
        MultiThreadedTest.runThreads(task);
    }

    private static void runThreads(Callable<JsonNode> task)
            throws InterruptedException, ExecutionException {
        int threadCount = 1024;
        List<Callable<JsonNode>> tasks = Collections.nCopies(threadCount, task);
        ExecutorService executorService = Executors
                .newFixedThreadPool(threadCount);
        List<Future<JsonNode>> futures = executorService.invokeAll(tasks);

        for (Future<JsonNode> future : futures) {
            JsonNode record = future.get();
            assertEquals(268435456, record.get("uint32").intValue());
            assertEquals("unicode! ☯ - ♫", record.get("utf8_string")
                    .textValue());
        }
    }
}
