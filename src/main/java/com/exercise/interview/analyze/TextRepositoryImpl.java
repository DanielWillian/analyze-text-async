package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Slf4j
public class TextRepositoryImpl implements TextRepository {
    private final Set<TextCache> textCacheSet = new HashSet<>();

    @Override
    public Flowable<TextCache> getTexts() {
        return Flowable.fromIterable(textCacheSet);
    }

    @Override
    public Completable saveText(Single<TextCache> text) {
        text.doOnSuccess(t -> {
            log.info("Saving text: {}", t);

            textCacheSet.add(t);
        })
                .subscribe();

        return Completable.fromSingle(text);
    }
}
