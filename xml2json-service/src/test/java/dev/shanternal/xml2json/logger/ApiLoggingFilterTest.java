package dev.shanternal.xml2json.logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiLoggingFilterTest {

    @Mock
    ApiInteractionLogger apiInteractionLogger;

    @InjectMocks
    ApiLoggingFilter filter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "maxRequestBodySize", DataSize.ofMegabytes(10));
    }

    @Nested
    @DisplayName("Успешный запрос")
    class SuccessfulRequest {

        @Test
        @DisplayName("Логгер вызывается один раз, с timestamp и failure == null")
        void loggerCalledOnceWithNullFailure() throws Exception {
            ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Throwable> failureCaptor = ArgumentCaptor.forClass(Throwable.class);

            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/xml2json"),
                    new MockHttpServletResponse(),
                    emptyChain()
            );

            verify(apiInteractionLogger, times(1)).log(
                    any(), any(), timestampCaptor.capture(), anyLong(), failureCaptor.capture()
            );

            assertThat(failureCaptor.getValue()).isNull();
            assertThat(timestampCaptor.getValue()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Исключение в цепочке фильтров")
    class ExceptionInFilterChain {

        @Test
        @DisplayName("Логгер вызывается один раз, и в него передаётся то же исключение")
        void loggerStillCalledWithSameException() {
            ServletException cause = new ServletException("downstream failure");
            FilterChain failingChain = (req, res) -> { throw cause; };

            ArgumentCaptor<Throwable> failureCaptor = ArgumentCaptor.forClass(Throwable.class);

            assertThatThrownBy(() ->
                    filter.doFilterInternal(
                            new MockHttpServletRequest("POST", "/xml2json"),
                            new MockHttpServletResponse(),
                            failingChain
                    )
            ).isInstanceOf(ServletException.class);

            verify(apiInteractionLogger, times(1))
                    .log(any(), any(), any(), anyLong(), failureCaptor.capture());

            assertThat(failureCaptor.getValue()).isSameAs(cause);
        }

        @Test
        @DisplayName("ServletException пробрасывается как есть")
        void servletExceptionIsRethrown() {
            FilterChain failingChain = (req, res) -> {
                throw new ServletException("servlet error");
            };

            assertThatThrownBy(() ->
                    filter.doFilterInternal(
                            new MockHttpServletRequest(),
                            new MockHttpServletResponse(),
                            failingChain
                    )
            )
                    .isInstanceOf(ServletException.class)
                    .hasMessage("servlet error");
        }

        @Test
        @DisplayName("IOException пробрасывается как есть")
        void ioExceptionIsRethrown() {
            FilterChain failingChain = (req, res) -> {
                throw new IOException("io error");
            };

            assertThatThrownBy(() ->
                    filter.doFilterInternal(
                            new MockHttpServletRequest(),
                            new MockHttpServletResponse(),
                            failingChain
                    )
            )
                    .isInstanceOf(IOException.class)
                    .hasMessage("io error");
        }

        @Test
        @DisplayName("RuntimeException из цепочки тоже пробрасывается и логируется")
        void runtimeExceptionIsRethrownAndLogged() {
            RuntimeException cause = new RuntimeException("unexpected failure");
            FilterChain failingChain = (req, res) -> { throw cause; };

            ArgumentCaptor<Throwable> failureCaptor = ArgumentCaptor.forClass(Throwable.class);

            assertThatThrownBy(() ->
                    filter.doFilterInternal(
                            new MockHttpServletRequest(),
                            new MockHttpServletResponse(),
                            failingChain
                    )
            ).isSameAs(cause);

            verify(apiInteractionLogger)
                    .log(any(), any(), any(), anyLong(), failureCaptor.capture());
            assertThat(failureCaptor.getValue()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Делегирование цепочке фильтров и кеширующие обёртки")
    class ChainDelegationAndWrappers {

        @Test
        @DisplayName("chain.doFilter() вызывается ровно один раз")
        void delegatesToFilterChainExactlyOnce() throws Exception {
            FilterChain chain = mock(FilterChain.class);

            filter.doFilterInternal(
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse(),
                    chain
            );

            verify(chain, times(1)).doFilter(any(), any());
        }

        @Test
        @DisplayName("В логгер и в цепочку передаются одни и те же кеширующие обёртки")
        void chainAndLoggerReceiveSameWrapperInstances() throws Exception {
            FilterChain mockChain = mock(FilterChain.class);

            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/xml2json"),
                    new MockHttpServletResponse(),
                    mockChain
            );

            ArgumentCaptor<ContentCachingRequestWrapper> chainRequestCaptor =
                    ArgumentCaptor.forClass(ContentCachingRequestWrapper.class);
            ArgumentCaptor<ContentCachingResponseWrapper> chainResponseCaptor =
                    ArgumentCaptor.forClass(ContentCachingResponseWrapper.class);
            verify(mockChain).doFilter(chainRequestCaptor.capture(), chainResponseCaptor.capture());

            ArgumentCaptor<ContentCachingRequestWrapper> loggerRequestCaptor =
                    ArgumentCaptor.forClass(ContentCachingRequestWrapper.class);
            ArgumentCaptor<ContentCachingResponseWrapper> loggerResponseCaptor =
                    ArgumentCaptor.forClass(ContentCachingResponseWrapper.class);
            verify(apiInteractionLogger).log(
                    loggerRequestCaptor.capture(), loggerResponseCaptor.capture(), any(), anyLong(), any()
            );

            assertThat(chainRequestCaptor.getValue()).isSameAs(loggerRequestCaptor.getValue());
            assertThat(chainResponseCaptor.getValue()).isSameAs(loggerResponseCaptor.getValue());
        }

        @Test
        @DisplayName("chain.doFilter() вызывается раньше, чем logger.log()")
        void chainIsCalledBeforeLogger() throws Exception {
            FilterChain mockChain = mock(FilterChain.class);

            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/xml2json"),
                    new MockHttpServletResponse(),
                    mockChain
            );

            InOrder inOrder = inOrder(mockChain, apiInteractionLogger);
            inOrder.verify(mockChain).doFilter(any(), any());
            inOrder.verify(apiInteractionLogger).log(any(), any(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Тело ответа копируется клиенту (copyBodyToResponse)")
    class ResponseBodyCopied {

        @Test
        @DisplayName("При успешном запросе тело, записанное в chain, доходит до реального response")
        void responseBodyIsCopiedOnSuccess() throws Exception {
            MockHttpServletResponse realResponse = new MockHttpServletResponse();
            byte[] expectedBody = "{\"result\":\"ok\"}".getBytes();

            FilterChain chain = (req, res) -> res.getOutputStream().write(expectedBody);

            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/xml2json"),
                    realResponse,
                    chain
            );

            assertThat(realResponse.getContentAsByteArray()).isEqualTo(expectedBody);
        }

        @Test
        @DisplayName("Даже при исключении в цепочке тело, записанное до сбоя, копируется в response")
        void responseBodyIsCopiedEvenOnException() {
            MockHttpServletResponse realResponse = new MockHttpServletResponse();
            byte[] partialBody = "{\"error\":\"partial\"}".getBytes();

            FilterChain failingChain = (req, res) -> {
                res.getOutputStream().write(partialBody);
                throw new ServletException("downstream failure");
            };

            assertThatThrownBy(() ->
                    filter.doFilterInternal(
                            new MockHttpServletRequest("POST", "/xml2json"),
                            realResponse,
                            failingChain
                    )
            ).isInstanceOf(ServletException.class);

            assertThat(realResponse.getContentAsByteArray()).isEqualTo(partialBody);
        }
    }

    @Nested
    @DisplayName("Длительность запроса передаётся в логгер")
    class Duration {

        @Test
        @DisplayName("durationInMs отражает реальное время выполнения цепочки, а не захардкожен")
        void durationReflectsActualElapsedTime() throws Exception {
            FilterChain slowChain = (req, res) -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            ArgumentCaptor<Long> durationCaptor = ArgumentCaptor.forClass(Long.class);

            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/xml2json"),
                    new MockHttpServletResponse(),
                    slowChain
            );

            verify(apiInteractionLogger)
                    .log(any(), any(), any(), durationCaptor.capture(), any());

            assertThat(durationCaptor.getValue()).isGreaterThan(0L);
        }
    }

    private FilterChain emptyChain() {
        return (req, res) -> {};
    }
}