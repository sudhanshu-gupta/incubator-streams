package org.apache.streams.instagram.provider;

import org.apache.streams.core.StreamsResultSet;
import org.apache.streams.instagram.InstagramUserInformationConfiguration;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class InstagramRecentMediaProviderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramRecentMediaProviderTest.class);

    @Test
    public void testStartStream() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final InstagramRecentMediaCollector collectorStub = new InstagramRecentMediaCollector(new ConcurrentLinkedQueue<MediaFeedData>(), new InstagramUserInformationConfiguration()) {

            private volatile boolean isFinished = false;

            @Override
            public void run() {
                this.isFinished = true;
                latch.countDown();
            }

            @Override
            public boolean isCompleted() {
                return this.isFinished;
            }
        };

        InstagramRecentMediaProvider provider = new InstagramRecentMediaProvider(null) {
            @Override
            protected InstagramRecentMediaCollector getInstagramRecentMediaCollector() {
                return collectorStub;
            }
        };

        provider.startStream();

        latch.await();
        assertTrue(collectorStub.isCompleted());
        StreamsResultSet result = provider.readCurrent();
        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(!provider.isRunning());
        try {
            provider.cleanUp();
        } catch (Throwable throwable){
            throwable.printStackTrace();
            fail("Error durring clean up");
        }
    }

    @Test
    public void testReadCurrent() {
        final long seed = System.nanoTime();
        final Random rand = new Random(seed);
        final CyclicBarrier test = new CyclicBarrier(2);
        final CyclicBarrier produce = new CyclicBarrier(2);
        final AtomicInteger batchCount = new AtomicInteger(0);
        InstagramRecentMediaProvider provider = new InstagramRecentMediaProvider(new InstagramUserInformationConfiguration()) {
            @Override
            protected InstagramRecentMediaCollector getInstagramRecentMediaCollector() {
                return new InstagramRecentMediaCollector(super.mediaFeedQueue, new InstagramUserInformationConfiguration()) {

                    private volatile boolean isFinished = false;



                    public int getBatchCount() {
                        return batchCount.get();
                    }

                    @Override
                    public boolean isCompleted() {
                        return isFinished;
                    }

                    @Override
                    public void run() {
                        int randInt = rand.nextInt(5);
                        while(randInt != 0) {
                            int batchSize = rand.nextInt(200);
                            for(int i=0; i < batchSize; ++i) {
                                while(!super.dataQueue.add(mock(MediaFeedData.class))) {
                                    Thread.yield();
                                }
                            }
                            batchCount.set(batchSize);
                            try {
                                test.await();
                                produce.await();
                            } catch (InterruptedException ie ) {
                                Thread.currentThread().interrupt();
                            } catch (BrokenBarrierException bbe) {
                                Thread.currentThread().interrupt();
                            }
                            randInt = rand.nextInt(5);
                        }
                        batchCount.set(0);
                        isFinished = true;
                        try {
                            test.await();
                            produce.await();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } catch (BrokenBarrierException bbe) {
                            Thread.currentThread().interrupt();
                        }
                    }

                };
            }
        };
        provider.startStream();
        while(provider.isRunning()) {
            try {
                test.await();
                assertEquals("Seed == "+seed, batchCount.get(), provider.readCurrent().size());
                produce.await();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException bbe) {
                Thread.currentThread().interrupt();
            }

        }
    }

}
