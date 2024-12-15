package org.dstadler.htmlunit;

import java.io.Serial;


public class NoElementFoundException extends HtmlUnitException {
    @Serial
    private static final long serialVersionUID = -7638075911892583052L;

    public NoElementFoundException(String arg0) {
        super(arg0);
    }
}
