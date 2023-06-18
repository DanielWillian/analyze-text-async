package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;

import java.util.Set;

public interface TextRepository {
    Future<Void> loadTexts();

    Single<Set<TextCache>> getTexts();

    Future<Void> saveText(Single<TextCache> text);
}
