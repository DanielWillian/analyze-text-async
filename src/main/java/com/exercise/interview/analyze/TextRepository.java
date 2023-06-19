package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;

import java.util.List;

public interface TextRepository {
    Future<Void> loadTexts();

    Single<List<String>> getOrderedText();

    Single<List<Integer>> getOrderedValue();

    Single<List<String>> getTextsWithValue(int value);

    Future<Void> saveText(TextCache text);
}
