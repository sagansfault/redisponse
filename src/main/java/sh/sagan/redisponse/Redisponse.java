package sh.sagan.redisponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @since 1.0.0
 */
public class Redisponse {

    private final Cache<UUID, CompletableFuture<String>> responseDesk;
    private final Map<String, Function<String, String>> responseHandlers;
    private final Set<String> subscribed;

    private final RedisClient redisClient;
    private final Duration timeout;

    private final RedisPubSubAsyncCommands<String, String> asyncPubSubSubscribeConn;
    private final RedisAsyncCommands<String, String> asyncConn;

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param redisURI The redis URI
     */
    public Redisponse(RedisURI redisURI) {
        this(Duration.ofSeconds(5), redisURI);
    }

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param timeout The cache desk timeout. This the time Redisponse will wait for a response to come in before
     *                expiring the request
     * @param redisURI The redis URI
     */
    public Redisponse(Duration timeout, RedisURI redisURI) {
        this(timeout, RedisClient.create(redisURI));
    }

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param host The redis host
     * @param port The redis port
     * @param password The redis password
     */
    public Redisponse(String host, String port, String password) {
        this(Duration.ofSeconds(5), RedisClient.create(RedisURI.builder().withHost(host).withPort(Integer.parseInt(port)).withPassword(password).build()));
    }

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param timeout The cache desk timeout. This the time Redisponse will wait for a response to come in before
     *                expiring the request
     * @param host The redis host
     * @param port The redis port
     * @param password The redis password
     */
    public Redisponse(Duration timeout, String host, String port, String password) {
        this(timeout, RedisClient.create(RedisURI.builder().withHost(host).withPort(Integer.parseInt(port)).withPassword(password).build()));
    }

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param redisClient A redis lettuce.io client
     */
    public Redisponse(RedisClient redisClient) {
        this(Duration.ofSeconds(5), redisClient);
    }

    /**
     * Constructs a new Redisponse client with the given redis arguments. For more extensive redis server args, use the
     * other constructors.
     *
     * @param timeout The cache desk timeout. This the time Redisponse will wait for a response to come in before
     *                expiring the request
     * @param redisClient A redis lettuce.io client
     */
    public Redisponse(Duration timeout, RedisClient redisClient) {
        this.responseDesk = CacheBuilder.newBuilder().expireAfterWrite(timeout).build();
        this.responseHandlers = new HashMap<>();
        this.subscribed = new HashSet<>();

        this.redisClient = redisClient;
        this.timeout = timeout;

        StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();
        connection.addListener(new IntakeDelegator(this));
        asyncPubSubSubscribeConn = connection.async();
        asyncConn = redisClient.connect().async();
    }

    /**
     * Sends a new request out to handled by a potential response handler.
     *
     * @param channel The pubsub channel to send this request to
     * @param body The body of this request
     * @return A completable future in an initial non-varying state that completes if either a response is given or the
     *          future times out. The timeout is based on the duration given in the constructor for this Redisponse.
     */
    public CompletableFuture<@Nullable String> request(String channel, String body) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseFuture.completeOnTimeout(null, this.timeout.toMillis(), TimeUnit.MILLISECONDS);
        UUID uuid = UUID.randomUUID();
        responseDesk.put(uuid, responseFuture);
        asyncConn.publish(channel, Type.REQUEST.name() + ":" +  uuid + ":" + body);
        return responseFuture;
    }

    /**
     * Registers a new "endpoint" to be handled by the intake delegator. Similar to an HTTP end point.
     *
     * @param channel The channel to be listening on.
     * @param responseHandler The response handler function. This function takes a string argument which is the body of
     *                        the request received. The string returned is the response to be sent back and completed
     *                        in the future registered to this exchange.
     */
    public void response(String channel, Function<String, String> responseHandler) {
        this.responseHandlers.put(channel, responseHandler);
        boolean notPresent = this.subscribed.add(channel);
        if (notPresent) {
            this.asyncPubSubSubscribeConn.subscribe(channel);
        }
    }

    void publish(String channel, String msg) {
        this.asyncConn.publish(channel, msg);
    }

    Optional<CompletableFuture<String>> getResponseFuture(UUID uuid) {
        return Optional.ofNullable(this.responseDesk.getIfPresent(uuid));
    }

    Optional<Function<String, String>> getResponseHandler(String channel) {
        return Optional.ofNullable(this.responseHandlers.get(channel));
    }

    enum Type {
        RESPONSE, REQUEST;

        public static Optional<Type> fromString(String s) {
            Type found = null;
            try {
                found = Type.valueOf(s);
            } catch (IllegalArgumentException ignored) {}
            return Optional.ofNullable(found);
        }
    }
}
