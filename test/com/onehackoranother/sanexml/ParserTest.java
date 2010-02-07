package com.onehackoranother.sanexml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jason
 */
public class ParserTest {

    public ParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private Parser p;

    @Test
    public void testParser() {

        try {
            p = openFile("test1.xml");
            p.setSkipAllWhitespace(true);
            
            p.startDocument();

            open("root");
                open("people");

                    expect("person");
                    assertEquals("0", p.attribute("id"));

                    open("person");

                        open("name");
                        assertEquals("Jason", p.acceptText());
                        p.close();

                        open("age");
                        assertEquals("29", p.acceptText());
                        p.close();

                    p.close();

                p.close();


            p.close();

            p.endDocument();


        } catch (XMLStreamException ex) {
            fail();
        } catch (ParseException ex) {
            fail();
        }

    }

    private void expect(String tagName) throws ParseException {
        assertTrue(p.atTag(tagName));
        p.expect(tagName);
    }

    private void open(String tagName) throws ParseException, XMLStreamException {
        assertTrue(p.atTag(tagName));
        p.expect(tagName);
        p.accept(tagName);
    }

    private Parser openFile(String name) {
        try {
            URL url = this.getClass().getResource(name);
            return new Parser(new BufferedReader(new InputStreamReader(url.openStream())));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}