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

    /**
     * Create a new <tt>Parser</tt>
     * @param r <tt>Reader</tt> from which XML should be read and parsed
     *
     * @throws IOException
     * @throws XMLStreamException
     */
    public Parser(Reader r) throws IOException, XMLStreamException { this(null, r); }

    /**
     * Create a new <tt>Parser</tt>
     *
     * @param factory <tt>XMLInputFactory</tt> used to create underlying <tt>XMLEventReader</tt>
     * @param r <tt>Reader</tt> from which XML should be read and parsed
     * @throws IOException
     * @throws XMLStreamException
     */
    public Parser(XMLInputFactory factory, Reader r) throws IOException, XMLStreamException {
        if (factory == null) {
            factory = XMLInputFactory.newInstance();
        }
        eventReader = factory.createXMLEventReader(r);
        next();
    }

    /**
     * Create a new <tt>Parser</tt>
     * @param f <tt>File</tt> from which XML should be read and parsed
     *
     * @throws IOException
     * @throws XMLStreamException
     */
    public Parser(File f) throws IOException, XMLStreamException { this(null, f); }

    /**
     * Create a new <tt>Parser</tt>
     *
     * @param factory <tt>XMLInputFactory</tt> used to create underlying <tt>XMLEventReader</tt>
     * @param f <tt>File</tt> from which XML should be read and parsed
     * @throws IOException
     * @throws XMLStreamException
     */
    public Parser(XMLInputFactory factory, File f) throws IOException, XMLStreamException {
        this(factory, new FileReader(f));
    }

    //
    // Configuration

    /**
     * Returns the parser's whitespace skipping mode.
     *
     * @return parser's whitespace skipping mode
     */
    public boolean getSkipAllWhitespace() { return skipAllWhitespace; }

    /**
     * Sets the parser's whitespace skipping mode.
     *
     * When <tt>true</tt>, parser will ignore any character nodes consisting only
     * of whitespace. "Ignoreable whitespace", as defined by an XML document's DTD,
     * is always skipped, regardless of this setting.
     *
     * @param s new value of whitespace skipping mode
     */
    public void setSkipAllWhitespace(boolean s) { skipAllWhitespace = s; }

    /**
     * Returns the parser's comment skipping mode.
     *
     * @return parser's comment skipping mode.
     */
    public boolean getSkipComments() { return skipComments; }

    /**
     * Sets the parser's comment skipping mode.
     *
     * When <tt>true</tt>, parser will ignore any comment nodes.
     *
     * @param s new value of comment skipping mode
     */
    public void setSkipComments(boolean s) { skipComments = s; }

    //
    // State inspection

    /**
     * Returns the parser's current nesting depth.
     *
     * @return the parser's current nesting depth
     */
    public int getDepth() { return depth; }

    /**
     * Returns the parser's current XML event.
     * 
     * @return the parser's current XML event
     */
    public XMLEvent getEvent() { return event; }

    /**
     * Returns the text of the current node, or null if the current node is
     * not a text node.
     * 
     * @return the text of the current node
     */
    public String getText() {
        if (atText()) {
            return event.asCharacters().getData();
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the current opening or closing tag.
     *
     * @return the name of the current opening/closing tag, or <tt>null</tt> if
     *         the current node if neither an opening nor a closing tag.
     */
    public QName currentTagName() {
        if (event.isStartElement()) {
            return event.asStartElement().getName();
        } else if (event.isEndElement()) {
            return event.asEndElement().getName();
        } else {
            return null;
        }
    }

    /**
     * Returns <tt>true</tt> if the current tag name equals the give tag name.
     *
     * @param q tag name current node name should match
     * @return <tt>true</tt> if current node is opening/closing tag, and tag name
     *         matches <tt>q</tt>, or if not at opening/closing tag and <tt>q</tt>
     *         is null. <tt>false</tt> otherwise.
     */
    public boolean tagNameEquals(QName q) {
        QName tag = currentTagName();
        if (tag == null) {
            return q == null;
        } else {
            return tag.equals(q);
        }
    }

    /**
     * Returns <tt>true</tt> if the local part of the current tag name equals
     * the give tag name.
     *
     * @param s tag name local part current node should match
     * @return <tt>true</tt> if current node is opening/closing tag, and local
     *         part of its name matches <tt>s</tt>, or if not at opening/closing
     *         tag and <tt>s</tt> is null. <tt>false</tt> otherwise.
     */
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

    /**
     * Consume document start or throw error.
     *
     * @throws XMLStreamException if error in underlying XML document
     * @throws ParseException if current node is not document start
     */
    public void startDocument() throws XMLStreamException, ParseException {
        if (!event.isStartDocument()) {
            error("expecting document start");
        }
        next();
    }

    /**
     * Consume document end or throw error.
     *
     * @throws XMLStreamException if error in underlying XML document
     * @throws ParseException if current node is not document end
     */
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

    /**
     * Ensure that the current node is an opening with the given name, throw
     * an error otherwise.
     *
     * @param tag expected tag name
     * @throws ParseException if current node is not matching opening tag
     */
    public void expect(String tag) throws ParseException {
        expect(tag, "expecting opening tag: " + tag);
    }

    /**
     * Ensure that the current node is an opening with the given name, throw
     * an error otherwise.
     *
     * @param tag expected tag name
     * @param errorMessage error message passed to exception on failure
     * @throws ParseException if current node is not matching opening tag
     */
    public void expect(String tag, String errorMessage) throws ParseException {
        if (!atTag(tag)) throw new ParseException(errorMessage);
    }

    /**
     * Returns true if the current node is an opening tag, false otherwise.
     *
     * @return true if the current node is an opening tag, false otherwise
     */
    public boolean atTag() {
        return event.isStartElement();
    }

    /**
     * Returns true if the current node is an opening tag matching any of the
     * given tag names, false otherwise.
     *
     * @param arguments tag names to check for
     * @return true if current node if opening tag with name matching any of
     *         <tt>arguments</tt>
     */
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

    /**
     * Returns true if current node is a closing tag, false otherwise.
     *
     * @return true if current node is a closing tag, false otherwise
     */
    public boolean atClosingTag() {
        return event.isEndElement();
    }

    /**
     * Returns true if current node is text, false otherwise.
     *
     * @return true if current node is text, false otherwise.
     */
    public boolean atText() {
        return event.isCharacters();
    }
    
    //
    //

    /**
     * Consume any opening tag or throw error.
     * 
     * @throws XMLStreamException
     * @throws ParseException if current node is not an opening tag
     */
    public void accept() throws XMLStreamException, ParseException {
        if (event.getEventType() == XMLEvent.START_ELEMENT) {
            depth++;
            next();
        } else {
            error("expecting opening tag");
        }
    }

    /**
     * Consume opening tag with local name part matching given tag name.
     *
     * @param tagName expected local part of tag name
     * @param errorMessage error message to be thrown on error
     * @throws XMLStreamException
     * @throws ParseException if current node is not opening tag with matching name
     */
    public void accept(String tagName, String errorMessage) throws XMLStreamException, ParseException {
        if (atTag(tagName)) {
            accept();
        } else {
            error(errorMessage);
        }
    }

    /**
     * Consume opening tag with local name part matching given tag name.
     *
     * @param tagName expected local part of tag name
     * @throws XMLStreamException
     * @throws ParseException if current node is not opening tag with matching name
     */
    public void accept(String tagName) throws XMLStreamException, ParseException {
        accept(tagName, "expecting tag: " + tagName);
    }

    /**
     * Consume text node or throw error.
     *
     * @param errorMessage error message to throw if not at text node
     * @return contents of text node
     * @throws XMLStreamException
     * @throws ParseException if current node is not a text node
     */
    public String acceptText(String errorMessage) throws XMLStreamException, ParseException {
        if (atText()) {
            String out = getText();
            next();
            return out;
        } else {
            throw new ParseException(errorMessage);
        }
    }

    /**
     * Consume text node or throw error.
     *
     * @return contents of text node
     * @throws XMLStreamException
     * @throws ParseException if current node is not a text node
     */
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
     * @throws XMLStreamException if there was an error with the underlying XML
     *         stream, such as document not being well-formed.
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
     * @throws ParseException if current node is not an opening tag.
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

    /**
     * Returns <tt>true</tt> if current node has attribute with given name.
     * Attributes are only ever present at opening tags.
     *
     * @param name name of attribute for which to check
     * @return <tt>true</tt> if node has named attribute, <tt>false</tt> otherwise.
     */
    public boolean hasAttribute(String name) {
        return attribute(name) != null;
    }

    /**
     * Returns the value of the attribute with the given name at the current node.
     * Attributes are only ever present at opening tags.
     *
     * @param name attribute name to lookup
     * @return attribute value if present, <tt>null</tt> otherwise.
     */
    public String attribute(String name) {
        if (!event.isStartElement()) return null;
        StartElement se = event.asStartElement();
        Attribute a = se.getAttributeByName(new QName(name));
        return a == null ? null : a.getValue();
    }

    /**
     * Returns the value of attribute with the given name at the current node,
     * or a given default value if the current node has no such attribute.
     * Attributes are only ever present at opening tags.
     *
     * @param name attribute name to lookup
     * @param defaultValue value to return if attribue is not present
     * @return attribute value if present, <tt>defaultValue</tt> otherwise
     */
    public String attribute(String name, String defaultValue) {
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
    public String attrib(String name) {
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