
package cop5556sp17;


import cop5556sp17.AST.*;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;


public class CodeGenVisitorTest
{

    static final boolean doPrint = true;

    static void show(Object s)
    {
        if( doPrint )
        {
            System.out.println(s);
        }
    }

    boolean devel = false;
    boolean grade = true;

    private static ASTNode doStuff(String source) throws Exception
    {
        Scanner s = new Scanner(source);
        s.scan();
        Parser p = new Parser(s);
        ASTNode program = p.parse();
        TypeCheckVisitor v = new TypeCheckVisitor();
        program.visit(v, null);
        return program;
    }

    private static void executeByteCode(byte[] bytecode, String name, String[] args)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        // create command line argument array to initialize params, none in this
        // case
        Runnable instance = CodeGenUtils.getInstance(name, bytecode, args);
        instance.run();
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
         * @param program
         *            Program to be transpiled to java
         * @return Source string in pure glorious java
         */
        private String walkProgram(ASTNode program)
        {
            String name = ((Program) program).getName();
            List<ParamDec> pdecs = ((Program) program).getParams();
            Block block = ((Program) program).getB();

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
         * @param paramDecs
         *            list of declarations to traverse
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
         * @param block
         *            Block object to traverse
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
         * @param decs
         *            list of declarations to traverse
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
         * @param stmts
         *            List of statements to traverse
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
                                    ifStmtTemplate, walkExpression(((IfStatement) stmt).getE()),
                                    walkBlock(((IfStatement) stmt).getB())
                            )
                    );
                }
                else if( stmt instanceof WhileStatement )
                {
                    sb.append(
                            String.format(
                                    whileStmtTemplate, walkExpression(((WhileStatement) stmt).getE()),
                                    walkBlock(((WhileStatement) stmt).getB())
                            )
                    );
                }
                else if( stmt instanceof AssignmentStatement )
                {
                    sb.append(
                            String.format(
                                    assignStmtTemplate, ((AssignmentStatement) stmt).getVar().getText(),
                                    walkExpression(((AssignmentStatement) stmt).getE())
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
         * @param e
         *            Expression to traverse
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
                Expression left = ((BinaryExpression) e).getE0();
                Expression right = ((BinaryExpression) e).getE1();
                Scanner.Token op = ((BinaryExpression) e).getOp();

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

    @Test
    public void testEmptyProgram() throws Exception
    {
        String input = "emptyProgram {}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        // show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJavaCodeFile(jt);
        
        show("");
        show("========== SOURCE ===========");
        show("");
        show(input);
        
        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);
        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        // no cli args this time
        CodeGenVisitorTest.executeByteCode(bytecode, name, new String[0]);
    }
    

    @Test
    public void testEmptyProgramWithParams() throws Exception
    {
        String input = "emptyProgramWithParams " + "integer i0," + "boolean b0," + "integer i1," + "boolean b1,"
                + "boolean b2," + "integer i2," + "boolean b3" + "{}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        // show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJtavaCodeFile(jt);
        show("");
        show("========== SOURCE ===========");
        show("");
        show(input);

        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        String[] args = { "10", "true", "12", "false", "false", "151161", "true" };
        CodeGenVisitorTest.executeByteCode(bytecode, name, args);
    }

    
    @Test
    public void testWithPBDecsNoStatement() throws Exception
    {
        String input = "prog integer i, boolean b, integer i2, boolean b2 {\n\tinteger ii\n\tboolean bb\n}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        // show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJtavaCodeFile(jt);
        show("");
        show("========== SOURCE ===========");
        show("");
        show(input);

        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        String[] args = { "10", "true", "12", "false"};
        CodeGenVisitorTest.executeByteCode(bytecode, name, args);
    }
    

    @Test
    public void testPWithAssignmentStatementAndExpressions() throws Exception
    {
        String input = "prog integer gint0, boolean gbul0, integer gint1, boolean gbul1 {\n" + 
                "\tinteger lint0\n" +
                "\tinteger lint1\n" + 
                "\tboolean lbul0\n" +
                "\tboolean lbul1\n" +
                "\tlint0 <- 66;\n" + 
                "\tgint0 <- 32;\n" + 
                "\tlbul0 <- false;\n" + 
                "\tgbul0 <- lint0 < gint0;\n" +
                "\tlbul1 <- lint0 > gint0;\n" +
                "\tlbul1 <- lint0 <= gint0;\n" +
                "\tlbul1 <- lint0 >= gint0;\n" +
                "\tlbul1 <- true < true;\n" +
                "\tlbul1 <- true < false;\n" +
                "\tlbul1 <- false < true;\n" +
                "\tlbul1 <- false < false;\n" +
                "\tlbul1 <- false > false;\n" +
                "\tlbul1 <- false > true;\n" +
                "\tlbul1 <- true > false;\n" +
                "\tlbul1 <- true > true;\n" +
                "\tgbul0 <- true == true;\n" +
                "\tgbul0 <- true == false;\n" +
                "\tgbul0 <- false == true;\n" +
                "\tgbul0 <- false == false;\n" +
                "\tgbul0 <- false != false;\n" +
                "\tgbul0 <- false != true;\n" +
                "\tgbul0 <- true != false;\n" +
                "\tgbul0 <- true != true;\n" + 
                "\tgbul0 <- lint0 != gint0;\n" +
                "\tgbul0 <- lint0 != lint0;\n" + 
                "\tgbul0 <- gint0 != lint0;\n" + 
                "\tgbul0 <- gint0 != gint0;\n" +
                "\tgbul0 <- lint0 != gint0;\n" +
                "\tgbul0 <- lint0 != lint0;\n" + 
                "\tgbul0 <- gint0 != lint0;\n" + 
                "\tgbul0 <- gint0 != gint0;\n" + 
                "\tlint1 <- lint0 + gint0;\n" +
                "\tlint1 <- gint0 - lint0;\n" +
                "\tlint1 <- lint0 * gint0;\n" +
                "\tlint1 <- lint0 / gint0;\n" +
                "\tlint1 <- (lint0*lint0 + gint0*gint0 - 2*lint0*gint0) / 4;" + 
                "\n}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJtavaCodeFile(jt);
        show("========== SOURCE ===========");
        show("");
        show(input);
        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        String[] args = { "151161", "true", "12", "false" };
        CodeGenVisitorTest.executeByteCode(bytecode, name, args);
    }
    
    @Test
    public void testPWitIfStatement() throws Exception
    {
        String input = "prog integer gint0, boolean gbul0, integer gint1, boolean gbul1 {\n" + 
                "\tinteger lint0\n" +
                "\tinteger lint1\n" + 
                "\tboolean lbul0\n" +
                "\tboolean lbul1\n" +
                "\tlint0 <- 66;\n" + 
                "\tgint0 <- 32;\n" + 
                "\tlbul0 <- false;\n" + 
                "\tgbul0 <- lint0 < gint0;\n" +
                "\tif(gbul0 != true) {\n" +
                "\t\tlint1 <- lint0 - gint0;" + 
                "\n\t}" +
                "\n" +
                "\tif(gbul0 == false | lbul0) {\n" +
                "\t\tlint1 <- gint0 - lint0;}\n" +
                "\n}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJtavaCodeFile(jt);
        show("========== SOURCE ===========");
        show("");
        show(input);
        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        String[] args = { "151161", "true", "12", "false" };
        CodeGenVisitorTest.executeByteCode(bytecode, name, args);
    }

    @Test
    public void testPWithWhileStatement() throws Exception
    {
        String input = "collatz integer val {\n" + 
                "\tinteger cEven\n" +
                "\tinteger cOdd\n" +
                "\tinteger disc\n" +   
                "\twhile(val != 1) {\n" +
                "\t\tdisc <- val % 2;\n" +
                "\t\tcEven <- val / 2;\n" +
                "\t\tcOdd <- 3 * val + 1;\n" +
                "\t\tval <- (1 - disc) * cEven + disc * cOdd;\n" +
                "\t}" +
                "\n}";
        Program p = (Program) CodeGenVisitorTest.doStuff(input);
        JavaTranslator jt = new JavaTranslator(p);
        CodeGenVisitor v = new CodeGenVisitor(devel, grade, null);
        byte[] bytecode = (byte[]) p.visit(v, null);

        // output the generated bytecode
        show("");
        show("========== BYTECODE ==============");
        show("");
        CodeGenUtils.dumpBytecode(bytecode);
        // show("");
        // show("========== JAVA SOURCE ===========");
        // show("");
        // show(jt.translate());
        // writeJtavaCodeFile(jt);
        show("");
        show("========== SOURCE ===========");
        show("");
        show(input);
        // write byte code to file
        String name = p.getName();
        CodeGenVisitorTest.writeByteCodeToFile(name, bytecode);

        show("");
        show("========== EXECUTION ============");
        show("");

        // directly execute bytecode
        String[] args = {"9"};
        CodeGenVisitorTest.executeByteCode(bytecode, name, args);
    }
}
