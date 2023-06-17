package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class AnalyzeServiceImpl implements AnalyzeService {
    private final TextRepository textRepository;

    @Override
    public Single<AnalyzeResponse> analyze(Single<String> text) {
        return text.flatMap(t -> analyzeInternal(t).subscribeOn(Schedulers.computation()));
    }

    private Single<AnalyzeResponse> analyzeInternal(String text) {
        String lowerCase = text.toLowerCase();
        log.info("Analyzing text: {}", lowerCase);

        int charValue = charValue(lowerCase);

        Maybe<TextCacheComparison> lexical = textRepository.getTexts()
                .map(t -> TextCacheComparison.of(t, text.compareTo(t.getText())))
                .reduce((lhs, rhs) -> closerLexical(lhs, rhs) ? lhs : rhs);

        Maybe<TextCacheComparison> value = textRepository.getTexts()
                .map(t -> TextCacheComparison.of(t, Math.abs(t.getCharValue() - charValue)))
                .reduce((lhs, rhs) -> closerValue(lhs, rhs) ? lhs : rhs);

        return Maybe.zip(value, lexical, (TextCacheComparison v, TextCacheComparison l) -> {
                log.info("Text {} has closest value {} with distance {} and closest lexical {} with distance {}",
                    lowerCase,
                    v.getTextCache().getText(),
                    v.getComparison(),
                    l.getTextCache().getText(),
                    l.getComparison());

                return AnalyzeResponse.of(v.getTextCache().getText(), l.getTextCache().getText());
            })
                .defaultIfEmpty(AnalyzeResponse.of(null, null))
                .doOnSuccess(r -> {
                    log.info("Response: {}", r);

                    textRepository.saveText(Single.just(TextCache.of(lowerCase, charValue)));
                });
    }

    private static boolean closerLexical(TextCacheComparison lhs, TextCacheComparison rhs) {
        int lhsAbs = Math.abs(lhs.getComparison());
        int rhsAbs = Math.abs(rhs.getComparison());

        if (lhsAbs == rhsAbs) {
            return lhs.getTextCache().getText().compareTo(rhs.getTextCache().getText()) > 0;
        }

        return lhsAbs < rhsAbs;
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
