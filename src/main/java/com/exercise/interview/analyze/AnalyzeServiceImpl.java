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
    public Single<AnalyzeResponse> analyze(String text) {
        int charValue = charValue(text);

        Maybe<String> closestLexical = textRepository.getOrderedText()
                .flatMapMaybe(l -> closestLexical(text, l));

        Maybe<String> closestValue = textRepository.getOrderedValue()
                .flatMapMaybe(l -> closestValue(charValue, l));

        return Maybe.zip(closestValue, closestLexical, AnalyzeResponse::of)
                .defaultIfEmpty(AnalyzeResponse.of(null, null))
                .doOnSuccess(r -> {
                    log.debug("text: {}, response: {}", text, r);

                    textRepository.saveText(TextCache.of(text, charValue))
                            .onFailure(t -> log.error("Could not save text: " + text, t));
                });
    }

    private static Maybe<String> closestLexical(String text, List<String> texts) {
        if (texts.isEmpty()) return Maybe.empty();
        if (texts.size() == 1) return Maybe.just(texts.get(0));

        return Maybe.fromCallable(() -> {
            int start = 0;
            int end = texts.size() - 1;

            while (start <= end) {
                int mid = (start + end) / 2;

                int compare = text.compareToIgnoreCase(texts.get(mid));
                if (compare == 0) return texts.get(mid);
                else if (compare < 0) end = mid - 1;
                else start = mid + 1;
            }

            int lhs = Math.max(0, start - 1);
            int rhs = Math.min(texts.size() - 1, start);
            int[] lhsDist = calcDist(text, texts.get(lhs));
            int[] rhsDist = calcDist(text, texts.get(rhs));

            return isCloser(lhsDist, rhsDist) ? texts.get(lhs) : texts.get(rhs);
        })
                .subscribeOn(Schedulers.computation());
    }

    private static int[] calcDist(String lhs, String rhs) {
        int[] result = new int[Math.min(lhs.length(), rhs.length())];

        for (int i = 0; i < result.length; i++) {
            result[i] = Math.abs(Character.toLowerCase(lhs.charAt(i)) - Character.toLowerCase(rhs.charAt(i)));
        }

        return result;
    }

    private static boolean isCloser(int[] lhs, int[] rhs) {
        int minSize = Math.min(lhs.length, rhs.length);

        for (int i = 0; i < minSize; i++) {
            if (lhs[i] < rhs[i]) return true;
            else if (lhs[i] > rhs[i]) return false;
        }

        return false;
    }

    private Maybe<String> closestValue(int charValue, List<Integer> values) {
        if (values.isEmpty()) return Maybe.empty();

        return Maybe.fromCallable(() -> {
            if (values.size() == 1) return values.get(0);

            int start = 0;
            int end = values.size() - 1;

            while (start <= end) {
                int mid = (start + end) / 2;

                if (charValue == values.get(mid)) return values.get(mid);
                else if (charValue < values.get(mid)) end = mid - 1;
                else start = mid + 1;
            }

            int lhs = Math.max(0, start - 1);
            int rhs = Math.min(values.size() - 1, start);
            return Math.abs(values.get(rhs) - charValue) <= Math.abs(values.get(lhs) - charValue) ?
                    values.get(rhs) : values.get(lhs);
        })
                .subscribeOn(Schedulers.computation())
                .flatMapSingle(textRepository::getTextsWithValue)
                .map(l -> l.get(0));
    }

    private static int charValue(String text) {
        int result = 0;

        for (char c : text.toCharArray()) {
            if (!Character.isLetter(c)) {
                throw new InvalidRequestException("Invalid character " + c + " on text: " + text);
            }
            result += Character.compare(Character.toLowerCase(c), 'a') + 1;
        }

        return result;
    }
}
