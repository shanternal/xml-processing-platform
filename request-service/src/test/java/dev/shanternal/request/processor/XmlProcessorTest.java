package dev.shanternal.request.processor;

import org.apache.xml.security.Init;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlProcessorTest {

    @BeforeAll
    static void initXmlSecurity() {
        Init.init();
    }

    private final XmlProcessor processor =
            new XmlProcessor(DocumentBuilderFactory.newInstance());

    @Nested
    @DisplayName("Парсинг XML")
    class ParseXml {

        @Test
        @DisplayName("Корректный XML успешно преобразуется в Document")
        void validXmlIsParsed() {
            Document document = processor.parseXml("""
                    <user>
                        <id>1</id>
                        <name>Ivan</name>
                    </user>
                    """);

            assertThat(document).isNotNull();
            assertThat(document.getDocumentElement().getTagName()).isEqualTo("user");
        }

        @Test
        @DisplayName("Текстовое содержимое элементов сохраняется после парсинга")
        void textContentIsPreserved() {
            Document document = processor.parseXml("""
                    <user>
                        <name>Ivan</name>
                    </user>
                    """);

            assertThat(
                    document.getDocumentElement()
                            .getElementsByTagName("name")
                            .item(0)
                            .getTextContent()
            ).isEqualTo("Ivan");
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                "<user>",
                "<user><name></user>",
                "<<broken>>",
                "not xml"
        })
        @DisplayName("Некорректный XML приводит к IllegalArgumentException")
        void invalidXmlThrowsException(String xml) {
            assertThatThrownBy(() -> processor.parseXml(xml))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Failed to parse XML")
                    .hasCauseInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Каноникализация XML")
    class Canonicalization {

        @Test
        @DisplayName("Результат не содержит XML-комментарии")
        void commentsAreRemoved() {
            Document document = processor.parseXml("""
                    <root>
                        <!-- comment -->
                        <value>1</value>
                    </root>
                    """);

            String canonical = processor.canonicalizeXml(document);

            assertThat(canonical)
                    .contains("<value>1</value>")
                    .doesNotContain("comment");
        }

        @Test
        @DisplayName("Логически одинаковый XML имеет одинаковую каноническую форму")
        void logicallyEquivalentDocumentsProduceSameCanonicalForm() {

            Document first = processor.parseXml("""
                    <root a="1" b="2">
                        <child>text</child>
                    </root>
                    """);

            Document second = processor.parseXml("""
                    <root b="2" a="1">
                        <child>text</child>
                    </root>
                    """);

            assertThat(processor.canonicalizeXml(first))
                    .isEqualTo(processor.canonicalizeXml(second));
        }

        @Test
        @DisplayName("Каноникализация возвращает непустую строку")
        void canonicalXmlIsNotBlank() {
            Document document = processor.parseXml("<root><child/></root>");

            assertThat(processor.canonicalizeXml(document))
                    .isNotBlank();
        }
    }

    @Nested
    @DisplayName("Подсчёт XML-тегов")
    class CountTags {

        @Test
        @DisplayName("Одиночный корневой элемент считается одним тегом")
        void rootOnly() {
            Document document = processor.parseXml("<root/>");

            assertThat(processor.countXmlTags(document)).isEqualTo(1);
        }

        @Test
        @DisplayName("Подсчитываются все вложенные элементы")
        void nestedElements() {
            Document document = processor.parseXml("""
                    <root>
                        <user>
                            <id>1</id>
                            <name>Ivan</name>
                        </user>
                    </root>
                    """);

            assertThat(processor.countXmlTags(document)).isEqualTo(4);
        }

        @Test
        @DisplayName("Повторяющиеся элементы учитываются по отдельности")
        void repeatedElements() {
            Document document = processor.parseXml("""
                    <items>
                        <item/>
                        <item/>
                        <item/>
                    </items>
                    """);

            assertThat(processor.countXmlTags(document)).isEqualTo(4);
        }

        @Test
        @DisplayName("Текстовые узлы не учитываются как XML-теги")
        void textNodesAreIgnored() {
            Document document = processor.parseXml("""
                    <root>
                        hello
                        <child>world</child>
                    </root>
                    """);

            assertThat(processor.countXmlTags(document)).isEqualTo(2);
        }

        @Test
        @DisplayName("Глубокая вложенность корректно подсчитывается")
        void deeplyNestedDocument() {
            Document document = processor.parseXml("""
                    <a>
                        <b>
                            <c>
                                <d>
                                    <e/>
                                </d>
                            </c>
                        </b>
                    </a>
                    """);

            assertThat(processor.countXmlTags(document)).isEqualTo(5);
        }

        @Test
        @DisplayName("Атрибуты элементов не считаются XML-тегами")
        void attributesAreNotCounted() {
            Document document = processor.parseXml("""
            <user id="1" role="admin">
                <name>Ivan</name>
            </user>
            """);

            assertThat(processor.countXmlTags(document)).isEqualTo(2);
        }
    }
}