package de.undercouch.gradle.tasks.download;

import de.undercouch.gradle.tasks.download.internal.SanitizingLocationUriRedirectStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SanitizingLocationUriRedirectStrategyTest {


    SanitizingLocationUriRedirectStrategy underTest;

    private final String basePath = "https://example.com";
    private final String compliantLocation = "/somepath/some%20other%20path";

    @BeforeEach
    void setup() {
        underTest = new SanitizingLocationUriRedirectStrategy();
    }

    @Test
    void compliantLocationURI() throws URISyntaxException, HttpException {

        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.getUri()).thenReturn(URI.create(basePath));
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Header locationHeader = Mockito.mock(Header.class);
        when(locationHeader.getValue()).thenReturn(basePath + compliantLocation);
        when(response.getFirstHeader(any())).thenReturn(locationHeader);
        assertEquals(URI.create(basePath + compliantLocation), underTest.getLocationURI(request, response, Mockito.mock(HttpContext.class)));
    }

    @Test
    void nonCompliantLocationURI() throws URISyntaxException, HttpException {

        HttpRequest request = Mockito.mock(HttpRequest.class);
        when(request.getUri()).thenReturn(URI.create(basePath));
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Header locationHeader = Mockito.mock(Header.class);
        String nonCompliantLocation = "/somepath/some other path";
        when(locationHeader.getValue()).thenReturn(basePath + nonCompliantLocation);
        when(response.getFirstHeader(any())).thenReturn(locationHeader);
        assertEquals(URI.create(basePath + compliantLocation), underTest.getLocationURI(request, response, Mockito.mock(HttpContext.class)));
    }
}
