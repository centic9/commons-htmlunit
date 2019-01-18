package org.dstadler.htmlunit;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class HtmlUnitExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new HtmlUnitException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
