package com.exercise.interview.analyze;

import lombok.Value;

@Value(staticConstructor = "of")
public class TextCache {
    String text;
    int charValue;
}
