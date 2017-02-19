package org.dstadler.htmlunit;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dstadler.commons.http.NanoHTTPD;
import org.dstadler.commons.net.SocketUtils;
import org.dstadler.commons.testing.MemoryLeakVerifier;
import org.dstadler.commons.testing.MockRESTServer;
import org.dstadler.commons.testing.PrivateConstructorCoverage;
import org.dstadler.commons.testing.TestHelpers;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HtmlUnitUtilsTest {
    private static final Log logger = LogFactory.getLog(HtmlUnitUtilsTest.class);

    @Parameterized.Parameters(name = "Debug-Log: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { Boolean.TRUE }, { Boolean.FALSE }
        });
    }

    @Parameterized.Parameter
    public Boolean enableJavascript;

    private final MemoryLeakVerifier verifier = new MemoryLeakVerifier();

    @After
    public void tearDown() {
        verifier.assertGarbageCollected();
    }

    @Test
    public void testGetElementById() throws Exception {
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
            try {
                HtmlUnitUtils.getElementById(page, "nonexistingid", HtmlElement.class);
                fail("Expected exception when looking for a non-existing id");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find element", "nonexistingid");
            }

            try {
                HtmlUnitUtils.getElementById(page, "testid", HtmlTextInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with id", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "testid", "HtmlImage");
            }

            assertNotNull(HtmlUnitUtils.getElementById(page, "testid", HtmlImage.class));

            // get by name
            try {
                HtmlUnitUtils.getElementByName(page, "nonexistingname", HtmlElement.class);
                fail("Expected exception");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "nonexistingname");
            }

            try {
                HtmlUnitUtils.getElementByName(page, "testname", HtmlTextInput.class);
                fail("Expected exception with non-matching type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with name", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "testname", "HtmlImage");
            }


            assertNotNull(HtmlUnitUtils.getElementByName(page, "testname", HtmlImage.class));


            // verify forms
            assertNotNull("Should find element in existing form",
                    HtmlUnitUtils.getFormElementByType(page, "testform", HtmlImageInput.class));

            try {
                // fails for form that does not exist
                HtmlUnitUtils.getFormElementByType(page, "notexistingform", HtmlImageInput.class);
                fail("Expected exception");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "notexistingform");
            }

            try {
                // fails for type that is not found
                HtmlUnitUtils.getFormElementByType(page, "testform", HtmlRadioButtonInput.class);
                fail("Expected exception");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find element of type", "HtmlRadioButtonInput");
            }

            try {
                // fails for type that is available multiple times
                HtmlUnitUtils.getFormElementByType(page, "testform", HtmlTextInput.class);
                fail("Expected exception");
            } catch (HtmlUnitException e) {
                TestHelpers.assertContains(e, "Did find more than one element", "HtmlTextInput");
            }

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetElementByAttribute() throws Exception {
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
            assertTrue("Had: " + elements, elements.isEmpty());

            try {
                HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlTextInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with tag", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "testid", "HtmlImage");
            }

            // found with correct content
            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertFalse("Had: " + elementsByAttribute, elementsByAttribute.isEmpty());

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetElementByTextContains() throws Exception {
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
            assertFalse("Had: " + elements, elements.isEmpty());

            try {
                HtmlUnitUtils.getElementsByTextContents(page, "form", "bla text1 bla", HtmlTextInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with tag", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "text1", "HtmlForm");
            }

            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByTextContents(page, "img", "notfoundtext", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertTrue("Had: " + elementsByAttribute, elementsByAttribute.isEmpty());

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetFailingStatus() throws Exception {
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

            try {
                HtmlUnitUtils.getInitialPage(client, "http://localhost:" + port);
                fail("Should have caught an exception");
            } catch (IOException e) {
                TestHelpers.assertContains(e, "FailingHttpStatusCodeException", Integer.toString(port), "localhost");
            }
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    // helper method to get coverage of the unused constructor
    @Test
    public void testPrivateConstructor() throws Exception {
        PrivateConstructorCoverage.executePrivateConstructor(HtmlUnitUtils.class);
    }

    @Test
    public void testGetFormElementByName() throws Exception {
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

            try {
                HtmlUnitUtils.getFormElementByName(form, "input1", HtmlImageInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with name", "type com.gargoylesoftware.htmlunit.html.HtmlImageInput", "input1", "HtmlTextInput");
            }

            try {
                HtmlUnitUtils.getFormElementByName(form, "duplicate", HtmlTextInput.class);
                fail("Expected exception when having duplicate element");
            } catch (HtmlUnitException e) {
                TestHelpers.assertContains(e, "Did find more than one element", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "duplicate");
            }

            try {
                HtmlUnitUtils.getFormElementByName(form, "notexisting", HtmlImageInput.class);
                fail("Expected exception when not finding the element");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find", "type com.gargoylesoftware.htmlunit.html.HtmlImageInput", "notexisting");
            }

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetFormElementByType() throws Exception {
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

            try {
                HtmlUnitUtils.getFormElementByType(form, HtmlButtonInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find", "type com.gargoylesoftware.htmlunit.html.HtmlButtonInput");
            }

            try {
                HtmlUnitUtils.getFormElementByType(form, HtmlTextInput.class);
                fail("Expected exception when having duplicate element");
            } catch (HtmlUnitException e) {
                TestHelpers.assertContains(e, "Did find more than one element", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput");
            }

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetFormElementByNameAndValue() throws Exception {
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

            try {
                HtmlUnitUtils.getFormElementByNameAndValue(form, "input1", "value1", HtmlImageInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with name", "type com.gargoylesoftware.htmlunit.html.HtmlImageInput", "input1", "value1", "HtmlTextInput");
            }

            try {
                HtmlUnitUtils.getFormElementByNameAndValue(form, "input1", "valuenotexisting", HtmlTextInput.class);
                fail("Expected exception when using a non-matching element type");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "input1", "valuenotexisting");
            }

            try {
                HtmlUnitUtils.getFormElementByNameAndValue(form, "duplicate", "value1", HtmlTextInput.class);
                fail("Expected exception when having duplicate element");
            } catch (HtmlUnitException e) {
                TestHelpers.assertContains(e, "Did find more than one element", "type com.gargoylesoftware.htmlunit.html.HtmlTextInput", "duplicate", "value1");
            }

            try {
                HtmlUnitUtils.getFormElementByNameAndValue(form, "notexisting", "value1", HtmlImageInput.class);
                fail("Expected exception when not finding the element");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find", "type com.gargoylesoftware.htmlunit.html.HtmlImageInput", "notexisting", "value1");
            }

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetFormByAction() throws Exception {
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

            try {
                HtmlUnitUtils.getFormByAction(page, "notexisting");
                fail("Expected exception when using a non-existing form action");
            } catch (NoElementFoundException e) {
                TestHelpers.assertContains(e, "Could not find", "notexisting");
            }

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testGetElementByAttributeContains() throws Exception {
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
            assertEquals("Had: " + elements, 0, elements.size());

            try {
                HtmlUnitUtils.getElementsByAttributeContains(page, "div", "href", "testid", HtmlInlineFrame.class);
                fail("Expected exception");
            } catch (WrongElementException e) {
                TestHelpers.assertContains(e, "Expected a field with tag", "type com.gargoylesoftware.htmlunit.html.HtmlInlineFrame", "testid", "HtmlImage");
            }

            // none found with normal ByAttribute
            HtmlUnitUtils.getElementsByAttribute(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elements);
            assertEquals("Had: " + elements, 0, elements.size());

            // element found with ByAttributeContains
            List<HtmlImage> elementsByAttribute = HtmlUnitUtils.getElementsByAttributeContains(page, "img", "id", "testid", HtmlImage.class);
            assertNotNull(elementsByAttribute);
            assertFalse("Had: " + elementsByAttribute, elementsByAttribute.isEmpty());

            verifier.addObject(page);
        } finally {
            server.stop();
        }

        verifier.addObject(server);
    }

    @Test
    public void testMemoryLeak() throws IOException {
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

    @Ignore("Should not run always")
    @Test
    public void testMemoryLeakLoop() throws IOException {
        for(int i = 0;i < 1000;i++) {
            testMemoryLeak();
        }

        logger.info("Debug here and check for remaining memory after close was called");
    }

    @Test
    public void testWaitForText() throws Exception {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
            verifier.addObject(webClient);

            try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, "<html>Ok</html>")) {
                verifier.addObject(server);
                SgmlPage page = webClient.getPage("http://localhost:" + server.getPort());
                HtmlUnitUtils.waitForText(page, "Ok", 1000);
            }
        }
    }

    @Test
    public void testWaitForTextNotFound() throws Exception {
        try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
            verifier.addObject(webClient);

            try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_HTML, "<html>Ok</html>")) {
                verifier.addObject(server);
                SgmlPage page = webClient.getPage("http://localhost:" + server.getPort());
                try {
                    HtmlUnitUtils.waitForText(page, "Not found", 100);
                    fail("Should catch Exception here");
                } catch (IllegalStateException e) {
                    // expected here
                }
            }
        }
    }

    @Test
    public void testCreate() {
        // cover the create() without parameter as well
        try (WebClient webClient = HtmlUnitUtils.createWebClient()) {
            assertNotNull(webClient);
        }
    }
}
