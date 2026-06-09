package dev.shanternal.xml2json.service;

import dev.shanternal.xml2json.exception.InvalidXmlException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class Xml2JsonServiceTest {

    private final Xml2JsonService service = new Xml2JsonService();

    /**
     * Конвертирует XML и парсит результат как обобщённый JSON-документ.
     */
    private JSONObject convertAndParse(String xml) {
        String json = service.convert(xml);
        return assertDoesNotThrow(() -> new JSONObject(json),
                () -> "convert() вернул невалидный JSON: " + json);
    }

    /** Рекурсивно ищет в дереве JSON значение, чья строковая форма равна expected. */
    private boolean containsValueRecursively(Object node, String expected) {
        if (node instanceof JSONObject obj) {
            for (String key : obj.keySet()) {
                if (containsValueRecursively(obj.get(key), expected)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof org.json.JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                if (containsValueRecursively(arr.get(i), expected)) {
                    return true;
                }
            }
            return false;
        }
        return String.valueOf(node).equals(expected);
    }

    /** Рекурсивно ищет ключ с заданным именем в дереве JSON на любом уровне вложенности. */
    private boolean containsKeyRecursively(Object node, String key) {
        if (node instanceof JSONObject obj) {
            if (obj.has(key)) {
                return true;
            }
            for (String k : obj.keySet()) {
                if (containsKeyRecursively(obj.get(k), key)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof org.json.JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                if (containsKeyRecursively(arr.get(i), key)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Nested
    @DisplayName("Результат всегда является валидным JSON-документом")
    class ResultIsValidJson {

        @ParameterizedTest(name = "[{index}] {0}")
        @DisplayName("Любой корректный XML конвертируется в строку, разбираемую как JSON")
        @ValueSource(strings = {
                "<ping>pong</ping>",
                "<user><id>1</id><name>Ivan</name></user>",
                "<product id=\"42\" category=\"books\"><name>Clean Code</name></product>",
                "<item><price>99.99</price><inStock>true</inStock></item>",
                "<order><customer><name>Maria</name></customer></order>",
        })
        void validXmlProducesParsableJson(String xml) {
            assertDoesNotThrow(() -> convertAndParse(xml));
        }
    }

    @Nested
    @DisplayName("Текстовое содержимое тегов сохраняется в результате")
    class TextContentIsPreserved {

        @Test
        @DisplayName("Значение единственного тега присутствует где-то в результирующем JSON")
        void singleTagValueIsPresent() {
            JSONObject json = convertAndParse("<message>Hello</message>");

            assertThat(containsValueRecursively(json, "Hello"))
                    .as("JSON должен содержать значение 'Hello' из <message>: %s", json)
                    .isTrue();
        }

        @Test
        @DisplayName("Значения нескольких тегов одного уровня присутствуют в результате")
        void multipleTagValuesArePresent() {
            JSONObject json = convertAndParse(
                    "<user><id>1</id><name>Ivan</name><email>ivan@example.com</email></user>");

            assertThat(containsValueRecursively(json, "Ivan")).isTrue();
            assertThat(containsValueRecursively(json, "ivan@example.com")).isTrue();
            // "1" может быть представлено как число или строка в зависимости от реализации —
            // проверяем обеими формами, важен сам факт что значение не потеряно
            boolean idPresent = containsValueRecursively(json, "1");
            assertThat(idPresent).as("значение id=1 должно присутствовать в JSON: %s", json).isTrue();
        }

        @Test
        @DisplayName("Значение глубоко вложенного тега присутствует в результате")
        void deeplyNestedValueIsPresent() {
            String xml = """
                    <order>
                        <customer>
                            <address>
                                <city>Amsterdam</city>
                            </address>
                        </customer>
                    </order>
                    """;

            JSONObject json = convertAndParse(xml);

            assertThat(containsValueRecursively(json, "Amsterdam"))
                    .as("JSON должен содержать значение 'Amsterdam' независимо от глубины вложенности: %s", json)
                    .isTrue();
        }

        @Test
        @DisplayName("Значение атрибута присутствует в результате")
        void attributeValueIsPresent() {
            JSONObject json = convertAndParse(
                    "<product id=\"42\" category=\"books\"><name>Clean Code</name></product>");

            assertThat(containsValueRecursively(json, "books"))
                    .as("значение атрибута category='books' должно присутствовать в JSON: %s", json)
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Имена тегов присутствуют как ключи в результате")
    class TagNamesBecomeKeys {

        @Test
        @DisplayName("Имя каждого дочернего тега есть среди ключей JSON (на любом уровне)")
        void everyTagNameIsPresentAsKey() {
            JSONObject json = convertAndParse(
                    "<order><id>1</id><customer><name>Maria</name></customer></order>");

            assertThat(containsKeyRecursively(json, "order")).isTrue();
            assertThat(containsKeyRecursively(json, "id")).isTrue();
            assertThat(containsKeyRecursively(json, "customer")).isTrue();
            assertThat(containsKeyRecursively(json, "name")).isTrue();
        }
    }

    @Nested
    @DisplayName("Вложенность XML так или иначе отражена в структуре JSON")
    class NestingIsPreserved {

        @Test
        @DisplayName("Результат для вложенного XML — не плоский объект из одних строк/чисел")
        void nestedXmlIsNotFlattenedToScalarsOnly() {
            String xml = """
                    <order>
                        <customer>
                            <name>Maria</name>
                        </customer>
                    </order>
                    """;

            JSONObject json = convertAndParse(xml);

            boolean hasNestedStructure = json.keySet().stream()
                    .map(json::get)
                    .anyMatch(v -> v instanceof JSONObject || v instanceof org.json.JSONArray);

            assertThat(hasNestedStructure)
                    .as("JSON должен сохранять вложенную структуру, а не быть плоским: %s", json)
                    .isTrue();
        }

        @Test
        @DisplayName("Два разных вложенных тега с одинаковым именем поля не перезаписывают друг друга")
        void siblingTagsWithSameChildNameDoNotOverwriteEachOther() {
            String xml = """
                    <order>
                        <buyer><name>Ivan</name></buyer>
                        <seller><name>Maria</name></seller>
                    </order>
                    """;

            JSONObject json = convertAndParse(xml);

            assertThat(containsValueRecursively(json, "Ivan")).isTrue();
            assertThat(containsValueRecursively(json, "Maria")).isTrue();
        }
    }

    @Nested
    @DisplayName("Повторяющиеся теги одного уровня не теряют данные")
    class RepeatedTagsAreNotLost {

        @Test
        @DisplayName("Значения всех повторяющихся тегов <item> присутствуют в результате")
        void allRepeatedTagValuesArePresent() {
            String xml = """
                    <items>
                        <item>first</item>
                        <item>second</item>
                        <item>third</item>
                    </items>
                    """;

            JSONObject json = convertAndParse(xml);

            assertThat(containsValueRecursively(json, "first")).isTrue();
            assertThat(containsValueRecursively(json, "second")).isTrue();
            assertThat(containsValueRecursively(json, "third")).isTrue();
        }
    }

    @Nested
    @DisplayName("Пустой и blank ввод отклоняется до попытки конвертации")
    class BlankInput {

        @ParameterizedTest(name = "[{index}] xml = \"{0}\"")
        @DisplayName("Пустая строка или пробелы бросают InvalidXmlException с сообщением 'XML is empty'")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void emptyOrBlankXml(String xml) {
            assertThatThrownBy(() -> service.convert(xml))
                    .isInstanceOf(InvalidXmlException.class)
                    .hasMessage("XML is empty");
        }

        @Test
        @DisplayName("null бросает InvalidXmlException с сообщением 'XML is empty'")
        void nullXml() {
            assertThatThrownBy(() -> service.convert(null))
                    .isInstanceOf(InvalidXmlException.class)
                    .hasMessage("XML is empty");
        }
    }

    @Nested
    @DisplayName("Строки, не являющиеся XML, отклоняются")
    class NotXmlInput {

        @Test
        @DisplayName("Произвольный текст без XML-разметки бросает InvalidXmlException")
        void plainText() {
            assertThatThrownBy(() -> service.convert("not xml at all"))
                    .isInstanceOf(InvalidXmlException.class)
                    .hasMessage("Invalid XML");
        }

        @Test
        @DisplayName("Строка, содержащая '<' и '>', но не являющаяся XML, бросает InvalidXmlException")
        void angleBracketsButNoValidXml() {
            assertThatThrownBy(() -> service.convert("<<broken>>"))
                    .isInstanceOf(InvalidXmlException.class)
                    .hasMessage("Invalid XML");
        }
    }

    @Nested
    @DisplayName("Структурно некорректный XML приводит к InvalidXmlException")
    class InvalidXml {

        @ParameterizedTest(name = "[{index}] xml = \"{0}\"")
        @DisplayName("Невалидный XML бросает InvalidXmlException с сообщением 'Invalid XML'")
        @ValueSource(strings = {
                "<user><name>Ivan</user>",   // закрыт не тем тегом
                "<user><name>Ivan",          // незакрытый тег
        })
        void malformedXml(String xml) {
            assertThatThrownBy(() -> service.convert(xml))
                    .isInstanceOf(InvalidXmlException.class)
                    .hasMessage("Invalid XML");
        }

        @Test
        @DisplayName("InvalidXmlException на невалидном XML содержит исходную причину (cause)")
        void malformedXmlExceptionHasCause() {
            assertThatThrownBy(() -> service.convert("<broken"))
                    .isInstanceOf(InvalidXmlException.class)
                    .satisfies(e -> assertThat(e.getCause()).isNotNull());
        }
    }
}