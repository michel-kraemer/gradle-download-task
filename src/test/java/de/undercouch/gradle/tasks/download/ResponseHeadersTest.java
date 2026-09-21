package de.undercouch.gradle.tasks.download;

import com.sun.net.httpserver.HttpServer;
import org.apache.hc.core5.http.MessageConstraintException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that response headers are bounded before downloading the response body.
 */
@Timeout(10)
public class ResponseHeadersTest extends TestBase {
    private HttpServer server;

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void acceptsResponseHeadersWithinLimits() throws Exception {
        Download t = makeProjectAndTask();
        t.src(serveResponseHeaders(50, 32));
        File dst = newTempFile();
        t.dest(dst);
        execute(t);

        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS);
    }

    @ParameterizedTest
    @CsvSource({"101, 1", "1, 9000"})
    public void rejectsExcessiveResponseHeaders(int headerCount, int headerLength) throws Exception {
        Download t = makeProjectAndTask();
        t.src(serveResponseHeaders(headerCount, headerLength));
        t.readTimeout(2000);
        t.tempAndMove(true);
        File dst = newTempFile();
        Files.write(dst.toPath(), CONTENTS2.getBytes(StandardCharsets.UTF_8));
        t.dest(dst);

        assertThatThrownBy(() -> execute(t))
                .hasRootCauseInstanceOf(MessageConstraintException.class);
        assertThat(dst).usingCharset(StandardCharsets.UTF_8).hasContent(CONTENTS2);
    }

    private String serveResponseHeaders(int count, int length) throws IOException {
        char[] value = new char[length];
        Arrays.fill(value, 'a');
        // WireMock's Jetty server rejects oversized response headers itself.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/" + TEST_FILE_NAME, exchange -> {
            for (int i = 0; i < count; ++i) {
                exchange.getResponseHeaders().add("X-Test-" + i, new String(value));
            }
            byte[] body = CONTENTS.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/" + TEST_FILE_NAME;
    }
}
