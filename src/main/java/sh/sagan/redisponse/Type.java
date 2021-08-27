package sh.sagan.redisponse;

import java.util.Optional;

public enum Type {
    RESPONSE, REQUEST;

    public static Optional<Type> fromString(String s) {
        Type found = null;
        try {
            found = Type.valueOf(s);
        } catch (IllegalArgumentException ignored) {}
        return Optional.ofNullable(found);
    }
}
