package org.dstadler.htmlunit;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class NoElementFoundExceptionTest {

    @Test
    public void test() throws Exception {
        // just cover the class
        assertNotNull(new NoElementFoundException("teststring"));
    }
}
