package org.dstadler.htmlunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.dstadler.commons.http.NanoHTTPD;
import org.dstadler.commons.testing.MemoryLeakVerifier;
import org.dstadler.commons.testing.MockRESTServer;
import org.junit.After;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;


public class WebPageFileCacheTest {
	private static final String AMAZON_URL = "http://www.amazon.de/gp/offer-listing/B009S4DVI2/ref=dp_olp_new?ie=UTF8&condition=new";

	private final MemoryLeakVerifier verifier = new MemoryLeakVerifier();

	@After
	public void tearDown() {
		verifier.assertGarbageCollected();
	}

	@Test
	public void testHandle() throws Exception {
		try (MockRESTServer server = new MockRESTServer(NanoHTTPD.HTTP_OK, "text/html", "<html><body><a href=\"http://www.google.at/\">link</a></body></html")) {
			try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
				verifier.addObject(webClient);

				WebPageFileCache cache = new WebPageFileCache();

				// make sure the cache is empty initially
				cache.clear();

				// load the file the first time
				HtmlPage page = cache.handle(webClient, "http://localhost:" + server.getPort());
				checkLink(page);

				// load a second time, now from the cache
				page = cache.handle(webClient, "http://localhost:" + server.getPort());
				checkLink(page);

				verifier.addObject(page);
			}
		}
	}

	@Test
	public void testHandleComplexPage() throws Exception {
		try (WebClient webClient = HtmlUnitUtils.createWebClient(false)) {
			verifier.addObject(webClient);

			WebPageFileCache cache = new WebPageFileCache();

			// make sure the cache is empty initially
			cache.clear();

			// load the file the first time
			HtmlPage page = cache.handle(webClient, AMAZON_URL);
			checkAmazon(page);

			// load a second time, now from the cache
			page = cache.handle(webClient, AMAZON_URL);
			checkAmazon(page);

			verifier.addObject(page);
		}
	}

	private void checkAmazon(HtmlPage page) throws HtmlUnitException {
		assertNotNull(page);
		DomNodeList<DomElement> hrefs = page.getElementsByTagName("a");
		assertTrue(hrefs.size() > 0);

		final List<HtmlSpan> priceSpans;
		priceSpans = HtmlUnitUtils.getElementsByAttribute(page, "span", "class", "price", HtmlSpan.class);
		priceSpans.addAll(HtmlUnitUtils.getElementsByAttributeContains(page, "span", "class", "a-color-price", HtmlSpan.class));
		priceSpans.addAll(HtmlUnitUtils.getElementsByAttributeContains(page, "span", "class", "olpOfferPrice", HtmlSpan.class));

		for(HtmlSpan span : priceSpans) {
			DomNode seller = span.getParentNode().getParentNode();
			for (HtmlElement element : seller.getHtmlElementDescendants()) {
				if (element instanceof HtmlAnchor) {
					String href = element.getAttribute("href");
					if (href.contains("A3TN1BADY8I80N")) {
						// done!
						return;
					}
				}
			}
		}
		fail("Could not find link to shop baby-direkt at " + AMAZON_URL);
	}

	private void checkLink(HtmlPage page) {
		assertNotNull(page);
		DomNodeList<DomElement> hrefs = page.getElementsByTagName("a");
		assertEquals(1, hrefs.size());

		assertEquals("http://www.google.at/", ((HtmlAnchor)hrefs.get(0)).getHrefAttribute());
	}
}
