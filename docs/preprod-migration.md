# Обновление preprod.properties: Kafka-ключи основного кластера

Новые ключи основного кластера Kafka — с префиксом `kafka.batm.` (docs/kafka-design.md, раздел 1.3).
До обновления файла прогон на preprod работает со старыми ключами, но в логе при старте есть предупреждения:

```
[WARNING] ... Устаревший ключ 'bootstrap.servers' в properties: переименуйте его в 'kafka.batm.bootstrap.servers' (docs/preprod-migration.md)
```

Значения **не меняются**: переименовываются только ключи (пароли остаются зашифрованными тем же `-Dekey`).

## Что переименовать

| Было | Стало |
|---|---|
| `bootstrap.servers` | `kafka.batm.bootstrap.servers` |
| `ssl.truststore.type` | `kafka.batm.ssl.truststore.type` |
| `ssl.truststore.password` | `kafka.batm.ssl.truststore.password` |
| `ssl.keystore.type` | `kafka.batm.ssl.keystore.type` |
| `ssl.keystore.password` | `kafka.batm.ssl.keystore.password` |

Ключи `kafka.uasp.*`, `kafka.ods.*` и все остальные не трогаются.

## Шаги на контуре

Файл лежит рядом с fat-jar: `src/main/resources/properties/preprod.properties`. Команды выполняются из каталога,
где лежит jar.

1. Резервная копия:

```bash
cp src/main/resources/properties/preprod.properties src/main/resources/properties/preprod.properties.bak
```

2. Переименование ключей (только 5 строк из таблицы; строки, где ключ уже с префиксом, не меняются):

```bash
sed -i -E 's/^(bootstrap\.servers|ssl\.(truststore|keystore)\.(type|password))=/kafka.batm.\1=/' src/main/resources/properties/preprod.properties
```

3. Проверка — должно быть ровно 5 строк, все начинаются с `kafka.batm.`:

```bash
grep -nE '^(kafka\.batm\.)?(bootstrap\.servers|ssl\.(truststore|keystore)\.(type|password))=' src/main/resources/properties/preprod.properties
```

4. Проверка, что значения не изменились (вывод должен быть пустым):

```bash
diff <(sed -E 's/^kafka\.batm\.//' src/main/resources/properties/preprod.properties) src/main/resources/properties/preprod.properties.bak
```

5. Первый прогон после обновления: в логе нет предупреждений «Устаревший ключ», есть строка
   `Kafka-кластер 'batm': читаем N партиций топиков [...] с конца`.

## Если что-то пошло не так

- **Ошибка «Заданы оба ключа 'kafka.batm.X' и устаревший 'X' с разными значениями»** — в файле остались обе строки
  с разными значениями. Удалите строку со старым ключом `X` (значение в `kafka.batm.X` должно быть правильным).
- **Прогон падает при старте с «Kafka-кластер 'batm': ...»** — это не связано с переименованием (позиции не
  зафиксированы, топик не существует, ошибка подключения/SSL). Текст ошибки называет топик или этап.
- **Откат**:

```bash
mv src/main/resources/properties/preprod.properties.bak src/main/resources/properties/preprod.properties
```

Старые ключи поддерживаются до следующего релиза (после него — только `kafka.batm.*`).

## Необязательные новые ключи (значения по умолчанию подходят, добавлять не нужно)

| Ключ | По умолчанию | Назначение |
|---|---|---|
| `kafka.<имя>.ssl.truststore.location` / `.keystore.location` | `kafka/<cenv>-batm-*.p12` | своё хранилище для кластера (путь от `src/main/resources`) |
| `kafka.<имя>.topic.prefix` | пусто | префикс имён топиков кластера на контуре |
| `kafka.startup.timeout` | `PT60S` | ожидание фиксации позиций при старте |
| `kafka.store.max.messages.per.topic` | `50000` | лимит сообщений в памяти на топик |
| `kafka.store.ttl` | `PT60M` | время жизни сообщения в памяти |
| `kafka.partitions.refresh` | `PT30S` | как часто искать новые партиции |
