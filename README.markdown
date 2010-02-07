sane-xml - enjoyable XML processing for Java
============================================

(c) 2010 Jason Frame [jason@onehackoranother.com]

Will explain why this exists later.

Example
-------

Given the following XML document:

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