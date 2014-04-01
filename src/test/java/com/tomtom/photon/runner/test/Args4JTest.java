package com.tomtom.photon.runner.test;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Before;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


public class Args4JTest extends TestCase {
    
    private enum Test {
        T1
    }

    @Option(name = "-a", usage = "this is X")
    private int x;

    @Option(name = "-b", usage = "this is Y", metaVar = "<output>")
    private File y;

    @Option(name = "-t", usage = "this is T", metaVar = "<output>")
    private Test t;

    @Before
    public void before() {
        this.x = 0;
        this.y = null;
        this.t = null;
    }

    public void testParsingAllArgs() throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument("-a", "1", "-b", "foo");
        assertEquals(1, x);
        assertEquals(new File("foo"), y);
    }
    
    public void testParsingEnumArgs() throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument("-t", "T1");
        assertEquals(Test.T1, t);
    }

}
