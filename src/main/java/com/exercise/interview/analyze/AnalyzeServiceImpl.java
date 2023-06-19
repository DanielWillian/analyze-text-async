package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
        log.info("Analyzing text: {}", text);

        int charValue = charValue(text);

        Maybe<String> closestLexical = textRepository.getOrderedText()
                .flatMapMaybe(l -> closestLexical(text, l));

        Maybe<String> closestValue = textRepository.getOrderedValue()
                .flatMapMaybe(l -> closestValue(charValue, l));

        return Maybe.zip(closestValue, closestLexical, (String v, String l) -> {
                    log.info("Text {} has closest value {} and closest lexical {}", text, v, l);

                    return AnalyzeResponse.of(v, l);
                })
                .defaultIfEmpty(AnalyzeResponse.of(null, null))
                .doOnSuccess(r -> {
                    log.info("Response: {}", r);

                    textRepository.saveText(Single.just(TextCache.of(text, charValue)))
                        .onFailure(t -> log.error("Could not save text: " + text, t));
                });
    }

    private static Maybe<String> closestLexical(String text, List<String> texts) {
        if (texts.isEmpty()) return Maybe.empty();

        return Maybe.fromCallable(() -> {
            int start = 0;
            int end = texts.size() - 1;

            while (start <= end) {
                int mid = (start + end) / 2;

                int compare = text.compareTo(texts.get(mid));
                if (compare == 0) return texts.get(mid);
                else if (compare < 0) end = mid - 1;
                else start = mid + 1;
            }

            return texts.get(start);
        })
                .subscribeOn(Schedulers.computation());
    }

    private Maybe<String> closestValue(int charValue, List<TextCache> texts) {
        if (texts.isEmpty()) return Maybe.empty();

        return Maybe.fromCallable(() -> {
            int start = 0;
            int end = texts.size() - 1;

            while (start <= end) {
                int mid = (start + end) / 2;

                if (charValue == texts.get(mid).getCharValue()) {
                    end = mid - 1;
                    start = mid;
                } else if (charValue < texts.get(mid).getCharValue()) end = mid - 1;
                else start = mid + 1;
            }

            while (start > 0 && texts.get(start).getCharValue() == texts.get(start - 1).getCharValue()) start--;

            return texts.get(start).getText();
        })
            .subscribeOn(Schedulers.computation());
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
