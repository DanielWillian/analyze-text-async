package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzeServiceImplTest {
    @InjectMocks
    AnalyzeServiceImpl analyzeService;

    @Mock
    TextRepository textRepository;

    @Test
    void testAnalyzeEmptyCache() {
        when(textRepository.getOrderedText()).thenReturn(Single.just(List.of()));
        when(textRepository.getOrderedValue()).thenReturn(Single.just(List.of()));
        when(textRepository.saveText(TextCache.of("word", 60))).thenReturn(Future.succeededFuture());
        AnalyzeResponse response = analyzeService.analyze("word").blockingGet();
        assertEquals(AnalyzeResponse.of(null, null), response);
    }

    @Test
    void testAnalyzeOneSaved() {
        when(textRepository.getOrderedText()).thenReturn(Single.just(List.of("c")));
        when(textRepository.getOrderedValue()).thenReturn(Single.just(List.of(3)));
        when(textRepository.getTextsWithValue(3)).thenReturn(Single.just(List.of("c")));
        when(textRepository.saveText(TextCache.of("ab", 3))).thenReturn(Future.succeededFuture());
        AnalyzeResponse response = analyzeService.analyze("ab").blockingGet();
        assertEquals(AnalyzeResponse.of("c", "c"), response);
    }

    @ParameterizedTest
    @MethodSource("values")
    void testAnalyzeValueScenarios(List<Integer> orderedValue, Integer closerValue,
            List<String> textsWithValue, String text, Integer charValue, String closerText) {
        when(textRepository.getOrderedText()).thenReturn(Single.just(List.of("c")));
        when(textRepository.getOrderedValue()).thenReturn(Single.just(orderedValue));
        when(textRepository.getTextsWithValue(closerValue)).thenReturn(Single.just(textsWithValue));
        when(textRepository.saveText(TextCache.of(text, charValue))).thenReturn(Future.succeededFuture());
        AnalyzeResponse response = analyzeService.analyze(text).blockingGet();
        assertEquals(AnalyzeResponse.of(closerText, "c"), response);
    }

    static Stream<Arguments> values() {
        return Stream.of(
            arguments(List.of(3, 8), 8, List.of("dd"), "h", 8, "dd"),
            arguments(List.of(3, 8), 8, List.of("dd"), "f", 6, "dd"),
            arguments(List.of(3, 8), 8, List.of("dd"), "j", 10, "dd"),
            arguments(List.of(3, 8), 3, List.of("aaa"), "e", 5, "aaa"),
            arguments(List.of(3, 8), 3, List.of("aaa"), "a", 1, "aaa"),
            arguments(List.of(3, 7), 7, List.of("dc"), "e", 5, "dc"),
            arguments(List.of(7), 7, List.of("abd", "bad", "g"), "bad", 7, "abd")
        );
    }

    @ParameterizedTest
    @MethodSource("lexical")
    void testAnalyzeLexicalScenarios(List<String> orderedText, String text,
        Integer value, String closerText) {
        when(textRepository.getOrderedText()).thenReturn(Single.just(orderedText));
        when(textRepository.getOrderedValue()).thenReturn(Single.just(List.of(3)));
        when(textRepository.getTextsWithValue(3)).thenReturn(Single.just(List.of("c")));
        when(textRepository.saveText(TextCache.of(text, value))).thenReturn(Future.succeededFuture());
        AnalyzeResponse response = analyzeService.analyze(text).blockingGet();
        assertEquals(AnalyzeResponse.of("c", closerText), response);
    }

    static Stream<Arguments> lexical() {
        return Stream.of(
            arguments(List.of("ab", "cde"), "ab", 3, "ab"),
            arguments(List.of("ab", "cde"), "cde", 12, "cde"),
            arguments(List.of("bdf", "egi"), "cd", 7, "bdf"),
            arguments(List.of("bdf", "egi"), "dc", 7, "egi"),
            arguments(List.of("b", "d", "h"), "a", 1, "b"),
            arguments(List.of("a", "d", "h"), "b", 2, "a"),
            arguments(List.of("a", "d", "h"), "e", 5, "d"),
            arguments(List.of("a", "d", "h"), "f", 6, "h"),
            arguments(List.of("a", "ddd", "hhh"), "fff", 18, "hhh"),
            arguments(List.of("a", "dde", "hhh"), "fff", 18, "dde"),
            arguments(List.of("a", "ddd", "hhi"), "fff", 18, "ddd"),
            arguments(List.of("a", "d", "h"), "g", 7, "h"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abab", 6, "abbc"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abcd", 10, "abbc"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abde", 12, "abef"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abfg", 16, "abef"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abgh", 18, "abhi"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abij", 22, "abhi"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "abjk", 24, "abkl"),
            arguments(List.of("abbc", "abef", "abhi", "abkl"), "ablm", 28, "abkl")
        );
    }
}
