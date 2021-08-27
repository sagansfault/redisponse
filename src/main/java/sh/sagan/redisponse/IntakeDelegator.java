package sh.sagan.redisponse;

import io.lettuce.core.pubsub.RedisPubSubListener;

import java.util.UUID;

public class IntakeDelegator implements RedisPubSubListener<String, String>  {

    private final Redisponse redisponse;

    public IntakeDelegator(Redisponse redisponse) {
        this.redisponse = redisponse;
    }

    @Override
    public void message(String channel, String message) {
        String[] parts = message.split(":", 3);
        if (parts.length != 3) {
            return;
        }
        String typeString = parts[0];
        String uuidString = parts[1];
        String body = parts[2];

        Redisponse.Type.fromString(typeString).ifPresent(type -> {
            switch (type) {
                case RESPONSE -> {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        redisponse.getResponseFuture(uuid).ifPresent(future -> future.complete(body));
                    } catch (IllegalArgumentException ignored) {}
                }
                case REQUEST -> redisponse.getResponseHandler(channel).ifPresent(handler -> {
                    String response = handler.apply(body);
                    redisponse.publish(channel, Redisponse.Type.RESPONSE.name() + ":" + uuidString + ":" + response);
                });
            }
        });
    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }
}
