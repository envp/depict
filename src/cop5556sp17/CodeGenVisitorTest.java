package cop5556sp17;


import cop5556sp17.AST.*;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class CodeGenVisitorTest
{

    static boolean doPrint = true;

    static void show(Object s)
    {
        if( doPrint )
        {
            System.out.println(s);
        }
    }

    boolean devel = false;
    boolean grade = true;

    private static String executeByteCode(byte[] bytecode, String name, String[] args)
        throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        PrintStream oldStream = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));

        Runnable instance = CodeGenUtils.getInstance(name, bytecode, args);
        instance.run();

        System.setOut(oldStream);

        return outContent.toString();
    }

    private static void writeByteCodeToFile(String name, byte[] bytecode) throws IOException
    {
        String classFileName = "bin/" + name + ".class";
        OutputStream output = new FileOutputStream(classFileName);

        output.write(bytecode);
        output.close();
        System.out.println("wrote classfile to " + classFileName);
    }


    /**
     * This class encapsulates the behaviour for mapping programs
     * in the source langugae to Java8 per the specifications given
     * in the assignment, given a parsed program.
     * <p>
     * I'm assuming that there is at least a one-one correspondence between the
     * original source
     * and the java source.
     * <p>
     * This depends on com.google.googlejavaformat:google-java-format:1.3
     * for getting a (more) readable output
     */
    private class JavaTranslator
    {
        private ASTNode program;

        private HashMap<Type.TypeName, String> typeMap = new HashMap<>();

        /**
         * Template for equivalent java source
         */
        private String progTemplate = "public class %1$s implements Runnable {\n"
            + "   // Instance vars generated from List<ParamDec>\n" + "   %2$s\n" + "   // Constructor\n"
            + "   public %1$s(String[] args) {\n" + "       // TODO: Initialize with values from args\n"
            + "       %4$s\n" + "   }\n" + "   public static void main(String[] args) {\n"
            + "       (new %1$s(args)).run();\n" + "   }\n"
            + "   public void run() %3$s" + "}";

        /**
         * Template for Dec & ParamDec
         */
        private String declarationTemplate = "%1$s %2$s;\n";
        private String instanceDecTemplate = "public %1$s %2$s;\n";

        /**
         * Generic block template
         */
        private String blockTemplate = "{\n" + "   // declaration list\n" + "   %1$s" + "   // statement list\n"
            + "   %2$s" + "}\n";

        /**
         * All statement templates
         */
        private String ifStmtTemplate = "if( %1$s ) %2$s\n";

        private String whileStmtTemplate = "while( %1$s ) %2$s\n";

        private String assignStmtTemplate = "%1$s = %2$s;\n";

        /**
         * Generates equivalent java source for the given program. Poor man's
         * transpiler
         *
         * @param program Program to be transpiled to java
         * @return Source string in pure glorious java
         */
        private String walkProgram(ASTNode program)
        {
            String name = (( Program ) program).getName();
            List<ParamDec> pdecs = (( Program ) program).getParams();
            Block block = (( Program ) program).getB();

            return String.format(progTemplate, name, walkParams(pdecs), walkBlock(block), genCons(pdecs));
        }

        private String genCons(List<ParamDec> paramDecs)
        {
            StringBuffer sb = new StringBuffer();
            for( ParamDec p : paramDecs )
            {
                switch( p.getTypeName() )
                {
                    case INTEGER:
                        sb.append(
                            String.format(
                                "this.%s = Integer.parseInt(args[%d]);\n", p.getIdent().getText(),
                                paramDecs.indexOf(p)
                            )
                        );
                        break;
                    case BOOLEAN:
                        sb.append(
                            String.format(
                                "this.%s = Boolean.parseBoolean(args[%d]);\n", p.getIdent().getText(),
                                paramDecs.indexOf(p)
                            )
                        );
                        break;
                    // For now complex types will not be initialized
                    // But the same assignment pattern applies
                    case IMAGE:
                    case URL:
                    case FILE:
                    case FRAME:
                        break;
                    case NONE:
                        sb.append(String.format("this.%s = null;\n", p.getIdent().getText()));
                        break;
                    default:
                        break;
                }
            }
            return sb.toString();
        }

        /**
         * Generates java variable declarations given a list of ParamDecs
         *
         * @param paramDecs list of declarations to traverse
         * @return equivalent newline seperated java declarations
         */
        private String walkParams(List<ParamDec> paramDecs)
        {
            StringBuffer sb = new StringBuffer();

            if( paramDecs.size() == 0 )
            {
                return "";
            }

            for( ParamDec paramDec : paramDecs )
            {
                sb.append(
                    String.format(
                        instanceDecTemplate, typeMap.get(paramDec.getTypeName()), paramDec.getIdent().getText()
                    )
                );
            }

            return sb.toString();
        }

        /**
         * A healthy method. Walks a block for you. Everytime.
         *
         * @param block Block object to traverse
         * @return Java source for the block passed in
         */
        private String walkBlock(Block block)
        {
            List<Dec> decs = block.getDecs();
            List<Statement> stmts = block.getStatements();

            return String.format(blockTemplate, walkDeclarations(decs), walkStatements(stmts));
        }

        /**
         * Generates java variable declarations given a list of of Decs
         *
         * @param decs list of declarations to traverse
         * @return equivalent newline seperated java declarations
         */
        private String walkDeclarations(List<Dec> decs)
        {
            StringBuffer sb = new StringBuffer();

            if( decs.size() == 0 )
            {
                return "";
            }

            for( Dec dec : decs )
            {
                sb.append(String.format(declarationTemplate, typeMap.get(dec.getTypeName()), dec.getIdent().getText()));
            }

            return sb.toString();
        }

        /**
         * Thou shalt walk
         *
         * @param stmts List of statements to traverse
         * @return Java source representation of the statements
         */
        private String walkStatements(List<Statement> stmts)
        {
            StringBuffer sb = new StringBuffer();

            if( stmts.size() == 0 )
            {
                return "";
            }

            for( Statement stmt : stmts )
            {
                if( stmt instanceof IfStatement )
                {
                    sb.append(
                        String.format(
                            ifStmtTemplate, walkExpression((( IfStatement ) stmt).getE()),
                            walkBlock((( IfStatement ) stmt).getB())
                        )
                    );
                }
                else if( stmt instanceof WhileStatement )
                {
                    sb.append(
                        String.format(
                            whileStmtTemplate, walkExpression((( WhileStatement ) stmt).getE()),
                            walkBlock((( WhileStatement ) stmt).getB())
                        )
                    );
                }
                else if( stmt instanceof AssignmentStatement )
                {
                    sb.append(
                        String.format(
                            assignStmtTemplate, (( AssignmentStatement ) stmt).getVar().getText(),
                            walkExpression((( AssignmentStatement ) stmt).getE())
                        )
                    );
                }
                // TODO: Handle sleep statments (InterruptedException in calling
                // method?)
                // TODO: Handle chains
            }

            return sb.toString();
        }

        /**
         * Walks an expression recursively to generate a string representation
         *
         * @param e Expression to traverse
         * @return Expression represented as a string in infix notation
         */
        private String walkExpression(Expression e)
        {
            if( e instanceof ConstantExpression )
            {
                return e.getFirstToken().getText();
            }
            else if( e instanceof IntLitExpression )
            {
                return e.getFirstToken().getText();
            }
            else if( e instanceof BooleanLitExpression )
            {
                return e.getFirstToken().getText();
            }
            else if( e instanceof IdentExpression )
            {
                return e.getFirstToken().getText();
            }
            else if( e instanceof BinaryExpression )
            {
                Expression left = (( BinaryExpression ) e).getE0();
                Expression right = (( BinaryExpression ) e).getE1();
                Scanner.Token op = (( BinaryExpression ) e).getOp();

                return "(" + walkExpression(left) + " " + op.getText() + " " + walkExpression(right) + ")";
            }

            // This should never happen
            return null;
        }

        public JavaTranslator(ASTNode program)
        {
            this.program = program;

            this.typeMap.put(Type.TypeName.BOOLEAN, "boolean");
            this.typeMap.put(Type.TypeName.INTEGER, "int");
            this.typeMap.put(Type.TypeName.FILE, "java.io.File");
            this.typeMap.put(Type.TypeName.IMAGE, "java.awt.image.BufferedImage");
            this.typeMap.put(Type.TypeName.FRAME, "cop5556sp17.MyFrame");
            this.typeMap.put(Type.TypeName.URL, "java.net.URL");

            // Don't know how to deal with this for now
            this.typeMap.put(Type.TypeName.NONE, "");
        }

        /**
         * @return
         * @see JavaTranslator#walkProgram(Program)
         */
        public String translate()
        {
            return this.walkProgram(this.program);
        }
    }

    private class Gen
    {
        private String prog(String name, String paramDec, String block)
        {
            return (name + " " + paramDec + block);
        }

        private String block(String... statements)
        {
            StringBuffer result = new StringBuffer("{");
            for( String statement : statements )
            {
                result.append(statement + "\n");
            }
            result.append("}");
            return result.toString();
        }
    }

    private byte[] test(String source, String expOut, String[] args, boolean verbose) throws Exception
    {
        boolean oldDoPrint = doPrint;
        doPrint = verbose;
        TypeCheckVisitor typechecker = new TypeCheckVisitor();
        CodeGenVisitor codegen = new CodeGenVisitor(devel, grade, null);

        Scanner scanner = new Scanner(source);
        scanner.scan();

        Parser parser = new Parser(scanner);
        ASTNode program = parser.parse();
        JavaTranslator jt = new JavaTranslator(program);

        // Type-check
        program.visit(typechecker, null);

        // Generate code
        byte[] bytecode = ( byte[] ) program.visit(codegen, null);

        // output the generated bytecode
        show("\n========== BYTECODE ==============\n");

        CodeGenUtils.dumpBytecode(bytecode);

        show("\n========== JAVA SOURCE ===========\n");
        show(jt.translate());

        show("\n========== SOURCE ================\n");
        show(source);

        // write byte code to file
        String name = (( Program ) program).getName();

        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("\n========== EXECUTION ============\n");

        String output = CodeGenVisitorTest.executeByteCode(bytecode, name, args);

        if( expOut != null )
        {
            assertEquals(expOut, output);
        }

        // Restore old global print state
        doPrint = oldDoPrint;

        return bytecode;
    }

    private ClassNode analyse(byte[] klass)
    {
        ClassReader classReader = new ClassReader(klass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }

    private static void testClassMetaData(ClassNode classNode, String progName, int nFields)
    {
        // Fixed, so bundled into method
        String superName = "java/lang/Object";
        String[] methNames = {"<init>", "main", "run"};
        String[] desc = {"([Ljava/lang/String;)V", "([Ljava/lang/String;)V", "()V"};

        assertEquals(progName, classNode.name);
        assertEquals(superName, classNode.superName);
        assertEquals(ACC_PUBLIC | ACC_SUPER, classNode.access);
        assertEquals(nFields, classNode.fields.size());
        assertEquals(3, classNode.methods.size());

        for( int i = 0; i < classNode.methods.size(); i++ )
        {
            MethodNode m = ( MethodNode ) classNode.methods.get(i);
            assertEquals(methNames[i], m.name);
            assertEquals(desc[i], m.desc);
        }
    }

    @Test
    public void testEmptyCompiles() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        String input = (new Gen()).prog(progName, "", "{}");

//        test(input, null, new String[0], false);
        // Extract data from generated bytecode
        ClassNode classNode = analyse(test(input, null, new String[0], false));

        testClassMetaData(classNode, progName, 0);
    }

    @Test
    public void testParamInitEmptyBlockCompiles() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        String input = (new Gen()).prog(progName, "integer i0, boolean b0, file f1, url u1", "{}");
        String[] args = {"10", "true", "abc.txt", "https://stackoverflow.com/"};
        test(input, null, args, false);

        // Extract data from generated bytecode
        ClassNode classNode = analyse(test(input, null, args, false));

        testClassMetaData(classNode, progName, 4);
    }

    @Test
    public void testParamInitBlockNoStmtCompiles() throws Exception
    {
        Gen gen = new Gen();
        String progName = String.format("prog%d", new Date().getTime());
        String input = gen.prog(progName, "integer i, boolean b, file f, url u", "{}");
        String[] args = {"10", "true", "abc.txt", "https://stackoverflow.com/"};

        test(input, null, args, false);

        ClassNode classNode = analyse(test(input, null, args, false));
        testClassMetaData(classNode, progName, 4);
    }

    @Test
    public void testIntLitAssignment() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName), "x:I");
        Gen gen = new Gen();
        String input = gen.prog(
            progName,
            "integer a, boolean b",
            gen.block("integer x", "x <- 1;", "a <- 0;")
        );
        String[] args = {"12", "true"};
        String expOut = "10";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }


    @Test
    public void testIdentExpressionAssignment() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName), "x:I", "y:Z");

        Gen gen = new Gen();
        String input = gen.prog(
            progName,
            "integer a, boolean b",
            gen.block("integer x boolean y", "x <- a;", "y <- b;")
        );

        String[] args = {"12", "true"};
        String expOut = "12true";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }

    }

    @Test
    public void testBoolExpressionAssignment() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName), "x:Z", "y:Z");

        Gen gen = new Gen();
        String input = gen.prog(
            progName,
            "boolean a, boolean b",
            gen.block("boolean x boolean y", "x <- a;", "y <- b;")
        );

        String[] args = {"false", "true"};
        String expOut = "falsetrue";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }

    }

    @Test
    public void testBinaryExpressionAssignment() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName));

        Gen gen = new Gen();
        String input = gen.prog(
            progName,
            "integer x, integer y",
            gen.block(
                "",
                "x <- x + 1 / 2 + 3 * 4;",
                "y <- y + 2 / 2 - 6 % 2;",
                "y <- x & y;",
                "y <- x | y;"
            )
        );

        String[] args = {"0", "0"};
        String expOut = "121012";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }

    @Test
    public void testIfSimple() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName));

        Gen gen = new Gen();
        String input = gen.prog(
            progName,
            "boolean x",
            gen.block(
                "if(x)",
                gen.block(
                    "x <- false;"
                )
            )
        );

        String[] args = {"true"};
        String expOut = "false";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 1);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }

    @Test
    public void testLogic() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName), "lb0:Z", "lb1:Z");

        Gen gen = new Gen();

        String input = gen.prog(
            progName,
            "boolean gb0, boolean gb1",
            gen.block(
                "boolean lb0 boolean lb1",
                "lb0 <- true == true;",
                "lb0 <- true == false;",
                "lb0 <- false == true;",
                "lb0 <- false == false;"
                ,
                "lb0 <- true != true;",
                "lb0 <- true != false;",
                "lb0 <- false != true;",
                "lb0 <- false != false;"
                ,
                "lb0 <- true > true;",
                "lb0 <- true > false;",
                "lb0 <- false > true;",
                "lb0 <- false > false;"
                ,
                "lb0 <- true < true;",
                "lb0 <- true < false;",
                "lb0 <- false < true;",
                "lb0 <- false < false;"
                ,
                "lb0 <- true >= true;",
                "lb0 <- true >= false;",
                "lb0 <- false >= true;",
                "lb0 <- false >= false;"
                ,
                "lb0 <- true <= true;",
                "lb0 <- true <= false;",
                "lb0 <- false <= true;",
                "lb0 <- false <= false;"
            )
        );
        String[] args = {"true", "false"};
        String expOutShort = new StringBuffer().
                                                   append("tfft").
                                                   append("fttf").
                                                   append("ftff").
                                                   append("fftf").
                                                   append("ttft").
                                                   append("tftt").
                                                   toString();
        String expOut = Arrays.
                                  stream(expOutShort.split("")).
                                  map(c -> c.equals("t") ? "true" : "false").
                                  collect(Collectors.joining(""));

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }

    @Test
    public void testLoopingNestedBlocks() throws Exception
    {
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName));

        Gen gen = new Gen();

        String input = gen.prog(
            progName,
            "integer x",
            gen.block(
                "while(x < 1000)",
                gen.block(
                    "if (x % 2 == 0)",
                    gen.block("x <- 2 * x + 1;"),
                    "if (x % 2 == 1)",
                    gen.block("x <- 2 * x;")
                )
            )
        );
        String[] args = {"0"};
        String expOut = "1251021428517034168213652730";

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 1);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }

    @Test
    public void testLoopingNestedLoops() throws Exception
    {
        // Sum of all pairs of numbers
        String progName = String.format("prog%d", new Date().getTime());
        List<String> lvars = Arrays.asList(String.format("this:L%s;", progName), "i:I", "j:I", "sum:I");

        Gen gen = new Gen();

        String input = gen.prog(
            progName,
            "integer r, integer c",
            gen.block(
                "integer i integer j integer sum",
                "i <- 0;",
                "while(i < r)",
                gen.block(
                    "j <- 0;",
                    "while (j < c)",
                    gen.block(
                        "sum <- i + j;",
                        "j <- j + 1;"
                    ),
                    "i <- i + 1;"
                )
            )
        );
        String[] args = {"2", "2"};
        String expOut = "0";

        for( int i = 0; i < Integer.parseInt(args[0]); ++i )
        {
            expOut += "0";
            for( int j = 0; j < Integer.parseInt(args[1]); ++j )
            {
                expOut += Integer.valueOf(i + j).toString();
                expOut += Integer.valueOf(j + 1).toString();
            }
            expOut += Integer.valueOf(i + 1).toString();
        }

        test(input, expOut, args, false);

        ClassNode classNode = analyse(test(input, expOut, args, false));

        testClassMetaData(classNode, progName, 2);

        // Additional assertions about program structure
        MethodNode runMethod = ( MethodNode ) classNode.methods.get(2);

        for( int i = 0; i < runMethod.localVariables.size(); ++i )
        {
            LocalVariableNode lv = ( LocalVariableNode ) runMethod.localVariables.get(i);
            assertTrue(lvars.contains(lv.name + ":" + lv.desc));
        }
    }
}
