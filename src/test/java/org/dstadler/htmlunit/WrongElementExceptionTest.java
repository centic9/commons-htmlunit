package org.dstadler.htmlunit;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class WrongElementExceptionTest {

    @Test
    public void test() throws Exception {
        // just cover the class
        assertNotNull(new WrongElementException("teststring"));
    }
}
