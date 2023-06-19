package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;

import java.util.List;
import java.util.Set;

public interface TextRepository {
    Future<Void> loadTexts();

    Single<List<String>> getOrderedText();

    Single<List<TextCache>> getOrderedValue();

    Future<Void> saveText(Single<TextCache> text);
}
