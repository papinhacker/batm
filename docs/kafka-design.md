# Дизайн Kafka-слоя auto-testing (BATM)

Статус: предложение, Kafka-слой не реализован. Ответы на вопросы от 27.11.2026 учтены (раздел 9).
Номера строк ниже — по ветке `claude/kafka-design` после коммита `77b589d` (удаление устаревших кейсов, BATM-257/283 → `@Ignore`, `STUB_` → `ift_`).

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
- Хранилища SSL сейчас у всех кластеров общие (решение), но `kafka.<имя>.ssl.*.location` позволяет задать
  отдельные файлы для любого кластера без правки кода.
- **Префикс имён топиков по контуру**: `kafka.<имя>.topic.prefix` (необязательно). В `KafkaTopic` имя хранится без
  префикса, фактическое имя = префикс + имя. Нужно для UASP: на ift топики `ift_batm_uasp_retail_adm_atm_cash_money_transfer_*`.
  До реализации в `KafkaTopic` прописаны имена ift (`ift_…`, раньше `STUB_…`); при переходе — `kafka.uasp.topic.prefix=ift_`
  в `test.properties`, имя в enum без префикса. Префикс preprod — открытый вопрос.

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
  (открытый вопрос 6).
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
  **Решение: ошибка старта любого включённого кластера роняет весь прогон** — `onStart` бросает исключение с понятным
  текстом, например: «Kafka-кластер UASP: за 60 с не зафиксированы позиции партиций [topicA-0, topicA-3];
  топик topicB не существует». На ift код уже исправлен, поэтому падение сразу показывает проблему окружения.
  Ошибка consumer **во время** прогона — как сейчас: падают тесты этого кластера, остальные продолжают.
  Невключённый кластер (пустой `bootstrap.servers`) — не ошибка старта; его тесты падают с «кластер не настроен».
- Новые партиции во время прогона: раз в `kafka.partitions.refresh` (по умолчанию `PT30S`) consumer-поток вызывает
  `partitionsFor`, новые партиции добавляет в `assign` и читает **с начала** (`seekToBeginning` только для них).
- Последствие: сообщения, записанные до старта прогона, не видны. Тесты, которые ищут «старые» сообщения, падают —
  таких в неархивных тестах не нашёл (все ищут ответ на отправленное в этом же тесте).

## 5. Найденное сообщение не удаляется (требование 5)

Тесты, которые сейчас полагаются на удаление (повторный поиск с тем же условием должен вернуть другое сообщение):

| Тест | Где | Как полагается | Как переписать |
|---|---|---|---|
| BATM-349 | `BatmClearingTests.java:167` | цикл `ceil(2100/1000)=3` раза `getLastRecord(CLEARING_OUT, b2lFilename)` собирает пачки | `awaitAll(CLEARING_OUT, Root.class, m -> любая rrn из m ∈ rrn сгенерированных, 3, …)`; пачка 1000 подтверждена |
| BATM-257 | `BatmZodTests.java:32` и `:42` | `SUTP_EVENTS` по одному `transparentId` дважды; без удаления второй вызов сразу вернёт первое сообщение | **помечен нерабочим** (`@Ignore`): два сообщения должны различаться статусом — при возврате в работу два `await` с разным статусом |
| ~~BATM-485, BATM-486~~ | — | поиск по сумме в цикле | **удалены** как устаревшие вместе с BATM-481…484 и классом `BatmTransAgrToGeneratorTests` |

Проверены и **не зависят** от удаления: `testPerformance` (`BatmAccRouteS3Tests.java:321`, разные `messageId`),
BATM-415 (разные статусы), BATM-416 (`SUCCESS_PP`/`SUCCESS_GP`), BATM-658…665 (разные статусы).
Не зависят от удаления, но ищут ответ на второе («дублирующее») из двух входных сообщений с одним id: BATM-347/348/418/435
(для 347/435 — открытый вопрос 1).

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
- `awaitAll`: успех, когда совпадений **ровно** `expectedCount` (решение); больше — падение со списком лишних.
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
Для raw-топиков (без модели) условие поиска остаётся прежним (подстроки) — через `awaitRaw` (решение).

| Тест / метод | Строка | Топик | Сейчас ищем | Новый поиск |
|---|---|---|---|---|
| BATM-334 | BatmAccTests:52 | BATM_ACC_OUT_BATM_TRANS_ACC | номер счёта (подстрока) | `account` == номер счёта из запроса |
| BATM-336 | BatmAccTests:100 | BATM_ACC_OUT_BATM_TRANS_REVERSAL_RESULT | atmId | `originalMessageId` == messageId запроса reversal (подтверждено) |
| BATM-509 | BatmAccTests:161 | то же | accountId | `originalMessageId` == messageId запроса reversal (подтверждено) |
| BATM-304…306 | BatmAccRouteS3Tests:135,157,185 | BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT | originalMessageId | `originalMessageId` |
| BATM-307, testPerformance | BatmAccRouteS3Tests:242,321 | то же (raw) | originalMessageId | `awaitRaw` с прежним условием |
| BATM-306, 307 | BatmAccRouteS3Tests:220,271 | BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT | zodMsg.messageId | `originalMessageId` |
| testPerformance | BatmAccRouteS3Tests:349 | S3_OUT_…_RESULT | zodMsg.messageId | `originalMessageId` |
| BATM-279 | BatmBinTests:55 | SOLAR_OUT_RUN_JOB (raw) | имя BIN-файла | `awaitRaw` с прежним условием (имя уникально — 10 случайных символов) |
| BATM-242 | BatmClearingTests:41 | BATM_CLEARING_IN_BATM_DEVICES_ATM | id | `atmId` |
| BATM-197, 310, 311, 312 | BatmSiorIntegrationTests:48,131,164,196 | то же | id | `atmId` |
| BATM-197 | BatmSiorIntegrationTests:61 | BATM_DP_IN_BATM_DEVICES_ATM | id | `id` |
| BATM-348 | BatmSiorIntegrationTests:246 | BATM_ACC_IN_BATM_DEVICES_ACC | id | ответ на второе (дублирующее) сообщение: `atmId` + `accounts[].number` == новый номер счёта |
| BATM-418 | BatmSiorIntegrationTests:286 | то же | id | ответ на второе сообщение: `atmId` + в `accounts` есть добавленный счёт 643 |
| BATM-347, 435 | BatmSiorIntegrationTests:223,336 | то же | id | ответ на второе сообщение; тела одинаковые, отличается только заголовок — **открытый вопрос 1** |
| checkUfxJson (338, 401…430, 505), 346, 415 | BatmClearingTests:87,120; BatmTransAgrTests:99 | BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA | имя B2L-файла | `transactions[].rrn` ∈ rrn сгенерированных транзакций (подтверждено) |
| BATM-349 | BatmClearingTests:167 | то же | имя файла, цикл | `awaitAll(..., 3)` по rrn, пачка 1000 (подтверждено) |
| BATM-422, 425, 426 | BatmClearingTests:295,339,354 | то же | имя файла, `needFail=true` | `assertNoMessage` по rrn сгенерированных |
| BATM-658…665 | BatmDpStreamingTests:307,311,315,319 | BATM_TRANS_OUT_BATM_DP_INST / _DP_TRANS | rrn / instructionId / transactionId + статус (подстроки) | `rrn`/`instructionId`/`transactionId` + `status` (поля модели) |
| BATM-658…665, шаги UASP | BatmDpStreamingTests:329–344 (закомментировано) | UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_* (`ift_…`) | — | **восстановить**: `awaitRaw` + `jsonFieldsEqual(fields("/Object/CashMachine/Id/Id", atmId, "/Object/Status", status))`; ФТ — по `/Object/Instruction/Id/Id` и статусу |
| BATM-664, 665, шаг ODS | BatmDpStreamingTests:347 (закомментировано) | ODS_BATMADMIN_OUTBOX | — | **восстановить вызов**; пока критерия нет, шаг **падает с явной ошибкой** «критерий поиска ODS не определён» (решение) — **открытый вопрос 3** |
| BATM-416 | BatmOperationsSiorTests:41 | BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_DATA | messageId входящей инструкции (подстрока) | прежнее значение по полю модели — `messageId`? (**открытый вопрос 5**) |
| BATM-416 | BatmOperationsSiorTests:51,52 | BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT (raw) | transId + SUCCESS_PP/GP | `awaitRaw` с прежним условием |
| BATM-416 | BatmOperationsSiorTests:76 | BATM_OUT_SIORUS_INSTRUCTION | transId | `transactionId` |
| testTransAgrToAggregator (337…478) | BatmTransAgrTests:36 | BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA | rrn+authCode (подстрока) | `transactions[].aggregationId` содержит rrn+authCode (подтверждено) |
| testTransAgrToAggregator | BatmTransAgrTests:43 | BATM_ACC_ROUTE_OUT_BATM_TRANS_S3_RESULT (raw) | messageId шага 3 | `awaitRaw` с прежним условием |
| BATM-415 шаги 6–15 | BatmTransAgrTests:114–138 | PP_IN_…_PP_DATA, DP_INST, DP_TRANS, REPORT_IN_…, PP_OUT_…_RESULT | step3Id / step1Id / instrId + статус | `transactionId` / `transId` / `instructionId` + `status` по модели топика |
| ~~BATM-481…484, 485, 486, to_generator BATM-415~~ | — | ROUTE_IN_…_PP/S3_DATA | сумма | **удалены** как устаревшие |
| BATM-257 | BatmZodTests | SUTP_EVENTS, ZOD_* | requestId | **нерабочий** (`@Ignore`) — открытый вопрос 4 |
| BATM-351 | BatmZodTests:76,84 | ZOD_OUT_BATM_TRANS_AGR_ZOD, ZOD_IN_…_ZOD_RESULT (raw) | requestId | `awaitRaw` с прежним условием |
| BATM-283 | BatmClearingSingleThreadTests | SUTP_EVENTS | банковская дата | **нерабочий** (`@Ignore`) — открытый вопрос 4 |
| NotTests#testb2lGenerator | NotTests:72–126 | CLEARING_OUT | имя файла | `transactions[].rrn` (утилита, не в сьютах) |

Последствие восстановления UASP/ODS: пока кластер UASP не настроен на ift (пустой `kafka.uasp.bootstrap.servers`),
а критерий ODS не определён, BATM-658…665 будут красными. Это ожидаемо: сейчас эти шаги молча ничего не проверяют.

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

## 9. Вопросы

### Закрыты (ответы 27.11.2026)

| Вопрос | Решение |
|---|---|
| Поле для ROUTE_IN_…_PP/S3_DATA (481…484, генератор) | кейсы устарели — удалены (BATM-481…484, класс `BatmTransAgrToGeneratorTests`: 415/485/486) |
| `aggregationId` содержит rrn+authCode | да |
| CLEARING_OUT: поиск по `transactions[].rrn`, пачка 1000 | да |
| BATM-257: различие двух SUTP_EVENTS | статусы должны различаться; кейс помечен нерабочим |
| BATM-347/348/418/435: какой ответ ожидается | на дублирующее (второе) сообщение |
| reversal: `originalMessageId` = messageId запроса | да |
| raw-топики | поиск оставить как есть (через `awaitRaw`) |
| UASP/ODS | `STUB_` → `ift_` для тестового контура; проверки восстанавливать; шаг ODS без критерия падает с явной ошибкой |
| SSL других кластеров | сейчас общие; возможность задать хранилища на кластер заложить (`kafka.<имя>.ssl.*.location`) |
| Ошибка старта кластера | падает весь прогон |
| `awaitAll` | ровно `expectedCount` |
| BATM-283 | помечен нерабочим |

### Открыты

1. **BATM-347/435**: второе сообщение отличается от первого только заголовком. Сколько ответов приходит в
   `BATM_ACC_IN_BATM_DEVICES_ACC` на пару входных (один или два) и связан ли `messageId` ответа с заголовком входного?
   Без этого ответ на «дублирующее» не отличить от ответа на первое.
2. **Префикс UASP-топиков на preprod** (`kafka.uasp.topic.prefix`).
3. **Критерий поиска в ODS** `tsss.batmadmin.outbox` (до ответа шаг ODS в BATM-664/665 падает с явной ошибкой).
4. **BATM-257/283** — что нужно, чтобы вернуть в работу: статусы двух сообщений SUTP_EVENTS (257) и поле,
   идентифицирующее сообщение SUTP_EVENTS теста (283).
5. **BATM-416**: поле `messageId` в `PP_IN_…_OPER_SIOR_PP_DATA` — это messageId входящей инструкции?
6. **Значения по умолчанию** (`kafka.store.max.messages.per.topic`=50 000, `kafka.store.ttl`=PT60M,
   `kafka.startup.timeout`=PT60S) и тип timestamp на топиках (CreateTime/LogAppendTime) — пока приняты предложенные.
7. **preprod**: есть ли в его файле Kafka-ключи кроме `bootstrap.servers` и четырёх `ssl.*` (для инструкции из 1.3).
