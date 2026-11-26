# Дизайн Kafka-слоя auto-testing (BATM)

Статус: предложение, код не написан. Ревизия кода, по которой составлен документ: ветка `claude/remove-unused-code`.
Номера строк ниже — по этой ревизии.

## 0. Текущее состояние (что есть и что мешает)

| Компонент | Сейчас | Проблема |
|---|---|---|
| `KafkaCluster` | enum BATM/UASP/ODS, хранит только ключ bootstrap (`bootstrap.servers`, `kafka.uasp.…`, `kafka.ods.…`); SSL у всех общий из `EnvConstants` | Добавление кластера требует правки `EnvConstants` и `switch` в `getBootstrapServers()`; SSL нельзя задать для кластера отдельно |
| SSL | `ssl.truststore.type/password`, `ssl.keystore.type/password` без префикса; пути **не** в properties, а вычисляются: `kafka/<cenv>-batm-{truststore,keystore}.p12` | — |
| `KafkaTopic` | одно хранилище `Map<Long, String>` (offset → value) на топик | ключ только offset (у разных партиций одинаковые offset затирают друг друга); нет key/headers/timestamp; нет лимитов |
| `KafkaClient.startReadingTopics` | `subscribe` + `auto.offset.reset=earliest` + случайный group.id; поток из `Executors.newSingleThreadExecutor()` (не daemon); ошибка → `readingErrors` по кластеру | читается вся история топиков; поток держит JVM (отсюда `System.exit` в `TestsRunner`); ничего не закрывается |
| `KafkaHelper.get*Record` | поиск по подстрокам (`compilePredicate`: `value.contains(k)` для каждого ключевого слова); найденное **удаляется** (`current.remove`); FIRST/LAST по offset; `waitForMessage` глотает таймаут | ложные совпадения по подстроке (сумма, 8-значный id, имя файла); удаление делает тесты зависимыми от порядка вызовов; при таймауте нет диагностики |
| `KafkaHelper.sendMessage` | **всегда** через `BATM_KAFKA`, **новый `KafkaProducer` на каждое сообщение** | топики UASP/ODS отправлялись бы в BATM; дорогое создание продюсера |
| `KafkaHelper.getLastRawRecord` | **в текущем коде отсутствует**: добавлен в `917d007`, удалён коммитом `db61937 «micro fix»` | тем же коммитом из `BatmDpStreamingTests` удалены `jsonFieldsEqual`/`fields`, а `checkUaspInstructionMessage`, `checkUaspTransactionMessage`, `checkOdsOutboxMessage` закомментированы — шаги UASP/ODS в BATM-658…665 сейчас **ничего не проверяют**. Исходники для восстановления: `git show ffecc53:src/test/java/ru/vtb/api/tests/batm_dp/BatmDpStreamingTests.java` (строки 450–484) |

Вызовов `KafkaHelper.get*` в неархивных тестах — 75 в 11 файлах (пакета архива в репозитории нет).

## 1. Конфигурация кластеров (требование 1)

### 1.1 Ключи

```properties
kafka.<имя>.bootstrap.servers=host1:9092,host2:9092      # обязателен; нет/пусто — кластер выключен
kafka.<имя>.ssl.truststore.type=PKCS12                    # необязательно
kafka.<имя>.ssl.truststore.password=<зашифровано -Dekey>  # необязательно
kafka.<имя>.ssl.truststore.location=kafka/test-uasp-truststore.p12   # необязательно, путь от src/main/resources
kafka.<имя>.ssl.keystore.type=PKCS12
kafka.<имя>.ssl.keystore.password=<зашифровано>
kafka.<имя>.ssl.keystore.location=kafka/test-uasp-keystore.p12
```

- `<имя>` = `KafkaCluster.name().toLowerCase()` (`batm`, `uasp`, `ods`).
- Любой `kafka.<имя>.ssl.*`, которого нет, берётся из `kafka.batm.ssl.*` — как сейчас (общие SSL-настройки).
- `location` по умолчанию — текущее правило `kafka/<cenv>-batm-{truststore,keystore}.p12`; это поведение для BATM
  не меняется, файлы хранилищ не переименовываются.
- Пароли читаются через `EnvConstants.getEncryptedProperty` (расшифровка `-Dekey`), остальные — `getProperty`.

### 1.2 Классы

- `KafkaCluster` — только константы (`BATM, UASP, ODS`), без ключей и `switch`. Добавление кластера:
  строки в properties + константа в `KafkaCluster` + топики в `KafkaTopic` (с `KafkaCluster.X`).
- `KafkaClusterConfig` (новый, `ru.vtb.core.utils.kafka`): `static KafkaClusterConfig of(KafkaCluster)` — читает ключи
  с fallback, `isEnabled()`, `toClientProperties()` (bootstrap + SSL + сериализаторы — то, что сейчас собирает
  конструктор `KafkaClient`). Ошибка конфигурации (задан `location`, а файла нет; пароль задан, а тип нет) —
  исключение при старте с именем ключа.
- `EnvConstants.BOOTSTRAP_SERVERS`, `SSL_*`, `UASP_/ODS_BOOTSTRAP_SERVERS` удаляются после перехода (см. 1.3).

### 1.3 Переход основного кластера на `kafka.batm.*`

Релиз N (переходный):
- читаем `kafka.batm.X`; если его нет — старый ключ `X` (`bootstrap.servers`, `ssl.truststore.type`, …)
  и пишем WARNING «устаревший ключ 'X', переименуйте в 'kafka.batm.X'»;
- заданы оба и значения различаются — ошибка старта (защита от полуобновлённого файла);
- `test.properties` в репозитории переводим сразу.

Релиз N+1: fallback на старые ключи удаляется.

Инструкция для preprod (выполняется на контуре, значения не меняются — они зашифрованы тем же ключом):

```bash
sed -i -E 's/^(bootstrap\.servers|ssl\.(truststore|keystore)\.(type|password))=/kafka.batm.\1=/' preprod.properties
```

```bash
grep -nE '^(kafka\.batm\.)?(bootstrap\.servers|ssl\.)' preprod.properties
```

Ожидаемый результат второй команды — 5 строк, все с префиксом `kafka.batm.`:
`bootstrap.servers`, `ssl.truststore.type`, `ssl.truststore.password`, `ssl.keystore.type`, `ssl.keystore.password`.
Строки `kafka.uasp.*`/`kafka.ods.*` команда не трогает. Если до релиза N+1 файл не обновили — прогон работает,
но в логе WARNING.

## 2. Жизненный цикл: один consumer и один producer на кластер (требование 2)

`KafkaClusterRuntime` (новый) — по одному на включённый кластер, реестр `KafkaRuntimeRegistry` (статический, потокобезопасный).

- **Consumer**: один поток на кластер, daemon, имя `kafka-consumer-<имя>` (своя `ThreadFactory`). Цикл `poll(200 ms)`
  → запись в хранилище (раздел 3). Ошибка — как сейчас: в `readingErrors` по кластеру с полным stack trace,
  поток завершается, тесты этого кластера падают с этой ошибкой, остальные работают.
- **Producer**: один `KafkaProducer` на кластер, создаётся лениво при первой отправке (потокобезопасен, переиспользуется).
  `sendMessage(topic, …)` берёт продюсер по `topic.getCluster()` (сейчас всегда BATM). Отправка синхронная
  (`send(...).get(timeout)`), ошибка — `Assert.fail` с причиной, как сейчас.
- **Старт**: `TestNGListener.onStart(ISuite)` (как сейчас `initKafkaConsumers`).
- **Закрытие**: `TestNGListener.onFinish(ISuite)` → для каждого runtime `consumer.wakeup()`, `join(5 s)`,
  `consumer.close()` в самом потоке, `producer.close(Duration.ofSeconds(10))`. Если в одной JVM несколько сьютов —
  счётчик активных сьютов, закрытие при выходе последнего.
- После этого потоки Kafka не держат JVM. `System.exit(testNG.getStatus())` в `TestsRunner` остаётся — только так
  задаётся код возврата, — но перестаёт быть единственным способом завершить процесс с висящими потоками.
  Итерация 2: проверить, что не-daemon потоков не оставляют и другие клиенты (S3, JDBC), и закрыть их в `onFinish`.

## 3. Хранилище сообщений (требование 3)

```java
record KafkaMessage(String topic, int partition, long offset,
                    String key, Map<String, String> headers,   // значения заголовков — UTF-8 строки
                    long timestamp, TimestampType timestampType,
                    String value, Instant receivedAt) {}
record MessageId(String topic, int partition, long offset) {}
```

- `TopicStore` на каждый `KafkaTopic`: `ConcurrentSkipListMap<OrderKey, KafkaMessage>`, где
  `OrderKey = (timestamp, partition, offset)`; повторное получение того же `MessageId` не создаёт дубля.
- **Порядок FIRST/LAST — по timestamp**, при равенстве — partition, затем offset. Оговорка: при `CreateTime`
  timestamp ставит продюсер сервиса; если его часы расходятся, порядок между партициями может не совпасть с реальным
  (см. вопрос 12).
- **Лимиты**: `kafka.store.max.messages.per.topic` (по умолчанию 50 000) и `kafka.store.ttl` (по умолчанию `PT60M`,
  считается от `receivedAt`). Вытеснение старейших делает consumer-поток после каждой пачки; число вытесненных
  учитывается и показывается в диагностике таймаута («вытеснено N сообщений — увеличьте лимит»).
- Только чтение: тесты получают неизменяемые `KafkaMessage`; **найденное не удаляется** (раздел 5).
- `KafkaTopic` теряет `topicMap`/`getTopicStorageByName`; доступ к хранилищу — через реестр.

## 4. Начальные позиции (требование 4)

- Без group management: `group.id` не задаётся, `enable.auto.commit=false`, `assign` вместо `subscribe`
  (ребалансов нет, коммитов нет).
- Старт runtime: `partitionsFor(topic)` для каждого топика кластера → `assign(все партиции)` → `seekToEnd(все)` →
  `position(tp)` для каждой партиции (вынуждает получить реальные позиции) → `CountDownLatch` готовности.
- `onStart` ждёт готовность всех включённых кластеров не дольше `kafka.startup.timeout` (по умолчанию `PT60S`).
  Таймаут или ошибка → ошибка кластера в `readingErrors` с понятным текстом, например:
  «Kafka-кластер UASP: за 60 с не зафиксированы позиции партиций [topicA-0, topicA-3]; топик topicB не существует».
  По умолчанию падают только тесты этого кластера (как сейчас для ошибок consumer) — см. вопрос 11.
- Новые партиции во время прогона: раз в `kafka.partitions.refresh` (по умолчанию `PT30S`) consumer-поток вызывает
  `partitionsFor`, новые партиции добавляет в `assign` и читает **с начала** (`seekToBeginning` только для них).
- Последствие: сообщения, записанные до старта прогона, не видны. Тесты, которые ищут «старые» сообщения, падают —
  таких в неархивных тестах не нашёл (все ищут ответ на отправленное в этом же тесте).

## 5. Найденное сообщение не удаляется (требование 5)

Тесты, которые сейчас полагаются на удаление (повторный поиск с тем же условием должен вернуть другое сообщение):

| Тест | Где | Как полагается | Как переписать |
|---|---|---|---|
| BATM-349 | `BatmClearingTests.java:167` | цикл `ceil(2100/1000)=3` раза `getLastRecord(CLEARING_OUT, b2lFilename)` собирает пачки | `awaitAll(CLEARING_OUT, Root.class, m -> любая rrn из m ∈ rrn сгенерированных, 3, …)` (вопрос 3) |
| BATM-257 | `BatmZodTests.java:32` и `:42` | `SUTP_EVENTS` по одному `transparentId` дважды; без удаления второй вызов сразу вернёт первое сообщение | два `await` с разным признаком сообщения (вопрос 4) |
| BATM-485, BATM-486 (через `checkToGeneratorTransactions`) | `BatmTransAgrTests.java:182` | цикл по телам, поиск по сумме; при одинаковой сумме у двух тел второй вызов получил бы то же сообщение | поиск по полю, связывающему с исходной транзакцией (вопрос 1) |

Проверены и **не зависят** от удаления: `testPerformance` (`BatmAccRouteS3Tests.java:321`, разные `messageId`),
BATM-415 (разные статусы), BATM-416 (`SUCCESS_PP`/`SUCCESS_GP`), BATM-658…665 (разные статусы).
Неоднозначны, но не из-за удаления: BATM-347/348/418/435 — два входных сообщения с одним id, одно чтение (вопрос 6).

## 6. Новый API поиска (требование 6)

```java
<T> T await(KafkaTopic topic, Class<T> type, Predicate<T> condition, Duration timeout);
<T> List<T> awaitAll(KafkaTopic topic, Class<T> type, Predicate<T> condition, int expectedCount, Duration timeout);
void assertNoMessage(KafkaTopic topic, Predicate<String> condition, Duration duration);
String awaitRaw(KafkaTopic topic, Predicate<String> condition, Duration timeout);   // замена удалённого getLastRawRecord
```

- Ищется среди всех сообщений хранилища (с начала прогона), ожидание — Awaitility, проверка раз в 200 мс;
  если в хранилище несколько подходящих — возвращается **последнее по timestamp** (как сейчас LAST).
- Десериализация — `CommonHelper.deserializeJsonToObject`; сообщение, которое не десериализуется в `T`,
  считается несовпадением и учитывается в диагностике. Исключение в самом предикате — тест падает сразу.
- `awaitAll`: успех, когда совпадений ровно `expectedCount`; больше — падение со списком лишних (вопрос 14).
- `assertNoMessage` ждёт всю `duration` и падает, если подходящее сообщение есть — в том числе пришедшее до вызова.
- Описание условия для логов/ошибок: `Described.of("rrn=123", predicate)` (обёртка `Predicate` с `toString`),
  иначе `toString()` предиката.
- **При таймауте** (текст ошибки и вложение Allure «Kafka: диагностика»): топик, кластер, описание условия, время
  ожидания, сколько сообщений пришло в топик за ожидание, сколько из них не десериализовались, сколько вытеснено
  лимитом, последние N (`kafka.report.last.n`, по умолчанию 10) сообщений топика с partition/offset/timestamp/key.
- **Вложения Allure**: найденное сообщение («Kafka ← <топик>», JSON pretty, плюс key/headers/partition/offset)
  и отправленное («Kafka → <топик>»). Шаг Allure — как сейчас `Log.step`.
- `jsonFieldsEqual(Map<String,String>)` и `fields(String...)` из `BatmDpStreamingTests` (версия `ffecc53`) →
  `ru.vtb.core.utils.kafka.JsonPredicates` без изменения логики (JSON Pointer, сравнение `asText()`,
  некорректный JSON = несовпадение). Для `await` добавить типизированный аналог не нужно — поля берутся из модели.
- Старые `getFirstRecord/getLastRecord/getRecord` остаются `@Deprecated` до конца миграции, затем удаляются.

## 7. Миграция вызовов (требование 7)

Правило: искать по уникальному идентификатору модели. Везде, где в сообщении есть rrn, — по rrn: имя B2L-файла даёт
лишь 10 вариантов на маску в день. Поля указаны по моделям `ru.vtb.core.models.kafka.*`.

| Тест / метод | Строка | Топик | Сейчас ищем | Новый поиск |
|---|---|---|---|---|
| BATM-334 | BatmAccTests:52 | BATM_ACC_OUT_BATM_TRANS_ACC | номер счёта (подстрока) | `account` == номер счёта из запроса |
| BATM-336 | BatmAccTests:100 | BATM_ACC_OUT_BATM_TRANS_REVERSAL_RESULT | atmId | `originalMessageId` == messageId запроса (вопрос 7) |
| BATM-509 | BatmAccTests:161 | то же | accountId | `originalMessageId` == messageId запроса (вопрос 7) |
| BATM-304…307 | BatmAccRouteS3Tests:135,157,185,242 | BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT | originalMessageId | `originalMessageId` (242 — raw, вопрос 8) |
| BATM-306, 307 | BatmAccRouteS3Tests:220,271 | BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT | zodMsg.messageId | `originalMessageId` |
| testPerformance | BatmAccRouteS3Tests:321,349 | S3_OUT_…_RESULT | originalMessageId / zodMsg.messageId | `originalMessageId` |
| BATM-279 | BatmBinTests:55 | SOLAR_OUT_RUN_JOB (raw) | имя BIN-файла | поле с именем файла (имя уникально — 10 случайных символов; путь в JSON — вопрос 8) |
| BATM-242 | BatmClearingTests:41 | BATM_CLEARING_IN_BATM_DEVICES_ATM | id | `atmId` |
| BATM-197, 310, 311, 312 | BatmSiorIntegrationTests:48,131,164,196 | то же | id | `atmId` |
| BATM-197 | BatmSiorIntegrationTests:61 | BATM_DP_IN_BATM_DEVICES_ATM | id | `id` |
| BATM-347, 348, 418, 435 | BatmSiorIntegrationTests:223,246,286,336 | BATM_ACC_IN_BATM_DEVICES_ACC | id | `atmId` + различитель второго сообщения, напр. для 348 `accounts[].number` (вопрос 6) |
| checkUfxJson (338, 401…430, 505), 346, 415 | BatmClearingTests:87,120; BatmTransAgrTests:204 | BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA | имя B2L-файла | `transactions[].rrn` ∈ rrn сгенерированных транзакций (вопрос 3) |
| BATM-349 | BatmClearingTests:167 | то же | имя файла, цикл | `awaitAll(..., 3)` по rrn (раздел 5) |
| BATM-422, 425, 426 | BatmClearingTests:295,339,354 | то же | имя файла, `needFail=true` | `assertNoMessage` по rrn сгенерированных |
| BATM-658…665 | BatmDpStreamingTests:307,311,315,319 | BATM_TRANS_OUT_BATM_DP_INST / _DP_TRANS | rrn / instructionId / transactionId + статус (подстроки) | `rrn`/`instructionId`/`transactionId` + `status` (поля модели) |
| BATM-658…665, шаги UASP | BatmDpStreamingTests:329–344 (закомментировано) | UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_* | — | `awaitRaw` + `jsonFieldsEqual(fields("/Object/CashMachine/Id/Id", atmId, "/Object/Status", status))` (вопрос 9) |
| BATM-664, 665, шаг ODS | BatmDpStreamingTests:347 (закомментировано) | ODS_BATMADMIN_OUTBOX | — | критерий не определён (вопрос 9) |
| BATM-416 | BatmOperationsSiorTests:41 | BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_DATA | messageId входящего | `messageId`? или `transactionId` (вопрос 15) |
| BATM-416 | BatmOperationsSiorTests:51,52 | BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT (raw) | transId + SUCCESS_PP/GP | поля transactionId + status (raw, вопрос 8) |
| BATM-416 | BatmOperationsSiorTests:76 | BATM_OUT_SIORUS_INSTRUCTION | transId | `transactionId` |
| BATM-481…484 | BatmTransAgrTests:48,63,79,94 | BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA / _S3_DATA | сумма с комиссией | **вопрос 1** (в моделях нет rrn) |
| testTransAgrToAggregator (337…478) | BatmTransAgrTests:104 | BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA | rrn+authCode (подстрока) | `transactions[].aggregationId` содержит rrn+authCode? (**вопрос 2**) |
| testTransAgrToAggregator | BatmTransAgrTests:111 | BATM_ACC_ROUTE_OUT_BATM_TRANS_S3_RESULT (raw) | messageId шага 3 | `originalMessageId`? (вопрос 8) |
| testTransAgrToGenerator | BatmTransAgrTests:131 | BATM_ACC_ROUTE_OUT_BATM_TRANS_PP_RESULT (raw) | transactionId | поле transactionId (вопрос 8) |
| checkToGeneratorTransactions (485, 486) | BatmTransAgrTests:182 | BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA | `body.getAmount().toString()` | **вопрос 1** |
| BATM-415 шаги 6–15 | BatmTransAgrTests:219–243 | PP_IN_…_PP_DATA, DP_INST, DP_TRANS, REPORT_IN_…, PP_OUT_…_RESULT | step3Id / step1Id / instrId + статус | `transactionId` / `transId` / `instructionId` + `status` по модели топика |
| BATM-257 | BatmZodTests:32,35,39,42 | SUTP_EVENTS, ZOD_OUT_…_S3_EVENT, ZOD_IN_…_LOAD_RESULT | requestId | SUTP_EVENTS — `referenceId`? (**вопрос 4**); S3_EVENT — `messageId`?; LOAD_RESULT — `originalMessageId` |
| BATM-351 | BatmZodTests:72,80 | ZOD_OUT_BATM_TRANS_AGR_ZOD, ZOD_IN_…_ZOD_RESULT (raw) | requestId | поле с requestId (вопрос 8) |
| BATM-283 | BatmClearingSingleThreadTests:31 | SUTP_EVENTS | банковская дата | **вопрос 5** |
| NotTests#testb2lGenerator | NotTests:72–126 | CLEARING_OUT | имя файла | `transactions[].rrn` (утилита, не в сьютах) |

## 8. План реализации

1. `KafkaClusterConfig` + переходный fallback ключей; `test.properties` → `kafka.batm.*`. Selftest: резолвинг ключей
   и fallback на `Properties` без стенда.
2. `KafkaMessage`/`TopicStore` с лимитами и порядком. Selftest: порядок по timestamp, дубли, вытеснение по размеру/TTL.
3. `KafkaClusterRuntime`: assign/seekToEnd/latch, новые партиции, daemon-поток, producer на кластер, закрытие
   в `onFinish`. Selftest — Testcontainers Kafka (пропуск без Docker): старт с конца, новые партиции с начала,
   закрытие без висящих потоков.
4. API `await/awaitAll/assertNoMessage/awaitRaw`, диагностика и вложения Allure, `JsonPredicates`.
   Selftest на хранилище без брокера.
5. Миграция тестов по сьютам (по таблице раздела 7, сначала строки без вопросов), затем удаление старых методов.
6. Итерация 2: убрать `System.exit` из `TestsRunner`.

## 9. Вопросы, без которых нельзя начинать реализацию

1. **ROUTE_IN_…_PP_DATA / _S3_DATA** (BATM-481…484, `checkToGeneratorTransactions`): по какому полю сообщение
   связано с исходной транзакцией? В моделях нет rrn; сейчас ищем по сумме (подстрока).
2. **testTransAgrToAggregator**: содержит ли `transactions[].aggregationId` строку rrn+authCode (как `SemiTransaction:35`)
   и в каком формате (`WAY4_…`/`BATM_…`)?
3. **CLEARING_OUT**: подтвердите поиск по `transactions[].rrn` и размер пачки 1000 транзакций (BATM-349 → 3 сообщения).
4. **BATM-257**: чем различаются два сообщения `SUTP_EVENTS` по одному запросу (`eventName`/`statusCode`?) и в каком
   поле requestId (`referenceId`?).
5. **BATM-283**: какое поле, кроме банковской даты, идентифицирует сообщение `SUTP_EVENTS` этого теста?
6. **BATM-347/348/418/435**: на какое из двух входных сообщений ожидается ответ в `BATM_ACC_IN_BATM_DEVICES_ACC`
   и по какому полю его отличить (для 348 — новый `accounts[].number`?)?
7. **BATM-336/509**: в `…_REVERSAL_RESULT` поле `originalMessageId` равно messageId запроса reversal?
8. **Raw-топики** (без модели: `ROUTE_OUT_…_S3/PP_RESULT`, `PP_OUT_…_OPER_SIOR_PP_RESULT`, `SOLAR_OUT_RUN_JOB`,
   `ZOD_OUT_BATM_TRANS_AGR_ZOD`, `ZOD_IN_…_ZOD_RESULT`): пример сообщения или путь к идентификатору.
9. **UASP/ODS**: реальные имена топиков UASP (сейчас `STUB_…`), критерий поиска в `tsss.batmadmin.outbox`;
   восстанавливать ли закомментированные проверки UASP/ODS в BATM-658…665 в рамках этой работы?
10. **SSL других кластеров**: будут свои хранилища (тогда имена файлов) или всегда общие с BATM?
11. **Ошибка старта кластера** (позиции не зафиксированы за таймаут): падают только тесты этого кластера (как сейчас
    для ошибок consumer) или весь прогон?
12. **Значения по умолчанию**: `kafka.store.max.messages.per.topic`, `kafka.store.ttl`, `kafka.startup.timeout`;
    тип timestamp на топиках — CreateTime или LogAppendTime (влияет на надёжность порядка FIRST/LAST)?
13. **preprod**: есть ли в его файле Kafka-ключи кроме `bootstrap.servers` и четырёх `ssl.*` — чтобы инструкция
    из 1.3 была полной?
14. **awaitAll**: «ровно `expectedCount`» (лишние — падение) или «не меньше»?
15. **BATM-416**: в `PP_IN_…_OPER_SIOR_PP_DATA` поле `messageId` — это messageId входящей инструкции или новый?
