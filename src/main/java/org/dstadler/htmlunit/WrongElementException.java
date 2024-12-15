package org.dstadler.htmlunit;

import java.io.Serial;


public class WrongElementException extends HtmlUnitException {
    @Serial
    private static final long serialVersionUID = 1871130851511111467L;

    public WrongElementException(String arg0) {
        super(arg0);
    }
}
