package org.dstadler.htmlunit;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class HtmlUnitUtils {
    private static final Log logger = LogFactory.getLog(HtmlUnitUtils.class);

    private HtmlUnitUtils() {
    }

    public static WebClient createWebClient() {
        return createWebClient(true);
    }

    public static WebClient createWebClient(boolean enableJavaScript) {
        return createWebClient(enableJavaScript, BrowserVersion.FIREFOX_60);
    }

    public static WebClient createWebClient(boolean enableJavaScript, BrowserVersion browserVersion) {
        logger.debug("Creating client");

        // proxy for some machines
        final WebClient webClient = new WebClient(browserVersion);
        webClient.waitForBackgroundJavaScriptStartingBefore(1000);
        webClient.getOptions().setTimeout(60000);
        webClient.getOptions().setJavaScriptEnabled(enableJavaScript);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setRedirectEnabled(true); // follow old-school HTTP 302 redirects - standard behaviour

        webClient.setHTMLParserListener(null);
        webClient.setIncorrectnessListener((message, origin) -> {
            // Swallow for now, but maybe collect it for optional retrieval?
        });
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setThrowExceptionOnScriptError(false);    // ignore script errors

        return webClient;
    }

    public static HtmlPage getInitialPage(final WebClient webClient, final String url) throws IOException {
        try {
            HtmlPage page = webClient.getPage(url);
            logger.debug("Page title = " + page.getTitleText());

            /*
             * webClient.setAjaxController(new MyAjaxController());
             * page.addDomChangeListener( new MyDomChangeListener());
             */
            return page;
        } catch (FailingHttpStatusCodeException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getElementById(final HtmlPage page, String id, Class<T> type) throws HtmlUnitException {
        DomElement element = page.getElementById(id);
        if(element == null) {
            logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
            throw new NoElementFoundException("Could not find element with id '" + id + "' on page " + page.getUrl());
        }
        if(!type.isAssignableFrom(element.getClass())) {
            logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
            throw new WrongElementException("Expected a field with id '" + id + "' and type " + type.getName() +
                    ", but had an element of type " + element.getClass() + " on page: " + page.getUrl());
        }

        return (T) element;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getElementByName(final HtmlPage page, String name, Class<T> type) throws HtmlUnitException {
        final HtmlElement element;
        try {
            element = page.getElementByName(name);
        } catch (ElementNotFoundException e) {
            throw new NoElementFoundException("Could not find element with name '" + name + "' on page " + page.getUrl() + ": " + e);
        }
        /*will throw exception anyway:
        if(element == null) {
            logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
            throw new ElementNotFoundException("Could not find element with name '" + name + "' on page " + page.getUrl());
        }*/
        if(!type.isAssignableFrom(element.getClass())) {
            logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
            throw new WrongElementException("Expected a field with name '" + name + "' and type " + type.getName() +
                    ", but had an element of type " + element.getClass() + " on page: " + page.getUrl());
        }

        return (T) element;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> List<T> getElementsByAttribute(final HtmlPage page, String tagName, String attribute, String value, Class<T> type) throws HtmlUnitException {
        List<T> list = new ArrayList<>();
        DomNodeList<DomElement> elementsByTagName = page.getElementsByTagName(tagName);
        for(DomElement element : elementsByTagName) {
            String attValue = element.getAttribute(attribute);
            if(attValue.equals(value)) {
                if(!type.isAssignableFrom(element.getClass())) {
                    logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
                    throw new WrongElementException("Expected a field with tag '" + tagName + "', attribute '" + attribute +
                            "', value '" + value + "' and type " + type.getName() +
                            ", but had an element of type " + element.getClass() + " on page: " + page.getUrl());
                }

                list.add((T) element);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> List<T> getElementsByAttributeContains(final HtmlPage page, String tagName, String attribute, String value, Class<T> type) throws WrongElementException {
        List<T> list = new ArrayList<>();
        DomNodeList<DomElement> elementsByTagName = page.getElementsByTagName(tagName);
        for(DomElement element : elementsByTagName) {
            String attValue = element.getAttribute(attribute);
            if(attValue.contains(value)) {
                if(!type.isAssignableFrom(element.getClass())) {
                    logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
                    throw new WrongElementException("Expected a field with tag '" + tagName + "', attribute '" + attribute +
                            "', which contains value '" + value + "' and type " + type.getName() +
                            ", but had an element of type " + element.getClass() + " on page: " + page.getUrl());
                }

                list.add((T) element);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> List<T> getElementsByTextContents(final HtmlPage page, String tagName, String text, Class<T> type) throws WrongElementException {
        List<T> list = new ArrayList<>();
        DomNodeList<DomElement> elementsByTagName = page.getElementsByTagName(tagName);
        for(DomElement element : elementsByTagName) {
            if(element.getTextContent().equals(text)) {
                if(!type.isAssignableFrom(element.getClass())) {
                    logger.warn("Page contents (" + page.getUrl() + "): " + page.asXml());
                    throw new WrongElementException("Expected a field with tag '" + tagName + "', " +
                            "which contains text '" + text + "' and type " + type.getName() +
                            ", but had an element of type " + element.getClass() + " on page: " + page.getUrl());
                }

                list.add((T) element);
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getFormElementByType(HtmlPage page, String formName, Class<T> type) throws HtmlUnitException {
        final HtmlForm form;
        try {
            form = page.getFormByName(formName);
        } catch (ElementNotFoundException e) {
            throw new NoElementFoundException("Could not find form with name '" + formName + "' on page " + page.getUrl() + ": " + e);
        }

        // use a stack to recursively walk into all sub-elements, not just the first level
        Stack<DomElement> elements = new Stack<>();
        Iterators.addAll(elements, form.getChildElements().iterator());
        T search = null;
        while(!elements.isEmpty()) {
            DomElement it = elements.pop();
            if(type.isAssignableFrom(it.getClass())) {
                if(search != null) {
                    logger.warn("Form contents (" + page.getUrl() + '/' + formName + "): " + form.asXml());
                    throw new HtmlUnitException("Did find more than one element of type " + type.getName() + " in form '" + formName + "' on page " + page.getUrl());
                }

                search = (T) it;
            }
            Iterators.addAll(elements, it.getChildElements().iterator());
        }
        if(search == null) {
            logger.warn("Form contents (" + page.getUrl() + '/' + formName + "): " + form.asXml());
            throw new NoElementFoundException("Could not find element of type " + type.getName() + " in form '" + formName + "' on page " + page.getUrl());
        }
        return search;
    }


    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getFormElementByName(final HtmlForm form, String name, Class<T> type) throws HtmlUnitException {
        // use a stack to recursively walk into all sub-elements, not just the first level
        Stack<DomElement> elements = new Stack<>();
        Iterators.addAll(elements, form.getChildElements().iterator());
        HtmlElement element = null;
        while(!elements.isEmpty()) {
            DomElement it = elements.pop();
            if(it.getAttribute("name").equals(name)) {
                // don't allow to find it twice
                if(element != null) {
                    logger.warn("Form contents: " + form.asXml());
                    throw new HtmlUnitException("Did find more than one element with name " + name + " and type " + type.getName() + " in form.");
                }

                element = (HtmlElement)it;
            }
            Iterators.addAll(elements, it.getChildElements().iterator());
        }

        if(element == null) {
            logger.warn("Form contents (" + form.asXml());
            throw new NoElementFoundException("Could not find element with name " + name + " of type " + type.getName() + " in form '" + form.getNameAttribute());
        }

        if(!type.isAssignableFrom(element.getClass())) {
            logger.warn("Form contents: " + form.asXml());
            throw new WrongElementException("Expected a field with name '" + name + "' and type " + type.getName() +
                    ", but had an element of type " + element.getClass());
        }

        return (T) element;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getFormElementByNameAndValue(final HtmlForm form, String name, String value, Class<T> type) throws HtmlUnitException {
        // use a stack to recursively walk into all sub-elements, not just the first level
        Stack<DomElement> elements = new Stack<>();
        Iterators.addAll(elements, form.getChildElements().iterator());
        HtmlElement element = null;
        while(!elements.isEmpty()) {
            DomElement it = elements.pop();
            if(it.getAttribute("name").equals(name) && it.getAttribute("value").equals(value)) {
                // don't allow to find it twice
                if(element != null) {
                    logger.warn("Form contents: " + form.asXml());
                    throw new HtmlUnitException("Did find more than one element with name " + name + ", value " + value + " and type " + type.getName() + " in form.");
                }

                element = (HtmlElement)it;
            }
            Iterators.addAll(elements, it.getChildElements().iterator());
        }

        if(element == null) {
            logger.warn("Form contents (" + form.asXml());
            throw new NoElementFoundException("Could not find element with name " + name + ", value " + value + " of type " + type.getName() + " in form '" + form.getNameAttribute());
        }

        if(!type.isAssignableFrom(element.getClass())) {
            logger.warn("Form contents: " + form.asXml());
            throw new WrongElementException("Expected a field with name '" + name + "', value '" + value + "' and type " + type.getName() +
                    ", but had an element of type " + element.getClass());
        }

        return (T) element;
    }

    @SuppressWarnings("unchecked")
    public static <T extends HtmlElement> T getFormElementByType(final HtmlForm form, Class<T> type) throws HtmlUnitException {
        // use a stack to recursively walk into all sub-elements, not just the first level
        Stack<DomElement> elements = new Stack<>();
        Iterators.addAll(elements, form.getChildElements().iterator());
        HtmlElement element = null;
        while(!elements.isEmpty()) {
            DomElement it = elements.pop();
            if(type.isAssignableFrom(it.getClass())) {
                // don't allow to find it twice
                if(element != null) {
                    logger.warn("Form contents: " + form.asXml());
                    throw new HtmlUnitException("Did find more than one element of type " + type.getName() + " in form.");
                }

                element = (HtmlElement)it;
            }
            Iterators.addAll(elements, it.getChildElements().iterator());
        }

        if(element == null) {
            logger.warn("Form contents (" + form.asXml());
            throw new NoElementFoundException("Could not find element of type " + type.getName() + " in form '" + form.getNameAttribute());
        }

        return (T) element;
    }

    /**
     * Returns the first form on the page with the given action-attribute.
     *
     * @param page The page to look at
     * @param action The action-attribute that the form should have.
     * @return The found form, not null always
     *
     * @throws NoElementFoundException if no form with the given action attribute is found.
     */
    public static HtmlForm getFormByAction(HtmlPage page, String action) throws HtmlUnitException {
        // <form action="add-perm.action">
        for(HtmlForm form : page.getForms()) {
            if(form.getActionAttribute().equals(action)) {
                return form;
            }
        }

        throw new NoElementFoundException("Could not find form with action '" + action + '\'');
    }

    /**
     * Wait for up to the given time for the given text to appear
     *
     * @param page The page to look at
     * @param str The text that is looked for
     * @param waitMS The amount of milliseconds to wait until waiting is ended
     *
     * @throws IllegalStateException If the text does not appear withing the given time.
     */
    public static void waitForText(SgmlPage page, String str, int waitMS) {
        for(int i = 0;i < waitMS/100;i++) {
            if(!page.asXml().contains(str)) {
                // found
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        Preconditions.checkState(page.asXml().contains(str), "Still found %s", str);
    }

    /**
     * Call {@link WebClient#waitForBackgroundJavaScript(long)} with
     * 1 second delay until it returns 0 or the given number of seconds
     * has passed.
     *
     * @param client The WebClient to call.
     * @param seconds The number of seconds that the call will take at max
     */
    public static void waitForJavascript(WebClient client, int seconds) {
        for(int i = 0;i < seconds;i++) {
            int jobs = client.waitForBackgroundJavaScript(1000);
            if(jobs == 0) {
                break;
            }
        }

    }
}
