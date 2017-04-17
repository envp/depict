/**
 * Important to test the error cases in case the
 * AST is not being completely traversed.
 * <p>
 * Only need to test syntactically correct programs, or
 * program fragments.
 */

package cop5556sp17;

import cop5556sp17.AST.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static cop5556sp17.AST.Type.TypeName.BOOLEAN;
import static cop5556sp17.AST.Type.TypeName.INTEGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeCheckVisitorTest
{
    private class TestRunner
    {
        String input;
        Object arg;

        TestRunner(String input, Object arg)
        {
            this.input = input;
            this.arg = arg;
        }

        public void test() throws Exception
        {
            Scanner scanner = new Scanner(this.input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, this.arg);

            // Release references
            scanner = null;
            parser = null;
            program = null;
            v = null;
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssignmentBoolLit() throws Exception
    {
        String[] inputs = {
            "p {\nboolean y \ny <- false;}"
        };

        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentBoolLitError() throws Exception
    {
        String[] inputs = {
            "p {\nboolean y \ny <- 3;}"
        };

        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testMultipleUniqueDeclarations() throws Exception
    {
        String[] inputs = {
            "p {integer x \n integer y \n boolean b \n image i}"
        };
        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentCorrect() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- 121; boolean b \n b <- false;}"
        };
        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentWithoutDeclarationError() throws Exception
    {
        /**
         * None of these test assignment *before* declaration since the
         * specification doesn't care about the order of assignment and
         * declaration in a scope block.
         */
        String[] inputs = {
            "p {x <- 121;}",
            "p {x <- true; \n boolean b \n b <- false;}"
        };
        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testSingleScopeRedeclarationError() throws Exception
    {
        String[] inputs = {
            "p {integer x\n integer y \n integer x}",
            "p {integer x\n integer x}",
            "p boolean x {integer x\n x <- 10; \n integer x}",
            "p image x {boolean x\n x <- true; \n integer x}",
            "abc integer x, integer x {}"
        };
        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testMultiScopeRedeclaration() throws Exception
    {
        String[] inputs = {
            "p integer x {integer x}",
            "p integer x { image x \n }"
        };
        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testSleepStatement() throws Exception
    {
        String[] inputs = {
            "p integer x {integer x}",
            "p integer x { image x \n }"
        };

        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testSleepStatementTypeError() throws Exception
    {
        String[] inputs = {
            "p { sleep true; }",
            "p { image im \n sleep im; }",
            "p { frame fr \n sleep fr; }"
        };

        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentStatement() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- 1; }",
            "p integer x { x <- 1; \n x <- 2;\n integer y y <- 15;}",
            "p boolean x { x <- true;\n  boolean y y <- true;}",
        };

        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentStatementTypeError() throws Exception
    {
        String[] inputs = {
            "p boolean x { x <- 1; }",
            "p integer x { x <- 1; \n x <- 2;\n image y y <- 15;}",
            "p file f { f <- 1;\n  image y y <- true;}"
        };

        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testAssignmentStatementError() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- 1; }",
            "p integer x { x <- 1; \n x <- true;\n integer y y <- 15;}",
            "p boolean x { x <- true;\n  boolean y y <- true;}",
        };

        for( String input : inputs )
        {
            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testIfStatement() throws Exception
    {
        String[] inputs = {
            "p \n" +
                "boolean x {" +
                "   x <- true; " +
                // The x in if(x) is from the outer scope
                // The x inside is from the inner scope
                "   if(x) { " +
                "       boolean x \n" +
                "       x <- false;" +
                "   }" +
                "}",
            "p \n" +
                "integer x,\n" +
                "integer y {\n" +
                "   x <- 1; y <-2;\n" +
                "   if(x < y) {\n" +
                "       y <- 0;\n" +
                "   }\n" +
                "   if(x == y) {\n" +
                "       y <- 1;" +
                "   }" +
                "}"
        };

        for( String input : inputs )
        {
            (new TestRunner(input, null)).test();
        }
    }

    @Test
    public void testIntLitExpression() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- 1; }",
            "p integer x { x <- 6; \n x <- 2;\n integer y y <- 15;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, null);

            Program p = ( Program ) program;

            List<Statement> ss = p.getB().getStatements();

            for( Statement s : ss )
            {
                if( s instanceof AssignmentStatement )
                {
                    assertEquals(INTEGER, (( AssignmentStatement ) s).getE().getType());
                }
            }
        }
    }

    @Test
    public void testIntLitExpressionError() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- true; }",
            "p integer x { x <- 1; \n x <- 2;\n integer y y <- false;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            program.visit(v, null);
        }
    }

    @Test
    public void testBooleanLitExpression() throws Exception
    {
        String[] inputs = {
            "p boolean x { x <- true; }",
            "p boolean x { x <- false; \n x <- true;\n boolean y y <- false;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, null);

            Program p = ( Program ) program;

            List<Statement> ss = p.getB().getStatements();

            for( Statement s : ss )
            {
                if( s instanceof AssignmentStatement )
                {
                    assertEquals(BOOLEAN, (( AssignmentStatement ) s).getE().getType());
                }
            }
        }
    }

    @Test
    public void testBooleanLitExpressionError() throws Exception
    {
        String[] inputs = {
            "p boolean x { x <- 1; }",
            "p boolean x { x <- 1; \n x <- 2;\n integer y y <- false;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            program.visit(v, null);
        }
    }

    @Test
    public void testConstantExpression() throws Exception
    {
        String[] inputs = {
            "p integer x { x <- screenwidth; }",
            "p integer x { x <- screenheight; \n x <- screenwidth;\n integer y y <- screenwidth;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, null);

            Program p = ( Program ) program;

            List<Statement> ss = p.getB().getStatements();

            for( Statement s : ss )
            {
                if( s instanceof AssignmentStatement )
                {
                    assertEquals(INTEGER, (( AssignmentStatement ) s).getE().getType());
                }
            }
        }
    }

    @Test
    public void testConstantExpressionError() throws Exception
    {
        String[] inputs = {
            "p boolean x { x <- screenwidth; }",
            "p boolean x { x <- screenheight; \n x <- 2;\n integer y y <- false;}",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            program.visit(v, null);
        }
    }

    @Test
    public void testIdentExpression() throws Exception
    {
        String[] inputs = {
            "p integer x, integer y{ y <- 1; x <- y; }"
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, null);

            Program p = ( Program ) program;

            List<Statement> ss = p.getB().getStatements();

            for( Statement s : ss )
            {
                if( s instanceof AssignmentStatement )
                {
                    if( (( AssignmentStatement ) s).getE() instanceof IdentExpression )
                    {
                        IdentExpression e = (( IdentExpression ) (( AssignmentStatement ) s).getE());
                        IdentLValue l = (( AssignmentStatement ) s).getVar();

                        assertTrue(e.getType() == Type.getTypeName(l.getDec().getType()));
                    }
                }
            }
        }
    }

    @Test
    public void testIdentExpressionError() throws Exception
    {
        String[] inputs = {
            "p integer x, boolean y { x <- screenwidth; y <- x;}",
            "p boolean x, integer y { y <- screenheight; x <- y;}",
            "p boolean x, integer y { y <- screenheight; x <- y;}",
            "p { integer x url y\nx <- y}"
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            program.visit(v, null);
        }
    }

    @Test
    public void testBinaryExpression() throws Exception
    {
        String[] inputs = {
            "p integer x, integer y{ y <- 1; x <- y; }",
            "p boolean x, boolean y, boolean z { y <- true; x <- false; z <- x < y; }",
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            program.visit(v, null);

            Program p = ( Program ) program;

            List<Statement> ss = p.getB().getStatements();

            for( Statement s : ss )
            {
                if( s instanceof AssignmentStatement )
                {
                    if( (( AssignmentStatement ) s).getE() instanceof BinaryExpression )
                    {
                        Expression e0 = (( BinaryExpression ) (( AssignmentStatement ) s).getE()).getE0();
                        Expression e1 = (( BinaryExpression ) (( AssignmentStatement ) s).getE()).getE1();
                        Scanner.Token op = (( BinaryExpression ) (( AssignmentStatement ) s).getE()).getOp();
                    }
                }
            }
        }
    }

    @Test
    public void testBinaryExpressionError() throws Exception
    {
        String[] inputs = {
            "p boolean x, boolean y { x <- screenwidth; y <- x;}",
            "p boolean x, integer y { y <- screenheight; x <- y;}",
            "p boolean x, integer y { y <- screenheight; x <- y;}",
            "p { integer x url y\nx <- y}",
            "p { integer x image i i <- x % i}",
            "p { integer x image i i <- x / i}",
            "p { integer x image i i <- x - i}"
        };

        for( String input : inputs )
        {
            Scanner scanner = new Scanner(input);
            scanner.scan();

            Parser parser = new Parser(scanner);
            ASTNode program = parser.parse();

            TypeCheckVisitor v = new TypeCheckVisitor();

            thrown.expect(TypeCheckVisitor.TypeCheckException.class);
            program.visit(v, null);
        }
    }

    @Test
    public void testProgram() throws Exception
    {
        String input =
            "program " +
                "{" +
                "   frame fram1" +
                "   while (true)" +
                "   {" +
                "       if(true)\n" +
                "       {" +
                "           sleep 0;" +
                "       }" +
                "   }\n" +
                "   fram1 ->yloc;\n" +
                "}";

        (new TestRunner(input, null)).test();
    }
}
