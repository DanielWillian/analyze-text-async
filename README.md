# analyze-text-async

analyze-text-async is a demo app exposing REST API for analyzing texts using Vert.x

## Explanation

The app exposes an endpoint at `/analyze` which expects a JSON object with a string property `text` containing letters. The endpoint returns a json object with two string properties containing one of the previously sent `texts`:

- `value`: the text closest in terms of the sum of all character values of the text, where character values are listed as `a = 1`, `b = 2` and so on, in a non-sensitive case way. If there are ties, the higher value is picked. If there are ties again, the higher sorted in ascendent lexical order is picked.
- `lexical`: the text closest in terms of lexical closeness, in a non-sensitive case way. If there are ties, the lower sorted in ascendent lexical order is picked.

The server persists the texts to a postgres database. It does so after it returns the result, for better latency, it also means the text may not be persisted.

The server caches the texts already sent, so it does not go to the database in a request. Persisted texts are loaded on start up.

The closest value and lexical texts are calculated concurrently, outside the verticle event loop. Binary search are used to find them.

## Building
To build the app run `mvn clean package`. This creates a fat jar at `./target`.

## Running
The server uses the following environment variables:

- `PORT`: port the server listens. Default `8888`.
- `PGHOST`: host of postgres database. Default `localhost`.
- `PGPORT`: port of postgres database. Default `5432`.
- `PGDATABASE`: database of postgres database. Default `postgres`.
- `PGUSER`: user of postgres database. Default `postgres`.
- `PGPASSWORD`: password of postgres database. Default `postgres`.
- `PGCONNECTIONS`: number of pooled connections. Default `20`.

After the database is up, run the fat jar with `java -jar target/*-fat.jar` or `mvn exec:java` to start the server.

There is `docker-compose.yaml` that sets up a postgres database, exposes it at port 5432, starts the server and exposes it at port 8888. Run `docker-compose up` to deploy.

## Testing
To test the API, send requests to `/analyze`, for example using curl:

```
$ curl localhost:8888/analyze -d '{"text":"word"}'
```

