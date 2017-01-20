package cop5556sp17;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp17.Scanner.IllegalCharException;
import cop5556sp17.Scanner.IllegalNumberException;
import cop5556sp17.Scanner.Kind;

import java.util.Arrays;


public class ScannerTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmpty() throws IllegalCharException, IllegalNumberException
    {
        String input = "";
        Scanner scanner = new Scanner(input);
        scanner.scan();
        Scanner.Token token = scanner.nextToken();
        assertEquals(Kind.EOF, token.kind);
    }

    @Test
    public void testWellFormedComments() throws IllegalCharException, IllegalNumberException
    {
        // This program should be read as equivalent to an empty string
        String input = "/* This is a comments */\n/* Followed by another comment */";
        Scanner scanner = new Scanner(input);
        scanner.scan();
        Scanner.Token token = scanner.nextToken();
        assertEquals(Kind.EOF, token.kind);
    }

    @Test
    public void testMalFormedComments() throws IllegalCharException, IllegalNumberException
    {
        String input = "/* /* Scanned comment ends here */ IllegalCharException here */";
        Scanner scanner = new Scanner(input);
        scanner.scan();
        Scanner.Token[] tokens = new Scanner.Token[4];
        tokens[0] = scanner.nextToken();
        tokens[1] = scanner.nextToken();
        tokens[2] = scanner.nextToken();
        tokens[3] = scanner.nextToken();

        assertEquals(Kind.IDENT, tokens[0].kind);
        assertEquals("IllegalCharException", tokens[0].getText());

        assertEquals(Kind.IDENT, tokens[1].kind);
        assertEquals("here", tokens[1].getText());

        assertEquals(Kind.TIMES, tokens[2].kind);
        assertEquals("*", tokens[2].getText());

        assertEquals(Kind.DIV, tokens[3].kind);
        assertEquals("/", tokens[3].getText());
    }

    @Test
    public void testOpenComments() throws IllegalCharException, IllegalNumberException
    {
        String input = "/* This comment was mistakenly left open";
        Scanner scanner = new Scanner(input);
        thrown.expect(IllegalCharException.class);
        scanner.scan();
    }

    @Test
    public void testSemiConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = ";;;";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.SEMI.text;

        for(int i = 0; i < 3; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.SEMI, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testCommaConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = ",,,,,";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.COMMA.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.COMMA, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testLParenConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = "(((((";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.LPAREN.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.LPAREN, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testRParenConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = ")))))";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.RPAREN.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.RPAREN, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testLBraceConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = "{{{{{";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.LBRACE.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.LBRACE, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testRBraceConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = "}}}}}";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.RBRACE.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.RBRACE, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testPipeConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = "||||||";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.OR.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.OR, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testAmpConcat() throws IllegalCharException, IllegalNumberException
    {

        String input = "&&&&&";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token;
        String text = Kind.AND.text;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.AND, token.kind);
            assertEquals(i, token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testEqualsSingle() throws IllegalCharException, IllegalNumberException
    {

        String input = "=";

        Scanner scanner = new Scanner(input);
        thrown.expect(IllegalCharException.class);
        scanner.scan();
    }

    @Test
    public void testEqualsDouble() throws IllegalCharException, IllegalNumberException
    {

        String input = "== ==   ==";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Scanner.Token token = scanner.nextToken();
        String text = Kind.EQUAL.text;

        int[] position = {0, 3, 8};

        for(int i = 0; i < 3; ++i)
        {
            token = scanner.nextToken();

            assertEquals(Kind.EQUAL, token.kind);
            assertEquals(position[i], token.pos);
            assertEquals(text, token.getText());
            assertEquals(text.length(), token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testComparators() throws IllegalCharException, IllegalNumberException
    {
        // String containing all the comparators
        String input = "><>=<=!===";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Kind[] kind = {Kind.GT, Kind.LT, Kind.GE, Kind.LE, Kind.NOTEQUAL, Kind.EQUAL};
        int[] length = {1, 1, 2, 2, 2, 2};
        int[] position = {0, 1, 2, 4, 6, 8};

        Scanner.Token token;

        for(int i = 0; i < 6; ++i)
        {
            token = scanner.nextToken();

            assertEquals(kind[i], token.kind);
            assertEquals(position[i], token.pos);
            assertEquals(kind[i].getText(), token.getText());
            assertEquals(length[i], token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testOperators() throws IllegalCharException, IllegalNumberException
    {
        // String containing all the comparators
        String input = "+-*/%";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Kind[] kind = {Kind.PLUS, Kind.MINUS, Kind.TIMES, Kind.DIV, Kind.MOD};
        int[] position = {0, 1, 2, 3, 4};

        Scanner.Token token;

        for(int i = 0; i < 5; ++i)
        {
            token = scanner.nextToken();

            assertEquals(kind[i], token.kind);
            assertEquals(position[i], token.pos);
            assertEquals(kind[i].getText(), token.getText());
            assertEquals(1, token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    @Test
    public void testArrowOperators() throws IllegalCharException, IllegalNumberException
    {
        // String containing all the comparators
        String input = "-><-|->";

        Scanner scanner = new Scanner(input);
        scanner.scan();

        Kind[] kind = {Kind.ARROW, Kind.ASSIGN, Kind.BARARROW};
        int[] position = {0, 2, 4};
        int[] length = {2, 2, 3};

        Scanner.Token token;

        for(int i = 0; i < 3; ++i)
        {
            token = scanner.nextToken();

            assertEquals(kind[i], token.kind);
            assertEquals(position[i], token.pos);
            assertEquals(kind[i].getText(), token.getText());
            assertEquals(length[i], token.length);
        }
        // Check that EOF was inserted
        token = scanner.nextToken();
        assertEquals(Scanner.Kind.EOF, token.kind);
    }

    /**
     * This test illustrates how to check that the Scanner detects errors
     * properly.
     * In this test, the input contains an int literal with a value that exceeds
     * the range of an int.
     * The scanner should detect this and throw and IllegalNumberException.
     *
     * @throws IllegalCharException
     * @throws IllegalNumberException
     */
    @Test
    public void testIntOverflowError() throws IllegalCharException, IllegalNumberException
    {
        String input = "99999999999999999";
        Scanner scanner = new Scanner(input);
        thrown.expect(IllegalNumberException.class);
        scanner.scan();
    }

    @Test
    public void testIntegerLiteralZ() throws IllegalCharException, IllegalNumberException
    {
        String zero = "0";

        Scanner scanner = new Scanner(zero);
        scanner.scan();
        Scanner.Token token = scanner.nextToken();
        assertEquals(token.kind, Kind.INT_LIT);
        assertEquals("0", token.getText());

        // Confirm presence of an EOF token
        token = scanner.nextToken();
        assertEquals(Kind.EOF, token.kind);
    }

    @Test
    public void testIntegerLiteral() throws IllegalCharException, IllegalNumberException
    {
        String vanillaInt = "1234567890";

        Scanner scanner = new Scanner(vanillaInt);
        scanner.scan();

        Scanner.Token token = scanner.nextToken();

        assertEquals(Kind.INT_LIT, token.kind);
        assertEquals(vanillaInt, token.getText());
        assertEquals(10, token.length);

        token = scanner.nextToken();

        assertEquals(Kind.EOF, token.kind);
    }

    @Test
    public void testIntegerLiteralMixed() throws IllegalCharException, IllegalNumberException
    {
        String zeroAndMore = "012345678*3";

        Scanner scanner = new Scanner(zeroAndMore);
        scanner.scan();

        Scanner.Token token = scanner.nextToken();

        assertEquals(Kind.INT_LIT, token.kind);
        assertEquals("0", token.getText());
        assertEquals(1, token.length);

        token = scanner.nextToken();

        assertEquals(Kind.INT_LIT, token.kind);
        assertEquals("12345678", token.getText());
        assertEquals(8, token.length);

        token = scanner.nextToken();

        assertEquals(Kind.TIMES, token.kind);
        assertEquals("*", token.getText());
        assertEquals(1, token.length);

        token = scanner.nextToken();

        assertEquals(Kind.INT_LIT, token.kind);
        assertEquals("3", token.getText());
        assertEquals(1, token.length);

        token = scanner.nextToken();

        assertEquals(Kind.EOF, token.kind);
    }

}
