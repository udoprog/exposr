package eu.toolchain.exposr.http;

import lombok.Getter;

public class ErrorMessage {
    @Getter
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}