package org.dstadler.htmlunit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WrongElementExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new WrongElementException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
