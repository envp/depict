package cop5556sp17;

import cop5556sp17.AST.*;
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
        StrongOp(false),
        WeakOp(false),
        RelOp(false),
        Factor(false),
        Elem(false),
        Term(false),
        Expression(false),
        Arg(true),
        ImageOp(false),
        FrameOp(false),
        FilterOp(false),
        ChainElem(false),
        ArrowOp(false),
        IfStatement(false),
        WhileStatement(false),
        Chain(false),
        Assign(false),
        Statement(false),
        Dec(false),
        Block(false),
        ParamDec(false),
        ProgramTail(false),
        Program(false);
        //</editor-fold>

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

        Productions(boolean epsilon)
        {
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

    public static String getErrorMessage(Token actual, Kind... expected)
    {
        String expNames = Arrays.stream(expected).
            map(k -> String.format("%s(%s)", k.toString(), k.getText())).
                                    collect(Collectors.joining(", "));
        return String.format(
            "Saw token %s at %s, but expecting one of (%s)",
            actual.errorString(),
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
    ASTNode parse() throws SyntaxException
    {
        // System.out.println("parse");
        Program _p = program();
        matchEOF();
        return _p;
    }

    /**
     * Matches an _program pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _program -> IDENT _programTail
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Program program() throws SyntaxException
    {
        // System.out.println("program");
        Token first = tok;
        match(IDENT);
        return _programTail(first);
    }

    /**
     * Matches an _programRail pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _programTail -> _block | _paramDec ( COMMA _paramDec )*   _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private Program _programTail(Token firstToken) throws SyntaxException
    {
        ArrayList<ParamDec> paramList = new ArrayList<>();
        ParamDec param;
        Block blk = null;
        if( !Productions.ProgramTail.predictContains(tok.kind) )
        {
            throw new SyntaxException(getErrorMessage(
                tok,
                LBRACE, KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN
            ));
        }

        if( Productions.Block.predictContains(tok.kind) )
        {
            blk = block();
        }
        else if( Productions.ParamDec.predictContains(tok.kind) )
        {
            param = paramDec();
            paramList.add(param);
            while( tok.isKind(COMMA) )
            {
                consume();
                param = paramDec();
                paramList.add(param);
            }
            blk = block();
        }
        return new Program(firstToken, paramList, blk);
    }

    /**
     * Matches an _paramDec pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _paramDec -> ( KW_URL | KW_FILE | KW_INTEGER | KW_BOOLEAN ) IDENT
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    ParamDec paramDec() throws SyntaxException
    {
        // System.out.println("paramDec");
        Token first = tok;
        match(KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN);
        Token id = tok;
        match(IDENT);
        return new ParamDec(first, id);
    }

    /**
     * Matches an _block pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _block -> LBRACE ( dec | statement )* RBRACE
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Block block() throws SyntaxException
    {
        // System.out.println("block");
        Token first = tok;
        match(LBRACE);

        ArrayList<Dec> decList = new ArrayList<>();
        ArrayList<Statement> stmtList = new ArrayList<>();
        Dec d;
        Statement s;

        while( Productions.Dec.firstContains(tok.kind) || Productions.Statement.firstContains(tok.kind) )
        {
            if( Productions.Dec.firstContains(tok.kind) )
            {
                d = dec();
                decList.add(d);
            }
            else
            {
                s = statement();
                stmtList.add(s);
            }
        }
        match(RBRACE);
        return new Block(first, decList, stmtList);
    }

    /**
     * Matches an _dec pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _dec -> ( KW_INTEGER | KW_BOOLEAN | KW_IMAGE | KW_FRAME ) IDENT
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Dec dec() throws SyntaxException
    {
        // System.out.println("dec");
        Token first = tok;
        match(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME);
        Token id = tok;
        match(IDENT);
        return new Dec(first, id);
    }

    /**
     * Matches an _statement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _statement      -> OP_SLEEP _expression SEMI | _whileStatement | _ifStatement | _chain SEMI | _assign SEMI
     *      _ifStatement    -> KW_IF LPAREN _expression RPAREN _block
     *      _whileStatement -> KW_WHILE LPAREN _expression RPAREN _block
     *      _assign         -> IDENT ASSIGN _expression
     *      _chain          -> _chainElem _arrowOp _chainElem ( _arrowOp  _chainElem )*
     *      _arrowOp        -> ARROW | BARARROW
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Statement statement() throws SyntaxException
    {
        // System.out.println("statement");
        Statement s = null;
        Token first = tok;

        if( !Productions.Statement.predictContains(tok.kind) )
        {
            throw new SyntaxException(getErrorMessage(
                tok,
                OP_SLEEP,
                KW_WHILE,
                KW_IF,
                IDENT, OP_BLUR, OP_GRAY, OP_CONVOLVE, KW_SHOW, KW_HIDE,
                KW_MOVE, KW_XLOC, KW_YLOC, OP_WIDTH, OP_HEIGHT, KW_SCALE
            ));
        }

        if( tok.isKind(OP_SLEEP) )
        {
            consume();
            Expression e = expression();
            s = new SleepStatement(first, e);
            match(SEMI);
        }
        else if( Productions.WhileStatement.predictContains(tok.kind) )
        {
            s = _whileStatement();
        }
        else if( Productions.IfStatement.predictContains(tok.kind) )
        {
            s = _ifStatement();
        }
        else if( tok.isKind(IDENT) )
        {
            // This maketh yon grammar LL(2)
            if( scanner.peek().isKind(ASSIGN) )
            {
                s = _assign();
            }
            else
            {
                s = chain();
            }
            match(SEMI);
        }
        else if( Productions.Chain.predictContains(tok.kind) )
        {
            s = chain();
            match(SEMI);
        }

        return s;

    }

    /**
     * Matches an _ifStatement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _ifStatement -> KW_IF LPAREN _expression RPAREN _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private Statement _ifStatement() throws SyntaxException
    {
        // System.out.println("_ifStatement");
        Token first = tok;
        match(KW_IF);
        match(LPAREN);
        Expression expr = expression();
        match(RPAREN);
        Block blk = block();
        return new IfStatement(first, expr, blk);
    }

    /**
     * Matches an _whileStatement pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _whileStatement -> KW_WHILE LPAREN _expression RPAREN _block
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private Statement _whileStatement() throws SyntaxException
    {
        // System.out.println("_whileStatement");
        Token first = tok;
        match(KW_WHILE);
        match(LPAREN);
        Expression expr = expression();
        match(RPAREN);
        Block blk = block();
        return new WhileStatement(first, expr, blk);
    }

    /**
     * Matches an _assign pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _assign -> IDENT ASSIGN _expression
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private Statement _assign() throws SyntaxException
    {
        // System.out.println("assign");
        Token first = tok;
        IdentLValue var = new IdentLValue(tok);
        match(IDENT);
        match(ASSIGN);
        Expression e = expression();
        return new AssignmentStatement(first, var, e);
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
    Chain chain() throws SyntaxException
    {
        // System.out.println("chain");
        Chain c = null;
        Token first = tok;
        ChainElem left = chainElem();
        Token arrow = tok;
        _arrowOp();
        ChainElem right = chainElem();
        c = new BinaryChain(first, left, arrow, right);
        while( Productions.ArrowOp.firstContains(tok.kind) )
        {
            arrow = tok;
            consume();
            right = chainElem();
            c = new BinaryChain(first, c, arrow, right);
        }
        return c;
    }

    /**
     * Matches a _arrowOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _arrowOp -> ARROW | BARARROW
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private void _arrowOp() throws SyntaxException
    {
        match(ARROW, BARARROW);
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
    ChainElem chainElem() throws SyntaxException
    {
        // System.out.println("chainElem");
        Token first = null;
        ChainElem c = null;
        if( !Productions.ChainElem.predictContains(tok.kind) )
        {
            throw new SyntaxException(getErrorMessage(
                tok,
                IDENT,
                OP_BLUR, OP_GRAY, OP_CONVOLVE,
                KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC,
                OP_WIDTH, OP_HEIGHT, KW_SCALE
            ));
        }

        if( tok.isKind(IDENT) )
        {
            first = tok;
            consume();
            c = new IdentChain(first);
        }
        else if( Productions.FilterOp.predictContains(tok.kind) )
        {
            c = _filterOp();
        }
        else if( Productions.FrameOp.predictContains(tok.kind) )
        {
            c = _frameOp();
        }
        else if( Productions.ImageOp.predictContains(tok.kind) )
        {
            c = _imageOp();
        }
        return c;
    }

    /**
     * Matches a _filterOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _filterOp -> OP_BLUR | OP_GRAY | OP_CONVOLVE
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private ChainElem _filterOp() throws SyntaxException
    {
        Token first = tok;
        match(OP_BLUR, OP_GRAY, OP_CONVOLVE);
        Tuple argList = arg();
        return new FilterOpChain(first, argList);
    }

    /**
     * Matches a _sframeOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _frameOp -> KW_SHOW | KW_HIDE | KW_MOVE | KW_XLOC | KW_YLOC
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private ChainElem _frameOp() throws SyntaxException
    {
        Token first = tok;
        match(KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC);
        Tuple argList = arg();
        return new FrameOpChain(first, argList);
    }

    /**
     * Matches a _imageOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _imageOp -> OP_WIDTH | OP_HEIGHT | KW_SCALE
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    private ImageOpChain _imageOp() throws SyntaxException
    {
        Token first = tok;
        match(OP_WIDTH, OP_HEIGHT, KW_SCALE);
        Tuple argList = arg();
        return new ImageOpChain(first, argList);
    }

    /**
     * Matches an _arg pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _arg -> EPSILON | LPAREN _expression (   , _expression )* RPAREN
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Tuple arg() throws SyntaxException
    {
        // System.out.println("arg");
        Tuple t = null;
        Token first = null;
        ArrayList<Expression> args = new ArrayList<>();
        if( tok.isKind(LPAREN) )
        {
            first = scanner.peek();
            do
            {
                consume();
                args.add(expression());
            } while( tok.isKind(COMMA) );
            match(RPAREN);
        }
        /*
         * The alternative is one of ARROW, BARARROW, SEMI which we do not need to consume,
         * and will be ensured by the calling method (esp. matching SEMIs)
         */
        return new Tuple(first, args);
    }

    /**
     * Matches a _expression pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _expression  -> _term ( _relOp _term )*
     *      _relOp       -> LT | LE | GT | GE | EQUAL | NOTEQUAL
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Expression expression() throws SyntaxException
    {
        // System.out.println("expression");
        Token first = tok;
        Expression left = term();
        Expression right = null;
        Token op = null;
        while( Productions.RelOp.firstContains(tok.kind) )
        {
            op = tok;
            consume();
            right = term();
            left = new BinaryExpression(first, left, op, right);
        }
        return left;
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
    Expression term() throws SyntaxException
    {
        // System.out.println("term");
        Token first = tok;
        Expression left = elem();
        Expression right = null;
        Token op = null;
        while( Productions.WeakOp.firstContains(tok.kind) )
        {
            op = tok;
            consume();
            right = elem();
            left = new BinaryExpression(first, left, op, right);
        }
        return left;
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
    Expression elem() throws SyntaxException
    {
        // System.out.println("elem");
        Token first = tok;
        Expression left = factor();
        Expression right = null;
        Token op = null;

        while( Productions.StrongOp.firstContains(tok.kind) )
        {
            op = tok;
            consume();
            right = factor();
            left = new BinaryExpression(first, left, op, right);
        }
        return left;
    }

    /**
     * Matches a _relOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _factor -> IDENT | INT_LIT | KW_TRUE | KW_FALSE | KW_SCREENWIDTH | KW_SCREENHEIGHT | LPAREN _expression RPAREN
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    Expression factor() throws SyntaxException
    {
        // System.out.println("factor");
        Token first = tok;
        Expression e = null;
        if( Productions.Factor.predictContains(tok.kind) )
        {
            if( tok.isKind(LPAREN) )
            {
                consume();
                e = expression();
                match(RPAREN);
            }
            else
            {
                first = tok;
                switch( tok.kind )
                {
                    case IDENT:
                        e = new IdentExpression(first);
                        break;
                    case INT_LIT:
                        e = new IntLitExpression(first);
                        break;
                    case KW_TRUE:
                    case KW_FALSE:
                        e = new BooleanLitExpression(first);
                        break;
                    case KW_SCREENHEIGHT:
                    case KW_SCREENWIDTH:
                        e = new ConstantExpression(first);
                        break;
                    default:
                        throw new SyntaxException(getErrorMessage(
                            tok,
                            IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENHEIGHT, KW_SCREENWIDTH
                        ));
                }
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

        return e;
    }

    /**
     * Matches a _relOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _relOp -> LT | LE | GT | GE | EQUAL | NOTEQUAL
     * </pre>
     * <p>
     *
     * @throws SyntaxException if the token sequence does not match definition
     *                         </p>
     */
    void relOp() throws SyntaxException
    {
        // System.out.println("factor");
        match(LT, LE, GT, GE, EQUAL, NOTEQUAL);
    }

    /**
     * Matches a _weakOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _weakOp -> PLUS | MINUS | OR
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void weakOp() throws SyntaxException
    {
        match(PLUS, MINUS, OR);
    }

    /**
     * Matches a _strongOp pattern given a sequence of tokens, non terminals denoted with a '_' prefix
     * <pre>
     *      _strongOp -> TIMES | DIV | AND | MOD
     * </pre>
     *
     * @throws SyntaxException if the token sequence does not match definition
     */
    void strongOp() throws SyntaxException
    {
        match(TIMES, DIV, AND, MOD);
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

    public Token matchEOFForTest() throws SyntaxException
    {
        return matchEOF();
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
