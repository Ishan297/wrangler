package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ByteSize implements Token {

    private final String original;
    private final long bytes;

    public ByteSize(String original) {
        this.original = original;
        this.bytes = parseToBytes(original);
    }

    private long parseToBytes(String input) {
        input = input.trim().toUpperCase();

        double value;
        long multiplier;

        if (input.endsWith("KB")) {
            value = Double.parseDouble(input.replace("KB", ""));
            multiplier = 1024L;
        } else if (input.endsWith("MB")) {
            value = Double.parseDouble(input.replace("MB", ""));
            multiplier = 1024L * 1024;
        } else if (input.endsWith("GB")) {
            value = Double.parseDouble(input.replace("GB", ""));
            multiplier = 1024L * 1024 * 1024;
        } else if (input.endsWith("B")) {
            value = Double.parseDouble(input.replace("B", ""));
            multiplier = 1L;
        } else {
            throw new IllegalArgumentException("Unsupported byte size unit: " + input);
        }

        return (long) (value * multiplier);
    }

    public long getBytes() {
        return bytes;
    }

    @Override
    public String value() {
        return original;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
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
