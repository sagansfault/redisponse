package sh.sagan.redisponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Redisponse {

    private final Cache<UUID, CompletableFuture<String>> responseDesk;
    private final Map<String, Function<String, String>> responseHandlers;

    private final RedisClient redisClient;

    private final RedisPubSubAsyncCommands<String, String> asyncPubSubSubscribeConn;
    private final RedisAsyncCommands<String, String> asyncConn;

    public Redisponse(String host, String port, String password) {
        this(Duration.ofSeconds(5), RedisClient.create(RedisURI.builder().withHost(host).withPort(Integer.parseInt(port)).withPassword(password).build()));
    }

    public Redisponse(Duration timeout, String host, String port, String password) {
        this(timeout, RedisClient.create(RedisURI.builder().withHost(host).withPort(Integer.parseInt(port)).withPassword(password).build()));
    }

    public Redisponse(RedisClient redisClient) {
        this(Duration.ofSeconds(5), redisClient);
    }

    public Redisponse(Duration timeout, RedisClient redisClient) {
        this.responseDesk = CacheBuilder.newBuilder().expireAfterWrite(timeout).build();
        this.responseHandlers = new HashMap<>();
        this.redisClient = redisClient;

        StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();
        connection.addListener(new IntakeDelegator(this));
        asyncPubSubSubscribeConn = connection.async();
        asyncConn = redisClient.connect().async();
    }

    Optional<CompletableFuture<String>> getResponseFuture(UUID uuid) {
        return Optional.ofNullable(this.responseDesk.getIfPresent(uuid));
    }

    Optional<Function<String, String>> getResponseHandler(String channel) {
        return Optional.ofNullable(this.responseHandlers.get(channel));
    }

    public CompletableFuture<String> request(String channel, String body) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        UUID uuid = UUID.randomUUID();
        responseDesk.put(uuid, responseFuture);
        asyncConn.publish(channel, Type.REQUEST.name() + ":" +  uuid + ":" + body);
        return responseFuture;
    }

    public void response(String channel, Function<String, String> responseHandler) {
        this.responseHandlers.put(channel, responseHandler);
        this.asyncPubSubSubscribeConn.subscribe(channel);
    }

    void publish(String channel, String msg) {
        this.asyncConn.publish(channel, msg);
    }
}
