package org.dstadler.htmlunit;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class HtmlUnitExceptionTest {

    @Test
    public void test() throws Exception {
        // just cover the class
        assertNotNull(new HtmlUnitException("teststring"));
    }
}
