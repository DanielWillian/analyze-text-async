package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface TextRepository {
    Flowable<TextCache> getTexts();

    Completable saveText(Single<TextCache> text);
}
