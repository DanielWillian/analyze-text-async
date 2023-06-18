package com.exercise.interview.analyze;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@AllArgsConstructor
@Slf4j
public class TextRepositoryImpl implements TextRepository {
    private final SqlClient sqlClient;
    private final Set<TextCache> textCacheSet = new ConcurrentHashSet<>();

    @Override
    public Future<Void> loadTexts() {
        return sqlClient.query("SELECT txt, value FROM Texts")
            .execute()
            .onFailure(t -> log.error("Can not execute query", t))
            .onSuccess(this::fromRows)
            .mapEmpty();
    }

    private void fromRows(RowSet<Row> rows) {
        log.info("Clearing cache");

        textCacheSet.clear();

        rows.forEach(this::fromRow);
    }

    private void fromRow(Row row) {
        String text = row.getString("txt");
        int charValue = row.getInteger("value");
        TextCache textCache = TextCache.of(text, charValue);
        log.info("Adding cache: {}", textCache);
        textCacheSet.add(textCache);
    }

    @Override
    public Single<Set<TextCache>> getTexts() {
        return Single.just(textCacheSet);
    }

    @Override
    public Future<Void> saveText(Single<TextCache> text) {
        Promise<Void> promise = Promise.promise();
        text.doOnSuccess(t -> {
            if (!textCacheSet.contains(t)) {
                log.info("Saving text: {}", t);
                sqlClient.preparedQuery("INSERT INTO Texts (txt, value) VALUES ($1, $2)")
                    .execute(Tuple.of(t.getText(), t.getCharValue()))
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            log.info("Saved text: {}", t);
                            textCacheSet.add(t);
                            promise.complete();
                        } else {
                            log.info("Could not save text: {}", t);
                            promise.fail(ar.cause());
                        }
                    });
            } else {
                log.info("Text already saved: {}", t);
                promise.fail("Already exists!");
            }
        })
                .doOnError(promise::fail)
                .subscribe();

        return promise.future();
    }
}
