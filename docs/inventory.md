# Инвентаризация auto-testing (BATM)

Дата: 2026-11-26. Ревизия: `82c660d` (ветка `claude/test-inventory-analysis-4ojpb8`, рабочее дерево без изменений).
Документ содержит только факты. Решения («в архив», «чинить», «удалить») принимает владелец репозитория.
Код, pom.xml, XML-сьюты и settings.xml не менялись.

## 0. Расхождения CLAUDE.md с репозиторием (обнаружены попутно)

| В CLAUDE.md | В репозитории |
|---|---|
| `mvn -q test -Pselftest` — офлайн-тесты фреймворка | В pom.xml нет `<profiles>`, профиля `selftest` нет |
| Архив: `ru.vtb.api.tests.archive` | Пакета нет, 0 файлов |
| Репозитории в `ru.vtb.core.db`, entity в `ru.vtb.core.db.entity.<бд>` | Пакета `ru.vtb.core.db` нет; все проверки БД идут через `DBHelper` / `SQLHelper` |
| Java 17 | `maven.compiler.source/target` = 17, но в `maven-compiler-plugin` явно `<source>16</source><target>16</target>`, и фактически компиляция идёт с `target 16` (см. лог) |
| dev мёртв | `src/main/resources/properties/dev.properties` есть (вместе с `test.properties`; `preprod.properties` нет — ожидаемо) |
| Однопоточные тесты — группа `"serial"`; новые тесты — `groups = {"<сервис>"}` | Ни у одного из 84 `@Test` нет атрибута `groups` |
| Никаких `System.exit` вне TestsRunner | `KafkaClient.java:80` — `System.exit(-1)` |
| Никаких `Thread.sleep` в тестах | В тестах нет; есть в ядре: `CommonHelper.java:118` |
| Исключения не глотать | `SQLHelper.java:240` ловит `SQLException \| ConditionTimeoutException` и пишет warning; `KafkaHelper.java:126` — `catch (ConditionTimeoutException ignored)` |

## 1. Сборка

Команда: `mvn -B test-compile -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true`
(флаги только в командной строке). Maven 3.9.11, JDK 21.0.13 (Ubuntu), maven-compiler-plugin 3.13.0
(версия не задана в pom, взята из super-POM Maven).

**Результат: `BUILD SUCCESS`**, 106 файлов `src/main`, 79 файлов `src/test`, ошибок нет.

Зависимости скачаны из Maven Central (`~/.m2` был пуст). В `src/main/resources/settings.xml` прописано
зеркало `central → https://sfera.test.vtb.ru/app/repo-cdl-maven/...`, но Maven читает этот файл только с `-s`,
поэтому здесь он не использовался. Со стороны этой среды корпоративное зеркало недоступно. Версии артефактов
зафиксированы в pom, поэтому дерево зависимостей должно совпадать, но с зеркалом я его не сверял.

### 1.1 Предупреждения Maven (дословно)

```
[WARNING] Some problems were encountered while building the effective model for ru.vtb:auto-testing:jar:1.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-compiler-plugin is missing. @ line 190, column 21
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
```

### 1.2 Предупреждения javac из сборки Maven (дословно)

```
[WARNING] system modules path not set in conjunction with -source 16            (compile и testCompile)
[WARNING] /home/user/batm/src/main/java/ru/vtb/core/utils/CommonHelper.java:[26,52] Locale(java.lang.String,java.lang.String) in java.util.Locale has been deprecated
[WARNING] /home/user/batm/src/main/java/ru/vtb/core/utils/CommonHelper.java:[48,38] setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include) in com.fasterxml.jackson.databind.ObjectMapper has been deprecated
[WARNING] /home/user/batm/src/main/java/ru/vtb/core/utils/CommonHelper.java:[57,38] setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include) in com.fasterxml.jackson.databind.ObjectMapper has been deprecated
[INFO] Annotation processing is enabled because one or more processors were found on the class path. A future release of javac may disable annotation processing unless at least one processor is specified by name (-processor) ... (compile и testCompile)
```

### 1.3 Полный lint: `javac -Xlint:all` (отдельный прогон в scratchpad, классы в репозиторий не писались)

Те же исходники, тот же classpath (`dependency:build-classpath`), `-source 16 -target 16`. В `src/main` 9 предупреждений, в `src/test` 2:

| Файл:строка | Категория | Текст |
|---|---|---|
| — | options | system modules path not set in conjunction with -source 16 (main и test) |
| — | processing | No processor claimed any of these annotations: … (main и test; Lombok-процессор найден на classpath) |
| core/utils/DBHelper.java:766 | text-blocks | trailing white space will be removed |
| core/common/listeners/TestNGListener.java:52 | rawtypes | found raw type: Class |
| core/common/listeners/TestNGListener.java:52 | rawtypes | found raw type: Constructor |
| core/utils/CommonHelper.java:26 | deprecation | Locale(String,String) in Locale has been deprecated |
| core/utils/CommonHelper.java:48 | deprecation | setSerializationInclusion(Include) in ObjectMapper has been deprecated |
| core/utils/CommonHelper.java:57 | deprecation | setSerializationInclusion(Include) in ObjectMapper has been deprecated |
| core/utils/S3Service.java:34 | this-escape | possible 'this' escape before subclass is fully initialized |

Предупреждений `unchecked` нет. В тестовом коде (`src/test`) нет ни одного предупреждения по существу.

## 2. Зависимости (`mvn dependency:tree`)

### 2.1 Прямые зависимости

| Артефакт | Версия | Scope | Замечание (факт) |
|---|---|---|---|
| org.testng:testng | 7.4.0 | compile | |
| io.qameta.allure:allure-testng | 2.35.2 | **test** | |
| io.qameta.allure:allure-rest-assured | 2.35.2 | compile | |
| org.apache.commons:commons-lang3 | 3.20.0 | compile | |
| org.apache.commons:commons-compress | **1.26.1** | compile | версия прописана в pom литералом; свойство `apache.commons-compress.version` = 1.28.0 объявлено, но нигде не используется |
| com.fasterxml.jackson.core:jackson-databind | 2.22.0 | compile | |
| com.fasterxml.jackson.dataformat:jackson-dataformat-xml | 2.22.0 | compile | |
| io.rest-assured:rest-assured | 6.0.0 | compile | |
| io.rest-assured:json-schema-validator | 6.0.0 | compile | |
| org.projectlombok:lombok | 1.18.46 | compile | |
| org.postgresql:postgresql | 42.7.11 | compile | |
| com.github.javafaker:javafaker | 1.0.2 | compile | |
| software.amazon.awssdk:s3 | 2.46.15 | compile | |
| org.apache.kafka:kafka-clients | 4.3.0 | compile | |
| org.awaitility:awaitility | 4.3.0 | compile | |
| com.networknt:json-schema-validator | 1.5.6 | compile | |
| com.opencsv:opencsv | 5.12.0 | compile | |
| org.simplejavamail:simple-java-mail | 8.6.2 | compile | |

Плагины из pom: maven-resources-plugin 2.6 (`default-resources` отключён через `<phase>none</phase>`), maven-assembly-plugin 3.7.1,
allure-maven 2.10.0, maven-compiler-plugin без версии (по факту 3.13.0).

### 2.2 joda-time и commons-io: используются в коде, в pom не объявлены

`mvn dependency:tree -Dverbose -Dincludes=joda-time,commons-io`:

```
ru.vtb:auto-testing:jar:1.0
+- org.apache.commons:commons-compress:jar:1.26.1:compile
|  \- commons-io:commons-io:jar:2.15.1:compile
\- io.rest-assured:json-schema-validator:jar:6.0.0:compile
   \- com.github.java-json-tools:json-schema-validator:jar:2.2.14:compile
      \- joda-time:joda-time:jar:2.10.5:compile
```

Каждая библиотека приходит по одному пути, конфликтов версий (`omitted for conflict`) нет.

| Библиотека | Версия | Приходит через | Где используется в коде |
|---|---|---|---|
| joda-time | 2.10.5 | `io.rest-assured:json-schema-validator:6.0.0` → `com.github.java-json-tools:json-schema-validator:2.2.14` | `org.joda.time.DateTime`: core/utils/CommonHelper.java:11, core/data/static_data/dto/TransactionDto.java:6, тесты BatmZodTests.java:5, BatmAccTests.java:4, BatmClearingSingleThreadTests.java:4, NotTests.java:4 |
| commons-io | 2.15.1 | `org.apache.commons:commons-compress:1.26.1` | `org.apache.commons.io.IOUtils`: core/utils/ZipUtils.java:7; `org.apache.commons.io.FileUtils`: core/utils/TestRunnerUtils.java:3 |

Следствие: версия commons-io зависит от литерала 1.26.1 у commons-compress, а joda-time пропадёт с classpath,
если убрать `io.rest-assured:json-schema-validator`.

<details>
<summary>Полное дерево <code>mvn dependency:tree</code> (129 строк)</summary>

```
ru.vtb:auto-testing:jar:1.0
+- org.testng:testng:jar:7.4.0:compile
|  +- com.beust:jcommander:jar:1.78:compile
|  \- org.webjars:jquery:jar:3.5.1:compile
+- io.qameta.allure:allure-testng:jar:2.35.2:test
|  +- io.qameta.allure:allure-java-commons:jar:2.35.2:compile
|  |  \- io.qameta.allure:allure-model:jar:2.35.2:compile
|  \- io.qameta.allure:allure-test-filter:jar:2.35.2:test
+- io.qameta.allure:allure-rest-assured:jar:2.35.2:compile
|  \- io.qameta.allure:allure-attachments:jar:2.35.2:compile
|     \- org.freemarker:freemarker:jar:2.3.33:runtime
+- org.apache.commons:commons-lang3:jar:3.20.0:compile
+- org.apache.commons:commons-compress:jar:1.26.1:compile
|  +- commons-codec:commons-codec:jar:1.16.1:compile
|  \- commons-io:commons-io:jar:2.15.1:compile
+- com.fasterxml.jackson.core:jackson-databind:jar:2.22.0:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.22:compile
|  \- com.fasterxml.jackson.core:jackson-core:jar:2.22.0:compile
+- com.fasterxml.jackson.dataformat:jackson-dataformat-xml:jar:2.22.0:compile
|  +- org.codehaus.woodstox:stax2-api:jar:4.3.0:compile
|  \- com.fasterxml.woodstox:woodstox-core:jar:7.2.0:compile
+- io.rest-assured:rest-assured:jar:6.0.0:compile
|  +- org.apache.groovy:groovy:jar:5.0.3:compile
|  +- org.apache.groovy:groovy-xml:jar:5.0.3:compile
|  +- org.apache.httpcomponents:httpclient:jar:4.5.13:compile
|  |  +- org.apache.httpcomponents:httpcore:jar:4.4.13:compile
|  |  \- commons-logging:commons-logging:jar:1.2:compile
|  +- org.apache.httpcomponents:httpmime:jar:4.5.13:compile
|  +- org.hamcrest:hamcrest:jar:2.2:compile
|  +- org.ccil.cowan.tagsoup:tagsoup:jar:1.2.1:compile
|  +- io.rest-assured:json-path:jar:6.0.0:compile
|  |  +- org.apache.groovy:groovy-json:jar:5.0.3:compile
|  |  \- io.rest-assured:rest-assured-common:jar:6.0.0:compile
|  \- io.rest-assured:xml-path:jar:6.0.0:compile
+- io.rest-assured:json-schema-validator:jar:6.0.0:compile
|  +- com.github.java-json-tools:json-schema-validator:jar:2.2.14:compile
|  |  +- com.github.java-json-tools:jackson-coreutils-equivalence:jar:1.0:compile
|  |  |  \- com.github.java-json-tools:jackson-coreutils:jar:2.0:compile
|  |  |     \- com.github.java-json-tools:msg-simple:jar:1.2:compile
|  |  |        \- com.github.java-json-tools:btf:jar:1.3:compile
|  |  +- com.github.java-json-tools:json-schema-core:jar:1.2.14:compile
|  |  |  +- com.github.java-json-tools:uri-template:jar:0.10:compile
|  |  |  \- org.mozilla:rhino:jar:1.7.7.2:compile
|  |  +- com.sun.mail:mailapi:jar:1.6.2:compile
|  |  +- joda-time:joda-time:jar:2.10.5:compile
|  |  +- com.googlecode.libphonenumber:libphonenumber:jar:8.11.1:compile
|  |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
|  |  \- net.sf.jopt-simple:jopt-simple:jar:5.0.4:compile
|  \- com.google.guava:guava:jar:33.2.1-jre:compile
|     +- com.google.guava:failureaccess:jar:1.0.2:compile
|     +- com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile
|     +- org.checkerframework:checker-qual:jar:3.42.0:compile
|     +- com.google.errorprone:error_prone_annotations:jar:2.26.1:compile
|     \- com.google.j2objc:j2objc-annotations:jar:3.0.0:compile
+- org.projectlombok:lombok:jar:1.18.46:compile
+- org.postgresql:postgresql:jar:42.7.11:compile
+- com.github.javafaker:javafaker:jar:1.0.2:compile
|  +- org.yaml:snakeyaml:jar:android:1.23:compile
|  \- com.github.mifmif:generex:jar:1.0.2:compile
|     \- dk.brics.automaton:automaton:jar:1.11-8:compile
+- software.amazon.awssdk:s3:jar:2.46.15:compile
|  +- software.amazon.awssdk:aws-xml-protocol:jar:2.46.15:compile
|  |  \- software.amazon.awssdk:aws-query-protocol:jar:2.46.15:compile
|  +- software.amazon.awssdk:protocol-core:jar:2.46.15:compile
|  +- software.amazon.awssdk:arns:jar:2.46.15:compile
|  +- software.amazon.awssdk:profiles:jar:2.46.15:compile
|  +- software.amazon.awssdk:crt-core:jar:2.46.15:compile
|  +- software.amazon.awssdk:http-auth:jar:2.46.15:compile
|  +- software.amazon.awssdk:identity-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:http-auth-spi:jar:2.46.15:compile
|  |  \- org.reactivestreams:reactive-streams:jar:1.0.4:compile
|  +- software.amazon.awssdk:http-auth-aws:jar:2.46.15:compile
|  +- software.amazon.awssdk:checksums:jar:2.46.15:compile
|  +- software.amazon.awssdk:checksums-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:retries-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:sdk-core:jar:2.46.15:compile
|  |  \- software.amazon.awssdk:retries:jar:2.46.15:compile
|  +- software.amazon.awssdk:auth:jar:2.46.15:compile
|  |  +- software.amazon.awssdk:http-auth-aws-eventstream:jar:2.46.15:compile
|  |  \- software.amazon.eventstream:eventstream:jar:1.0.1:compile
|  +- software.amazon.awssdk:http-client-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:regions:jar:2.46.15:compile
|  +- software.amazon.awssdk:annotations:jar:2.46.15:compile
|  +- software.amazon.awssdk:utils:jar:2.46.15:compile
|  +- software.amazon.awssdk:aws-core:jar:2.46.15:compile
|  |  \- software.amazon.awssdk:utils-lite:jar:2.46.15:compile
|  +- software.amazon.awssdk:metrics-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:json-utils:jar:2.46.15:compile
|  |  \- software.amazon.awssdk:third-party-jackson-core:jar:2.46.15:compile
|  +- software.amazon.awssdk:endpoints-spi:jar:2.46.15:compile
|  +- software.amazon.awssdk:apache5-client:jar:2.46.15:runtime
|  |  +- org.apache.httpcomponents.client5:httpclient5:jar:5.6.1:runtime
|  |  |  \- org.apache.httpcomponents.core5:httpcore5-h2:jar:5.4:runtime
|  |  \- org.apache.httpcomponents.core5:httpcore5:jar:5.4.2:runtime
|  \- software.amazon.awssdk:netty-nio-client:jar:2.46.15:runtime
|     +- io.netty:netty-codec-http:jar:4.1.135.Final:runtime
|     +- io.netty:netty-codec-http2:jar:4.1.135.Final:runtime
|     +- io.netty:netty-codec:jar:4.1.135.Final:runtime
|     +- io.netty:netty-transport:jar:4.1.135.Final:runtime
|     +- io.netty:netty-common:jar:4.1.135.Final:runtime
|     +- io.netty:netty-buffer:jar:4.1.135.Final:runtime
|     +- io.netty:netty-handler:jar:4.1.135.Final:runtime
|     |  \- io.netty:netty-transport-native-unix-common:jar:4.1.135.Final:runtime
|     +- io.netty:netty-transport-classes-epoll:jar:4.1.135.Final:runtime
|     \- io.netty:netty-resolver:jar:4.1.135.Final:runtime
+- org.apache.kafka:kafka-clients:jar:4.3.0:compile
|  +- com.github.luben:zstd-jni:jar:1.5.6-10:runtime
|  +- at.yawk.lz4:lz4-java:jar:1.10.2:runtime
|  +- org.xerial.snappy:snappy-java:jar:1.1.10.7:runtime
|  \- org.slf4j:slf4j-api:jar:1.7.36:compile
+- org.awaitility:awaitility:jar:4.3.0:compile
+- com.networknt:json-schema-validator:jar:1.5.6:compile
|  +- com.ethlo.time:itu:jar:1.10.3:compile
|  \- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.18.1:compile
|     \- org.yaml:snakeyaml:jar:2.3:compile
+- com.opencsv:opencsv:jar:5.12.0:compile
|  +- org.apache.commons:commons-text:jar:1.13.1:compile
|  +- commons-beanutils:commons-beanutils:jar:1.11.0:compile
|  |  \- commons-collections:commons-collections:jar:3.2.2:compile
|  \- org.apache.commons:commons-collections4:jar:4.5.0:compile
\- org.simplejavamail:simple-java-mail:jar:8.6.2:compile
   +- org.simplejavamail:core-module:jar:8.6.2:compile
   |  +- jakarta.mail:jakarta.mail-api:jar:2.1.2:compile
   |  |  \- jakarta.activation:jakarta.activation-api:jar:2.1.2:compile
   |  +- org.eclipse.angus:angus-mail:jar:2.0.2:runtime
   |  |  \- org.eclipse.angus:angus-activation:jar:2.0.1:runtime
   |  \- com.sanctionco.jmail:jmail:jar:1.4.1:compile
   +- com.github.bbottema:jetbrains-runtime-annotations:jar:1.0.1:compile
   \- com.pivovarit:throwing-function:jar:1.5.1:compile
```
</details>

## 3. Тестовые методы

### 3.1 Как собраны данные

- **Источник `@Test`:** все `.java` в `src/test` и `src/main`. Найдено **84** метода с `@Test`: 82 в `src/test`
  и 2 в `src/main` (`EnvConstants.encryptString/decryptString`). Пакет `archive` отсутствует.
- **Сьюты:** все 14 XML в `TestXML/`. Сопоставление `<class>/<include>` → метод выполнено с учётом наследования:
  include унаследованного метода считается существующим, и такой метод записан в строку класса, где он объявлен,
  с пометкой «через класс …». Всего 87 пар `<test>` × метод.
- **Дата:** `git log -L <начало>,<конец>:<файл>`; берётся последний коммит, затронувший строки метода от первой
  аннотации до закрывающей скобки. **В репозитории всего 7 коммитов (2026-11-24…27)**, поэтому у 76 методов дата — первый
  импорт (`a86b9ad init`). Реальную историю до импорта git не содержит, эта колонка почти неинформативна.
- **suite name / parallel:** атрибуты `<suite>` из XML. Важно: `TestNGListener.onStart` при `-DthreadCount>1`
  выставляет `parallel=TESTS` всем сьютам, в имени которых нет подстроки `singleThread`.
  Единственный сьют с `parallel="none"` — `BatmClearingSingleThread.xml`, и его имя `batm-clearing`, то есть
  при `threadCount>1` его `parallel="none"` перекрывается.

**Правила колонки «Признаки устаревания»** (только факты, формальные правила):

| Признак | Как определяется |
|---|---|
| не входит ни в один сьют | метод не достижим ни из одного `<include>` ни одного XML (с учётом наследования) |
| нет @TmsLink | у метода нет `@TmsLink` / `@TmsLinks` |
| нет проверок | ни в теле метода, ни в транзитивно вызываемых методах проекта нет ничего, что может уронить тест: `softAssertAll()`, `Assert.*` / `assert*(…)` / `fail(…)`. Исключено: `Assert.fail` в `KafkaHelper.sendMessage` (срабатывает только при ошибке отправки). Ожидания `KafkaHelper.waitForMessage` и `SQLHelper.getResultsAndCompose` не считаются проверками: таймаут там перехватывается и не роняет тест (`KafkaHelper.java:126`, `SQLHelper.java:240`) |
| softAssert* без softAssertAll | `softAssert*` вызывается, а `softAssertAll()` нет ни в теле, ни в хелперах; `SoftAssertVerification.softAssertVerificationAfterMethod` только обнуляет `softAssert`, поэтому такие проверки не влияют на результат |
| единственная проверка — сообщение найдено в Kafka | других проверок нет, только `Assert.fail` внутри `KafkaHelper.getRecord` (сообщение с подстрокой не найдено за таймаут) |
| @Deprecated | `@Deprecated` на самом методе или вызов `@Deprecated`-метода проекта (в проекте их три: `B2LHelper.getNewPurchaseByCashTransactionReference`, `B2LHelper.getNewPurchaseByCashReversedTransactionReference`, `BatmAccTests.testBATM397`; ни один тест не вызывает два метода B2LHelper). Использование deprecated API библиотек — в п. 3.3 |
| только в этом тесте: топик / таблица | константы `KafkaTopic.*` и имена таблиц из SQL-строк (`from/join/into/update/truncate`) в теле теста и транзитивно вызываемых методах проекта, которые не встречаются ни у одного другого `@Test`-метода. Имена таблиц указаны как в SQL, без имени БД |

Ограничения метода: граф вызовов для колонки признаков построен по исходникам и по именам методов (вызовы с явным классом
разрешаются точно, остальные — по имени среди методов проекта; общие имена вроде `get`/`build` не учитываются).
Поэтому «только в этом тесте» может пропустить топик или таблицу, собранные динамически, а SQL, склеенный из переменных,
не распознаётся. Все строки с «нет проверок» и с «единственная проверка — Kafka» я проверил вручную по исходникам.

### 3.2 Таблица

| # | Класс | Метод | @TmsLink | description | XML-сьют → `<test name>` | suite name | parallel="none" | Последнее изменение (git log -L) | Признаки устаревания (факты) | Прочие атрибуты |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | batm_acc.BatmAccTests | `testBATM330` | BATM-330 | Приём сообщения содержащего счета УС | API/batm_acc/BatmAcc.xml → `BATM-330` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 2 | batm_acc.BatmAccTests | `testBATM334` | BATM-334 | Отгрузка нового счёта потребителю | API/batm_acc/BatmAcc.xml → `BATM-334` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_ACC_OUT_BATM_TRANS_ACC | — |
| 3 | batm_acc.BatmAccTests | `testBATM335` | BATM-335 | Получение статуса отгрузки заведённого счёта | API/batm_acc/BatmAcc.xml → `BATM-335` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_ACC_IN_BATM_TRANS_ACC_RESULT | — |
| 4 | batm_acc.BatmAccTests | `testBATM336` | BATM-336 | Поиск счетов для оформления reversal-операций | API/batm_acc/BatmAcc.xml → `BATM-336` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 5 | batm_acc.BatmAccTests | `testBATM397` | BATM-397 | Прием сообщения с измененными счетами УС | API/batm_acc/BatmAcc.xml → `BATM-397` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | сам метод помечен @Deprecated | @Ignore |
| 6 | batm_acc.BatmAccTests | `testBATM509` | BATM-509 | Поиск счетов для оформления reversal-операций из history_accounts | API/batm_acc/BatmAcc.xml → `BATM-509` | batm-acc | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_IN_SIORUS_ACC_MIGRATION | — |
| 7 | batm_acc_route_s3.BatmAccRouteS3Tests | `testBATM303` | BATM-303 | Отправка сообщения содержащую транзакцию | API/batm_acc_route_s3/BatmAccRouteS3.xml → `BATM-303` | batm-acc-route-s3 | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 8 | batm_acc_route_s3.BatmAccRouteS3Tests | `testBATM304` | BATM-304 | Успешный разбор сообщения | API/batm_acc_route_s3/BatmAccRouteS3.xml → `BATM-304` | batm-acc-route-s3 | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 9 | batm_acc_route_s3.BatmAccRouteS3Tests | `testBATM305` | BATM-305 | Неуспешный разбор сообщения | API/batm_acc_route_s3/BatmAccRouteS3.xml → `BATM-305` | batm-acc-route-s3 | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 10 | batm_acc_route_s3.BatmAccRouteS3Tests | `testBATM306` | BATM-306 | Формирование и отправка файла в агрегатор с уникальным file_name | API/batm_acc_route_s3/BatmAccRouteS3.xml → `BATM-306` | batm-acc-route-s3 | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 11 | batm_acc_route_s3.BatmAccRouteS3Tests | `testBATM307` | BATM-307 | Формирование и отправка файла в агрегатор с повторяющимся file_name | API/batm_acc_route_s3/BatmAccRouteS3.xml → `BATM-307` | batm-acc-route-s3 | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 12 | batm_acc_route_s3.BatmAccRouteS3Tests | `testPerformance` | — | performance test | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; нет @TmsLink | @Ignore |
| 13 | batm_bin.BatmBinTests | `testBATM277` | BATM-277 | Проверка чтения бакета микросервисом | API/batm_bin/BatmBin.xml → `BATM-277` | batm-bin | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: таблица bch_file_job, таблица bin_file_deleted_s3 | dataProvider="BatmBinFilenames" |
| 14 | batm_bin.BatmBinTests | `testBATM279` | BATM-279 | Проверка взаимодействия  batm_bin  и Solar SUCCESS | API/batm_bin/BatmBin.xml → `BATM-279` | batm-bin | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик SOLAR_OUT_RUN_JOB, таблица bin_file | dataProvider="BatmBinFilenames" |
| 15 | batm_clearing.BatmClearingTests | `testBATM242` | BATM-242 | Проверка чтения топика batm-in-siorus-devices мс batm_clearing | API/batm_clearing/BatmClearing.xml → `BATM-242` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 16 | batm_clearing.BatmClearingTests | `testBATM338` | BATM-338 | Проверка конвертации B2L->JSON | API/batm_clearing/BatmClearing.xml → `BATM-338` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 17 | batm_clearing.BatmClearingTests | `testBATM346` | BATM-346 | Проверка сохранения передаваемых в JSON транзакций  в бд batm_clearing | API/batm_clearing/BatmClearing.xml → `BATM-346` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 18 | batm_clearing.BatmClearingTests | `testBATM349` | BATM-349 | Проверка разделения транзакций на пачк при конвертации из B2L в JSON  | API/batm_clearing/BatmClearing.xml → `BATM-349` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 19 | batm_clearing.BatmClearingTests | `testBATM401` | BATM-401 | Проверка транзакционных ограничений конвертации по MCC в UFX 298cto | API/batm_clearing/BatmClearing.xml → `BATM-401` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 20 | batm_clearing.BatmClearingTests | `testBATM402` | BATM-402 | Проверка транзакционных ограничений конвертации по MCC в JSON CORP_038ctd | API/batm_clearing/BatmClearing.xml → `BATM-402` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 21 | batm_clearing.BatmClearingTests | `testBATM414` | BATM-414 | Проверка транзакционных ограничений конвертации по MCC в JSON OPS_038ctd | API/batm_clearing/BatmClearing.xml → `BATM-414` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 22 | batm_clearing.BatmClearingTests | `testBATM419` | BATM-419 | Проверка конвертации B2L->JSON, atm_migration=true, CORP_038cto | API/batm_clearing/BatmClearing.xml → `BATM-419` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 23 | batm_clearing.BatmClearingTests | `testBATM420` | BATM-420 | Проверка конвертации B2L->JSON, atm_migration=true, CORP_038ctd | API/batm_clearing/BatmClearing.xml → `BATM-420` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 24 | batm_clearing.BatmClearingTests | `testBATM421` | BATM-421 | Проверка конвертации B2L->JSON, atm_migration=false, 298cto | API/batm_clearing/BatmClearing.xml → `BATM-421` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 25 | batm_clearing.BatmClearingTests | `testBATM422` | BATM-422 | Проверка конвертации B2L->JSON, atm_migration=false, VTB24_038cto | API/batm_clearing/BatmClearing.xml → `BATM-422` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 26 | batm_clearing.BatmClearingTests | `testBATM423` | BATM-423 | Проверка конвертации B2L->JSON, atm_migration=true, VTB24_038cto | API/batm_clearing/BatmClearing.xml → `BATM-423` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 27 | batm_clearing.BatmClearingTests | `testBATM424` | BATM-424 | Проверка конвертации B2L->JSON, atm_migration=true, D_038cto | API/batm_clearing/BatmClearing.xml → `BATM-424` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 28 | batm_clearing.BatmClearingTests | `testBATM425` | BATM-425 | Проверка конвертации B2L->JSON, atm_migration=false, D_038cto | API/batm_clearing/BatmClearing.xml → `BATM-425` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 29 | batm_clearing.BatmClearingTests | `testBATM426` | BATM-426 | Проверка конвертации B2L->JSON, atm_migration=false, OPS_038ctd | API/batm_clearing/BatmClearing.xml → `BATM-426` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 30 | batm_clearing.BatmClearingTests | `testBATM427` | BATM-427 | Проверка конвертации B2L->JSON, atm_migration=true, OPS_038ctd | API/batm_clearing/BatmClearing.xml → `BATM-427` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 31 | batm_clearing.BatmClearingTests | `testBATM428` | BATM-428 | Проверка транзакционных ограничений конвертации по MCC в JSON CORP_038cto | API/batm_clearing/BatmClearing.xml → `BATM-428` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 32 | batm_clearing.BatmClearingTests | `testBATM429` | BATM-429 | Проверка транзакционных ограничений конвертации по MCC в JSON VTB24_038cto | API/batm_clearing/BatmClearing.xml → `BATM-429` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 33 | batm_clearing.BatmClearingTests | `testBATM430` | BATM-430 | Проверка транзакционных ограничений конвертации по MCC в JSON D_038cto | API/batm_clearing/BatmClearing.xml → `BATM-430` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 34 | batm_clearing.BatmClearingTests | `testBATM505` | BATM-505 | Проверка конвертации B2L->JSON, atm_migration=true, 038cts (Самоинкассация) | API/batm_clearing/BatmClearing.xml → `BATM-505` | batm-clearing | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 35 | batm_devices.BatmSiorIntegrationTests | `testBATM197` | BATM-197 | Проверка чтения/записи топиков кафки сервисом batm_integr_sior | API/batm_devices/BatmDevices.xml → `BATM-197` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_DP_IN_BATM_DEVICES_ATM | — |
| 36 | batm_devices.BatmSiorIntegrationTests | `testBATM275` | BATM-275 | Проверка чтения/записи топиков кафки сервисом batm_devices негативный сценарий | API/batm_devices/BatmDevices.xml → `BATM-275` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | нет проверок (есть softAssert*, но softAssertAll не вызывается) | — |
| 37 | batm_devices.BatmSiorIntegrationTests | `testBATM292` | BATM-292 | Фиксация входящих сообщений по АТМ/АДМ в статусе <>operation/new/decommissioned/utilized/withdrawn | API/batm_devices/BatmDevices.xml → `BATM-292` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | dataProvider="BatmDevicesWrongAtmExplorationStatuses" |
| 38 | batm_devices.BatmSiorIntegrationTests | `testBATM310` | BATM-310 | Проверка создания УС в филиале Крым 3779 | API/batm_devices/BatmDevices.xml → `BATM-310` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 39 | batm_devices.BatmSiorIntegrationTests | `testBATM311` | BATM-311 | Проверка создания УС 511 проекта | API/batm_devices/BatmDevices.xml → `BATM-311` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 40 | batm_devices.BatmSiorIntegrationTests | `testBATM312` | BATM-312 | Проверка создания УС не 511 проект | API/batm_devices/BatmDevices.xml → `BATM-312` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 41 | batm_devices.BatmSiorIntegrationTests | `testBATM347` | BATM-347 | Передача данных о счетах устройств из batm_devices в batm_acc | API/batm_devices/BatmDevices.xml → `BATM-347` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 42 | batm_devices.BatmSiorIntegrationTests | `testBATM348` | BATM-348 | Проверка передачи данных по УС из batm_devices в batm_acc при изменении счетов | API/batm_devices/BatmDevices.xml → `BATM-348` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 43 | batm_devices.BatmSiorIntegrationTests | `testBATM418` | BATM-418 | Проверка передачи данных по УС из batm_devices в batm_acc при добавлении счетов | API/batm_devices/BatmDevices.xml → `BATM-418` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 44 | batm_devices.BatmSiorIntegrationTests | `testBATM434` | BATM-434 | Проверка перемещения АТМ/АДМ | API/batm_devices/BatmDevices.xml → `BATM-434` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 45 | batm_devices.BatmSiorIntegrationTests | `testBATM435` | BATM-435 | Проверка создания АТМ/АДМ | API/batm_devices/BatmDevices.xml → `BATM-435` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 46 | batm_devices.BatmSiorIntegrationTests | `testBATM436` | BATM-436 | Замена счетов в договоре | API/batm_devices/BatmDevices.xml → `BATM-436` | batm-integr-sior | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 47 | batm_dp.BatmDpStreamingTests | `testBATM658` | BATM-658 | Проверка формирования инструкции status=New | API/batm_dp/BatmDp.xml → `BATM-658` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 48 | batm_dp.BatmDpStreamingTests | `testBATM659` | BATM-659 | Проверка формирования инструкции status=Accepted | API/batm_dp/BatmDp.xml → `BATM-659` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 49 | batm_dp.BatmDpStreamingTests | `testBATM660` | BATM-660 | Проверка формирования транзакции status=New | API/batm_dp/BatmDp.xml → `BATM-660` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 50 | batm_dp.BatmDpStreamingTests | `testBATM661` | BATM-661 | Проверка формирования инструкции status=Processing | API/batm_dp/BatmDp.xml → `BATM-661` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 51 | batm_dp.BatmDpStreamingTests | `testBATM662` | BATM-662 | Проверка формирования транзакции status=Accepted | API/batm_dp/BatmDp.xml → `BATM-662` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 52 | batm_dp.BatmDpStreamingTests | `testBATM663` | BATM-663 | Проверка формирования транзакции status=Processing | API/batm_dp/BatmDp.xml → `BATM-663` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 53 | batm_dp.BatmDpStreamingTests | `testBATM664` | BATM-664 | Проверка формирования инструкции status=Executed | API/batm_dp/BatmDp.xml → `BATM-664` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 54 | batm_dp.BatmDpStreamingTests | `testBATM665` | BATM-665 | Проверка формирования транзакции status=Executed | API/batm_dp/BatmDp.xml → `BATM-665` | batm-dp | нет | 2026-11-27 (db61937 «micro fix») | — | — |
| 55 | batm_operations_sior.BatmOperationsSiorTests | `testBATM416` | BATM-416 | Проверка  обработки успешного сообщения об операции от Сиорус | API/batm_operations_sior/BatmOperationsSior.xml → `BATM-416` | batm-operations-sior | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_ACC_ROUTE_PP_IN_BATM_OPER_SIOR_PP_DATA, топик BATM_ACC_ROUTE_PP_OUT_BATM_OPER_SIOR_PP_RESULT, топик BATM_IN_SIORUS_INSTRUCTION, топик BATM_OUT_SIORUS_INSTRUCTION, таблица financial_instruction, таблица financial_transaction, таблица operation | — |
| 56 | batm_trans_agr.BatmTransAgrTests | `testBATM481` | BATM-481 | Проверка расчета суммы с комиссией Cash-In в генератор | API/batm_trans_agr/BatmTransAgr.xml → `BATM-481` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | единственная проверка — сообщение найдено в Kafka (KafkaHelper.getRecord → Assert.fail) | — |
| 57 | batm_trans_agr.BatmTransAgrTests | `testBATM482` | BATM-482 | Проверка расчета суммы с комиссией Cash-In в агрегатор | API/batm_trans_agr/BatmTransAgr.xml → `BATM-482` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | единственная проверка — сообщение найдено в Kafka (KafkaHelper.getRecord → Assert.fail) | — |
| 58 | batm_trans_agr.BatmTransAgrTests | `testBATM483` | BATM-483 | Копия Проверка расчета суммы с комиссией Cash-Withdrawal в генератор | API/batm_trans_agr/BatmTransAgr.xml → `BATM-483` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | единственная проверка — сообщение найдено в Kafka (KafkaHelper.getRecord → Assert.fail) | — |
| 59 | batm_trans_agr.BatmTransAgrTests | `testBATM484` | BATM-484 | Проверка расчета суммы с комиссией Cash-Withdrawal в агрегатор | API/batm_trans_agr/BatmTransAgr.xml → `BATM-484` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | единственная проверка — сообщение найдено в Kafka (KafkaHelper.getRecord → Assert.fail) | — |
| 60 | batm_trans_agr.BatmTransAgrTests | `testBATM415` | BATM-415 | Проверка Маршрутизации проводок в ROUTE-PP  | API/batm_trans_agr/to_generator/BatmTransAgrToGenerator.xml → `BATM-415` (через класс batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests) | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: топик BATM_ACC_ROUTE_PP_IN_BATM_TRANS_PP_DATA, топик BATM_ACC_ROUTE_PP_OUT_BATM_TRANS_PP_RESULT, топик BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION, таблица accounting_gp, таблица instruction_gp, таблица transaction_gp | — |
| 61 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM337` | BATM-337 | Проверка Маршрутизации проводок в витрину 298cto без миграции | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-337` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 62 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM341` | BATM-341 | Проверка Маршрутизации проводок в витрину  298cto Мигрированные нзр | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-341` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 63 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM342` | BATM-342 | Проверка Маршрутизации проводок в витрину extVTB | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-342` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 64 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM437` | BATM-437 | Проверка Маршрутизации проводок в витрину CORP_038cto без миграции | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-437` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 65 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM438` | BATM-438 | Проверка Маршрутизации проводок в витрину  CORP_038cto Мигрированные нзр | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-438` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 66 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM439` | BATM-439 | Проверка Маршрутизации проводок в витрину D_038cto | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-439` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 67 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM477` | BATM-477 | Проверка Маршрутизации проводок в витрину CORP_038ctd без миграции | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-477` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 68 | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM478` | BATM-478 | Проверка Маршрутизации проводок в витрину CORP_038ctd Мигрированные нзр | API/batm_trans_agr/to_aggregator/BatmTransAgrToAggregator.xml → `BATM-478` | batm-transactions-agr | нет | 2026-11-24 (a86b9ad «init») | — | — |
| 69 | batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM415` | BATM-415 | Проверка Маршрутизации проводок в ROUTE-PP | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют | — |
| 70 | batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM485` | BATM-485 | Проверка Маршрутизации проводок CORP_CARD  cash-in в ROUTE-PP | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют | — |
| 71 | batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM486` | BATM-486 | Проверка Маршрутизации проводок CORP_CARD Withdrawal в ROUTE-PP | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют | — |
| 72 | batm_zod.BatmZodTests | `testBATM257` | BATM-257 | Проверка обработки сообщения о выгрузке в Агрегатор | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют | — |
| 73 | batm_zod.BatmZodTests | `testBATM351` | BATM-351 | Проверка успешного ЗОД в batm_transactions_agr | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; только в этом тесте: топик BATM_ZOD_IN_BATM_TRANS_AGR_ZOD_RESULT, топик BATM_ZOD_OUT_BATM_TRANS_AGR_ZOD | — |
| 74 | end_to_end.SolarEndToEnds | `testSolarE2e` | BATM-286, BATM-287, BATM-288, BATM-289, BATM-290, BATM-291 | Solar Air full e2e API test | API/solar_air/SolarE2E.xml → `BATM-286`<br>API/solar_air/SolarE2E.xml → `BATM-287`<br>API/solar_air/SolarE2E.xml → `BATM-288`<br>API/solar_air/SolarE2E.xml → `BATM-289`<br>API/solar_air/SolarE2E.xml → `BATM-290`<br>API/solar_air/SolarE2E.xml → `BATM-291` | solar-e2e-suite | нет | 2026-11-24 (a86b9ad «init») | только в этом тесте: таблица acc_account, таблица bo_accessor, таблица bo_agreement, таблица cls_classifier, таблица cls_classifier_type, таблица cls_classifier_value | — |
| 75 | single_thread.BatmClearingSingleThreadTests | `testBATM283` | BATM-283 | Проверка автоматической отправки уведомления в СУТП | API/batm_clearing/BatmClearingSingleThread.xml → `BATM-283` | batm-clearing | да | 2026-11-24 (a86b9ad «init») | — | — |
| 76 | common.NotTests | `listS3Files` | — | list s3 | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; нет @TmsLink; нет проверок | — |
| 77 | common.NotTests | `testb2lGenerator` | — | Тест для проверки работы генератора | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; нет @TmsLink | — |
| 78 | common.NotTests | `debugMail` | — | debug mail | Applied_Automation/NotTests.xml → `SMTP debug` | not-tests | нет | 2026-11-24 (a86b9ad «init») | нет @TmsLink; нет проверок | — |
| 79 | common.NotTests | `generateDataForUAT` | — | UAT | Applied_Automation/NotTests.xml → `UAT` | not-tests | нет | 2026-11-24 (a86b9ad «init») | нет @TmsLink; нет проверок | — |
| 80 | common.PrepareTestEnvironment | `updateOperday` | — | Setting up bank date to current date | Applied_Automation/PrepareEnvironment.xml → `update operation day` | test-environment-preparation | нет | 2026-11-24 (a86b9ad «init») | нет @TmsLink; нет проверок | — |
| 81 | common.PrepareTestEnvironment | `updateZodStartTime` | — | update zod_management set time_start = '23:30:00' | Applied_Automation/PrepareEnvironment.xml → `update zod start time` | test-environment-preparation | нет | 2026-11-24 (a86b9ad «init») | нет @TmsLink; нет проверок | — |
| 82 | common.PrepareTestEnvironment | `truncateTransactionsInRouteS3` | — | truncate table transactions in route S3 | Applied_Automation/PrepareEnvironment.xml → `truncate table transactions in route S3` | test-environment-preparation | нет | 2026-11-24 (a86b9ad «init») | нет @TmsLink; нет проверок | — |
| 83 | core.common.EnvConstants | `encryptString` | — | — | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; нет @TmsLink; нет проверок | protected static, @Test в src/main |
| 84 | core.common.EnvConstants | `decryptString` | — | — | — | — | — | 2026-11-24 (a86b9ad «init») | не входит ни в один сьют; нет @TmsLink; нет проверок | protected static, @Test в src/main |

### 3.3 Deprecated API библиотек (не размечено построчно)

60 из 84 тестовых методов транзитивно вызывают `CommonHelper.serializeToJsonString` (43) и/или
`CommonHelper.serializeToJsonStringIgnoringNullableFields` (60), где используется deprecated
`ObjectMapper.setSerializationInclusion` (CommonHelper.java:48, :57). Поле `CommonHelper.rusFaker` (CommonHelper.java:26)
создаётся через deprecated `new Locale(String, String)`.

### 3.4 Сводка по признакам

- **Не входят ни в один сьют (10):** `EnvConstants#encryptString`, `EnvConstants#decryptString` (оба `protected static`, в `src/main`);
  `BatmAccRouteS3Tests#testPerformance` (`@Ignore`); `BatmTransAgrToGeneratorTests#testBATM415/485/486` (класс не упомянут ни в одном XML,
  см. 3.5); `BatmZodTests#testBATM257`, `#testBATM351` (класс не упомянут ни в одном XML); `NotTests#listS3Files`, `#testb2lGenerator`.
- **Нет @TmsLink (10):** 4 метода `NotTests` (`listS3Files`, `testb2lGenerator`, `debugMail`, `generateDataForUAT`), 3 метода
  `PrepareTestEnvironment`, 2 метода `EnvConstants`, `BatmAccRouteS3Tests#testPerformance`.
- **Нет проверок (9):** `BatmSiorIntegrationTests#testBATM275` (только `softAssertTrue`, без `softAssertAll`, BatmSiorIntegrationTests.java:82),
  `NotTests#listS3Files`, `#debugMail`, `#generateDataForUAT`, `PrepareTestEnvironment#updateOperday`, `#updateZodStartTime`,
  `#truncateTransactionsInRouteS3`, `EnvConstants#encryptString`, `#decryptString`.
- **Единственная проверка — сообщение найдено в Kafka (4):** `BatmTransAgrTests#testBATM481…484`: тест заканчивается вызовом
  `KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA, resultAmount, long_timeout)`, то есть поиск идёт по подстроке-сумме.
- **@Deprecated / @Ignore:** `BatmAccTests#testBATM397` — `@Deprecated` + `@Ignore`, рядом комментарий «Этот функционал временно выпилен из сервиса»
  (BatmAccTests.java:111), при этом метод включён в `BatmAcc.xml` → `BATM-397`. `BatmAccRouteS3Tests#testPerformance` — `@Ignore`.

### 3.5 `<include>` на несуществующие методы

Поиск учитывает методы класса и всех его предков.

| XML | `<test name>` | `<class>` | `<include>` | Факт |
|---|---|---|---|---|
| API/batm_dp/BatmDp.xml | `BATM-298` | batm_dp.BatmDpTests | `testBATM298` | метода нет в классе и его предках |
| API/batm_dp/BatmDp.xml | `BATM-299` | batm_dp.BatmDpTests | `testBATM299` | метода нет в классе и его предках |
| API/batm_dp/BatmDp.xml | `BATM-400` | batm_dp.BatmDpTests | `testBATM400` | метода нет в классе и его предках |
| API/batm_dp/BatmDp.xml | `BATM-300` | batm_dp.BatmDpTests | `testBATM300` | метода нет в классе и его предках |
| API/batm_dp/BatmDp.xml | `BATM-301` | batm_dp.BatmDpTests | `testBATM301` | метода нет в классе и его предках |
| API/batm_dp/BatmDp.xml | `BATM-302` | batm_dp.BatmDpTests | `testBATM302` | метода нет в классе и его предках |
| API/batm_trans_agr/to_generator/BatmTransAgrToGenerator.xml | `BATM-485` | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM485` | метода нет в классе и его предках; метод с таким именем есть в batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests |
| API/batm_trans_agr/to_generator/BatmTransAgrToGenerator.xml | `BATM-486` | batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM486` | метода нет в классе и его предках; метод с таким именем есть в batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests |

Дополнительно по этим XML:
- `BatmDpTests.java` сейчас пуст (0 методов): коммит `ffecc53` «delete old dp tests» удалил 235 строк. Файл держит
  неиспользуемые импорты (`ZipUtils`, `BatmDpDP`, `MapsValuesConstants.*`, `KafkaTopic.BATM_TRANS_OUT_BATM_DP_STREAMING`).
  `ru.vtb.data_providers.BatmDpDP` (DataProvider-ы `BatmDpBATM300/301/302`) после этого нигде не используется: единственное упоминание —
  импорт в пустом `BatmDpTests.java:14`.
- `to_generator/BatmTransAgrToGenerator.xml` ссылается на класс `…to_aggregator.BatmTransAgrToAggregatorTests`, а не на
  `…to_generator.BatmTransAgrToGeneratorTests`. Его `<include name="testBATM415">` существует только как унаследованный от
  `BatmTransAgrTests` метод, поэтому запускается `BatmTransAgrTests#testBATM415`, а не переопределение из `BatmTransAgrToGeneratorTests`.
  Класс `BatmTransAgrToGeneratorTests` не упомянут ни в одном XML.
- В `BatmDp.xml` порядок `<test>`: 298, 299, **400**, 300, 301, 302, 658… (это только порядок, на результат не влияет).

### 3.6 @TmsLink ≠ `<test name>`

У методов с `@TmsLink` значение `<test name>` совпадает с одним из `@TmsLink` во всех XML. Расхождения есть только там, где
`@TmsLink` нет вовсе:

| XML | `<test name>` | Метод | @TmsLink метода |
|---|---|---|---|
| Applied_Automation/NotTests.xml | `SMTP debug` | common.NotTests#debugMail | — |
| Applied_Automation/NotTests.xml | `UAT` | common.NotTests#generateDataForUAT | — |
| Applied_Automation/PrepareEnvironment.xml | `update operation day` | common.PrepareTestEnvironment#updateOperday | — |
| Applied_Automation/PrepareEnvironment.xml | `update zod start time` | common.PrepareTestEnvironment#updateZodStartTime | — |
| Applied_Automation/PrepareEnvironment.xml | `truncate table transactions in route S3` | common.PrepareTestEnvironment#truncateTransactionsInRouteS3 | — |

Один метод — несколько `@TmsLink`:

- end_to_end.SolarEndToEnds#testSolarE2e: 6 @TmsLink (BATM-286, BATM-287, BATM-288, BATM-289, BATM-290, BATM-291); вызывается из 6 `<test>`: `BATM-286`, `BATM-287`, `BATM-288`, `BATM-289`, `BATM-290`, `BATM-291`

То есть `SolarE2E.xml` запускает один и тот же метод 6 раз (6 `<test>`), и каждый прогон публикует в Allure все 6 ссылок.

### 3.7 Методы-дубли из-за наследования тестовых классов

`BatmTransAgrToAggregatorTests` и `BatmTransAgrToGeneratorTests` наследуют `BatmTransAgrTests`, в котором есть 5 `@Test`-методов.

| Класс-наследник | Метод | Объявлен в | Вид |
|---|---|---|---|
| batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM481` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM482` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM483` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM484` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_aggregator.BatmTransAgrToAggregatorTests | `testBATM415` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM481` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM482` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM483` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM484` | batm_trans_agr.BatmTransAgrTests | унаследован |
| batm_trans_agr.to_generator.BatmTransAgrToGeneratorTests | `testBATM415` | batm_trans_agr.BatmTransAgrTests | переопределяет |

Факты о запуске: все XML используют `<methods><include>`, поэтому сейчас унаследованные методы под наследниками запускаются
только там, где включены явно: `BatmTransAgrToGenerator.xml` → `BATM-415` запускает `BatmTransAgrTests#testBATM415`
через класс `BatmTransAgrToAggregatorTests`. Если класс-наследник будет подключён без `<methods>` (или через `<package>`/группы),
TestNG выполнит 481–484 и 415 повторно, внутри наследника.

## 4. Классы и методы ядра (`ru.vtb.core`) без вызовов из тестов

### 4.1 Метод

1. **Байткод:** `javap -c -p -v` по `target/classes` и `target/test-classes` (296 классов), граф вызовов на уровне
   `класс.метод:дескриптор` (invoke*, method handles лямбд и method references, `new`/`checkcast`/`ldc` класса, `getstatic`/`putstatic`).
   Виртуальные вызовы разрешаются вверх по иерархии и вниз на переопределения.
2. **Точки входа:** все методы всех классов `src/test` (TestNG вызывает их рефлексией), методы с аннотациями `org.testng.annotations.*`
   в любом классе (например, `SoftAssertVerification.@BeforeMethod`), `TestNGListener` (подключён `@Listeners` в `BaseAPIState`)
   и `RetryAnalyzer` (подключается в `TestNGListener.transform`). `TestsRunner.main` в точки входа **не** включён: это запуск fat-jar, а не тест.
3. **Недостижимое = нет вызова из тестов ни прямо, ни транзитивно через ядро.** Для каждого кандидата приведён
   `grep -rnwE '<имя>' src TestXML` (без строки объявления): ноль совпадений — вызова нет нигде; ненулевые совпадения показаны.
4. **Не считаются находками:** геттеры, сеттеры, `equals/hashCode/toString/builder` Lombok у моделей и DTO (их вызывает Jackson
   рефлексией) и сгенерированные компилятором методы. Ниже по методам перечислены только методы, **явно написанные в исходниках**, кроме моделей.
5. **Константы** (`static final` примитивы и `String`) javac подставляет в место использования, поэтому в байткоде на эти
   классы нет ссылок. Они отмечены отдельно, их использование в исходниках видно по grep.

Результат: из 173 class-файлов ядра (с вложенными классами) 23 верхнеуровневых класса недостижимы из тестов (из них 6 — только константы, 9 — модели Kafka).

### 4.2 Классы

| Класс | Тип находки | Упоминания вне собственного файла (`grep -rnwE`) |
|---|---|---|
| core.annotations.BaseState | класс | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:14` if (method.getDeclaringClass().isAnnotationPresent(BaseState.class)) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:15` BaseState baseState = method.getDeclaringClass().getAnnotation(BaseState.class);<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:31` if (method.getDeclaringClass().isAnnotationPresent(BaseState.class)) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:32` BaseState baseState = method.getDeclaringClass().getAnnotation(BaseState.class); |
| core.annotations.Precondition | класс | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:20` if (method.isAnnotationPresent(Precondition.class)) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:21` Precondition precondition = method.getAnnotation(Precondition.class);<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:37` if (method.isAnnotationPresent(Precondition.class)) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:38` Precondition precondition = method.getAnnotation(Precondition.class); |
| core.annotations.PreconditionAnnotationParser | класс | **0 упоминаний** |
| core.common.test_runners.TestsRunner | класс | **0 упоминаний** |
| core.data.common_data.PreconditionsConstants | константы (javac инлайнит, ссылок в байткоде нет) | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:5` import ru.vtb.core.data.common_data.PreconditionsConstants;<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:48` case PreconditionsConstants.SolarUI.User.ADMIN_USER:<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:50` case PreconditionsConstants.SolarUI.User.BASIC_USER:<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:62` case PreconditionsConstants.SolarUI.Page.LOGIN_PAGE:<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:64` case PreconditionsConstants.SolarUI.Page.HOME_PAGE: |
| core.data.common_data.UserData | класс | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:6` import ru.vtb.core.data.common_data.UserData;<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:12` public static UserData parseUser(Method method) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:13` UserData userData = null;<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:46` private static UserData getUser(String user) {<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:49` return new UserData(EnvConstants.BASIC_USER_LOGIN);<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:51` return new UserData("name"); |
| core.data.static_data.CodesNamesValues | константы (javac инлайнит, ссылок в байткоде нет) | `test/java/ru/vtb/api/services/GetAccountListService.java:17` import static ru.vtb.core.data.static_data.CodesNamesValues.EXTERNAL;<br>`test/java/ru/vtb/api/services/SaveAccountService.java:16` import static ru.vtb.core.data.static_data.CodesNamesValues.EXTERNAL;<br>`test/java/ru/vtb/api/models/batm/generators/SaveAgreementGenerator.java:15` import static ru.vtb.core.data.static_data.CodesNamesValues.*;<br>`main/java/ru/vtb/core/models/kafka/batm_in_siorus_devices/generators/BatmInSiorusDevicesMessageGenerator.java:4` import ru.vtb.core.data.static_data.CodesNamesValues;<br>`main/java/ru/vtb/core/models/kafka/batm_in_siorus_devices/generators/BatmInSiorusDevicesMessageGenerator.java:36` vendor = (String) fields.getOrDefault(MapsKeysConstants.VENDOR, CodesNamesValues.NCR); |
| core.data.static_data.MapsKeysConstants | константы (javac инлайнит, ссылок в байткоде нет) | `test/java/ru/vtb/api/services/GetAccountListService.java:18` import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;<br>`test/java/ru/vtb/api/services/SaveAccountService.java:17` import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;<br>`test/java/ru/vtb/api/models/batm/generators/SaveAgreementGenerator.java:16` import static ru.vtb.core.data.static_data.MapsKeysConstants.*;<br>`test/java/ru/vtb/api/models/batm/generators/HeaderGenerator.java:11` import static ru.vtb.core.data.static_data.MapsKeysConstants.ORIGINATOR;<br>`test/java/ru/vtb/api/models/batm/generators/HeaderGenerator.java:12` import static ru.vtb.core.data.static_data.MapsKeysConstants.RESPONSE_PARAMS;<br>`test/java/ru/vtb/api/tests/batm_trans_agr/to_aggregator/BatmTransAgrToAggregatorTests.java:17` import static ru.vtb.core.data.static_data.MapsKeysConstants.ACCOUNT_NUMBER;<br>… всего 40 |
| core.data.static_data.MapsValuesConstants | константы (javac инлайнит, ссылок в байткоде нет) | `test/java/ru/vtb/api/tests/batm_dp/BatmDpTests.java:25` import static ru.vtb.core.data.static_data.MapsValuesConstants.*;<br>`test/java/ru/vtb/data_providers/BatmDpDP.java:9` import static ru.vtb.core.data.static_data.MapsValuesConstants.*; |
| core.data.static_data.ResponseTypes | константы (javac инлайнит, ссылок в байткоде нет) | `test/java/ru/vtb/api/common/assertions/Conditions.java:7` import ru.vtb.core.data.static_data.ResponseTypes;<br>`test/java/ru/vtb/api/common/assertions/Conditions.java:16` return new ResponseTypeCondition(ResponseTypes.SUCCESS); |
| core.data.static_data.S3Constants | константы (javac инлайнит, ссылок в байткоде нет) | `test/java/ru/vtb/api/tests/batm_trans_agr/BatmTransAgrTests.java:30` import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;<br>`test/java/ru/vtb/api/tests/batm_trans_agr/BatmTransAgrTests.java:31` import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;<br>`test/java/ru/vtb/api/tests/batm_acc_route_s3/BatmAccRouteS3Tests.java:30` import static ru.vtb.core.data.static_data.S3Constants.AGGREGATION;<br>`test/java/ru/vtb/api/tests/batm_acc_route_s3/BatmAccRouteS3Tests.java:31` import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_AGR_OUT;<br>`test/java/ru/vtb/api/tests/batm_clearing/BatmClearingTests.java:25` import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;<br>`test/java/ru/vtb/api/tests/batm_clearing/BatmClearingTests.java:26` import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;<br>… всего 14 |
| core.models.kafka.batm_acc_route_pp_in_batm_oper_sior_pp_result.BatmAccRoutePpInBatmOperSiorPpResultRootJson | модель Kafka | **0 упоминаний** |
| core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_data.Credit | модель Kafka | `main/java/ru/vtb/core/models/kafka/batm_acc_route_pp_in_batm_trans_pp_data/BatmAccRoutePpInBatmTransPpDataRootMessage.java:41` private Credit credit; |
| core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_data.Debit | модель Kafka | `main/java/ru/vtb/core/models/kafka/batm_acc_route_pp_in_batm_trans_pp_data/BatmAccRoutePpInBatmTransPpDataRootMessage.java:38` private Debit debit; |
| core.models.kafka.batm_clearing_out_batm_trans_data.BodyItem | модель Kafka | **0 упоминаний** |
| core.models.kafka.batm_clearing_out_batm_trans_data.Header | модель Kafka | **0 упоминаний** |
| core.models.kafka.batm_trans_out_batm_dp_inst.Credit | модель Kafka | `main/java/ru/vtb/core/models/kafka/batm_trans_out_batm_dp_inst/BatmTransOutBatmDpInstRootMessage.java:65` private Credit credit; |
| core.models.kafka.batm_trans_out_batm_dp_inst.Debit | модель Kafka | `main/java/ru/vtb/core/models/kafka/batm_trans_out_batm_dp_inst/BatmTransOutBatmDpInstRootMessage.java:62` private Debit debit; |
| core.models.kafka.sutp_events.ParametersItem | модель Kafka | `main/java/ru/vtb/core/models/kafka/sutp_events/SutpEventsRootMessage.java:48` private List<ParametersItem> parameters;<br>`main/java/ru/vtb/core/models/kafka/sutp_events/generators/SutpEventsRootMessageGenerator.java:25` //                        ParametersItem.builder()<br>`main/java/ru/vtb/core/models/kafka/sutp_events/generators/SutpEventsRootMessageGenerator.java:29` //                        ParametersItem.builder() |
| core.models.kafka.sutp_events.generators.SutpEventsRootMessageGenerator | модель Kafka | **0 упоминаний** |
| core.utils.B2lDecodingHelper | класс | **0 упоминаний** |
| core.utils.TestRunnerUtils | класс | `main/java/ru/vtb/core/common/test_runners/TestsRunner.java:4` import ru.vtb.core.utils.TestRunnerUtils;<br>`main/java/ru/vtb/core/common/test_runners/TestsRunner.java:11` TestRunnerUtils.unpackResources(); |
| core.utils.ZipUtils | класс | `test/java/ru/vtb/api/tests/batm_dp/BatmDpTests.java:13` import ru.vtb.core.utils.ZipUtils;<br>`main/java/ru/vtb/core/utils/TestRunnerUtils.java:94` ZipUtils.extractEntry(jarPath.toString(), "custom.xml", PROJECT_DIRECTORY);<br>`main/java/ru/vtb/core/utils/TestRunnerUtils.java:95` ZipUtils.extractFolder(jarPath.toString(), "main_resources", PROJECT_DIRECTORY); |

Пояснения к классам (факты по выводу grep выше):
- `Precondition`, `BaseState`, `PreconditionAnnotationParser`, `PreconditionsConstants`, `UserData` — замкнутая группа: все
  упоминания внутри `PreconditionAnnotationParser`, а сам `PreconditionAnnotationParser` не упоминается нигде (0).
  Аннотации `@Precondition`/`@BaseState` не стоят ни на одном методе (grep `@Precondition\|@BaseState` по `src` — 0 совпадений вне объявлений).
- `TestsRunner` — `mainClass` fat-jar (pom.xml:177); из тестов не вызывается. `TestRunnerUtils` вызывается только из `TestsRunner`,
  `ZipUtils` — только из `TestRunnerUtils` (плюс неиспользуемый импорт в пустом `BatmDpTests.java:13`).
- `B2lDecodingHelper` — 0 упоминаний вне собственного файла.
- `SutpEventsRootMessageGenerator`, `BatmAccRoutePpInBatmOperSiorPpResultRootJson`, `batm_clearing_out_batm_trans_data.BodyItem`
  и `.Header` — 0 упоминаний по полному имени, по импорту пакета и внутри своего пакета.
- `batm_acc_route_pp_in_batm_trans_pp_data.Credit/Debit`, `batm_trans_out_batm_dp_inst.Credit/Debit`, `sutp_events.ParametersItem`
  упоминаются только как поля своих корневых моделей. Сами корневые модели достижимы (через `KafkaTopic`) — это
  десериализуемые вложенные объекты, и Jackson создаёт их рефлексией. В байткоде тестов обращений к ним нет.
- `MapsValuesConstants` используется только в `BatmDpDP` (сам не используется, см. 3.5) и в импорте пустого `BatmDpTests`.
  `CodesNamesValues`, `MapsKeysConstants`, `ResponseTypes`, `S3Constants` используются тестами (константы).

### 4.3 Методы (явно объявленные, в достижимых и недостижимых классах ядра, кроме моделей)

Перегрузки различаются по дескриптору: например, у `CommonHelper.deserializeJsonToObject` тесты вызывают вариант с `String`,
а вариант с `File` не вызывается. Совпадения grep вида `s3Client.createBucket(...)` — это вызовы метода AWS SDK, а не `S3Service`.
`SemiTransaction.compareTo` — реализация `Comparable`: её может вызывать JDK (сортировка, `TreeSet`), а явного вызова в коде нет.

| Класс | Метод (дескриптор байткода) | Строка | Упоминания имени вне строки объявления (`grep -rnwE`) |
|---|---|---|---|
| core.annotations.PreconditionAnnotationParser | `parseUser(Ljava/lang/reflect/Method;)` | 12 | **0** |
| core.annotations.PreconditionAnnotationParser | `parseUrl(Ljava/lang/reflect/Method;)` | 29 | **0** |
| core.annotations.PreconditionAnnotationParser | `getUser(Ljava/lang/String;)` | 46 | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:17` userData = getUser(baseState.page());<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:23` userData = getUser(precondition.user()); |
| core.annotations.PreconditionAnnotationParser | `getPageUrl(Ljava/lang/String;)` | 57 | `main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:34` url = getPageUrl(baseState.page());<br>`main/java/ru/vtb/core/annotations/PreconditionAnnotationParser.java:40` url = getPageUrl(precondition.page()); |
| core.common.test_runners.TestsRunner | `main([Ljava/lang/String;)` | 10 | `grep -rn TestsRunner src pom.xml TestXML` (вне собственного файла): `pom.xml:177` `<mainClass>ru.vtb.core.common.test_runners.TestsRunner</mainClass>` — точка входа fat-jar; `grep -w main` даёт только посторонние совпадения слова «main» |
| core.data.static_data.SemiTransaction | `compareTo(Lru/vtb/core/data/static_data/SemiTransaction;)` | 141 | `main/java/ru/vtb/core/models/kafka/batm_acc_route_in_batm_trans_pp_data/BatmAccRouteInBatmTransPpDataRootMessage.java:57` public int compareTo(@NotNull BatmAccRouteInBatmTransPpDataRootMessage o) {<br>`main/java/ru/vtb/core/models/kafka/batm_acc_route_s3_in_batm_trans_agr_data/TransactionsItem.java:49` public int compareTo(@NotNull TransactionsItem o) {<br>`main/java/ru/vtb/core/models/kafka/batm_acc_route_s3_in_batm_trans_agr_data/TransactionsItem.java:50` return Comparator.comparing(TransactionsItem::getAggregationId, Comparator.nulls |
| core.data.static_data.SemiTransaction | `compareTo(Ljava/lang/Object;)` | 141 | `main/java/ru/vtb/core/models/kafka/batm_acc_route_in_batm_trans_pp_data/BatmAccRouteInBatmTransPpDataRootMessage.java:57` public int compareTo(@NotNull BatmAccRouteInBatmTransPpDataRootMessage o) {<br>`main/java/ru/vtb/core/models/kafka/batm_acc_route_s3_in_batm_trans_agr_data/TransactionsItem.java:49` public int compareTo(@NotNull TransactionsItem o) {<br>`main/java/ru/vtb/core/models/kafka/batm_acc_route_s3_in_batm_trans_agr_data/TransactionsItem.java:50` return Comparator.comparing(TransactionsItem::getAggregationId, Comparator.nulls |
| core.utils.B2LHelper | `getNewPurchaseByCashTransactionReference()` | 74 | **0** |
| core.utils.B2LHelper | `getNewPurchaseByCashReversedTransactionReference()` | 85 | **0** |
| core.utils.B2LHelper | `convertB2lFileNameToUfx(Ljava/lang/String;)` | 147 | **0** |
| core.utils.B2LHelper | `convertB2lFileNameToJsonUfx(Ljava/lang/String;)` | 159 | **0** |
| core.utils.B2lDecodingHelper | `decodeFile(Ljava/lang/String;Ljava/lang/String;)` | 29 | **0** |
| core.utils.B2lDecodingHelper | `extractFilesFromZip(Ljava/io/InputStream;)` | 39 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:31` var map = extractFilesFromZip(zipStream); |
| core.utils.B2lDecodingHelper | `verifySignature([B[B)` | 67 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:33` verifySignature(b2lFileBytes, map.get("signature.sig")); |
| core.utils.B2lDecodingHelper | `getPublicKeyFromCertificate()` | 92 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:73` var publicKey = (RSAPublicKey) getPublicKeyFromCertificate(); |
| core.utils.B2lDecodingHelper | `decryptRsaSessionKey([B)` | 102 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:34` var decryptSessionKey = decryptRsaSessionKey(map.get("session.key.enc")); |
| core.utils.B2lDecodingHelper | `getPrivateKey()` | 115 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:107` cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(), parameterSpec); |
| core.utils.B2lDecodingHelper | `decryptAes([B[B[B)` | 131 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:35` var decryptedFile = decryptAes(b2lFileBytes, hexToBytes(decryptSessionKey), map. |
| core.utils.B2lDecodingHelper | `extractFileName(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)` | 145 | **0** |
| core.utils.B2lDecodingHelper | `getFileNameWithoutExt(Ljava/lang/String;)` | 153 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:32` var b2lFileBytes = map.get(getFileNameWithoutExt(Path.of(encodedFilePath).getFil |
| core.utils.B2lDecodingHelper | `hexToBytes([B)` | 165 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:35` var decryptedFile = decryptAes(b2lFileBytes, hexToBytes(decryptSessionKey), map.<br>`main/java/ru/vtb/core/utils/B2lDecodingHelper.java:135` var ivParameterSpec = new IvParameterSpec(hexToBytes(vector)); |
| core.utils.B2lDecodingHelper | `getDecompressData([B)` | 186 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:36` return FileHelper.saveInputStreamToFile(getDecompressData(decryptedFile), result |
| core.utils.CommonHelper | `deserializeXmlToObject(Ljava/lang/String;Ljava/lang/Class;)` | 64, 72 (перегрузок: 2) | **0** |
| core.utils.CommonHelper | `deserializeXmlToObject(Ljava/io/File;Ljava/lang/Class;)` | 64, 72 (перегрузок: 2) | **0** |
| core.utils.CommonHelper | `deserializeJsonToObject(Ljava/io/File;Ljava/lang/Class;)` | 80, 89 (перегрузок: 2) | `test/java/ru/vtb/api/tests/batm_acc_route_s3/BatmAccRouteS3Tests.java:244` CommonHelper.deserializeJsonToObject(kafkaResponse, BatmAccRouteS3OutBatmTransAg<br>`test/java/ru/vtb/api/tests/batm_clearing/BatmClearingTests.java:86` var myJson = CommonHelper.deserializeJsonToObject(myUfx, BatmClearingOutBatmTran<br>`test/java/ru/vtb/api/tests/batm_clearing/BatmClearingTests.java:122` var myJson = CommonHelper.deserializeJsonToObject(myUfx, BatmClearingOutBatmTran<br>`test/java/ru/vtb/common/NotTests.java:75` var myJson1 = CommonHelper.deserializeJsonToObject(UfxHelper.generateJsonUfxFile<br>… всего 11 |
| core.utils.CommonHelper | `convertJsonToXml(Ljava/lang/String;)` | 98 | **0** |
| core.utils.CommonHelper | `checkTableIsEmpty(Ljava/util/function/Supplier;)` | 144 | **0** |
| core.utils.DBHelper | `getServiceProviderDataByAgreementNumber(Ljava/lang/String;)` | 38 | **0** |
| core.utils.DBHelper | `getTransactionDataByRRN(Ljava/lang/String;)` | 73, 84 (перегрузок: 2) | **0** |
| core.utils.DBHelper | `getTransactionDataByRRN(Ljava/lang/String;Ljava/lang/String;[J)` | 73, 84 (перегрузок: 2) | **0** |
| core.utils.DBHelper | `getClassifierDataByAgreementNumber(Ljava/lang/String;J)` | 102 | **0** |
| core.utils.DBHelper | `getAgreementDataWithLinkedDevices(Ljava/lang/String;)` | 166 | **0** |
| core.utils.DBHelper | `getAgreementDataByAccessorNumber(Ljava/lang/String;J)` | 182 | **0** |
| core.utils.DBHelper | `getAgreementIdByAgreementNumber(Ljava/lang/String;)` | 222 | **0** |
| core.utils.DBHelper | `getDebitOrCreditAccountsNumber(Ljava/lang/String;)` | 245 | **0** |
| core.utils.DBHelper | `getTransactionShowcaseDataByAggregationKey(Ljava/lang/String;[J)` | 285 | **0** |
| core.utils.DBHelper$BatmDP | `getOperationDetails(Ljava/lang/String;)` | 508 | **0** |
| core.utils.DBHelper$BatmDP | `getOutboxMessagesData(Ljava/lang/String;Ljava/lang/String;)` | 517 | **0** |
| core.utils.DBHelper$BatmDP | `getOutboxMessagesSentData(Ljava/util/List;)` | 532 | **0** |
| core.utils.DBHelper$Clearing | `getTimeStartInManagementZod()` | 435 | **0** |
| core.utils.FileHelper | `saveInputStreamToFile(Ljava/io/InputStream;Ljava/lang/String;)` | 52 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:36` return FileHelper.saveInputStreamToFile(getDecompressData(decryptedFile), result |
| core.utils.FileHelper | `fileToInputStream(Ljava/lang/String;)` | 61 | `main/java/ru/vtb/core/utils/B2lDecodingHelper.java:30` InputStream zipStream = FileHelper.fileToInputStream(encodedFilePath); |
| core.utils.KafkaHelper | `getAllRecords(Lru/vtb/core/data/static_data/KafkaTopic;Ljava/util/function/Predicate;J)` | 37, 49, 53 (перегрузок: 3) | `main/java/ru/vtb/core/utils/KafkaHelper.java:50` return getAllRecords(topic, compilePredicate(keywords), timeout);<br>`main/java/ru/vtb/core/utils/KafkaHelper.java:54` return getAllRecords(topic, List.of(keyword), timeout); |
| core.utils.KafkaHelper | `getAllRecords(Lru/vtb/core/data/static_data/KafkaTopic;Ljava/util/List;J)` | 37, 49, 53 (перегрузок: 3) | `main/java/ru/vtb/core/utils/KafkaHelper.java:50` return getAllRecords(topic, compilePredicate(keywords), timeout);<br>`main/java/ru/vtb/core/utils/KafkaHelper.java:54` return getAllRecords(topic, List.of(keyword), timeout); |
| core.utils.KafkaHelper | `getAllRecords(Lru/vtb/core/data/static_data/KafkaTopic;Ljava/lang/String;J)` | 37, 49, 53 (перегрузок: 3) | `main/java/ru/vtb/core/utils/KafkaHelper.java:50` return getAllRecords(topic, compilePredicate(keywords), timeout);<br>`main/java/ru/vtb/core/utils/KafkaHelper.java:54` return getAllRecords(topic, List.of(keyword), timeout); |
| core.utils.S3Service | `doesBucketExist(Ljava/lang/String;)` | 54 | **0** |
| core.utils.S3Service | `createBucket(Ljava/lang/String;)` | 68 | `main/java/ru/vtb/core/utils/S3Service.java:74` s3Client.createBucket(bucketRequest); |
| core.utils.S3Service | `deleteBucket(Ljava/lang/String;)` | 77 | `main/java/ru/vtb/core/utils/S3Service.java:84` s3Client.deleteBucket(deleteBucketRequest); |
| core.utils.S3Service | `waitForObjectsNamesContain(Ljava/lang/String;Ljava/lang/String;J)` | 104 | **0** |
| core.utils.S3Service | `copyObject(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)` | 161 | `main/java/ru/vtb/core/utils/S3Service.java:175` return s3Client.copyObject(copyObjectRequest); |
| core.utils.S3Service | `deleteObjects(Ljava/lang/String;Ljava/util/List;)` | 189 | `main/java/ru/vtb/core/utils/S3Service.java:204` s3Client.deleteObjects(deleteObjectRequest); |
| core.utils.S3Service | `getBucketsNames()` | 207 | **0** |
| core.utils.S3Service | `getAllObjectsInBucket(Ljava/lang/String;)` | 213 | **0** |
| core.utils.TestRunnerUtils | `unpackResources()` | 27 | `main/java/ru/vtb/core/common/test_runners/TestsRunner.java:11` TestRunnerUtils.unpackResources(); |
| core.utils.TestRunnerUtils | `unpackSingleResource(Ljava/util/jar/JarFile;Ljava/util/jar/JarEntry;Ljava/io/File;)` | 77 | `main/java/ru/vtb/core/utils/TestRunnerUtils.java:60` //                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTOR<br>`main/java/ru/vtb/core/utils/TestRunnerUtils.java:63` //                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTOR<br>`main/java/ru/vtb/core/utils/TestRunnerUtils.java:69` //                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTOR |
| core.utils.TestRunnerUtils | `tryToUnpackFolder()` | 87 | **0** |
| core.utils.TestRunnerUtils | `createTempFile()` | 98 | **0** |
| core.utils.ZipUtils | `compressToZip(Ljava/io/File;[Ljava/lang/String;)` | 18 | **0** |
| core.utils.ZipUtils | `extractEntry(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)` | 35 | `main/java/ru/vtb/core/utils/TestRunnerUtils.java:94` ZipUtils.extractEntry(jarPath.toString(), "custom.xml", PROJECT_DIRECTORY); |
| core.utils.ZipUtils | `extractFolder(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)` | 57 | `main/java/ru/vtb/core/utils/TestRunnerUtils.java:95` ZipUtils.extractFolder(jarPath.toString(), "main_resources", PROJECT_DIRECTORY); |
| core.utils.sql.SQLHelper | `getRowsFromDB(Ljava/lang/String;[Ljava/lang/String;)` | 139 | **0** |
| core.utils.sql.SQLHelper | `getColumnValues(Ljava/lang/String;Lru/vtb/core/utils/sql/results/ColumnIndexSupplier;[Ljava/lang/Object;)` | 199 | `main/java/ru/vtb/core/utils/sql/SQLHelper.java:144` Map<String, List<String>> result = getColumnValues(executeQuery, rsmd -> getColu |

## 5. Что ещё бросилось в глаза (без правок, только факты)

- `TestsRunner.main` всегда завершается `System.exit(0)` (TestsRunner.java:17), поэтому код возврата fat-jar не зависит от результата тестов.
  В CLAUDE.md сказано, что CI опирается на код возврата.
- `KafkaClient.java:80` — `System.exit(-1)` внутри ядра.
- `TestNGListener` в статическом блоке вызывает `System.getProperty("threadCount").matches(...)`: без `-DthreadCount` будет NPE при загрузке класса (TestNGListener.java:23).
- `SoftAssertVerification.softAssertEquals/NotEquals` вызывают `actual.equals(expected)`: при `actual == null` — NPE.
- `src/test/resources/tempFiles/*` (temp.csv, temp.json, temp.xml, base2like, downloadedFiles/tempfile) лежат в репозитории.

## 6. Что проверить на ift

На стенде ничего не запускалось (сетевого доступа нет). Код не менялся, поэтому регрессии от этой работы нет.
Вопросы к сьютам, которые стоит посмотреть на ift:

| Сьют | Что посмотреть |
|---|---|
| `TestXML/API/batm_dp/BatmDp.xml` | 6 `<test>` (298–302, 400) ссылаются на методы, которых нет. Как TestNG/CI ведут себя на таком include (тест пропускается молча или сьют падает) |
| `TestXML/API/batm_trans_agr/to_generator/BatmTransAgrToGenerator.xml` | 485 и 486 указывают на несуществующие методы класса ToAggregator; 415 запускает версию из `BatmTransAgrTests`, а не переопределение из `BatmTransAgrToGeneratorTests` |
| `TestXML/API/batm_clearing/BatmClearingSingleThread.xml` | при `-DthreadCount>1` `parallel="none"` перекрывается слушателем (имя сьюта не содержит `singleThread`) |
| `TestXML/API/batm_acc/BatmAcc.xml` | `BATM-397` включён, а метод помечен `@Ignore` + `@Deprecated` |
| `TestXML/API/batm_devices/BatmDevices.xml` | `BATM-275` не может упасть по своей проверке (нет `softAssertAll`) |
| `TestXML/API/solar_air/SolarE2E.xml` | один метод запускается 6 раз |
| `TestXML/API/batm_trans_agr/BatmTransAgr.xml` | 481–484: проверка — только наличие сообщения с суммой в топике |
| — (нет XML) | `BatmZodTests` (BATM-257, BATM-351) и `BatmTransAgrToGeneratorTests` не входят ни в один сьют |
