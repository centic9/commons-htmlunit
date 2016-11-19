/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: WebPageFileCache.java
 * @date: 10.12.2014
 * @author: cwat-dstadler
 */
package org.dstadler.htmlunit;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


/**
 *
 * @author cwat-dstadler
 */
public class WebPageFileCache {
	private static final Log logger = LogFactory.getLog(WebPageFileCache.class);

	private static final File CACHE_DIR = new File(System.getProperty("java.io.tmpdir"), "htmlunit-cache");

	// How long we use the cached files
	private static final long CACHE_FILE_TIMEOUT = TimeUnit.HOURS.toMillis(20);

	public HtmlPage handle(WebClient webClient, String url) throws IOException {
		File file = new File(CACHE_DIR, stripUrl(url) + ".html");

		// need to load from scratch
		if(!file.exists() || file.length() == 0 || ((System.currentTimeMillis() - file.lastModified()) > CACHE_FILE_TIMEOUT)) {
			logger.info("Loading page from " + url + ", storing in cache at " + file);
			HtmlPage page = HtmlUnitUtils.getInitialPage(webClient, url);

			if(file.exists()) {
				if(!file.delete()) {
					throw new IOException("Could not remove file before updating cache: " + file);
				}
			}
			page.save(file);

			return page;
		}

		// we can use the cached file
		String fileUrl = file.toURI().toString();
		logger.info("Loading page for " + url + " from cache at " + fileUrl);
		return HtmlUnitUtils.getInitialPage(webClient, fileUrl);
	}

	/**
	 * Remove all stored cached files
	 * @throws IOException
	 */
	public void clear() throws IOException {
		FileUtils.deleteDirectory(CACHE_DIR);
	}

	private static String stripUrl(String url) {
		// remove special chars to allow to use the url as filename
		return StringUtils.removePattern(url, "[:/_&?=]");
	}
}
