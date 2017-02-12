package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;

import java.util.*;
import java.util.stream.Collectors;

import static cop5556sp17.Scanner.Kind.*;

public class Parser
{

    /**
     * Exception to be thrown if a syntax error is detected in the input.
     * You will want to provide a useful error message.
     */
    @SuppressWarnings("serial")
    public static class SyntaxException extends Exception
    {
        public SyntaxException(String message)
        {
            super(message);
        }
    }


    private enum Productions
    {
        //<editor-fold desc="Production names">
        StrongOp(true, false),
        WeakOp(true, false),
        RelOp(true, false),
        Factor(false, false),
        Elem(false, false),
        Term(false, false),
        Expression(false, false),
        Arg(false, true),
        ImageOp(true, false),
        FrameOp(true, false),
        FilterOp(true, false),
        ChainElem(false, false),
        ArrowOp(true, false),
        IfStatement(false, false),
        WhileStatement(false, false),
        Chain(false, false),
        Assign(false, false),
        Statement(false, false),
        Dec(true, false),
        Block(false, false),
        ParamDec(false, false),
        ProgramTail(false, false),
        Program(false, false);
        //</editor-fold>

        boolean terminal;
        boolean epsilon;

        /*
         * Containers for first and predict sets of productionsHere be dragons.
         * Initialized using an anonymous inner class
         * to skimp on writing the name of the data structure.
         * THIS AND THE NEXT INITIALIZATION
         */
        private static Map<Productions, Set<Kind>> FIRST = new HashMap<>();
        private static Map<Productions, Set<Kind>> PREDICT = new HashMap<>();

        static
        {
            FIRST.put(StrongOp, new HashSet<>(Arrays.asList(
                TIMES, DIV, AND, MOD
            )));

            FIRST.put(WeakOp, new HashSet<>(Arrays.asList(
                PLUS, MINUS, OR
            )));

            FIRST.put(RelOp, new HashSet<>(Arrays.asList(
                LT, LE, GT, GE, EQUAL, NOTEQUAL
            )));

            FIRST.put(Factor, new HashSet<>(Arrays.asList(
                IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENWIDTH, KW_SCREENHEIGHT, LPAREN
            )));

            FIRST.put(Elem, new HashSet<>(FIRST.get(Factor)));

            FIRST.put(Term, new HashSet<>(FIRST.get(Elem)));

            FIRST.put(Expression, new HashSet<>(FIRST.get(Term)));

            FIRST.put(Arg, new HashSet<>(Collections.singletonList(LPAREN)));

            FIRST.put(ImageOp, new HashSet<>(Arrays.asList(
                OP_WIDTH, OP_HEIGHT, KW_SCALE
            )));

            FIRST.put(FrameOp, new HashSet<>(Arrays.asList(
                KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC
            )));

            FIRST.put(FilterOp, new HashSet<>(Arrays.asList(
                OP_BLUR, OP_GRAY, OP_CONVOLVE
            )));

            FIRST.put(ChainElem, new HashSet<>(Arrays.asList(
                IDENT,
                OP_BLUR, OP_GRAY, OP_CONVOLVE,
                KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC,
                OP_WIDTH, OP_HEIGHT, KW_SCALE
            )));

            FIRST.put(ArrowOp, new HashSet<>(Arrays.asList(
                ARROW, BARARROW
            )));

            FIRST.put(IfStatement, new HashSet<>(Collections.singletonList(KW_IF)));

            FIRST.put(WhileStatement, new HashSet<>(Collections.singletonList(KW_WHILE)));

            FIRST.put(Chain, new HashSet<>(FIRST.get(ChainElem)));

            FIRST.put(Assign, new HashSet<>(Collections.singletonList(IDENT)));

            FIRST.put(Statement, new HashSet<>(Arrays.asList(
                OP_SLEEP,
                KW_WHILE,
                KW_IF,
                IDENT,
                OP_BLUR, OP_GRAY, OP_CONVOLVE,
                KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC,
                OP_WIDTH, OP_HEIGHT, KW_SCALE
            )));

            FIRST.put(Dec, new HashSet<>(Arrays.asList(
                KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME
            )));

            FIRST.put(Block, new HashSet<>(Collections.singletonList(LBRACE)));

            FIRST.put(ParamDec, new HashSet<>(Arrays.asList(
                KW_INTEGER, KW_BOOLEAN, KW_URL, KW_FILE
            )));

            FIRST.put(ProgramTail, new HashSet<>(Arrays.asList(
                LBRACE,
                KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN
            )));

            FIRST.put(Program, new HashSet<>(Collections.singletonList(IDENT)));

            // Add all the non-epsilon productions first to the predict set
            for( Productions production : FIRST.keySet() )
            {
                if( !production.epsilon )
                {
                    PREDICT.put(production, new HashSet<>(FIRST.get(production)));
                }
            }

            // Then adjust the epsilon production with a follow set
            // Normally I'd add a follow set map but that's quite the waste
            // since only 1 EPS production exists
            // FOLLOW(Arg) = {ARROW, BARARROW, SEMI}
            PREDICT.put(Arg, new HashSet<>(Arrays.asList(LPAREN, ARROW, BARARROW, SEMI)));
        }

        Productions(boolean terminal, boolean epsilon)
        {
            this.terminal = terminal;
            this.epsilon = epsilon;
        }

        boolean predictContains(Kind kind)
        {
            return PREDICT.get(this).contains(kind);
        }

        boolean firstContains(Kind kind)
        {
            return FIRST.get(this).contains(kind);
        }
    }

    Scanner scanner;
    Token tok;

    Parser(Scanner scanner)
    {
        this.scanner = scanner;
        tok = scanner.nextToken();
    }

    private String getErrorMessage(Token actual, Kind... expected)
    {
        String expNames = Arrays.stream(expected).
            map(k -> String.format("%s(%s)", k.toString(), k.getText())).
                                    collect(Collectors.joining(", "));
        return String.format(
            "Saw token %s(%s) at %s, but expecting one of (%s)",
            actual.kind, actual.getText(),
            actual.getLinePos(),
            expNames
        );
    }

    /**
     * parse the input using tokens from the scanner.
     * Check for EOF (i.e. no trailing junk) when finished
     *
     * @throws SyntaxException
     */
    void parse() throws SyntaxException
    {
        // System.out.println("parse");
        program();
        matchEOF();
    }

    /**
     * Matches an _program pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _program -> IDENT _programTail
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void program() throws SyntaxException
    {
        // System.out.println("program");
        match(IDENT);
        programTail();
    }

    /**
     * Matches an _programRail pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _programTail -> _block | _paramDec ( COMMA _paramDec )*   _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private void programTail() throws SyntaxException
    {
        switch( tok.kind )
        {
            case LBRACE:
                block();
                break;
            case KW_URL:
            case KW_FILE:
            case KW_INTEGER:
            case KW_BOOLEAN:
                paramDec();
                while( tok.isKind(COMMA) )
                {
                    consume();
                    paramDec();
                }
                block();
                break;
            default:
                throw new SyntaxException(getErrorMessage(
                    tok,
                    LBRACE, KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN
                ));
        }
    }

    /**
     * Matches an _paramDec pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _paramDec -> ( KW_URL | KW_FILE | KW_INTEGER | KW_BOOLEAN ) IDENT
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void paramDec() throws SyntaxException
    {
        // System.out.println("paramDec");
        match(KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN);
        match(IDENT);
    }

    /**
     * Matches an _block pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _block -> LBRACE ( dec | statement )* RBRACE
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void block() throws SyntaxException
    {
        // System.out.println("block");

        match(LBRACE);

        while( Productions.Dec.firstContains(tok.kind) || Productions.Statement.firstContains(tok.kind) )
        {
            if( Productions.Dec.firstContains(tok.kind) )
            {
                dec();
            }
            else
            {
                statement();
            }
        }
        match(RBRACE);
    }

    /**
     * Matches an _dec pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _dec -> ( KW_INTEGER | KW_BOOLEAN | KW_IMAGE | KW_FRAME ) IDENT
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void dec() throws SyntaxException
    {
        // System.out.println("dec");
        match(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME);
        match(IDENT);
    }

    /**
     * Matches an _statement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _statement      -> OP_SLEEP _expression SEMI | _whileStatement | _ifStatement | _chain SEMI | _assign SEMI
     *      _ifStatement    -> KW_IF LPAREN _expression RPAREN _block
     *      _whileStatement -> KW_WHILE LPAREN _expression RPAREN _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void statement() throws SyntaxException
    {
        // System.out.println("statement");
        switch( tok.kind )
        {
            case OP_SLEEP:
                consume();
                expression();
                match(SEMI);
                break;
            case KW_WHILE:
                whileStatement();
                break;
            case KW_IF:
                ifStatement();
                break;
            case IDENT:
                // This maketh yon grammar LL(2)
                if( scanner.peek().isKind(ASSIGN) )
                {
                    assign();
                }
                else
                {
                    chain();
                }
                match(SEMI);
                break;
            case OP_BLUR:
            case OP_GRAY:
            case OP_CONVOLVE:
            case KW_SHOW:
            case KW_HIDE:
            case KW_MOVE:
            case KW_XLOC:
            case KW_YLOC:
            case OP_WIDTH:
            case OP_HEIGHT:
            case KW_SCALE:
                chain();
                match(SEMI);
                break;
            default:
                throw new SyntaxException(getErrorMessage(
                    tok,
                    OP_SLEEP,
                    KW_WHILE,
                    KW_IF,
                    IDENT, OP_BLUR, OP_GRAY, OP_CONVOLVE, KW_SHOW, KW_HIDE,
                    KW_MOVE, KW_XLOC, KW_YLOC, OP_WIDTH, OP_HEIGHT, KW_SCALE
                ));
        }
    }

    /**
     * Matches an _ifStatement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _ifStatement -> KW_IF LPAREN _expression RPAREN _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private void ifStatement() throws SyntaxException
    {
        // System.out.println("ifStatement");
        match(KW_IF);
        match(LPAREN);
        expression();
        match(RPAREN);
        block();
    }

    /**
     * Matches an _whileStatement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _whileStatement -> KW_WHILE LPAREN _expression RPAREN _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private void whileStatement() throws SyntaxException
    {
        // System.out.println("whileStatement");
        match(KW_WHILE);
        match(LPAREN);
        expression();
        match(RPAREN);
        block();
    }

    /**
     * Matches an _assign pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _assign -> IDENT ASSIGN _expression
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private void assign() throws SyntaxException
    {
        // System.out.println("assign");
        match(IDENT);
        match(ASSIGN);
        expression();
    }

    /**
     * Matches an _chainElem pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _chain    -> _chainElem _arrowOp _chainElem ( _arrowOp  _chainElem )*
     *      _arrowOp  -> ARROW | BARARROW
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void chain() throws SyntaxException
    {
        // System.out.println("chain");
        chainElem();
        do
        {
            consume();
            chainElem();
        } while( Productions.ArrowOp.firstContains(tok.kind) );
    }

    /**
     * Matches an _chainElem pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _chainElem    -> IDENT | _filterOp _arg | _frameOp _arg | _imageOp _arg
     *      _arg         -> EPSILON | LPAREN _expression (   , _expression )* RPAREN
     *      _filterOp    -> OP_BLUR |OP_GRAY | OP_CONVOLVE
     *      _frameOp     -> KW_SHOW | KW_HIDE | KW_MOVE | KW_XLOC | KW_YLOC
     *      _imageOp     -> OP_WIDTH | OP_HEIGHT | KW_SCALE
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void chainElem() throws SyntaxException
    {
        // System.out.println("chainElem");
        switch( tok.kind )
        {
            case IDENT:
                consume();
                break;
            case OP_BLUR:
            case OP_GRAY:
            case OP_CONVOLVE:
                // filterOps
                consume();
                arg();
                break;
            case KW_SHOW:
            case KW_HIDE:
            case KW_MOVE:
            case KW_XLOC:
            case KW_YLOC:
                // frameOps
                consume();
                arg();
                break;
            case OP_WIDTH:
            case OP_HEIGHT:
            case KW_SCALE:
                // imageOps
                consume();
                arg();
                break;
            default:
                throw new SyntaxException(getErrorMessage(
                    tok,
                    IDENT,
                    OP_BLUR, OP_GRAY, OP_CONVOLVE,
                    KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC,
                    OP_WIDTH, OP_HEIGHT, KW_SCALE
                ));
        }
    }

    /**
     * Matches an _arg pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _arg -> EPSILON | LPAREN _expression (   , _expression )* RPAREN
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void arg() throws SyntaxException
    {
        // System.out.println("arg");
        switch( tok.kind )
        {
            case ARROW:
            case BARARROW:
            case SEMI:
                break;
            case LPAREN:
                do
                {
                    consume();
                    expression();
                } while( tok.isKind(COMMA) );
                match(RPAREN);
                break;
            default:
                throw new SyntaxException(getErrorMessage(
                    tok,
                    ARROW, BARARROW, LPAREN
                ));
        }
    }

    /**
     * Matches a _exprression pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _expression  -> _term ( _relOp _term )*
     *      _relOp       -> LT | LE | GT | GE | EQUAL | NOTEQUAL
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void expression() throws SyntaxException
    {
        // System.out.println("expression");
        term();
        while( Productions.RelOp.firstContains(tok.kind) )
        {
            consume();
            term();
        }
    }

    /**
     * Matches a _term pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _term    -> _elem ( _weakOp _elem )*
     *      _weakOp  -> PLUS | MINUS | OR
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void term() throws SyntaxException
    {
        // System.out.println("term");
        elem();
        while( Productions.WeakOp.firstContains(tok.kind) )
        {
            consume();
            elem();
        }
    }

    /**
     * Matches an _elem pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _elem        -> _factor ( _strongOp _factor )*
     *      _strongOp    -> TIMES | DIV | AND | MOD
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void elem() throws SyntaxException
    {
        // System.out.println("elem");
        factor();
        while( Productions.StrongOp.firstContains(tok.kind) )
        {
            consume();
            factor();
        }
    }

    /**
     * Matches a _factor pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _factor -> IDENT | INT_LIT | KW_TRUE | KW_FALSE | KW_SCREENWIDTH | KW_SCREENHEIGHT | ( _expression )
     * </pre>
     * <p>
     *
     * @throws SyntaxException if the token sequence does not match definition
     *                         </p>
     */
    void factor() throws SyntaxException
    {
        // System.out.println("factor");
        if( Productions.Factor.predictContains(tok.kind) )
        {
            if( tok.isKind(LPAREN) )
            {
                consume();
                expression();
                match(RPAREN);
            }
            else
            {
                consume();
            }
        }
        else
        {
            throw new SyntaxException(getErrorMessage(
                tok,
                IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENWIDTH, KW_SCREENHEIGHT, LPAREN
            ));
        }
    }

    /**
     * Checks whether the current token is the EOF token. If not, a
     * SyntaxException is thrown.
     *
     * @return current token from the scanner
     * @throws SyntaxException if the current token is not an EOF token
     */
    private Token matchEOF() throws SyntaxException
    {
        if( tok.isKind(EOF) )
        {
            return tok;
        }
        throw new SyntaxException(
            getErrorMessage(tok, EOF)
        );
    }

    /**
     * Checks if the current token has the given kind. If so, the current token
     * is consumed and returned. If not, a SyntaxException is thrown.
     * <p>
     * Precondition: kind != EOF
     * </p>
     *
     * @param kind
     * @return Next token from the scanner object
     * @throws SyntaxException
     */
    private Token match(Kind kind) throws SyntaxException
    {
        if( tok.isKind(kind) )
        {
            return consume();
        }
        throw new SyntaxException(
            getErrorMessage(tok, kind)
        );
    }

    /**
     * Checks if the current token has one of the given kinds. If so, the
     * current token is consumed and returned. If not, a SyntaxException is
     * thrown.
     * <p>
     * Precondition: for all given kinds, kind != EOF
     * </p>
     *
     * @param kinds list of kinds, matches any one
     * @return The next token from the scanner
     * @throws SyntaxException if none of the tokens match the current token
     */
    private Token match(Kind... kinds) throws SyntaxException
    {
        if( tok.isKind(kinds) )
        {
            return consume();
        }

        throw new SyntaxException(
            getErrorMessage(tok, kinds)
        );
    }

    /**
     * Gets the next token and returns the consumed token.
     * <p>
     * Precondition: t.kind != EOF
     * </p>
     *
     * @return Token next token from the scanner
     * @throws SyntaxException if the token match the current token
     */
    private Token consume() throws SyntaxException
    {
        // System.out.printf("%-15s\t@\t%s\n", tok.kind, tok.getLinePos());
        Token tmp = tok;
        tok = scanner.nextToken();
        return tmp;
    }

}
