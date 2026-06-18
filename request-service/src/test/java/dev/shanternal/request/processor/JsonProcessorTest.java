package dev.shanternal.request.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonProcessorTest {

    private final JsonProcessor processor = new JsonProcessor();

    @Nested
    @DisplayName("Подсчёт ключей в корректном JSON")
    class ValidJson {

        @Test
        @DisplayName("Пустой объект содержит 0 ключей")
        void emptyObject() {
            assertThat(processor.countKeys("{}")).isZero();
        }

        @Test
        @DisplayName("Плоский объект возвращает количество всех полей")
        void flatObject() {
            String json = """
                    {
                      "id": 1,
                      "name": "Ivan",
                      "active": true
                    }
                    """;

            assertThat(processor.countKeys(json)).isEqualTo(3);
        }

        @Test
        @DisplayName("Ключи во вложенных объектах тоже учитываются")
        void nestedObjects() {
            String json = """
                    {
                      "user": {
                        "id": 1,
                        "profile": {
                          "name": "Ivan"
                        }
                      }
                    }
                    """;

            assertThat(processor.countKeys(json)).isEqualTo(4);
        }

        @Test
        @DisplayName("Ключи объектов внутри массива учитываются")
        void objectsInsideArray() {
            String json = """
                    {
                      "users": [
                        {"id": 1},
                        {"id": 2, "name": "Maria"}
                      ]
                    }
                    """;

            assertThat(processor.countKeys(json)).isEqualTo(4);
        }

        @Test
        @DisplayName("Тип значения не влияет на подсчёт ключей")
        void differentValueTypes() {
            String json = """
                    {
                      "number": 42,
                      "string": "text",
                      "boolean": true,
                      "nullValue": null,
                      "array": [],
                      "object": {}
                    }
                    """;

            assertThat(processor.countKeys(json)).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Пустой ввод")
    class BlankInput {

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("Blank-строка возвращает 0")
        void blankInputReturnsZero(String json) {
            assertThat(processor.countKeys(json)).isZero();
        }

        @Test
        @DisplayName("null возвращает 0")
        void nullInputReturnsZero() {
            assertThat(processor.countKeys(null)).isZero();
        }
    }

    @Nested
    @DisplayName("Некорректный JSON")
    class InvalidJson {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                "{",
                "}",
                "{\"id\":}",
                "{\"id\":1",
                "not json"
        })
        @DisplayName("Невалидный JSON приводит к IllegalArgumentException")
        void invalidJsonThrowsException(String json) {
            assertThatThrownBy(() -> processor.countKeys(json))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid JSON format")
                    .hasCauseInstanceOf(java.io.IOException.class);
        }
    }
}