package org.dstadler.htmlunit;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.dstadler.commons.testing.MemoryLeakVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FileLeakTest {

    private final MemoryLeakVerifier verifier = new MemoryLeakVerifier();

    @AfterEach
    public void tearDown() {
        verifier.assertGarbageCollected();
    }

    @Test
    public void testDefaultHttpClientConnectionOperator() throws IOException {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        final HttpContext httpContext = HttpClientContext.create();

        try (CloseableHttpClient client = builder.build()) {

            //noinspection HttpUrlsUsage
            HttpUriRequest httpMethod = new HttpGet("http://dstadler.org");

            final HttpHost hostConfiguration = new HttpHost("dstadler.org");

            HttpResponse httpResponse = client.execute(hostConfiguration, httpMethod, httpContext);

            assertNotNull(httpResponse);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());

            verifier.addObject(builder);
            verifier.addObject(client);
            verifier.addObject(httpMethod);
            verifier.addObject(hostConfiguration);
            verifier.addObject(httpResponse);
        }
    }
}
