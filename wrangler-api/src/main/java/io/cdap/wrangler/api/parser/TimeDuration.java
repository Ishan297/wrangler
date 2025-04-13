package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class TimeDuration implements Token {

    private final String original;
    private final long millis;

    public TimeDuration(String original) {
        this.original = original;
        this.millis = parseToMilliseconds(original);
    }

    private long parseToMilliseconds(String input) {
        input = input.trim().toLowerCase();

        double value;
        long multiplier;

        if (input.endsWith("ms")) {
            value = Double.parseDouble(input.replace("ms", ""));
            multiplier = 1L;
        } else if (input.endsWith("s")) {
            value = Double.parseDouble(input.replace("s", ""));
            multiplier = 1000L;
        } else if (input.endsWith("min")) {
            value = Double.parseDouble(input.replace("min", ""));
            multiplier = 60_000L;
        } else if (input.endsWith("h")) {
            value = Double.parseDouble(input.replace("h", ""));
            multiplier = 60L * 60L * 1000L;
        } else {
            throw new IllegalArgumentException("Unsupported time unit: " + input);
        }

        return (long) (value * multiplier);
    }

    public long getMilliseconds() {
        return millis;
    }

    @Override
    public String value() {
        return original;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(original);
    }

    @Override
    public String toString() {
        return original;
    }
}
