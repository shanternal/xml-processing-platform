package dev.shanternal.xml2json.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Xml2JsonControllerTest {

    private final String XML_2_JSON_URL = "/api/v1/xml2json";

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("200 OK — успешная конвертация")
    class SuccessfulConversion {

        @Test
        @DisplayName("Простой XML с несколькими полями конвертируется в плоский JSON")
        void simpleXml() throws Exception {
            String xml = """
                    <user>
                        <id>1</id>
                        <name>Ivan</name>
                        <email>ivan@example.com</email>
                    </user>
                    """;

            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(xml))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.user.id").value(1))
                    .andExpect(jsonPath("$.user.name").value("Ivan"))
                    .andExpect(jsonPath("$.user.email").value("ivan@example.com"));
        }

        @Test
        @DisplayName("Вложенный XML сохраняет иерархию в JSON")
        void nestedXml() throws Exception {
            String xml = """
                    <order>
                        <id>42</id>
                        <customer>
                            <id>7</id>
                            <name>Maria</name>
                        </customer>
                    </order>
                    """;

            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(xml))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.order.id").value(42))
                    .andExpect(jsonPath("$.order.customer.id").value(7))
                    .andExpect(jsonPath("$.order.customer.name").value("Maria"));
        }

        @Test
        @DisplayName("Атрибуты XML попадают в JSON как поля рядом с содержимым тега")
        void xmlWithAttributes() throws Exception {
            String xml = """
                    <product id="101" category="electronics">
                        <name>Laptop</name>
                    </product>
                    """;

            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(xml))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.product.id").value(101))
                    .andExpect(jsonPath("$.product.category").value("electronics"))
                    .andExpect(jsonPath("$.product.name").value("Laptop"));
        }

        @Test
        @DisplayName("XML с одним корневым тегом конвертируется без ошибок")
        void singleTagXml() throws Exception {
            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("<message>Hello</message>"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Hello"));
        }

        @Test
        @DisplayName("XML с числами и булевыми значениями конвертирует типы корректно")
        void xmlWithTypedValues() throws Exception {
            String xml = """
                    <item>
                        <price>99.99</price>
                        <inStock>true</inStock>
                        <qty>5</qty>
                    </item>
                    """;

            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(xml))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.item.price").value(99.99))
                    .andExpect(jsonPath("$.item.inStock").value(true))
                    .andExpect(jsonPath("$.item.qty").value(5));
        }
    }

    @Nested
    @DisplayName("400 Bad Request — невалидный или отсутствующий XML")
    class InvalidInput {

        @ParameterizedTest(name = "[{index}] тело = \"{0}\"")
        @DisplayName("Пустое или состоящее из пробелов тело возвращает 400")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void emptyOrBlankBody(String body) throws Exception {
            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(problemDetail("Invalid XML", 400));
        }

        @ParameterizedTest(name = "[{index}] невалидный XML: {0}")
        @DisplayName("Структурно некорректный XML возвращает 400")
        @ValueSource(strings = {
                "<user><name>Ivan</user>",          // закрыт не тем тегом
                "<user><name>Ivan",                 // незакрытый тег
                "not xml at all",                   // просто текст
                "<<broken>>",                       // двойная угловая скобка
        })
        void malformedXml(String xml) throws Exception {
            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(xml))
                    .andExpect(status().isBadRequest())
                    .andExpect(problemDetail("Invalid XML", 400));
        }
    }

    @Nested
    @DisplayName("415 Unsupported Media Type — неверный Content-Type")
    class UnsupportedMediaType {

        @ParameterizedTest(name = "[{index}] Content-Type: {0}")
        @DisplayName("Запрос не с application/xml отклоняется с 415")
        @ValueSource(strings = {
                "application/json",
                "text/plain",
                "text/xml",
                "application/x-www-form-urlencoded"
        })
        void wrongContentType(String contentType) throws Exception {
            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(contentType)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("<user><name>Ivan</name></user>"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("Структура ProblemDetail в ответах об ошибках")
    class ErrorResponseStructure {

        @Test
        @DisplayName("Ответ об ошибке содержит все обязательные поля ProblemDetail")
        void errorResponseContainsProblemDetailFields() throws Exception {
            mockMvc.perform(post(XML_2_JSON_URL)
                            .contentType(MediaType.APPLICATION_XML)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("<broken"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.title").value("Invalid XML"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").isString())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    /**
     * Проверяет что ответ является ProblemDetail с нужным title и status.
     */
    private static org.springframework.test.web.servlet.ResultMatcher problemDetail(
            String title, int status) {
        return result -> {
            jsonPath("$.title").value(title).match(result);
            jsonPath("$.status").value(status).match(result);
        };
    }
}