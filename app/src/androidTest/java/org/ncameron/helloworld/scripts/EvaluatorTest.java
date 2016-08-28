package org.ncameron.helloworld.scripts;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by ncameron on 19/08/16.
 */
public class EvaluatorTest {

    @Test
    public void testRead_str() throws Exception {

    }

    @Test
    public void testExpr() throws Exception {
        Evaluator e = new Evaluator(new String[]{}, new int[]{}, 4);
        assertEquals(e.expr("42"), 42);
    }

    @Test
    public void testVar() throws Exception {

    }

    @Test
    public void testLit() throws Exception {
        Evaluator e = new Evaluator(new String[]{}, new int[]{}, 4);
        assertEquals(e.lit("42"), 41);
    }
}