package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;

public interface TextRepository {
    Future<Void> loadTexts();

    Flowable<TextCache> getTexts();

    Future<Void> saveText(Single<TextCache> text);
}
