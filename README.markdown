sane-xml - enjoyable XML processing for Java
============================================

(c) 2010 Jason Frame [jason@onehackoranother.com]

I've always found hand-writing recursive descent parsers strangely therapeutic. But whenever I'm faced with parsing XML - particularly in Java - I feel quite ill. SAX inevitably involves creating some bug-ridden state-machine that turns out to be neither simple nor extensible, and DOM is rife with unnecessary verbosity. Depending on what you're doing it might also turn out to be downright inefficient. And don't get me started on those enterprisey tools for auto-generating piles of code that involve learning a handful of technologies with 3 or 4 letter acronyms. Hey look, a field full of hairy yaks. So I was happy to discover the new `javax.xml.stream.EventReader` class in JDK6 that allowed me to access the underlying XML stream, Hollywood style*. It was close to what I was looking for, but just a little raw, only offering methods for getting the current state. I wanted to make it more like recursive-descent parsing, with the ability to directly ask questions like "am I current at one of these opening tags?", "what's the current node text?", and to issue commands: "advance if we're at tag 'foo', throw an error otherwise".

(* as in "Don't call me, I'll call you")

A quick word about recursive descent parsing
--------------------------------------------

I've been out of academia for some years now so please go easy on me :)

Classical recursive-descent parsing relies solely on standard control structures (if/else, switch, while), a method for each distinct entity (production) to be parsed, plus a couple of functions for managing the token stream: `peek()`, which returns the current token, and `accept()`, which advances to the next token. As a convenience we can also define `accept(token)` as:

    void accept(int token) {
        if (peek() != token) {
            error();
        } else {
            accept();
        }
    }

And then we can write code like this for actually doing the parsing:

    void parsePeople() {
        while (peek() == PERSON) {
            parsePerson();
        }
    }

    void parsePerson() {
        accept(PERSON)
        accept(NAME);
        accept(AGE);
        if (peek() == CHILDREN) {
            accept();
            parsePeople(); // recursion
        }
    }
    
In reality we would also do some real work during parsing, like creating domain objects and either returning them or adding them to some sort of data structure.
    
Hello, sane-xml
---------------

`sane-xml` provides an API allowing XML to be parsed in a similar fashion, including specific features for dealing with tags, text and attributes. It's best illustrated by an example. Given the following XML document:

    <level>
      <tilesets>
        <tileset id='0' filename='ts1.png' tilesize='16' />
        <tileset id='0' filename='ts2.png' tilesize='16' />
      </tilesets>
      <map width='2' height='2' tilesize='16'>
        <r>
          <c s='0' t='0' />
          <c s='0' t='1' />
        </r>
        <r>
          <c s='1' t='0' />
          <c s='1' t='1' />
        </r>
      </map>
    </level>

Here's some (heavily annotated) code to parse it:

    import com.onehackoranother.sanexml.Parser;
    import com.onehackoranother.sanexml.ParseException;

    class LevelParser extends Parser {
        public LevelParser(File f) throws IOException, XMLStreamException {
            
            super(f);
            
            // When true, all character elements containing only whitespace will be skipped
            // by the parser. This is distinct from "ignoreable whitespace", which is always
            // skipped regardless of this setting.
            setSkipAllWhitespace(true);
        
        }

        public void parse() throws IOException, XMLStreamException, ParseException {
            
            // Start parsing. Always call this first.
            startDocument();

            // Exception will be thrown if parser is not at opening tag <level>
            // After a call to accept(), internal cursor moves to next document element
            accept("level");

                accept("tilesets");
                
                    // Check for an opening tag without consuming it
                    while (atTag("tileset")) {
                        
                        // Read attributes from current tag
                        // Note: attributes are read *before* calling accept()
                        // attrib() is safe version of attribute() and will never return null
                        // (see javadoc for more details)
                        String filename = attrib("file");
                        String id = attrib("id");
                        int tilesize = Integer.valueOf(attrib("tilesize"));
                        
                        // Every call to accept() must be balanced by a call to close()
                        // close() will throw an exception if the cursor is not currently at a closing tag.
                        // In the XML above, <tileset /> is self-closing. This makes no difference to
                        // the parser - tags are always processed by a call to accept() then a corresponding
                        // call to close().
                        accept();
                        close();
                    
                    }
                close();

                // Exception will be thrown if parser is not a opening tag <map>
                // Calling expect() does *not* consume the current tag.
                // Use expect() when you need to ensure you're at a certain tag
                // but need to perform additional operations on it (e.g. reading
                // attributes)
                expect("map");

                int width       = Integer.valueOf(attrib("width"));
                int height      = Integer.valueOf(attrib("height"));
                int tilesize    = Integer.valueOf(attrib("tilesize"));

                // Unconditionally accept currenty opening tag
                // Exception will be thrown if cursor is not at an opening tag
                accept();

                    for (int j = 0; j < height; j++) {
                        accept("r");
                        for (int i = 0; i < width; i++) {
                            if (atTag("n")) {
                                // ... process null tile
                                
                                // Skips the current tag and all of its children
                                // Exception will be thrown if cursor is not at an opening tag
                                skipTag();
                            
                            } else if (atTag("c")) {
                                // ... process tile attributes & add to map
                                
                                skipTag();
                            }
                        }
                        close();
                    }

                close();

            close();

            // End parsing
            endDocument();
            
        }
    }

The interface to the parser is public so standalone parsers can be instantiated without the need for subclassing:

    Parser p = new Parser(new File("foo.xml"));
    p.startDocument();
    p.accept("hello");
    p.close();
    p.endDocument();
    
TODO
----

Not much, it's largely complete thanks to all the heavy lifting being handled by `javax.xml.stream`.

Maybe a C version?

Bug Reporting, Feature Requests
-------------------------------

Please use Github-provided facilities (issue tracker, pull requests).

