package com.rstmdb.client.transport;

import com.rstmdb.client.model.SubscriptionEvent;
import com.rstmdb.client.protocol.Operations;
import com.rstmdb.client.protocol.WireMessage.WireEvent;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Queue-based event stream for WATCH_INSTANCE and WATCH_ALL subscriptions.
 */
public class Subscription implements AutoCloseable {

    private static final int QUEUE_CAPACITY = 256;
    private static final SubscriptionEvent POISON = new SubscriptionEvent(
            "", "", "", 0, 0, "", "", "", null, null);

    private String id;
    private final ArrayBlockingQueue<SubscriptionEvent> events = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Connection connection;
    private volatile boolean closed;

    public Subscription(Connection connection) {
        this.connection = connection;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Blocking poll with timeout. Returns null on timeout.
     */
    public SubscriptionEvent poll(Duration timeout) throws InterruptedException {
        if (closed) return null;
        SubscriptionEvent evt = events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (evt == POISON) return null;
        return evt;
    }

    /**
     * Non-blocking poll. Returns null if empty.
     */
    public SubscriptionEvent poll() {
        if (closed) return null;
        SubscriptionEvent evt = events.poll();
        if (evt == POISON) return null;
        return evt;
    }

    /**
     * Blocking take. Waits indefinitely until an event is available.
     */
    public SubscriptionEvent take() throws InterruptedException {
        if (closed) return null;
        SubscriptionEvent evt = events.take();
        if (evt == POISON) return null;
        return evt;
    }

    /**
     * Returns events as a blocking Iterable. Terminates when the subscription closes.
     */
    public Iterable<SubscriptionEvent> events() {
        return () -> new Iterator<>() {
            private SubscriptionEvent next;

            @Override
            public boolean hasNext() {
                if (closed && events.isEmpty()) return false;
                try {
                    next = events.take();
                    if (next == POISON) {
                        next = null;
                        return false;
                    }
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            @Override
            public SubscriptionEvent next() {
                if (next == null) throw new NoSuchElementException();
                return next;
            }
        };
    }

    /**
     * Register a callback-based event handler. Runs on a daemon thread.
     */
    public void onEvent(Consumer<SubscriptionEvent> handler) {
        Thread thread = new Thread(() -> {
            try {
                while (!closed) {
                    SubscriptionEvent evt = events.take();
                    if (evt == POISON) break;
                    handler.accept(evt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "rstmdb-subscription-" + id);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * JDK Flow.Publisher adapter for reactive streams.
     */
    public Flow.Publisher<SubscriptionEvent> asPublisher() {
        return subscriber -> {
            subscriber.onSubscribe(new Flow.Subscription() {
                private final AtomicBoolean cancelled = new AtomicBoolean(false);
                private final Thread thread;

                {
                    thread = new Thread(() -> {
                        try {
                            while (!cancelled.get() && !closed) {
                                SubscriptionEvent evt = events.poll(100, TimeUnit.MILLISECONDS);
                                if (evt == null) continue;
                                if (evt == POISON) break;
                                subscriber.onNext(evt);
                            }
                            if (!cancelled.get()) {
                                subscriber.onComplete();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            if (!cancelled.get()) {
                                subscriber.onError(e);
                            }
                        }
                    }, "rstmdb-publisher-" + id);
                    thread.setDaemon(true);
                    thread.start();
                }

                @Override
                public void request(long n) {
                    // Delivery is push-based from the queue
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                    thread.interrupt();
                }
            });
        };
    }

    /**
     * Called by Connection read loop. Non-blocking enqueue, drops oldest if full.
     */
    void enqueue(WireEvent wireEvent) {
        if (closed) return;
        SubscriptionEvent event = new SubscriptionEvent(
                wireEvent.getSubscriptionId(),
                wireEvent.getInstanceId(),
                wireEvent.getMachine(),
                wireEvent.getVersion(),
                wireEvent.getWalOffset(),
                wireEvent.getFromState(),
                wireEvent.getToState(),
                wireEvent.getEvent(),
                wireEvent.getPayload(),
                wireEvent.getCtx()
        );
        // Drop oldest if full
        if (!events.offer(event)) {
            events.poll();
            events.offer(event);
        }
    }

    /**
     * Close: sends UNWATCH, removes from connection, signals termination.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;

        // Send UNWATCH
        if (id != null && connection != null && !connection.isClosed()) {
            try {
                connection.sendRequest(Operations.UNWATCH, Map.of("subscription_id", id));
            } catch (Exception ignored) {}
            connection.removeSubscription(id);
        }

        // Signal termination
        events.offer(POISON);
    }

    /**
     * Called by Connection on close — signals termination without sending UNWATCH.
     */
    void closeFromConnection() {
        if (closed) return;
        closed = true;
        events.offer(POISON);
    }
}
