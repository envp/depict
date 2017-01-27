package cop5556sp17;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class Scanner
{
    // @TODO Find better way to record Exception LinePos besides using a dummy 0 length EOF token (search code)
    final ArrayList<Token> tokens;
    final String chars;
    int tokenNum;

    HashMap<String, Kind> kinds;
    HashMap<String, Kind> keyWordMap;
    static ArrayList<Integer> newLines;

    /**
     * Kind enum
     * Stores the types of tokens that are considered legal as an enumerable constant.
     */
    public static enum Kind
    {
        IDENT(""),
        INT_LIT(""),
        SEMI(";"),
        COMMA(","),
        LPAREN("("),
        RPAREN(")"),
        LBRACE("{"),
        RBRACE("}"),
        ARROW("->"),
        BARARROW("|->"),
        OR("|"),
        AND("&"),
        EQUAL("=="),
        NOTEQUAL("!="),
        LT("<"),
        GT(">"),
        LE("<="),
        GE(">="),
        PLUS("+"),
        MINUS("-"),
        TIMES("*"),
        DIV("/"),
        MOD("%"),
        NOT("!"),
        ASSIGN("<-"),
        OP_BLUR("blur"),
        OP_GRAY("gray"),
        OP_CONVOLVE("convolve"),
        OP_WIDTH("width"),
        OP_HEIGHT("height"),
        OP_SLEEP("sleep"),
        KW_INTEGER("integer"),
        KW_BOOLEAN("boolean"),
        KW_IMAGE("image"),
        KW_URL("url"),
        KW_FILE("file"),
        KW_FRAME("frame"),
        KW_WHILE("while"),
        KW_IF("if"),
        KW_TRUE("true"),
        KW_FALSE("false"),
        KW_SCREENHEIGHT("screenheight"),
        KW_SCREENWIDTH("screenwidth"),
        KW_XLOC("xloc"),
        KW_YLOC("yloc"),
        KW_HIDE("hide"),
        KW_SHOW("show"),
        KW_MOVE("move"),
        KW_SCALE("scale"),
        EOF("eof");

        Kind(String text)
        {
            this.text = text;
        }

        final String text;

        String getText()
        {
            return text;
        }
    }

    // Internal state representation for the scanner
    // Useful for walking through comments
    private static enum State
    {
        START(0),
        INSIDE_COMMENT(0),
        EXPECT_SLASH(0);
        
        State(int start)
        {
            this.start = start;
        }

        public int start;
    }

    /**
     * Thrown by Scanner when an illegal character is encountered
     */
    @SuppressWarnings("serial")
    public static class IllegalCharException extends Exception
    {
        public IllegalCharException(String message)
        {
            super(message);
        }
    }

    /**
     * Thrown by Scanner when an int literal is not a value that can be
     * represented by an int.
     */
    @SuppressWarnings("serial")
    public static class IllegalNumberException extends Exception
    {
        public IllegalNumberException(String message)
        {
            super(message);
        }
    }

    /**
     * Holds the line and position in the line of a token.
     */
    static class LinePos
    {
        public final int line;
        public final int posInLine;

        public LinePos(int line, int posInLine)
        {
            super();
            this.line = line;
            this.posInLine = posInLine;
        }

        @Override
        public String toString()
        {
            return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
        }

        @Override
        public boolean equals(Object other)
        {
            if(other == null)
            {
                return false;
            }

            if(this == other)
            {
                return true;
            }

            if(other.getClass() != this.getClass())
            {
                return false;
            }

            LinePos _other = (LinePos) other;

            return (_other.line == line) && (_other.posInLine == posInLine);
        }
    }

    public class Token
    {
        public final Kind kind;
        public final int pos; // position in input array
        public final int length;

        // returns the text of this Token
        public String getText()
        {
            if(kind.getText().length() == length)
            {
                return kind.getText();
            }
            else
            {
                return chars.substring(pos, pos + length);
            }
        }

        // returns a LinePos object representing the line and column of this
        // Token
        LinePos getLinePos()
        {
            // newLines contains 0-offsets of all newLines in the source file
            // We want the index of the one that
            int line = 0, col = 0;

            line = Collections.binarySearch(Scanner.newLines, this.pos);
            line = line < 0 ? (-line - 1) : line;
            if(line == 0)
            {
                col = this.pos;
            }
            else if(line > 0)
            {
                col = this.pos - 1 - Scanner.newLines.get(line - 1);
            }

            return new LinePos(line, col);
        }

        Token(Kind kind, int pos, int length)
        {
            this.kind = kind;
            this.pos = pos;
            this.length = length;
        }

        /**
         * Precondition: kind = Kind.INT_LIT, the text can be represented with a
         * Java int. Note that the validity of the input should have been
         * checked when the Token was created. So the exception should never be
         * thrown.
         *
         * @return int value of this token, which should represent an INT_LIT
         * @throws NumberFormatException
         */
        public int intVal() throws NumberFormatException
        {
            return Integer.parseInt(getText());
        }
    }

    Scanner(String chars)
    {
        this.chars = chars;
        this.tokens = new ArrayList<Token>();
        this.tokenNum = 0;

        this.kinds = new HashMap<>();
        this.keyWordMap = new HashMap<>();
        newLines = new ArrayList<>();

        for(Kind kind : Kind.values())
        {
            if(!kind.getText().isEmpty())
            {
                kinds.put(kind.getText(), kind);

                // All keywords
                if(kind.name().contains("KW_") || kind.name().contains("OP_") )
                {
                    keyWordMap.put(kind.getText(), kind);
                }
            }
        }
    }

    /**
     * Initializes Scanner object by traversing chars and adding tokens to
     * tokens list.
     *
     * @return this scanner
     * @throws IllegalCharException
     * @throws IllegalNumberException
     */
    public Scanner scan() throws IllegalCharException, IllegalNumberException
    {
        int pos, j;
        char ch, _ch;
        State state = State.START;

        for(pos = 0; pos < chars.length(); ++pos)
        {
            ch = chars.charAt(pos);
            int start;
            if(!Character.isWhitespace(ch) || ch == '\n')
            {
                switch(state)
                {
                    case START:
                        switch(ch)
                        {
                            case '\n':
                                newLines.add(pos);
                                break;
                            case '0':
                                tokens.add(new Token(Kind.INT_LIT, pos, 1));
                                break;
                            case '&':
                            case '+':
                            case '*':
                            case '%':
                            case ';':
                            case ',':
                            case '(':
                            case ')':
                            case '{':
                            case '}':
                                tokens.add(new Token(kinds.get(Character.toString(ch)), pos, 1));
                                break;
                            //<editor-fold desc="case !:">
                            case '!':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '=')
                                    {
                                        tokens.add(new Token(Kind.NOTEQUAL, pos, 2));
                                        ++pos;
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.NOT, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.NOT, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case =:">
                            case '=':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '=')
                                    {
                                        tokens.add(new Token(Kind.EQUAL, pos, 2));
                                        ++pos;
                                    }
                                    else
                                    {
                                        throw new IllegalCharException(
                                            String.format("Illegal character %c at index %s",
                                                  chars.charAt(pos), new Token(Kind.EOF, pos, 0).getLinePos()
                                            )
                                        );
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    throw new IllegalCharException(
                                        String.format("Illegal character %c at index %d",
                                              chars.charAt(pos), new Token(Kind.EOF, pos, 0).getLinePos()
                                        )
                                    );
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case <:">
                            case '<':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '=')
                                    {
                                        tokens.add(new Token(Kind.LE, pos, 2));
                                        ++pos;
                                    }
                                    else if(_ch == '-')
                                    {
                                        tokens.add(new Token(Kind.ASSIGN, pos, 2));
                                        ++pos;
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.LT, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.LT, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case >:">
                            case '>':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '=')
                                    {
                                        tokens.add(new Token(Kind.GE, pos, 2));
                                        ++pos;
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.GT, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.GT, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case -:">
                            case '-':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '>')
                                    {
                                        tokens.add(new Token(Kind.ARROW, pos, 2));
                                        ++pos;
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.MINUS, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.MINUS, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case |:">
                            case '|':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '-')
                                    {
                                        try
                                        {
                                            _ch = chars.charAt(pos + 2);
                                            if(_ch == '>')
                                            {
                                                tokens.add(new Token(Kind.BARARROW, pos, 3));
                                                pos = pos + 2;
                                            }
                                            else
                                            {
                                                tokens.add(new Token(Kind.OR, pos, 1));
                                                tokens.add(new Token(Kind.MINUS, pos + 1, 1));
                                                ++pos;
                                            }
                                        }
                                        catch(StringIndexOutOfBoundsException ex)
                                        {
                                            tokens.add(new Token(Kind.OR, pos, 1));
                                            tokens.add(new Token(Kind.MINUS, pos + 1, 1));
                                            ++pos;
                                        }
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.OR, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.OR, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="case /:">
                            case '/':
                                try
                                {
                                    _ch = chars.charAt(pos + 1);
                                    if(_ch == '*')
                                    {
                                        state = State.INSIDE_COMMENT;
                                        state.start = pos;
                                        ++pos;
                                    }
                                    else
                                    {
                                        tokens.add(new Token(Kind.DIV, pos, 1));
                                    }
                                }
                                catch(StringIndexOutOfBoundsException ex)
                                {
                                    tokens.add(new Token(Kind.DIV, pos, 1));
                                }
                                //</editor-fold>
                                break;
                            //<editor-fold desc="INT_LIT, IDENT">
                            default:
                                if(isDigit(ch))
                                {
                                    j = pos;
                                    try
                                    {
                                        while(isDigit(chars.charAt(j)))
                                        {
                                            ++j;
                                        }
                                        int $ = Integer.parseInt(chars.substring(pos, j));
                                        tokens.add(new Token(Kind.INT_LIT, pos, j - pos));
                                        pos = j - 1;
                                    }
                                    catch(NumberFormatException ex)
                                    {
                                        throw new IllegalNumberException(
                                            "Found illegal integer beginning at " +
                                                new Token(Kind.EOF, pos, 0).getLinePos()
                                        );
                                    }
                                    catch(StringIndexOutOfBoundsException ex)
                                    {
                                        try
                                        {
                                            int $ = Integer.parseInt(chars.substring(pos, j));
                                        }
                                        catch(NumberFormatException _ex)
                                        {
                                            throw new IllegalNumberException(
                                                "Found illegal integer beginning at " +
                                                    new Token(Kind.EOF, pos, 0).getLinePos()
                                            );
                                        }
                                        tokens.add(new Token(Kind.INT_LIT, pos, j - pos));
                                        pos = j - 1;
                                    }
                                }
                                else if(isIdentStart(ch))
                                {
                                    j = pos;
                                    try
                                    {
                                        while(isIdentPart(chars.charAt(j)))
                                        {
                                            ++j;
                                        }
                                        if(isKeywordOrReserved(chars.substring(pos, j)))
                                        {
                                            Kind k = keyWordMap.get(chars.substring(pos, j));
                                            tokens.add(new Token(k, pos, j - pos));
                                        }
                                        else
                                        {
                                            tokens.add(new Token(Kind.IDENT, pos, j - pos));
                                        }
                                        pos = j - 1;
                                    }
                                    catch(StringIndexOutOfBoundsException ex)
                                    {
                                        Kind k = keyWordMap.get(chars.substring(pos, j));
                                        k = k == null ? Kind.IDENT : k;
                                        tokens.add(new Token(k, pos, j - pos));
                                        pos = j - 1;
                                    }
                                }
                                else
                                {
                                    // We don't recognize any of these chars
                                    throw new IllegalCharException(
                                        String.format(
                                            "Found unknown character %c at index %s",
                                                ch, new Token(Kind.EOF, pos, 0).getLinePos()
                                        )
                                    );
                                }
                                //</editor-fold>
                                break;
                        }
                        break;
                    //<editor-fold desc="case INSIDE_COMMENT">
                    case INSIDE_COMMENT:
                        if(pos + 1 == chars.length())
                        {
                            throw new IllegalCharException("End of source reached before comment close was seen");
                        }
                        switch(ch)
                        {
                            case '*':
                                state = State.EXPECT_SLASH;
                                break;
                            default:
                                state = State.INSIDE_COMMENT;
                                break;
                        }
                        //</editor-fold>
                        break;
                    //<editor-fold desc="case EXPECT_SLASH">
                    case EXPECT_SLASH:
                        if(pos + 1 > chars.length())
                        {
                            throw new IllegalCharException("End of source reached before comment close was seen");
                        }
                        switch(ch)
                        {
                            case '*':
                                state = State.EXPECT_SLASH;
                                break;
                            case '/':
                                state = State.START;
                                break;
                            default:
                                state = State.INSIDE_COMMENT;
                                break;
                        }
                        //</editor-fold>
                        break;
                    default:
                        break;
                }
            }
        }

        tokens.add(new Token(Kind.EOF, pos, 0));
        return this;
    }

    /**
     * Return the next token in the token list and update the state so that the
     * next call will return the Token..
     */
    public Token nextToken()
    {
        if(tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum++);
    }

    /**
     * Return the next token in the token list without updating the state. (So
     * the following call to next will return the same token.)
     */
    public Token peek()
    {
        if(tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum + 1);
    }

    /**
     * Returns a LinePos object containing the line and position in line of the
     * given token.
     * Line numbers start counting at 0
     *
     * @param t
     * @return line and column numbers corresponding to the token's start
     */
    public LinePos getLinePos(Token t)
    {
        return t.getLinePos();
    }

    /**
     * Checks whether the argument is a digit
     *
     * @param ch character to be tested
     * @return True if character is a digit literal, false otherwise
     */
    private boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Checks if the character is a valid starting character for an identifier
     *
     * @param ch character to be tested
     * @return true if valid identifier start, false otherwise
     */
    private boolean isIdentStart(char ch)
    {
        return (
            Character.toLowerCase(ch) >= 'a' &&
                Character.toLowerCase(ch) <= 'z'
        ) || ch == '$' || ch == '_';
    }

    /**
     * Checks if the character is a valid character for an identifier part
     *
     * @param ch character to be tested
     * @return true if character is a valid identifier part, false otherwise
     */
    private boolean isIdentPart(char ch)
    {
        return isIdentStart(ch) || isDigit(ch);
    }

    /**
     * Checks if the given string is a scanner keyword or not
     *
     * @param s String to be checked for keyword / reserved usage
     * @return true if {@code s} is a keyword or is reserved, false otherwise
     */
    private boolean isKeywordOrReserved(String s)
    {
        return keyWordMap.get(s) != null;
    }

}
