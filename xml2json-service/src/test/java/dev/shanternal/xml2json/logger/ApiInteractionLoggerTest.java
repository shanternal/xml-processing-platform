package dev.shanternal.xml2json.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ApiInteractionLoggerTest {

    private ApiInteractionLogger logger;
    private ListAppender<ILoggingEvent> logCaptor;

    @BeforeEach
    void setUp() {
        logger = new ApiInteractionLogger();

        logCaptor = new ListAppender<>();
        logCaptor.start();

        Logger apiLogger = (Logger) LoggerFactory.getLogger("API_INTERACTION");
        apiLogger.setLevel(Level.TRACE);
        apiLogger.addAppender(logCaptor);
    }

    @AfterEach
    void tearDown() {
        Logger apiLogger = (Logger) LoggerFactory.getLogger("API_INTERACTION");
        apiLogger.detachAppender(logCaptor);
    }

    @Nested
    @DisplayName("Уровень лога определяется HTTP-статусом ответа")
    class LogLevel {

        @ParameterizedTest(name = "status={0} → уровень {1}")
        @DisplayName("2xx/3xx → INFO, 4xx → WARN, 5xx → ERROR")
        @CsvSource({
                "200, INFO",
                "201, INFO",
                "204, INFO",
                "301, INFO",  // граница ниже 400 — текущая реализация трактует как INFO
                "399, INFO",  // верхняя граница диапазона "не ошибка"
                "400, WARN",
                "404, WARN",
                "422, WARN",
                "499, WARN",  // верхняя граница 4xx
                "500, ERROR",
                "503, ERROR",
        })
        void logLevelMatchesHttpStatus(int httpStatus, String expectedLevel) {
            logger.log(
                    requestWrapper("POST", "/xml2json", "<ping/>"),
                    responseWrapper(httpStatus, "{\"ping\":\"pong\"}"),
                    Instant.now(),
                    10L,
                    null
            );

            assertThat(logCaptor.list).hasSize(1);
            assertThat(logCaptor.list.getFirst().getLevel())
                    .isEqualTo(Level.valueOf(expectedLevel));
        }

        @Test
        @DisplayName("Ровно одно событие лога на один вызов log()")
        void logsExactlyOneEvent() {
            logger.log(
                    requestWrapper("GET", "/xml2json", ""),
                    responseWrapper(200, "{}"),
                    Instant.now(),
                    1L,
                    null
            );

            assertThat(logCaptor.list).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Содержимое сообщения лога")
    class MessageContent {

        @Test
        @DisplayName("Сообщение содержит все обязательные поля шаблона")
        void messageContainsAllTemplateFields() {
            Instant timestamp = Instant.parse("2026-06-18T12:00:00Z");

            logger.log(
                    requestWrapper("POST", "/xml2json", "<user><name>Ivan</name></user>"),
                    responseWrapper(200, "{\"user\":{\"name\":\"Ivan\"}}"),
                    timestamp,
                    42L,
                    null
            );

            String message = logCaptor.list.getFirst().getFormattedMessage();

            assertThat(message)
                    .contains("timestamp=2026-06-18T12:00:00Z")
                    .contains("method=POST")
                    .contains("uri=/xml2json")
                    .contains("status=200")
                    .contains("durationMs=42");
        }

        @Test
        @DisplayName("Тело запроса попадает в сообщение лога")
        void requestBodyIsLogged() {
            String requestBody = "<message>Hello</message>";

            logger.log(
                    requestWrapper("POST", "/xml2json", requestBody),
                    responseWrapper(200, "{\"message\":\"Hello\"}"),
                    Instant.now(),
                    5L,
                    null
            );

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("requestBody=" + requestBody);
        }

        @Test
        @DisplayName("Тело ответа попадает в сообщение лога")
        void responseBodyIsLogged() {
            String responseBody = "{\"message\":\"Hello\"}";

            logger.log(
                    requestWrapper("POST", "/xml2json", "<message>Hello</message>"),
                    responseWrapper(200, responseBody),
                    Instant.now(),
                    5L,
                    null
            );

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("responseBody=" + responseBody);
        }

        @Test
        @DisplayName("Тело запроса и тело ответа не перепутаны местами")
        void requestAndResponseBodiesAreNotSwapped() {
            // Намеренно разные XML/JSON, чтобы случайное совпадение подстрок было исключено
            logger.log(
                    requestWrapper("POST", "/xml2json", "<request-marker/>"),
                    responseWrapper(200, "{\"response-marker\":true}"),
                    Instant.now(),
                    5L,
                    null
            );

            String message = logCaptor.list.getFirst().getFormattedMessage();
            int requestIdx = message.indexOf("requestBody=<request-marker/>");
            int responseIdx = message.indexOf("responseBody={\"response-marker\":true}");

            assertThat(requestIdx).isPositive();
            assertThat(responseIdx).isPositive();
            assertThat(requestIdx).isLessThan(responseIdx);
        }

        @Test
        @DisplayName("При пустом теле запроса лог не падает и поле присутствует пустым")
        void emptyRequestBodyDoesNotThrow() {
            logger.log(
                    requestWrapper("POST", "/xml2json", ""),
                    responseWrapper(400, "{\"title\":\"Invalid XML\"}"),
                    Instant.now(),
                    1L,
                    null
            );

            assertThat(logCaptor.list).hasSize(1);
            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("requestBody= status=400");
            assertThat(logCaptor.list.getFirst().getLevel()).isEqualTo(Level.WARN);
        }

        @Test
        @DisplayName("Query string попадает в сообщение, если присутствует")
        void queryStringIsLogged() {
            logger.log(
                    requestWrapper("GET", "/xml2json", "", "format=json&strict=true"),
                    responseWrapper(200, "{}"),
                    Instant.now(),
                    1L,
                    null
            );

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("query=format=json&strict=true");
        }

        @Test
        @DisplayName("Отсутствующий query string логируется как query=null без исключений")
        void missingQueryStringDoesNotThrow() {
            logger.log(
                    requestWrapper("GET", "/xml2json", ""),
                    responseWrapper(200, "{}"),
                    Instant.now(),
                    1L,
                    null
            );

            assertThat(logCaptor.list).hasSize(1);
            assertThat(logCaptor.list.getFirst().getFormattedMessage()).contains("query=null");
        }
    }

    @Nested
    @DisplayName("Заголовки запроса и ответа")
    class Headers {

        @Test
        @DisplayName("Заголовки запроса попадают в сообщение лога")
        void requestHeadersAreLogged() {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/xml2json");
            request.setContentType("application/xml");
            request.addHeader("X-Request-Id", "abc-123");
            request.setContent("<x/>".getBytes(StandardCharsets.UTF_8));
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("X-Request-Id").contains("abc-123");
        }

        @Test
        @DisplayName("Заголовки ответа попадают в сообщение лога")
        void responseHeadersAreLogged() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setStatus(200);
            response.addHeader("X-Trace-Id", "trace-789");
            ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
            writeBody(wrapper, "{}");

            logger.log(requestWrapper("GET", "/xml2json", ""), wrapper, Instant.now(), 1L, null);

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("X-Trace-Id").contains("trace-789");
        }

        @Test
        @DisplayName("Множественные значения одного заголовка не теряются")
        void multiValuedHeaderIsLogged() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xml2json");
            request.addHeader("Accept", "application/json");
            request.addHeader("Accept", "application/xml");
            request.setContent(new byte[0]);
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("application/json").contains("application/xml");
        }

        @Test
        @DisplayName("Известный чувствительный заголовок Authorization сейчас логируется как есть "
                + "(фиксирует текущее поведение — потенциальный риск утечки секретов)")
        void authorizationHeaderIsNotMasked() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xml2json");
            request.addHeader("Authorization", "Bearer super-secret-token");
            request.setContent(new byte[0]);
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("super-secret-token");
        }

        @Test
        @DisplayName("Отсутствие заголовков не приводит к исключению")
        void noHeadersDoesNotThrow() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xml2json");
            request.setContent(new byte[0]);
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            assertThat(logCaptor.list).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Кодировка тела запроса/ответа")
    class Encoding {

        @Test
        @DisplayName("Тело в не-UTF-8 кодировке (windows-1251) декодируется корректно")
        void nonUtf8RequestEncodingIsDecodedCorrectly() {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/xml2json");
            request.setCharacterEncoding("windows-1251");
            String body = "<имя>Иван</имя>";
            request.setContent(body.getBytes(Charset.forName("windows-1251")));
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            String message = logCaptor.list.getFirst().getFormattedMessage();
            assertThat(message).contains("Иван");
        }

        @Test
        @DisplayName("Отсутствующая кодировка (null) не приводит к исключению — используется фолбэк UTF-8")
        void missingEncodingFallsBackToUtf8() {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/xml2json");
            request.setCharacterEncoding(null);
            request.setContent("<x/>".getBytes(StandardCharsets.UTF_8));
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            assertThat(logCaptor.list).hasSize(1);
            assertThat(logCaptor.list.getFirst().getFormattedMessage()).contains("<x/>");
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-a-real-charset", "utf-99", "???"})
        @DisplayName("Некорректное имя кодировки не приводит к исключению — используется фолбэк UTF-8")
        void invalidEncodingNameFallsBackToUtf8(String invalidEncoding) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/xml2json");
            request.setCharacterEncoding(invalidEncoding);
            request.setContent("<x/>".getBytes(StandardCharsets.UTF_8));
            ContentCachingRequestWrapper wrapper = readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));

            logger.log(wrapper, responseWrapper(200, "{}"), Instant.now(), 1L, null);

            assertThat(logCaptor.list).hasSize(1);
            assertThat(logCaptor.list.getFirst().getFormattedMessage()).contains("<x/>");
        }
    }

    @Nested
    @DisplayName("Логирование при 5xx с cause")
    class ErrorLogging {

        @Test
        @DisplayName("При 5xx cause прикрепляется к событию лога")
        void causeIsAttachedToLogEventOn5xx() {
            RuntimeException cause = new RuntimeException("unexpected failure");

            logger.log(
                    requestWrapper("POST", "/xml2json", "<x/>"),
                    responseWrapper(500, "{\"title\":\"Internal Server Error\"}"),
                    Instant.now(),
                    3L,
                    cause
            );

            ILoggingEvent event = logCaptor.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("unexpected failure");
        }

        @Test
        @DisplayName("При 5xx без cause (null) лог не падает и throwable отсутствует")
        void nullCauseOn5xxDoesNotThrow() {
            logger.log(
                    requestWrapper("POST", "/xml2json", "<x/>"),
                    responseWrapper(500, "{\"title\":\"Internal Server Error\"}"),
                    Instant.now(),
                    3L,
                    null
            );

            ILoggingEvent event = logCaptor.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy()).isNull();
        }

        @Test
        @DisplayName("При 4xx cause не прикрепляется к событию лога, даже если передан")
        void causeIsIgnoredOn4xxEvenIfProvided() {

            RuntimeException mistakenlyPassedCause = new RuntimeException("should be ignored");

            logger.log(
                    requestWrapper("POST", "/xml2json", "not xml"),
                    responseWrapper(400, "{\"title\":\"Invalid XML\"}"),
                    Instant.now(),
                    2L,
                    mistakenlyPassedCause
            );

            ILoggingEvent event = logCaptor.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();
        }

        @Test
        @DisplayName("При 2xx cause не прикрепляется к событию лога, даже если передан")
        void causeIsIgnoredOn2xxEvenIfProvided() {
            RuntimeException mistakenlyPassedCause = new RuntimeException("should be ignored");

            logger.log(
                    requestWrapper("POST", "/xml2json", "<x/>"),
                    responseWrapper(200, "{}"),
                    Instant.now(),
                    2L,
                    mistakenlyPassedCause
            );

            ILoggingEvent event = logCaptor.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getThrowableProxy()).isNull();
        }

        @Test
        @DisplayName("Цепочка вложенных причин (cause-of-cause) сохраняется в событии лога")
        void nestedCauseChainIsPreserved() {
            RuntimeException root = new RuntimeException("root cause");
            RuntimeException wrapper = new RuntimeException("wrapper", root);

            logger.log(
                    requestWrapper("POST", "/xml2json", "<x/>"),
                    responseWrapper(500, "{}"),
                    Instant.now(),
                    3L,
                    wrapper
            );

            ILoggingEvent event = logCaptor.list.getFirst();
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("wrapper");
            assertThat(event.getThrowableProxy().getCause()).isNotNull();
            assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo("root cause");
        }
    }

    @Nested
    @DisplayName("Ленивое построение сообщения")
    class LazyMessageBuilding {

        @Test
        @DisplayName("Сообщение строится даже когда уровень лога включён (sanity-check для Supplier API)")
        void messageIsBuiltWhenLevelEnabled() {

            logger.log(
                    requestWrapper("GET", "/xml2json", ""),
                    responseWrapper(200, "{}"),
                    Instant.now(),
                    1L,
                    null
            );

            assertThat(logCaptor.list.getFirst().getFormattedMessage()).isNotBlank();
        }
    }

    private ContentCachingRequestWrapper requestWrapper(String method, String uri, String body) {
        return requestWrapper(method, uri, body, null);
    }

    private ContentCachingRequestWrapper requestWrapper(String method, String uri, String body, String queryString) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContentType("application/xml");
        if (queryString != null) {
            request.setQueryString(queryString);
        }
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        request.setContent(bodyBytes);

        return readFully(new ContentCachingRequestWrapper(request, 1024 * 1024));
    }

    private ContentCachingRequestWrapper readFully(ContentCachingRequestWrapper wrapper) {
        try {
            wrapper.getInputStream().readAllBytes();
        } catch (Exception ignored) {
        }
        return wrapper;
    }

    private ContentCachingResponseWrapper responseWrapper(int status, String body) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        writeBody(wrapper, body);
        return wrapper;
    }

    private void writeBody(ContentCachingResponseWrapper wrapper, String body) {
        try {
            wrapper.getWriter().write(body);
            wrapper.getWriter().flush();
        } catch (Exception ignored) {
        }
    }
}