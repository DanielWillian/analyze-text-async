package com.exercise.interview.analyze;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException() {}

    public InvalidRequestException(String message) {
        super(message);
    }
}
