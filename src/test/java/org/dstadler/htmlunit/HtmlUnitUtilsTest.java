package org.dstadler.htmlunit;

import org.htmlunit.*;
import org.htmlunit.html.*;
import org.htmlunit.util.WebConnectionWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dstadler.commons.http.NanoHTTPD;
import org.dstadler.commons.net.SocketUtils;
import org.dstadler.commons.testing.MemoryLeakVerifier;
import org.dstadler.commons.testing.MockRESTServer;
import org.dstadler.commons.testing.PrivateConstructorCoverage;
import org.dstadler.commons.testing.TestHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlUnitUtilsTest {
    private static final Log logger = LogFactory.getLog(HtmlUnitUtilsTest.class);

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { Boolean.TRUE }, { Boolean.FALSE }
        });
    }

    private final MemoryLeakVerifier verifier = new MemoryLeakVerifier();

    @AfterEach
    public void tearDown() {
        verifier.assertGarbageCollected();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetElementById(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\"><input type=\"text\"/><input type=\"text\"/><input type=\"image\"/></form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // just cover the listener as well
            client.getIncorrectnessListener().notify(null, null);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            // get by id
            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getElementById(page, "nonexistingid", HtmlElement.class)),
                    "Could not find element", "nonexistingid");

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getElementById(page, "testid", HtmlTextInput.class)),
                    "Expected a field with id", "type org.htmlunit.html.HtmlTextInput", "testid", "HtmlImage");

            assertNotNull(HtmlUnitUtils.getElementById(page, "testid", HtmlImage.class));

            // get by name
            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getElementByName(page, "nonexistingname", HtmlElement.class)),
                    "nonexistingname");

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getElementByName(page, "testname", HtmlTextInput.class)),
                    "Expected a field with name", "type org.htmlunit.html.HtmlTextInput", "testname", "HtmlImage");


            assertNotNull(HtmlUnitUtils.getElementByName(page, "testname", HtmlImage.class));


            // verify forms
            assertNotNull(HtmlUnitUtils.getFormElementByType(page, "testform", HtmlImageInput.class),
                    "Should find element in existing form");

            // fails for form that does not exist
            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByType(page, "notexistingform", HtmlImageInput.class)),
                    "notexistingform");

            // fails for type that is not found
            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByType(page, "testform", HtmlRadioButtonInput.class)),
                    "Could not find element of type", "HtmlRadioButtonInput");

            // fails for type that is available multiple times
            TestHelpers.assertContains(
                    assertThrows(HtmlUnitException.class,
                            () -> HtmlUnitUtils.getFormElementByType(page, "testform", HtmlTextInput.class)),
                    "Did find more than one element", "HtmlTextInput");

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetElementByAttribute(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\"><input type=\"text\"/><input type=\"text\"/><input type=\"image\"/></form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            // not found with invalid content
            List<HtmlElement> elements = HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "nonexistingid", HtmlElement.class);
            assertNotNull(elements);
            assertTrue(elements.isEmpty(), "Had: " + elements);

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlTextInput.class)),
                    "Expected a field with tag", "type org.htmlunit.html.HtmlTextInput", "testid", "HtmlImage");

            List<HtmlTextInput> inputElements = HtmlUnitUtils.getElementsByAttribute(page, "noimg", "noid", "testid", HtmlTextInput.class);
            assertTrue(inputElements.isEmpty(), "Did not expect any elements, but had " + inputElements);


            // found with correct content
            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertFalse(elementsByAttribute.isEmpty(), "Had: " + elementsByAttribute);

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetElementByTextContains(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\"><input type=\"text\"/><input type=\"text\"/><input type=\"image\"/>bla text1 bla</form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            List<HtmlElement> elements = HtmlUnitUtils.getElementsByTextContents(page, "form", "bla text1 bla", HtmlElement.class);
            assertNotNull(elements);
            assertFalse(elements.isEmpty(), "Had: " + elements);

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getElementsByTextContents(page, "form", "bla text1 bla", HtmlTextInput.class)),
                    "Expected a field with tag", "type org.htmlunit.html.HtmlTextInput", "text1", "HtmlForm");

            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByTextContents(page, "img", "notfoundtext", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertTrue(elementsByAttribute.isEmpty(), "Had: " + elementsByAttribute);

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetFailingStatus(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_INTERNALERROR, MIME_HTML, "<html><body></body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            TestHelpers.assertContains(
                    assertThrows(IOException.class,
                            () -> HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port)),
                    "FailingHttpStatusCodeException", Integer.toString(port), "localhost");
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    // helper method to get coverage of the unused constructor
    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testPrivateConstructor(Boolean enableJavascript) throws Exception {
        PrivateConstructorCoverage.executePrivateConstructor(HtmlUnitUtils.class);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetFormElementByName(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\" action=\"do\">" +
                            "<input type=\"text\" name=\"duplicate\"/>" +
                            "<input type=\"text\" name=\"duplicate\"/>" +
                            "<input type=\"text\" name=\"input1\" value=\"value1\"/>" +
                            "<input type=\"image\"/>" +
                        "</form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            HtmlForm form = HtmlUnitUtils.getFormByAction(page, "do");
            assertNotNull(form);

            HtmlTextInput element = HtmlUnitUtils.getFormElementByName(form, "input1", HtmlTextInput.class);
            assertNotNull(element);
            assertEquals("value1", element.getText());

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getFormElementByName(form, "input1", HtmlImageInput.class)),
                    "Expected a field with name", "type org.htmlunit.html.HtmlImageInput", "input1", "HtmlTextInput");

            TestHelpers.assertContains(
                    assertThrows(HtmlUnitException.class,
                            () -> HtmlUnitUtils.getFormElementByName(form, "duplicate", HtmlTextInput.class)),
                    "Did find more than one element", "type org.htmlunit.html.HtmlTextInput", "duplicate");

            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByName(form, "notexisting", HtmlImageInput.class)),
                    "Could not find", "type org.htmlunit.html.HtmlImageInput", "notexisting");

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetFormElementByType(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\" action=\"do\">" +
                            "<input type=\"text\" name=\"name1\"/>" +
                            "<input type=\"text\" name=\"name2\"/>" +
                            "<input type=\"password\" name=\"name2\" value=\"value2\"/>" +
                            "<input type=\"image\"/>" +
                        "</form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            HtmlForm form = HtmlUnitUtils.getFormByAction(page, "do");
            assertNotNull(form);

            HtmlPasswordInput element = HtmlUnitUtils.getFormElementByType(form, HtmlPasswordInput.class);
            assertNotNull(element);
            assertEquals("value2", element.getText());
            assertEquals("name2", element.getNameAttribute());

            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByType(form, HtmlButtonInput.class)),
                    "Could not find", "type org.htmlunit.html.HtmlButtonInput");

            TestHelpers.assertContains(
                    assertThrows(HtmlUnitException.class,
                            () -> HtmlUnitUtils.getFormElementByType(form, HtmlTextInput.class)),
                    "Did find more than one element", "type org.htmlunit.html.HtmlTextInput");

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetFormElementByNameAndValue(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\" action=\"do\">" +
                            "<input type=\"text\" name=\"duplicate\" value=\"value1\"/>" +
                            "<input type=\"text\" name=\"duplicate\" value=\"value1\"/>" +
                            "<input type=\"text\" name=\"input1\" value=\"value1\"/>" +
                            "<input type=\"text\" name=\"input1\" value=\"value2\"/>" +
                            "<input type=\"image\"/>" +
                        "</form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            HtmlForm form = HtmlUnitUtils.getFormByAction(page, "do");
            assertNotNull(form);

            HtmlTextInput element = HtmlUnitUtils.getFormElementByNameAndValue(form, "input1", "value1", HtmlTextInput.class);
            assertNotNull(element);
            assertEquals("value1", element.getText());

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getFormElementByNameAndValue(form, "input1", "value1", HtmlImageInput.class)),
                    "Expected a field with name", "type org.htmlunit.html.HtmlImageInput", "input1", "value1", "HtmlTextInput");

            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByNameAndValue(form, "input1", "valuenotexisting", HtmlTextInput.class)),
                    "Could not find", "type org.htmlunit.html.HtmlTextInput", "input1", "valuenotexisting");

            TestHelpers.assertContains(
                    assertThrows(HtmlUnitException.class,
                            () -> HtmlUnitUtils.getFormElementByNameAndValue(form, "duplicate", "value1", HtmlTextInput.class)),
                    "Did find more than one element", "type org.htmlunit.html.HtmlTextInput", "duplicate", "value1");

            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormElementByNameAndValue(form, "notexisting", "value1", HtmlImageInput.class)),
                    "Could not find", "type org.htmlunit.html.HtmlImageInput", "notexisting", "value1");

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetFormByAction(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"testid\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\" action=\"do\">" +
                            "<input type=\"text\" name=\"blabla\" value=\"value1\"/>" +
                            "<input type=\"image\"/>" +
                        "</form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            HtmlForm form = HtmlUnitUtils.getFormByAction(page, "do");
            assertNotNull(form);

            TestHelpers.assertContains(
                    assertThrows(NoElementFoundException.class,
                            () -> HtmlUnitUtils.getFormByAction(page, "notexisting")),
                    "Could not find", "notexisting");

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testGetElementByAttributeContains(Boolean enableJavascript) throws Exception {
        int port = SocketUtils.getNextFreePort(8000, 9000);

        NanoHTTPD server = new NanoHTTPD(port) {
            @Override
            public Response serve(String uri, String method, Properties header, Properties parms) {
                return new Response(HTTP_OK, MIME_HTML, "<html><body>" +
                        "<img src=\"blabla\" id=\"someid testid someid2\"/>" +
                        "<img src=\"blabla\" name=\"testname\"/>" +
                        "<form name=\"testform\"><input type=\"text\"/><input type=\"text\"/><input type=\"image\"/></form>" +
                        "</body></html>");
            }
        };
        try (WebClient client = HtmlUnitUtils.createWebClient(enableJavascript)) {
            verifier.addObject(client);

            // set empty proxy for localhost
            client.getOptions().setProxyConfig(new ProxyConfig());

            HtmlPage page = HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);

            // get by attribute
            List<HtmlElement> elements = HtmlUnitUtils.getElementsByAttributeContains(page, "img", "id", "nonexistingid", HtmlElement.class);
            assertNotNull(elements);
            assertEquals(0, elements.size(), "Had: " + elements);

            TestHelpers.assertContains(
                    assertThrows(WrongElementException.class,
                            () -> HtmlUnitUtils.getElementsByAttributeContains(page, "form", "name", "test", HtmlInlineFrame.class)),
                    "Expected a field with tag", "type org.htmlunit.html.HtmlInlineFrame", "test", "HtmlForm");

            // none found with normal ByAttribute
            HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elements);
            assertEquals(0, elements.size(), "Had: " + elements);

            // element found with ByAttributeContains
            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByAttributeContains(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertFalse(elementsByAttribute.isEmpty(), "Had: " + elementsByAttribute);

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testMemoryLeak(Boolean enableJavascript) throws IOException {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
            verifier.addObject(webClient);

            // with a wrapped WebConnection we had trouble that closing did not free up stuff correctly
            // unfortunately we currently need to check manually for newly introduced leaks here...
            webClient.setWebConnection(new WebConnectionWrapper(webClient) {
                @Override
                public WebResponse getResponse(WebRequest request) throws IOException {
                    URL url = request.getUrl();
                    logger.info("Having " + url);

                    //logger.info("Loading: " + url);
                    return super.getResponse(request);
                }
            });

            try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_PLAINTEXT, "Ok")) {
                verifier.addObject(server);
                webClient.getPage("http://localhost:" + server.getPort());
            }
        }
    }

    @Disabled("Should not run always")
    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testMemoryLeakLoop(Boolean enableJavascript) throws IOException {
        for(int i = 0;i < 1000;i++) {
            testMemoryLeak(enableJavascript);
        }

        logger.info("Debug here and check for remaining memory after close was called");
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testWaitForText(Boolean enableJavascript) throws Exception {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
            verifier.addObject(webClient);

            try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, "<html>Ok</html>")) {
                verifier.addObject(server);
                SgmlPage page = webClient.getPage("http://localhost:" + server.getPort());
                HtmlUnitUtils.waitForText(page, "Ok", 1000);
            }
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testWaitForTextNotFound(Boolean enableJavascript) throws Exception {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
            verifier.addObject(webClient);

            try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, "<html>Ok</html>")) {
                verifier.addObject(server);
                SgmlPage page = webClient.getPage("http://localhost:" + server.getPort());
                assertThrows(IllegalStateException.class, () -> HtmlUnitUtils.waitForText(page, "Not found", 100));
            }
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testWaitForTextFoundWithDelay(Boolean enableJavascript) throws Exception {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(true)) {
            verifier.addObject(webClient);

            try (MockRESTServer server = new MockRESTServer(() -> {
                return new NanoHTTPD.Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML,
						// return a page which uses JavaScript to delay for 500ms
						// before the text "OkFound" appears as part of the HTML page source
                        "<html><body>" +
                        "<script>" +
                        "setTimeout(function() {" +
                        "  var el = document.createElement('span');" +
                        "  el.textContent = 'Ok' + 'Found';" +
                        "  document.body.appendChild(el);" +
                        "}, 500);" +
                        "</script>" +
                        "</body></html>");
            })) {
                verifier.addObject(server);
                SgmlPage page = webClient.getPage("http://localhost:" + server.getPort());
                HtmlUnitUtils.waitForText(page, "OkFound", 1000);
            }
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testCreate(Boolean enableJavascript) {
        // cover the create() without parameter as well
        try (WebClient webClient = HtmlUnitUtils.createWebClient()) {
            assertNotNull(webClient);
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "Debug-Log: {0}")
    public void testWaitForJavaScript(Boolean enableJavascript) {
        try (WebClient webClient = HtmlUnitUtils.createWebClient()) {
            HtmlUnitUtils.waitForJavascript(webClient, 0);
            HtmlUnitUtils.waitForJavascript(webClient, 10);
            HtmlUnitUtils.waitForJavascript(webClient, 1000);
        }
    }
}
