package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;

public interface AnalyzeService {
    Single<AnalyzeResponse> analyze(Single<String> text);
}
