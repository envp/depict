package cop5556sp17;

import cop5556sp17.AST.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cop5556sp17.AST.Type.TypeName;
import static cop5556sp17.AST.Type.getTypeName;
import static cop5556sp17.Scanner.Kind;

public class TypeCheckVisitor implements ASTVisitor
{

    @SuppressWarnings("serial")
    public static class TypeCheckException extends Exception
    {
        TypeCheckException(String message)
        {
            super(message);
        }
    }

    private String getErrorMessage(ASTNode actual, TypeName seen, TypeName... expected)
    {
        String expNames = Arrays.stream(expected).
            map(Enum::toString).collect(Collectors.joining(", "));

        return String.format(
            "For token %s expected types %s, but saw %s",
            actual.firstToken.errorString(),
            expNames,
            seen
        );
    }

    SymbolTable symtab;

    public TypeCheckVisitor()
    {
        this.symtab = new SymbolTable();
    }

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception
    {
        Chain ch = binaryChain.getE0();
        ChainElem chElem = binaryChain.getE1();
        Scanner.Token op = binaryChain.getArrow();

        _visitChain(ch, arg);
        _visitChain(chElem, arg);

        if( ch.getTypeName() == TypeName.URL )
        {
            if( op.isKind(Kind.ARROW) )
            {
                if( chElem.getTypeName() == TypeName.IMAGE )
                {
                    binaryChain.setTypeName(TypeName.IMAGE);
                }
                else
                {
                    throw new TypeCheckException(String.format(
                        "Operator must be Arrow(->) with an IMAGE on it's right. Error at %s",
                        binaryChain.getFirstToken().getLinePos()
                    ));
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expected operator ARROW(->) at %s, found %s",
                    binaryChain.getFirstToken().getLinePos(),
                    op
                ));
            }

        }
        else if( ch.getTypeName() == TypeName.FILE )
        {
            if( op.isKind(Kind.ARROW) )
            {
                if( chElem.getTypeName() == TypeName.IMAGE )
                {
                    binaryChain.setTypeName(TypeName.IMAGE);
                    return null;
                }
                else
                {
                    throw new TypeCheckException(getErrorMessage(
                        chElem,
                        chElem.getTypeName(),
                        TypeName.IMAGE
                    ));
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expected operator ARROW(->) at %s, found %s",
                    binaryChain.getFirstToken().getLinePos(),
                    op
                ));
            }
        }
        else if( ch.getTypeName() == TypeName.FRAME )
        {
            if( op.isKind(Kind.ARROW) )
            {
                if( chElem instanceof FrameOpChain )
                {
                    if( chElem.getFirstToken().isKind(Kind.KW_SHOW, Kind.KW_HIDE, Kind.KW_MOVE) )
                    {
                        binaryChain.setTypeName(TypeName.FRAME);
                        return null;
                    }
                    else if( chElem.getFirstToken().isKind(Kind.KW_XLOC, Kind.KW_YLOC) )
                    {
                        binaryChain.setTypeName(TypeName.INTEGER);
                        return null;
                    }
                    else
                    {
                        throw new TypeCheckException(String.format(
                            "Expected keywords show, hide at %s, saw %s",
                            chElem.getFirstToken().getLinePos(),
                            chElem.getFirstToken().kind
                        ));
                    }
                }
                else
                {
                    throw new TypeCheckException(String.format(
                        "Expected token %s to be a FrameOpChain at %s",
                        chElem.getFirstToken().errorString(),
                        chElem.getFirstToken().getLinePos()
                    ));
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expected operator ARROW(->) at %s, found %s",
                    binaryChain.getFirstToken().getLinePos(),
                    op
                ));
            }

        }
        else if( ch.getTypeName() == TypeName.IMAGE )
        {
            if( op.isKind(Kind.ARROW) )
            {
                // Groups share a similar error message
                if( chElem.getTypeName() == TypeName.FRAME )
                {
                    binaryChain.setTypeName(TypeName.FRAME);
                    return null;
                }
                else if( chElem.getTypeName() == TypeName.FILE )
                {
                    binaryChain.setTypeName(TypeName.NONE);
                    return null;
                }
                // instanceof based checks
                else if( chElem instanceof FilterOpChain )
                {
                    if( chElem.firstToken.isKind(Kind.OP_GRAY, Kind.OP_BLUR, Kind.OP_CONVOLVE) )
                    {
                        binaryChain.setTypeName(TypeName.IMAGE);
                        return null;
                    }
                    else
                    {
                        throw new TypeCheckException(Parser.getErrorMessage(
                            chElem.getFirstToken(),
                            Kind.OP_GRAY, Kind.OP_BLUR, Kind.OP_CONVOLVE
                        ));
                    }
                }
                else if( chElem instanceof ImageOpChain )
                {
                    if( chElem.getFirstToken().isKind(Kind.KW_SCALE) )
                    {
                        binaryChain.setTypeName(TypeName.IMAGE);
                        return null;
                    }
                    if( chElem.getFirstToken().isKind(Kind.OP_WIDTH, Kind.OP_HEIGHT) )
                    {
                        binaryChain.setTypeName(TypeName.INTEGER);
                        return null;
                    }
                    else
                    {
                        throw new TypeCheckException(Parser.getErrorMessage(
                            chElem.getFirstToken(),
                            Kind.KW_SCALE
                        ));
                    }
                }
                // Modified in assignment 6
                else if( chElem instanceof IdentChain && chElem.getTypeName() == TypeName.IMAGE )
                {
                    binaryChain.setTypeName(TypeName.IMAGE);
                    return null;
                }
                else
                {
                    throw new TypeCheckException(String.format(
                        "Type error in %s at %s",
                        chElem.getFirstToken().errorString(),
                        chElem.getFirstToken().getLinePos()
                    ));
                }
            }
            else if( op.isKind(Kind.BARARROW) )
            {
                if( chElem instanceof FilterOpChain )
                {
                    if( chElem.firstToken.isKind(Kind.OP_GRAY, Kind.OP_BLUR, Kind.OP_CONVOLVE) )
                    {
                        binaryChain.setTypeName(TypeName.IMAGE);
                        return null;
                    }
                    else
                    {
                        throw new TypeCheckException(Parser.getErrorMessage(
                            chElem.getFirstToken(),
                            Kind.OP_GRAY, Kind.OP_BLUR, Kind.OP_CONVOLVE
                        ));
                    }
                }
                else
                {
                    throw new TypeCheckException(String.format(
                        "Expected token %s to be a FilterOpChain at %s",
                        chElem.getFirstToken().errorString(),
                        chElem.getFirstToken().getLinePos()
                    ));
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expected operator ARROW(->) or BARARROW(|->) at %s, found %s",
                    binaryChain.getFirstToken().getLinePos(),
                    op
                ));
            }
        }
        // Added in assignment 6
        else if( ch.getTypeName() == TypeName.INTEGER )
        {
            if( op.isKind(Kind.ARROW) )
            {
                if( chElem instanceof IdentChain && chElem.getTypeName() == TypeName.INTEGER )
                {
                    binaryChain.setTypeName(TypeName.INTEGER);
                    return null;
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expected operator ARROW(->) at %s, found %s",
                    binaryChain.getFirstToken().getLinePos(),
                    op
                ));
            }
        }
        else
        {
            throw new TypeCheckException(String.format(
                "Expected %s to be of type %s but found %s",
                ch.getFirstToken().errorString(),
                "URL, FILE, IMAGE, FRAME",
                ch.getTypeName()
            ));
        }

        return null;
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception
    {
        Expression e0 = binaryExpression.getE0();
        Expression e1 = binaryExpression.getE1();
        Scanner.Token op = binaryExpression.getOp();

        _visitExpression(e0, arg);
        _visitExpression(e1, arg);

        if( op.isKind(Kind.EQUAL, Kind.NOTEQUAL) )
        {
            if( e0.getType() == e1.getType() )
            {
                binaryExpression.setTypeName(TypeName.BOOLEAN);
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "Expressions at %s cannot be compared (Incompatible types)",
                    binaryExpression.getFirstToken().getLinePos()
                ));
            }
        }
        else
        {
            if( e0.getType() == TypeName.INTEGER && e1.getType() == TypeName.INTEGER )
            {
                if( op.isKind(Kind.PLUS, Kind.MINUS, Kind.DIV, Kind.TIMES, Kind.MOD) )
                {
                    binaryExpression.setTypeName(TypeName.INTEGER);
                }
                else if( op.isKind(Kind.LT, Kind.GT, Kind.LE, Kind.GE) )
                {
                    binaryExpression.setTypeName(TypeName.BOOLEAN);
                }
                else
                {
                    throw new TypeCheckException(Parser.getErrorMessage(
                        op,
                        Kind.PLUS, Kind.MINUS, Kind.DIV, Kind.TIMES, Kind.MOD,
                        Kind.LT, Kind.GT, Kind.LE, Kind.GE
                    ));
                }
            }
            else if( e0.getType() == TypeName.IMAGE && e1.getType() == TypeName.INTEGER )
            {
                if( op.isKind(Kind.TIMES, Kind.DIV, Kind.MOD) )
                {
                    binaryExpression.setTypeName(TypeName.IMAGE);
                }
                else
                {
                    throw new TypeCheckException(Parser.getErrorMessage(
                        op,
                        Kind.DIV, Kind.TIMES, Kind.MOD
                    ));
                }
            }
            else if( e0.getType() == TypeName.INTEGER && e1.getType() == TypeName.IMAGE )
            {
                // Only commutative operations
                if( op.isKind(Kind.TIMES) )
                {
                    binaryExpression.setTypeName(TypeName.IMAGE);
                }
                else
                {
                    throw new TypeCheckException(Parser.getErrorMessage(
                        op,
                        Kind.TIMES
                    ));
                }
            }
            else if( e0.getType() == TypeName.IMAGE && e1.getType() == TypeName.IMAGE )
            {
                if( op.isKind(Kind.PLUS, Kind.MINUS) )
                {
                    binaryExpression.setTypeName(TypeName.IMAGE);
                }
                else
                {
                    throw new TypeCheckException(Parser.getErrorMessage(
                        op,
                        Kind.PLUS, Kind.MINUS
                    ));
                }
            }
            else if( e0.getType() == TypeName.BOOLEAN && e1.getType() == TypeName.BOOLEAN )
            {
                if( op.isKind(Kind.LT, Kind.GT, Kind.LE, Kind.GE, Kind.AND, Kind.OR) )
                {
                    binaryExpression.setTypeName(TypeName.BOOLEAN);
                }
                else
                {
                    throw new TypeCheckException(Parser.getErrorMessage(
                        op,
                        Kind.LT, Kind.GT, Kind.LE, Kind.GE, Kind.AND, Kind.OR
                    ));
                }
            }
            else
            {
                throw new TypeCheckException(String.format(
                    "%s: Type pairs (%s, %s) cannot be compared in %s",
                    binaryExpression.getFirstToken().getLinePos(),
                    e0.getType(), e1.getType(),
                    op.errorString()
                ));
            }
        }
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws Exception
    {
        symtab.enterScope();
        for( Dec dec : block.getDecs() )
        {
            visitDec(dec, arg);
        }

        for( Statement stmt : block.getStatements() )
        {
            _visitStatement(stmt, arg);
        }
        symtab.leaveScope();
        return null;
    }

    @Override
    public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception
    {
        booleanLitExpression.setTypeName(TypeName.BOOLEAN);
        return null;
    }

    @Override
    public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception
    {
        int argc = filterOpChain.getArg().getExprList().size();

        if( argc > 0 )
        {
            throw new TypeCheckException(String.format(
                "filter takes no arguments, found %d at %s",
                argc,
                filterOpChain.getFirstToken().getLinePos()
            ));
        }

        filterOpChain.setTypeName(TypeName.IMAGE);

        visitTuple(filterOpChain.getArg(), arg);

        return null;
    }

    @Override
    public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception
    {
        int argc = frameOpChain.getArg().getExprList().size();

        switch( frameOpChain.firstToken.kind )
        {
            case KW_SHOW:
            case KW_HIDE:
                if( argc != 0 )
                {
                    throw new TypeCheckException(String.format(
                        "keywords show, hide take no arguments, found %d at %s",
                        argc,
                        frameOpChain.getFirstToken().getLinePos()
                    ));
                }
                frameOpChain.setTypeName(TypeName.NONE);
                break;
            case KW_XLOC:
            case KW_YLOC:
                if( argc != 0 )
                {
                    throw new TypeCheckException(String.format(
                        "keywords xloc, yloc take no arguments, found %d at %s",
                        argc,
                        frameOpChain.getFirstToken().getLinePos()
                    ));
                }
                frameOpChain.setTypeName(TypeName.INTEGER);
                break;
            case KW_MOVE:
                if( argc != 2 )
                {
                    throw new TypeCheckException(String.format(
                        "keywords xloc, yloc take 2 arguments, found %d at %s",
                        argc,
                        frameOpChain.getFirstToken().getLinePos()
                    ));
                }

                frameOpChain.setTypeName(TypeName.NONE);
                break;
            default:
                // Yay, parser bugs!
                throw new Parser.SyntaxException(String.format(
                    "Parser admitted token of type %s as FrameOp token!",
                    frameOpChain.firstToken.kind
                ));
        }

        visitTuple(frameOpChain.getArg(), arg);

        return null;
    }

    @Override
    public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception
    {
        Dec dec = symtab.lookup(identChain.firstToken.getText());

        // Identifier used before declaration
        if( null == dec )
        {
            throw new TypeCheckException(String.format(
                "Identifier '%s' used before declaration at %s",
                identChain.firstToken,
                identChain.firstToken.getLinePos()
            ));
        }

        identChain.setTypeName(dec.getTypeName());
        identChain.setDec(dec);
        return null;
    }

    @Override
    public Object
    visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception
    {
        Dec dec = symtab.lookup(identExpression.getFirstToken().getText());

        // Identifier used before declaration
        if( null == dec )
        {
            throw new TypeCheckException(String.format(
                "Identifier %s used before declaration at %s",
                identExpression.getFirstToken().errorString(),
                identExpression.getFirstToken().getLinePos()
            ));
        }

        identExpression.setDec(dec);
        identExpression.setTypeName(dec.getTypeName());
        identExpression.setDec(dec);
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception
    {
        Expression e = ifStatement.getE();

        _visitExpression(e, arg);
        visitBlock(ifStatement.getB(), arg);

        if( e.getType() != TypeName.BOOLEAN )
        {
            throw new TypeCheckException(getErrorMessage(
                ifStatement,
                e.getType(),
                TypeName.BOOLEAN
            ));
        }
        return null;
    }

    @Override
    public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception
    {
        intLitExpression.setTypeName(TypeName.INTEGER);
        return null;
    }

    @Override
    public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception
    {
        Expression e = sleepStatement.getE();

        _visitExpression(e, arg);

        if( e.getType() != TypeName.INTEGER )
        {
            throw new TypeCheckException(getErrorMessage(e, e.getType(), TypeName.INTEGER));
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception
    {
        Expression e = whileStatement.getE();
        _visitExpression(e, arg);
        visitBlock(whileStatement.getB(), arg);

        if( e.getType() != TypeName.BOOLEAN )
        {
            throw new TypeCheckException(getErrorMessage(
                whileStatement,
                e.getType(),
                TypeName.BOOLEAN
            ));
        }

        return null;
    }

    @Override
    public Object visitDec(Dec declaration, Object arg) throws Exception
    {
        // Add typer information to declaration before insertion into table
        boolean unique = symtab.insert(declaration.getIdent().getText(), declaration);

        if( !unique )
        {
            throw new TypeCheckException(String.format(
                "Identifier %s redeclared at %s",
                declaration.getIdent().getText(),
                declaration.getIdent().getLinePos()
            ));
        }

        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception
    {
        for( ParamDec param : program.getParams() )
        {
            visitParamDec(param, arg);
        }
        visitBlock(program.getB(), arg);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception
    {
        IdentLValue var = assignStatement.getVar();
        Expression e = assignStatement.getE();

        // Get the declaration for the identifier being referenced
        Dec dec = symtab.lookup(var.getText());

        // Used before declaration
        if( null == dec )
        {
            throw new TypeCheckException(String.format(
                "Identifier '%s' used before declaration at %s",
                var.getText(),
                var.firstToken.getLinePos()
            ));
        }

        visitIdentLValue(var, arg);
        _visitExpression(e, arg);

        TypeName decType = getTypeName(dec.getType());

        if( decType != e.getType() )
        {
            throw new TypeCheckException(getErrorMessage(
                assignStatement,
                e.getType(),
                decType
            ));
        }

        return null;
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception
    {
        Dec dec = symtab.lookup(identX.getText());

        // Used before declaration
        if( null == dec )
        {
            throw new TypeCheckException(String.format(
                "Identifier '%s' used before declaration at %s",
                identX.getText(),
                identX.firstToken.getLinePos()
            ));
        }

        identX.setDec(dec);
        identX.setTypeName(dec.getTypeName());

        return null;
    }

    @Override
    public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception
    {
        symtab.insert(paramDec.getIdent().getText(), paramDec);

        return null;
    }

    @Override
    public Object visitConstantExpression(ConstantExpression constantExpression, Object arg)
    {
        constantExpression.setTypeName(TypeName.INTEGER);
        return null;
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception
    {
        int argc = imageOpChain.getArg().getExprList().size();
        switch( imageOpChain.getFirstToken().kind )
        {
            case OP_WIDTH:
            case OP_HEIGHT:
                if( argc != 0 )
                {
                    throw new TypeCheckException(String.format(
                        "keywords width, height take no arguments, found %d at %s",
                        argc,
                        imageOpChain.getFirstToken().getLinePos()
                    ));
                }
                imageOpChain.setTypeName(TypeName.INTEGER);
                break;
            case KW_SCALE:
                if( argc != 1 )
                {
                    throw new TypeCheckException(String.format(
                        "keyword scale takes 1 argument, found %d at %s",
                        argc,
                        imageOpChain.getFirstToken().getLinePos()
                    ));
                }
                imageOpChain.setTypeName(TypeName.IMAGE);
                break;
            default:
                // More parser bugs
                // The gift that keeps on giving!
                throw new Parser.SyntaxException(String.format(
                    "Parser admitted token of kind %s as ImageOp token",
                    imageOpChain.getFirstToken().kind
                ));
        }

        visitTuple(imageOpChain.getArg(), arg);

        return null;
    }

    @Override
    public Object visitTuple(Tuple tuple, Object arg) throws Exception
    {
        List<Expression> es = tuple.getExprList();

        // Empty list, nothing to do
        if( es.size() == 0 )
        {
            return null;
        }

        for( Expression e : es )
        {
            _visitExpression(e, arg);
        }

        List<TypeName> types = es.stream().map(Expression::getType).distinct().collect(Collectors.toList());

        if( types.size() == 1 )
        {
            if( types.get(0) != TypeName.INTEGER )
            {
                throw new TypeCheckException(String.format(
                    "Saw %s at %s, expected type %s",
                    tuple.getFirstToken().errorString(),
                    tuple.getFirstToken().getLinePos(),
                    TypeName.INTEGER
                ));
            }
        }
        else
        {
            throw new TypeCheckException(String.format(
                "Illegal tuple (heterogeneous) at %s",
                tuple.getFirstToken().getLinePos()
            ));
        }
        return null;
    }

    private Object _visitExpression(Expression e, Object arg) throws Exception
    {
        if( e instanceof IdentExpression )
        {
            visitIdentExpression(( IdentExpression ) e, arg);
        }
        else if( e instanceof IntLitExpression )
        {
            visitIntLitExpression(( IntLitExpression ) e, arg);
        }
        else if( e instanceof BooleanLitExpression )
        {
            visitBooleanLitExpression(( BooleanLitExpression ) e, arg);
        }
        else if( e instanceof ConstantExpression )
        {
            visitConstantExpression(( ConstantExpression ) e, arg);
        }
        else if( e instanceof BinaryExpression )
        {
            visitBinaryExpression(( BinaryExpression ) e, arg);
        }
        return null;
    }

    private Object _visitStatement(Statement stmt, Object arg) throws Exception
    {
        if( stmt instanceof WhileStatement )
        {
            visitWhileStatement(( WhileStatement ) stmt, arg);
        }
        else if( stmt instanceof IfStatement )
        {
            visitIfStatement(( IfStatement ) stmt, arg);
        }
        else if( stmt instanceof SleepStatement )
        {
            visitSleepStatement(( SleepStatement ) stmt, arg);
        }
        else if( stmt instanceof AssignmentStatement )
        {
            visitAssignmentStatement(( AssignmentStatement ) stmt, arg);
        }
        else if( stmt instanceof Chain )
        {
            _visitChain(( Chain ) stmt, arg);
        }

        return null;
    }

    private Object _visitChain(Chain chn, Object arg) throws Exception
    {
        if( chn instanceof BinaryChain )
        {
            visitBinaryChain(( BinaryChain ) chn, arg);
        }
        else if( chn instanceof FilterOpChain )
        {
            visitFilterOpChain(( FilterOpChain ) chn, arg);
        }
        else if( chn instanceof FrameOpChain )
        {
            visitFrameOpChain(( FrameOpChain ) chn, arg);
        }
        else if( chn instanceof ImageOpChain )
        {
            visitImageOpChain(( ImageOpChain ) chn, arg);
        }
        else if( chn instanceof IdentChain )
        {
            visitIdentChain(( IdentChain ) chn, arg);
        }
        return null;
    }
}
