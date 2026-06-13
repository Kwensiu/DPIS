# Localization Scripts

## English

Use `normalize-android-strings.py` from the repository root.

### Scenario 1: A contributor sends a full Russian strings file

Put the file in the repository root, for example `ru_string_02.xml`, then run:

```bash
python scripts/l10n/normalize-android-strings.py ru_string_02.xml \
  --output app/src/main/res/values-ru/strings.xml \
  --missing-report docs/generated/ru-missing-strings.xml
```

Result:

- `app/src/main/res/values-ru/strings.xml` is the Android resource file to keep.
- `docs/generated/ru-missing-strings.xml` is a full maintainer view: translated
  keys are normal lines, and missing keys are commented out with the English text.
- Missing keys are not written to `values-ru`; Android falls back to English.
- Keys marked `translatable="false"` in the default resource file are removed
  from localized output and are not listed as missing.

### Scenario 2: The script says placeholder mismatches were found

Do not force the output first. Open the reported keys and compare them with
`app/src/main/res/values/strings.xml`.

Fix the translation so placeholders such as `%1$s`, `%2$d`, and `%%` match the
default string, then run the command again.

### Scenario 3: The app added new English strings

Run the same command again:

```bash
python scripts/l10n/normalize-android-strings.py ru_string_02.xml \
  --output app/src/main/res/values-ru/strings.xml \
  --missing-report docs/generated/ru-missing-strings.xml
```

New untranslated keys will appear as commented-out lines in
`docs/generated/ru-missing-strings.xml`. They will fall back to English until
someone translates them.

### Scenario 4: Check the result before committing

Run:

```bash
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.StringResourceParityTest
```

For a quick build check, run:

```bash
./gradlew :app:assembleModern101Debug
```

### Scenario 5: Find possibly unused string keys

Run:

```bash
python scripts/l10n/find-unused-android-strings.py
```

This only reports candidates. Check each key manually before deleting it.

To also treat unit tests as references, run:

```bash
python scripts/l10n/find-unused-android-strings.py --include-tests
```

## 中文

在仓库根目录使用 `normalize-android-strings.py`。

### 场景 1：贡献者发来了完整俄语 strings 文件

把文件放到仓库根目录，例如 `ru_string_02.xml`，然后运行：

```bash
python scripts/l10n/normalize-android-strings.py ru_string_02.xml \
  --output app/src/main/res/values-ru/strings.xml \
  --missing-report docs/generated/ru-missing-strings.xml
```

结果：

- `app/src/main/res/values-ru/strings.xml` 是要保留的 Android 资源文件。
- `docs/generated/ru-missing-strings.xml` 是给维护者看的完整对照文件：已有翻译是普通行，缺失 key 会用英文内容注释掉。
- 缺失 key 不会写进 `values-ru`，Android 会自动回退到英文。
- 默认资源里标记为 `translatable="false"` 的 key 会从本地化输出中移除，也不会列入缺失翻译。

### 场景 2：脚本提示 placeholder mismatches

先不要强制输出。打开脚本报告的 key，和
`app/src/main/res/values/strings.xml` 对比。

把翻译里的 `%1$s`、`%2$d`、`%%` 这类占位符修到和默认英文一致，再重新运行命令。

### 场景 3：App 新增了英文字符串

还是运行同一条命令：

```bash
python scripts/l10n/normalize-android-strings.py ru_string_02.xml \
  --output app/src/main/res/values-ru/strings.xml \
  --missing-report docs/generated/ru-missing-strings.xml
```

新的未翻译 key 会以注释行出现在 `docs/generated/ru-missing-strings.xml`。
在有人翻译前，它们会自动回退到英文。

### 场景 4：提交前检查

运行：

```bash
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.StringResourceParityTest
```

如果想快速确认能构建，再运行：

```bash
./gradlew :app:assembleModern101Debug
```

### 场景 5：查看疑似已经没用的字符串 key

运行：

```bash
python scripts/l10n/find-unused-android-strings.py
```

这个脚本只给候选结果。删除前需要人工确认每个 key。

如果想把单元测试里的引用也算进去，运行：

```bash
python scripts/l10n/find-unused-android-strings.py --include-tests
```
