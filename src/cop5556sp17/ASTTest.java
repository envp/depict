package cop5556sp17;

import cop5556sp17.AST.*;
import cop5556sp17.Parser.SyntaxException;
import cop5556sp17.Scanner.IllegalCharException;
import cop5556sp17.Scanner.IllegalNumberException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static cop5556sp17.Scanner.Kind.*;
import static org.junit.Assert.assertEquals;

public class ASTTest
{

    static final boolean doPrint = true;

    static void show(Object s)
    {
        if( doPrint )
        {
            System.out.println(s);
        }
    }


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstantExpression() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {"screenwidth", "screenheight"};

        for( String s : input )
        {
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.expression();
            assertEquals(ConstantExpression.class, ast.getClass());
        }
    }

    @Test
    public void testBoolExpression() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {"true", "false"};

        for( String s : input )
        {
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.expression();
            assertEquals(BooleanLitExpression.class, ast.getClass());
        }
    }

    @Test
    public void testIntLitExpression() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {"2578819", "0"};

        for( String s : input )
        {
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.expression();
            assertEquals(IntLitExpression.class, ast.getClass());
        }
    }

    @Test
    public void testIdentExpression() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {"$an_obvious_varname", "$$$$subtle_variables$$$$"};

        for( String s : input )
        {
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.expression();
            assertEquals(IdentExpression.class, ast.getClass());
        }
    }

    @Test
    public void testBinaryExprSimple() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String input = "1+abc";
        Scanner scanner = new Scanner(input);
        scanner.scan();
        Parser parser = new Parser(scanner);
        ASTNode ast = parser.expression();
        assertEquals(BinaryExpression.class, ast.getClass());
        BinaryExpression be = ( BinaryExpression ) ast;
        assertEquals(IntLitExpression.class, be.getE0().getClass());
        assertEquals(IdentExpression.class, be.getE1().getClass());
        assertEquals(PLUS, be.getOp().kind);
    }

    @Test
    public void testBinaryExprLeftAssoc() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String input = "first * __this__ / then + that";
        Scanner scanner = new Scanner(input);
        scanner.scan();
        Parser parser = new Parser(scanner);
        ASTNode ast = parser.expression();
        assertEquals(BinaryExpression.class, ast.getClass());

        // "first * __this__ / then + that"
        BinaryExpression be = ( BinaryExpression ) ast;
        Expression that = be.getE1();
        assertEquals(IdentExpression.class, that.getClass());
        assertEquals(PLUS, be.getOp().kind);

        // "first * __this__ / then"
        be = ( BinaryExpression ) be.getE0();
        Expression then = be.getE1();
        assertEquals(IdentExpression.class, then.getClass());
        assertEquals(DIV, be.getOp().kind);

        // "first * __this__"
        be = ( BinaryExpression ) be.getE0();
        Expression __this__ = be.getE1();
        assertEquals(IdentExpression.class, __this__.getClass());
        assertEquals(TIMES, be.getOp().kind);

        // // "first * __this__"
        Expression first = be.getE0();
        assertEquals(IdentExpression.class, first.getClass());
    }

    @Test
    public void testTuple() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {"", "(1)", "(1,2)", "(1,a,b)"};
        String s;
        for( int i = 0; i < input.length; ++i )
        {
            s = input[i];
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.arg();
            assertEquals(Tuple.class, ast.getClass());
            assertEquals(i, (( Tuple ) ast).getExprList().size());
        }
    }

    @Test
    public void testStatements() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String[] input = {
            "sleep 1+2+3+4+(5/7);",
            "if (i % 2 == 0) { k <- i + j; }",
            "while (i < 40) { k <- i*i + i + 41; }",
            "a -> blur (10) -> convolve (1,0,2) |-> show -> scale (5) -> width;",
            "i <- i + j*k;"
        };
        Class<?>[] klasses = {SleepStatement.class, IfStatement.class, WhileStatement.class, BinaryChain.class, AssignmentStatement.class};
        String s;
        for( int i = 0; i < input.length; ++i )
        {
            s = input[i];
            Scanner sc = new Scanner(s);
            sc.scan();
            Parser p = new Parser(sc);
            ASTNode ast = p.statement();
            assertEquals(klasses[i], ast.getClass());
        }
    }

    @Test
    public void testProgramSimple() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        /*
        "/* SLEEP SORT. BEST SORT EVA. WORST CASE TIME? AS MANY SECONDS AS MAX(DATASET) \n" +
            "SO O(1) FOR SET OF ALL 32 bit INTEGERS. YES AN O(1) SORT(tm*)! ACADEMICS: 0, MEGACORPORATIONS: INT_MAX\n" +
            "\n" +
            "* - Terms and conditions apply, this complexity result assumes arbitrary concurrency*_/
        " +
            */
        String sleepSort =
            "slep_sort \n" +
                "file in,\n" +
                "file out\n" +
                "{\n" +
                "   integer n\n" +
                "   while (in) {n <- in; sleep (n);}\n" +
                "}";

        Scanner sc = new Scanner(sleepSort);
        sc.scan();
        Parser p = new Parser(sc);
        ASTNode ast = p.parse();
        assertEquals(Program.class, ast.getClass());

        Program prog = ( Program ) ast;
        List<ParamDec> pl = prog.getParams();
        assertEquals(2, pl.size());

        Block block = prog.getB();

        List<Dec> dl = block.getDecs();
        assertEquals(1, dl.size());
        assertEquals(KW_INTEGER, dl.get(0).firstToken.kind);

        List<Statement> sl = block.getStatements();
        assertEquals(1, sl.size());
    }

    @Test
    public void testProgramBigger() throws IllegalCharException, IllegalNumberException, SyntaxException
    {
        String collatz = "/* Sample program for running the Collatz conjecture for integers below 4e+6\n" +
            "Also some extra stuff that I came up with on the fly*/" +
            "proc \n" +
            "integer i, boolean b,\n file out, url res_link\n" +
            "{\n" +
            "   frame fr\n" +
            "   image img\n" +
            "   integer count\n" +
            "   count <- 0;\n" +
            "   i <- 10000;\n" +
            "   b <- i != 0;\n" +
            "   while (b)\n" +
            "   {\n" +
            "       if (i % 2 == 0) { i <- i / 2; }\n" +
            "       if (i % 2 == 1) { i <- 3*i + 1;}\n" +
            "       b <- i != 0;\n" +
            "   }" +
            "}";

        Scanner sc = new Scanner(collatz);
        sc.scan();
        Parser p = new Parser(sc);
        ASTNode ast = p.parse();
        assertEquals(Program.class, ast.getClass());

        Program prog = ( Program ) ast;
        List<ParamDec> pl = prog.getParams();
        assertEquals(4, pl.size());

        Block block = prog.getB();

        List<Dec> dl = block.getDecs();
        assertEquals(3, dl.size());

        List<Statement> sl = block.getStatements();
        assertEquals(4, sl.size());
        assertEquals(AssignmentStatement.class, sl.get(0).getClass());
        assertEquals(AssignmentStatement.class, sl.get(1).getClass());
        assertEquals(AssignmentStatement.class, sl.get(2).getClass());
        assertEquals(WhileStatement.class, sl.get(3).getClass());

        WhileStatement w = ( WhileStatement ) sl.get(3);
        assertEquals(3, w.getB().getStatements().size());
        assertEquals(IdentExpression.class, w.getE().getClass());

        sl = w.getB().getStatements();
        assertEquals(IfStatement.class, sl.get(0).getClass());
        assertEquals(IfStatement.class, sl.get(1).getClass());
        assertEquals(AssignmentStatement.class, sl.get(2).getClass());
    }
}