package com.onehackoranother.sanexml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import javax.xml.namespace.QName;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Jason Frame
 */
public class Parser {

    //
    // Configurable stuff

    private boolean                 skipAllWhitespace   = false;
    private boolean                 skipComments        = true;

    //
    // State

    private final XMLEventReader    eventReader;
    private XMLEvent                event;
    private int                     depth               = 0;
    
    //
    // Constructors

    public Parser(Reader r) throws IOException, XMLStreamException { this(null, r); }
    public Parser(XMLInputFactory factory, Reader r) throws IOException, XMLStreamException {
        if (factory == null) {
            factory = XMLInputFactory.newInstance();
        }
        eventReader = factory.createXMLEventReader(r);
        next();
    }

    public Parser(File f) throws IOException, XMLStreamException { this(null, f); }
    public Parser(XMLInputFactory factory, File f) throws IOException, XMLStreamException {
        this(factory, new FileReader(f));
    }

    //
    // Configuration

    public boolean getSkipAllWhitespace() { return skipAllWhitespace; }
    public void setSkipAllWhitespace(boolean s) { skipAllWhitespace = s; }

    public boolean getSkipComments() { return skipComments; }
    public void setSkipComments(boolean s) { skipComments = s; }

    //
    // State inspection

    /**
     * Returns the parser's current nesting depth.
     * @return the parser's current nesting depth
     */
    public int getDepth() { return depth; }

    /**
     * Returns the parser's current XML event.
     * @return the parser's current XML event
     */
    public XMLEvent getEvent() { return event; }

    /**
     * Returns the text of the current node, or null if the current node is
     * not a text node.
     * @return the text of the current node
     */
    public String getText() {
        if (atText()) {
            return event.asCharacters().getData();
        } else {
            return null;
        }
    }

    public QName currentTagName() {
        if (event.isStartElement()) {
            return event.asStartElement().getName();
        } else if (event.isEndElement()) {
            return event.asEndElement().getName();
        } else {
            return null;
        }
    }

    public boolean tagNameEquals(QName q) {
        QName tag = currentTagName();
        if (tag == null) {
            return q == null;
        } else {
            return tag.equals(q);
        }
    }

    public boolean tagNameEquals(String s) {
        QName tag = currentTagName();
        if (tag == null) {
            return s == null;
        } else {
            return tag.getLocalPart().equals(s);
        }
    }

    //
    // 

    public void startDocument() throws XMLStreamException, ParseException {
        if (!event.isStartDocument()) {
            error("expecting document start");
        }
        next();
    }

    public void endDocument() throws XMLStreamException, ParseException {
        if (!event.isEndDocument()) {
            error("expecting document end");
        }
        while (eventReader.hasNext()) {
            eventReader.next();
        }
    }

    //
    // Query current state

    public void expect(String tag) throws ParseException {
        expect(tag, "expecting opening tag: " + tag);
    }

    public void expect(String tag, String errorMessage) throws ParseException {
        if (!atTag(tag)) throw new ParseException(errorMessage);
    }

    public boolean atTag() {
        return event.isStartElement();
    }

    public boolean atTag(String... arguments) {
        if (event.isStartElement()) {
            for (String s : arguments) {
                StartElement se = (StartElement) event;
                if (se.getName().getLocalPart().equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean atClosingTag() {
        return event.isEndElement();
    }

    public boolean atText() {
        return event.isCharacters();
    }
    
    //
    //

    public void accept() throws XMLStreamException, ParseException {
        if (event.getEventType() == XMLEvent.START_ELEMENT) {
            depth++;
            next();
        } else {
            error("expecting opening tag");
        }
    }

    public void accept(String tagName, String errorMessage) throws XMLStreamException, ParseException {
        if (atTag(tagName)) {
            accept();
        } else {
            error(errorMessage);
        }
    }

    public void accept(String tagName) throws XMLStreamException, ParseException {
        accept(tagName, "expecting tag: " + tagName);
    }

    public String acceptText(String errorMessage) throws XMLStreamException, ParseException {
        if (atText()) {
            String out = getText();
            next();
            return out;
        } else {
            throw new ParseException(errorMessage);
        }
    }

    public String acceptText() throws XMLStreamException, ParseException {
        return acceptText("expecting text");
    }

    /**
     * Closes the current tag.
     * We present no distinction between empty and self-closing tags, so
     * &lt;foo /&gt; and &lt;foo&gt;&lt;/foo&gt; are both dealt with in the
     * same way:
     *
     * parser.accept("foo"); parser.close();
     *
     * @throws ParseException if the current event is not a closing tag
     * @throws XMLStreamException
     */
    public void close() throws ParseException, XMLStreamException {
        if (!this.atClosingTag()) {
            throw new ParseException("expecting closing tag");
        }
        depth--;
        next();
    }

    /**
     * Skips the current tag and all of its children.
     * You may only call this method when the current event is an opening tag,
     * i.e. if <tt>atTag()</tt> returns <tt>true</tt>.
     *
     * @throws ParseException
     * @throws XMLStreamException
     */
    public void skipTag() throws ParseException, XMLStreamException {
        if (!atTag()) {
            throw new ParseException("can't skip when not at opening tag");
        }
        int startDepth = depth;
        accept();
        while (depth > startDepth) {
            if (atTag()) {
                accept();
            } else if (atClosingTag()) {
                close();
            } else {
                next();
            }
        }
    }

    public boolean hasAttribute(String name) {
        return attribute(name) != null;
    }

    public String attribute(String name) {
        if (!event.isStartElement()) return null;
        StartElement se = event.asStartElement();
        Attribute a = se.getAttributeByName(new QName(name));
        return a == null ? null : a.getValue();
    }

    public String attribute(String name, String defaultValue) throws ParseException {
        String out = attribute(name);
        return out == null ? defaultValue : out;
    }

    /**
     * Shorthand syntax for <tt>attribute(name, "")</tt>
     *
     * @param name attribute name to lookup
     * @return value of attribute, or empty string if attribute is not present
     * @throws ParseException
     */
    public String attrib(String name) throws ParseException {
        return attribute(name, "");
    }

    //
    // Utility

    // Throw error with message
    private void error(String errorMessage) throws ParseException {
        throw new ParseException(errorMessage);
    }

    // Unconditionally move to the next event
    private void next() throws XMLStreamException {
        do {
            event = eventReader.nextEvent();
        } while (isSkippable());
    }

    private boolean isSkippable() {
        switch (event.getEventType()) {
            case XMLEvent.SPACE: return true;
            case XMLEvent.COMMENT: return skipComments;
            case XMLEvent.CHARACTERS: return skipAllWhitespace && event.asCharacters().isWhiteSpace();
            default: return false;
        }
    }
}