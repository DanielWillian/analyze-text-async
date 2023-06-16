package com.exercise.interview.analyze;

import lombok.Value;

@Value(staticConstructor = "of")
public class TextCacheComparison {
    TextCache textCache;
    int comparison;
}
