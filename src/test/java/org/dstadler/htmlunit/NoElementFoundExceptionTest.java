package org.dstadler.htmlunit;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class NoElementFoundExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new NoElementFoundException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
