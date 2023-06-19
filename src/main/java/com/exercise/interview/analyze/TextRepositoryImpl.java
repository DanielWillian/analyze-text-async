package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@AllArgsConstructor
@Slf4j
public class TextRepositoryImpl implements TextRepository {
    private final SqlClient sqlClient;
    private final Set<Integer> valueOrdered = new ConcurrentSkipListSet<>();
    private final Map<Integer, Set<String>> valueTextMap = new ConcurrentHashMap<>();
    private final Set<String> textOrdered = new ConcurrentSkipListSet<>();

    @Override
    public Future<Void> loadTexts() {
        return sqlClient.query("SELECT txt, value FROM Texts")
                .execute()
                .flatMap(this::fromRows)
                .onFailure(t -> log.error("Could not load texts", t));
    }

    private Future<Void> fromRows(RowSet<Row> rows) {
        log.info("Received rows");

        return Future.future(p -> setCache(p, rows));
    }

    private void setCache(Promise<Void> promise, RowSet<Row> rows) {
        Completable.fromAction(() -> setCache(rows))
                .subscribeOn(Schedulers.computation())
                .subscribe(promise::complete, promise::fail);
    }

    private void setCache(RowSet<Row> rows) {
        log.info("Clearing cache");

        valueOrdered.clear();
        textOrdered.clear();

        rows.forEach(this::fromRow);

        log.info("Cache loaded");
    }

    private void fromRow(Row row) {
        String text = row.getString("txt");
        int charValue = row.getInteger("value");
        TextCache textCache = TextCache.of(text, charValue);
        cacheText(textCache);
        textOrdered.add(text);
    }

    private void cacheText(TextCache textCache) {
        valueOrdered.add(textCache.getCharValue());
        valueTextMap.putIfAbsent(textCache.getCharValue(), new ConcurrentSkipListSet<>());
        valueTextMap.get(textCache.getCharValue()).add(textCache.getText());
    }


    @Override
    public Single<List<String>> getOrderedText() {
        return Single.<List<String>>fromCallable(() -> new ArrayList<>(textOrdered))
                .subscribeOn(Schedulers.computation());
    }

    @Override
    public Single<List<Integer>> getOrderedValue() {
        return Single.<List<Integer>>fromCallable(() -> new ArrayList<>(valueOrdered))
                .subscribeOn(Schedulers.computation());
    }

    @Override
    public Single<List<String>> getTextsWithValue(int value) {
        return Single.<List<String>>fromCallable(() -> new ArrayList<>(valueTextMap.get(value)))
                .subscribeOn(Schedulers.computation());
    }

    @Override
    public Future<Void> saveText(TextCache text) {
        Promise<Void> promise = Promise.promise();
        if (!textOrdered.contains(text.getText())) {
            log.info("Saving text: {}", text);
            sqlClient.preparedQuery("INSERT INTO Texts (txt, value) VALUES ($1, $2)")
                .execute(Tuple.of(text.getText(), text.getCharValue()))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        log.info("Saved text: {}", text);
                        cacheText(text);
                        textOrdered.add(text.getText());
                        promise.complete();
                    } else {
                        log.info("Could not save text: {}", text);
                        promise.fail(ar.cause());
                    }
                });
        } else {
            log.info("Text already saved: {}", text);
            promise.fail("Already exists!");
        }

        return promise.future();
    }
}
