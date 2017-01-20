package cop5556sp17;


import java.util.ArrayList;


public class Scanner
{
    /**
     * Kind enum
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

    // Internal state representation
    private static enum State
    {
        START(0),
        INSIDE_COMMENT(0),
        EXPECT_SLASH(0),
        EXPECT_PIPE_OR_BARARROW(0),
        EXPECT_BARARROW(0),
        ILLEGAL_CHAR(0),
        EXPECT_EQ(0),
        EXPECT_BANG_EQ(0),
        EXPECT_MAYBE_EQ_OR_MINUS(0),
        EXPECT_MAYBE_EQ(0),
        EXPECT_GT(0),
        EXPECT_INT_LIT(0),
        EXPECT_IDENT(0),
        EXPECT_DIV_OR_COMMENT(0);

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
            // @TODO Implement this
            return null;
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
        tokens = new ArrayList<Token>();
        tokenNum = 0;
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
        int pos;
        char ch;
        State state = State.START;
        ArrayList<Integer> newlines = new ArrayList<Integer>();

        for(pos = 0; pos < chars.length(); ++pos)
        {
            ch = chars.charAt(pos);
            if( !(Character.isWhitespace(ch) || ch == '\n') )
            {
                switch(state)
                {
                    case ILLEGAL_CHAR:
                        throw new IllegalCharException(String.format(
                            "Found illegal character %c at index %d",
                            chars.charAt(pos - 1), pos - 1
                        ));
                    case START:
                        switch(ch)
                        {
                            case '\n':
                                newlines.add(pos);
                                break;
                            case '0':
                                tokens.add(new Token(Kind.INT_LIT, pos , 1));
                                state = State.START;
                                break;
                            case '/':
                                state = State.EXPECT_DIV_OR_COMMENT;
                                state.start = pos;
                                break;
                            case '|':
                                state = State.EXPECT_PIPE_OR_BARARROW;
                                state.start = pos;
                                break;
                            case '&':
                                tokens.add(new Token(Kind.AND, pos, 1));
                                state = State.START;
                                break;
                            case '=':
                                state = State.EXPECT_EQ;
                                state.start = pos;
                                break;
                            case '!':
                                state = State.EXPECT_BANG_EQ;
                                state.start = pos;
                                break;
                            case '<':
                                state = State.EXPECT_MAYBE_EQ_OR_MINUS;
                                state.start = pos;
                                break;
                            case '>':
                                state = State.EXPECT_MAYBE_EQ;
                                state.start = pos;
                                break;
                            case '+':
                                tokens.add(new Token(Kind.PLUS, pos, 1));
                                state = State.START;
                                break;
                            case '-':
                                state = State.EXPECT_GT;
                                state.start = pos;
                                break;
                            case '*':
                                tokens.add(new Token(Kind.TIMES, pos, 1));
                                state = State.START;
                                break;
                            case '%':
                                tokens.add(new Token(Kind.MOD, pos , 1));
                                state = State.START;
                                break;
                            case ';':
                                tokens.add(new Token(Kind.SEMI, pos , 1));
                                state = State.START;
                                break;
                            case ',':
                                tokens.add(new Token(Kind.COMMA, pos , 1));
                                state = State.START;
                                break;
                            case '(':
                                tokens.add(new Token(Kind.LPAREN, pos , 1));
                                state = State.START;
                                break;
                            case ')':
                                tokens.add(new Token(Kind.RPAREN, pos , 1));
                                state = State.START;
                                break;
                            case '{':
                                tokens.add(new Token(Kind.LBRACE, pos , 1));
                                state = State.START;
                                break;
                            case '}':
                                tokens.add(new Token(Kind.RBRACE, pos , 1));
                                state = State.START;
                                break;
                            default:
                                /*
                                 * Set the state and force a rewind to correctly
                                 * read informationWe do this only for ints and
                                 * idents to avoid off by 1 errors.
                                 * The remainder of our tokens are
                                 * comparatively trivial to handle
                                 */
                                if(isDigit(ch))
                                {
                                    state = State.EXPECT_INT_LIT;
                                    pos = pos - 1;
                                }
                                else if(isIdentStart(ch))
                                {
                                    state = State.EXPECT_IDENT;
                                    pos = pos - 1;
                                }
                                else
                                {
                                    state = State.ILLEGAL_CHAR;
                                    pos = pos - 1;
                                }
                                break;
                        }
                        break;
                    case EXPECT_INT_LIT:
                        int j = pos;
                        char _ch = chars.charAt(j);
                        /*
                         * Keep reading while we get numeric digits to extend
                         * our token to the longest possible length
                         */
                        while(isDigit(_ch) && j < chars.length() - pos - 1)
                        {
                            ++j;
                            _ch = chars.charAt(j);
                        }
                        tokens.add(new Token(Kind.INT_LIT, pos, j));
                        state = State.START;
                        pos = pos + j;
                        break;
                    case EXPECT_IDENT:
                        // Apparently variables defined in another case
                        // Are in the scope of this case too...
                        j = pos;
                        _ch = chars.charAt(j);
                        /*
                         * IdentPart is a superset of identstart, safe to use
                         * this check here because if the DFA has reached this
                         * state it means we've encounted an IDENT_START and
                         * rewinded.
                         */
                        while(isIdentPart(_ch) && j < chars.length() - pos)
                        {
                            _ch = chars.charAt(j);
                            ++j;
                        }

                        String id = chars.substring(pos, pos + j);

                        if(isKeywordOrReserved(id))
                        {
                            tokens.add(new Token(Kind.valueOf(id), pos, j));
                        }
                        else
                        {
                            tokens.add(new Token(Kind.IDENT, pos, j));
                        }
                        state = State.START;
                        pos = pos + j;
                        break;
                    case EXPECT_GT:
                        switch(ch)
                        {
                            case '>':
                                tokens.add(new Token(Kind.ARROW, state.start, 2));
                                state = State.START;
                                break;
                            default:
                                tokens.add(new Token(Kind.MINUS, state.start, 1));
                                state = State.START;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case EXPECT_MAYBE_EQ:
                        switch(ch)
                        {
                            case '=':
                                tokens.add(new Token(Kind.GE, state.start, 2));
                                state = State.START;
                                break;
                            default:
                                tokens.add(new Token(Kind.GT, state.start, 1));
                                state = State.START;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case EXPECT_MAYBE_EQ_OR_MINUS:
                        switch(ch)
                        {
                            case '=':
                                tokens.add(new Token(Kind.LE, state.start, 2));
                                state = State.START;
                                break;
                            case '-':
                                tokens.add(new Token(Kind.ASSIGN, state.start, 2));
                                state = State.START;
                                break;
                            default:
                                tokens.add(new Token(Kind.LT, state.start, 1));
                                state = State.START;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case EXPECT_DIV_OR_COMMENT:
                        switch(ch)
                        {
                            case '*':
                                state = State.INSIDE_COMMENT;
                                break;
                            default:
                                tokens.add(new Token(Kind.DIV, state.start, 1));
                                state = State.START;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case INSIDE_COMMENT:
                        if(pos == (chars.length() - 1))
                        {
                            throw new IllegalCharException("Expected matching '*/', but found end of source file");
                        }
                        switch(ch)
                        {
                            case '*':
                                state = State.EXPECT_SLASH;
                                break;
                            default:
                                state = State.INSIDE_COMMENT;
                        }
                        break;
                    case EXPECT_SLASH:
                        switch(ch)
                        {
                            case '*':
                                state = State.EXPECT_SLASH;
                                break;
                            case '/':
                                // Directly jump back to start state since there
                                // is no 'accept' state for comments
                                state = State.START;
                                break;
                            default:
                                state = State.INSIDE_COMMENT;
                                break;
                        }
                        break;
                    case EXPECT_PIPE_OR_BARARROW:
                        switch(ch)
                        {
                            case '-':
                                state = State.EXPECT_BARARROW;
                                break;
                            default:
                                tokens.add(new Token(Kind.OR, state.start, 1));
                                pos = pos - 1;
                                state = State.START;
                                break;
                        }
                        break;
                    case EXPECT_BARARROW:
                        switch(ch)
                        {
                            case '>':
                                tokens.add(new Token(Kind.BARARROW, pos - 2, 3));
                                state = State.START;
                                pos = pos - 1;
                                break;
                            default:
                                state = State.ILLEGAL_CHAR;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case EXPECT_EQ:
                        switch(ch)
                        {
                            case '=':
                                tokens.add(new Token(Kind.EQUAL, state.start, 2));
                                state = State.START;
                                break;
                            default:
                                state = State.ILLEGAL_CHAR;
                                pos = pos - 1;
                                break;
                        }
                        break;
                    case EXPECT_BANG_EQ:
                        switch(ch)
                        {
                            case '=':
                                tokens.add(new Token(Kind.NOTEQUAL, pos - 1, 2));
                                state = State.START;
                                break;
                            default:
                                tokens.add(new Token(Kind.NOT, pos - 1, 1));
                                state = State.START;
                                pos = pos - 1;
                                break;
                        }
                        break;

                }
            }
        }

        tokens.add(new Token(Kind.EOF, pos, 0));
        return this;
    }

    final ArrayList<Token> tokens;
    final String chars;
    int tokenNum;

    /**
     * Return the next token in the token list and update the state so that the
     * next call will return the Token..
     */
    public Token nextToken()
    {
        if (tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum++);
    }

    /**
     * Return the next token in the token list without updating the state. (So
     * the following call to next will return the same token.)
     */
    public Token peek()
    {
        if (tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum + 1);
    }

    /**
     * Returns a LinePos object containing the line and position in line of the
     * given token.
     * Line numbers start counting at 0
     *
     * @param t
     * @return
     */
    public LinePos getLinePos(Token t)
    {
        return t.getLinePos();
    }

    /**
     * Checks whether the argument is a digit
     * @param ch character to be tested
     * @return True if character is a digit literal, false otherwise
     */
    private boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Checks if the character is a valid starting character for an identifier
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
     * @param ch character to be tested
     * @return true if character is a valid identifier part, false otherwise
     */
    private boolean isIdentPart(char ch)
    {
        return isIdentStart(ch) || isDigit(ch);
    }

    /**
     * Checks if the given string is a scanner keyword or not
     * @param s String to be checked for keyword / reserved usage
     * @return true if {@code s} is a keyword or is reserved, false otherwise
     */
    private boolean isKeywordOrReserved(String s)
    {
        Kind k;
        try
        {
            k = Kind.valueOf(s);
        } catch(IllegalArgumentException ex)
        {
            return false;
        }
        return true;
    }

}
