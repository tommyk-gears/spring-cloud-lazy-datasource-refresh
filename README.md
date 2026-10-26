# Config refresh with `spring.datasource.connection-fetch=lazy`

Reproducer for a config refresh breaking an application that uses Spring Boot's lazy connection
fetching (`spring.datasource.connection-fetch=lazy`) together with Spring Cloud Context.

Spring Boot 4.1.2, HikariCP, H2. Java 17.

## Run

```shell
./gradlew test                                   # Spring Cloud 2025.1.4 (spring-cloud-context 5.0.4)
./gradlew test -PspringCloudVersion=2025.1.3     # Spring Cloud 2025.1.3 (spring-cloud-context 5.0.3)
```

`LazyDataSourceRefreshTest` starts the application, runs a query, calls `ContextRefresher.refresh()`,
and checks that the refresh succeeds, that a query still works, and that the `refreshScope` health
indicator is UP. The same scenario runs twice:

- `Lazy` (`connection-fetch=lazy`) fails
- `Eager` (`connection-fetch=eager`) passes

## What happens with lazy fetching

| After a refresh                                    | 2025.1.3 | 2025.1.4 |
|----------------------------------------------------|----------|----------|
| The refresh throws (`POST /actuator/refresh` 500)  | yes      | yes      |
| Queries fail with `No 'targetDataSource' set`      | yes      | no       |
| `refreshScope` health indicator stays DOWN         | yes      | yes      |

The refresh fails with:

```
java.lang.IllegalArgumentException: 'existingValue' must be an instance of com.zaxxer.hikari.HikariDataSource
	at org.springframework.boot.context.properties.bind.Bindable.withExistingValue(Bindable.java:183)
	...
	at org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder.rebind(ConfigurationPropertiesRebinder.java:220)
```

None of this happens with eager fetching.

Why queries only fail on 2025.1.3: before rebinding, `ConfigurationPropertiesRebinder` resets the
bean to the defaults of a fresh instance. With lazy fetching, the `dataSource` bean is a
`LazyConnectionDataSourceProxy`, whose fresh instance has no target. spring-cloud-context 5.0.3 does
this reset for any class with a no-arg constructor, so the proxy's target becomes `null`. 5.0.4 only
does it for classes whose only constructor takes no arguments (gh-1733), and
`LazyConnectionDataSourceProxy` also has a `LazyConnectionDataSourceProxy(DataSource)` constructor,
so the reset is skipped and the target is kept. With
`logging.level.org.springframework.cloud.context.properties=DEBUG`, 5.0.4 logs
`No default constructor for org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy; skipping property reset before rebinding`.
On both versions, binding the Hikari properties onto the proxy then fails.

## Manually

```shell
./gradlew bootRun
curl localhost:8080/query                     # 1
curl -X POST localhost:8080/actuator/refresh  # 500
curl localhost:8080/actuator/health           # refreshScope DOWN
```
