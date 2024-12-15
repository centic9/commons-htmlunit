package org.dstadler.htmlunit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NoElementFoundExceptionTest {

    @Test
    public void test() {
        // just cover the class
        HtmlUnitException test = new NoElementFoundException("teststring");
        assertNotNull(test);
        assertNotNull(test.getMessage());
    }
}
