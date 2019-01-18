package org.dstadler.htmlunit;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class WrongElementExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new WrongElementException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
