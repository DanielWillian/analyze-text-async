package com.exercise.interview.analyze;

import lombok.Value;

@Value(staticConstructor = "of")
public class TextLexical {
    String text;
    int[] distance;
}
