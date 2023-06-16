package com.exercise.interview.analyze;

import lombok.Value;

@Value(staticConstructor = "of")
public class AnalyzeResponse {
    String value;
    String lexical;
}
