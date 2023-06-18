package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@AllArgsConstructor
@Slf4j
public class AnalyzeServiceImpl implements AnalyzeService {
    private final TextRepository textRepository;

    @Override
    public Single<AnalyzeResponse> analyze(Single<String> text) {
        return text.map(String::toLowerCase)
                .flatMap(this::analyzeInternal);
    }

    private Single<AnalyzeResponse> analyzeInternal(String text) {
        return textRepository.getTexts()
                .flatMap(s -> analyzeWithTexts(text, s).subscribeOn(Schedulers.computation()));
    }

    private Single<AnalyzeResponse> analyzeWithTexts(String text, Set<TextCache> texts) {
        log.info("Analyzing text: {}", text);

        int charValue = charValue(text);

        Maybe<TextLexical> lexical = Flowable.fromIterable(texts)
                .parallel()
                .map(t -> TextLexical.of(t.getText(), calcLexicalDistance(text, t.getText())))
                .reduce(AnalyzeServiceImpl::closerLexical)
                .reduce(AnalyzeServiceImpl::closerLexical);

        Maybe<TextCacheComparison> value = Flowable.fromIterable(texts)
                .parallel()
                .map(t -> TextCacheComparison.of(t, Math.abs(t.getCharValue() - charValue)))
                .reduce((lhs, rhs) -> closerValue(lhs, rhs) ? lhs : rhs)
                .reduce((lhs, rhs) -> closerValue(lhs, rhs) ? lhs : rhs);

        return Maybe.zip(value, lexical, (TextCacheComparison v, TextLexical l) -> {
                log.info("Text {} has closest value {} with distance {} and closest lexical {} with distance {}",
                    text,
                    v.getTextCache().getText(),
                    v.getComparison(),
                    l.getText(),
                    l.getDistance());

                return AnalyzeResponse.of(v.getTextCache().getText(), l.getText());
            })
                .defaultIfEmpty(AnalyzeResponse.of(null, null))
                .doOnSuccess(r -> {
                    log.info("Response: {}", r);

                    textRepository.saveText(Single.just(TextCache.of(text, charValue)));
                });
    }

    private static int[] calcLexicalDistance(String lhs, String rhs) {
        int size = Math.min(lhs.length(), rhs.length());
        int[] distance = new int[size];

        for (int i = 0; i < size; i++) {
            distance[i] = Math.abs(lhs.charAt(i) - rhs.charAt(i));
        }

        return distance;
    }

    private static TextLexical closerLexical(TextLexical lhs, TextLexical rhs) {
        int size = Math.min(lhs.getDistance().length, rhs.getDistance().length);

        for (int i = 0; i < size; i++) {
            if (lhs.getDistance()[i] == rhs.getDistance()[i]) continue;

            return lhs.getDistance()[i] < rhs.getDistance()[i] ? lhs : rhs;
        }

        return lhs.getText().compareTo(rhs.getText()) > 0 ? lhs : rhs;
    }

    private static boolean closerValue(TextCacheComparison lhs, TextCacheComparison rhs) {
        int lhsAbs = Math.abs(lhs.getComparison());
        int rhsAbs = Math.abs(rhs.getComparison());

        if (lhsAbs == rhsAbs) {
            if (lhs.getTextCache().getCharValue() == rhs.getTextCache().getCharValue()) {
                return lhs.getTextCache().getText().compareTo(rhs.getTextCache().getText()) < 0;
            }

            return lhs.getTextCache().getCharValue() > rhs.getTextCache().getCharValue();
        }

        return lhsAbs < rhsAbs;
    }

    private static int charValue(String text) {
        int result = 0;

        for (char c : text.toCharArray()) {
            if (!Character.isLetter(c)) {
                throw new IllegalArgumentException("Invalid character " + c + " on text: " + text);
            }
            result += Character.compare(c, 'a') + 1;
        }

        return result;
    }
}
