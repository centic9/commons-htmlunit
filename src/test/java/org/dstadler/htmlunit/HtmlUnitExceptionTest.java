package org.dstadler.htmlunit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HtmlUnitExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new HtmlUnitException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
