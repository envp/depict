package cop5556sp17;


import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.*;


public class CodeGenVisitor implements ASTVisitor, Opcodes
{

    /**
     * @param DEVEL
     *            used as parameter to genPrint and genPrintTOS
     * @param GRADE
     *            used as parameter to genPrint and genPrintTOS
     * @param sourceFileName
     *            name of source file, may be null.
     */
    public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName)
    {
        super();
        this.DEVEL = DEVEL;
        this.GRADE = GRADE;
        this.sourceFileName = sourceFileName;
    }

    private class Pair<K, V>
    {
        private K key;
        private V value;

        public Pair(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public K getKey()
        {
            return this.key;
        }

        public V getValue()
        {
            return this.value;
        }

        public boolean equals(Object other)
        {
            if( null == other )
            {
                return false;
            }
            if( !other.getClass().equals(this.getClass()) )
            {
                return false;
            }
            if( this == other )
            {
                return true;
            }
            Pair<K, V> _other = (Pair<K, V>) other;
            return _other.getKey() == this.key && _other.getValue() == this.value;
        }
    }

    ClassWriter cw;
    // visitor of method currently under construction
    MethodVisitor mv;
    FieldVisitor fv;
    String className;
    String classDesc;
    String sourceFileName;

    /** Indicates whether genPrint and genPrintTOS should generate code. */
    final boolean DEVEL;
    final boolean GRADE;

    // Declaration to start, end label map
    LinkedHashMap<Dec, Pair<Label, Label>> locals = new LinkedHashMap<>();

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception
    {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//        cw = new ClassWriter(0);
        className = program.getName();
        classDesc = "L" + className + ";";
        String sourceFileName = (String) arg;
        ArrayList<ParamDec> params = program.getParams();

        cw.visit(
                52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[] { "java/lang/Runnable" }
        );
        cw.visitSource(sourceFileName, null);

        // Visit fields
        for( int i = 0; i < params.size(); ++i )
        {
            fv = cw.visitField(
                    ACC_PUBLIC, params.get(i).getIdent().getText(), params.get(i).getTypeName().getJVMTypeDesc(), null,
                    null
            );
            fv.visitEnd();
        }

        // generate constructor code
        // get a MethodVisitor
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        // Create label at start of code
        Label constructorStart = new Label();
        mv.visitLabel(constructorStart);
        // this is for convenience during development--you can see that the code
        // is doing something.
        CodeGenUtils.genPrint(DEVEL, mv, "\nProgram\t\t: entering <init>");
        // generate code to call superclass constructor
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        // visit parameter decs to add each as field to the class
        // pass in mv so decs can add their initialization code to the
        // constructor.
        for( int i = 0; i < params.size(); ++i )
        {
            params.get(i).visit(this, Integer.valueOf(i));
        }
        mv.visitInsn(RETURN);
        // create label at end of code
        Label constructorEnd = new Label();
        mv.visitLabel(constructorEnd);
        // finish up by visiting local vars of constructor
        // the fourth and fifth arguments are the region of code where the local
        // variable is defined as represented by the labels we inserted.
        mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
        mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
        // indicates the max stack size for the method.
        // because we used the COMPUTE_FRAMES parameter in the classwriter
        // constructor, asm
        // will do this for us. The parameters to visitMaxs don't matter, but
        // the method must
        // be called.
        mv.visitMaxs(1, 1);
        // finish up code generation for this method.
        mv.visitEnd();
        // end of constructor

        // create main method which does the following
        // 1. instantiate an instance of the class being generated, passing the
        // String[] with command line arguments
        // 2. invoke the run method.
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        Label mainStart = new Label();
        mv.visitLabel(mainStart);
        // this is for convenience during development--you can see that the code
        // is doing something.
        CodeGenUtils.genPrint(DEVEL, mv, "\nProgram\t\t: entering main");
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
        mv.visitInsn(RETURN);
        Label mainEnd = new Label();
        mv.visitLabel(mainEnd);

        mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
        mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // create run method
        mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        mv.visitCode();
        Label startRun = new Label();
        mv.visitLabel(startRun);
        CodeGenUtils.genPrint(DEVEL, mv, "\nProgram\t\t: entering run");
        program.getB().visit(this, null);
        mv.visitInsn(RETURN);
        Label endRun = new Label();
        mv.visitLabel(endRun);
        mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);

        // TODO Local vars

        mv.visitMaxs(0, 0);
        mv.visitEnd(); // end of run method

        cw.visitEnd();// end of class

        // generate classfile and return it
        return cw.toByteArray();
    }

    @Override
    public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception
    {
        // TODO Implement this
        // For assignment 5, only needs to handle integers and booleans
        Label label = new Label();
        int idx = ((Integer) arg).intValue();

        mv.visitLabel(label);

        // this
        mv.visitVarInsn(ALOAD, 0);

        // args
        mv.visitVarInsn(ALOAD, 1);

        // index
        mv.visitIntInsn(BIPUSH, idx);

        // args[idex]
        mv.visitInsn(AALOAD);

        switch( paramDec.getTypeName() )
        {
            case INTEGER:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
                break;
            case BOOLEAN:
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
            default:
                break;
        }

        mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), paramDec.getTypeName().getJVMTypeDesc());
        CodeGenUtils.genPrint(
                DEVEL, mv, String.format(
                        "\nParamDec\t: initialized parameter %s<%s>", paramDec.getIdent().getText(),
                        paramDec.getTypeName()
                )
        );
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws Exception
    {
        CodeGenUtils.genPrint(DEVEL, mv, "\nBlock\t\t: {");
        ArrayList<Dec> decs = block.getDecs();

        // Scope start
        Label blockStart = new Label();
        mv.visitLabel(blockStart);

        // Set slot numbers of local vars in order of appearance
        // Slot 0 belongs to 'this' So start indices for local vars at 1
        for( int i = 0; i < decs.size(); ++i )
        {
            decs.get(i).visit(this, Integer.valueOf(i + 1));
        }

        for( Statement stmt : block.getStatements() )
        {
            stmt.visit(this, arg);
        }
        // End of scope
        Label blockEnd = new Label();
        mv.visitLabel(blockEnd);
        CodeGenUtils.genPrint(DEVEL, mv, "\nBlock\t\t: }");
        return null;
    }

    @Override
    public Object visitDec(Dec declaration, Object arg) throws Exception
    {
        CodeGenUtils.genPrint(
                DEVEL, mv, String.format(
                        "\nDec\t\t: Found local var declaration %s<%s>", declaration.getIdent().getText(),
                        declaration.getTypeName()
                )
        );

        declaration.setSlot(((Integer) arg).intValue());
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception
    {
        Label start = new Label();
        mv.visitLabel(start);
        // Visit RValue expression of assignment statement
        assignStatement.getE().visit(this, arg);
        
        CodeGenUtils.genPrint(DEVEL, mv, "\nAssignment\t: " + assignStatement.getVar().getText() + "=");
        CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getType());
        
        // Visit LValue of assignment statement
        assignStatement.getVar().visit(this, arg);
        return null;
    }

    @Override
    public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception
    {
        mv.visitLdcInsn(intLitExpression.getValue());
        return null;
    }

    @Override
    public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception
    {
        if( booleanLitExpression.getValue() )
        {
            mv.visitInsn(ICONST_1);
        }
        else
        {
            mv.visitInsn(ICONST_0);
        }
        return null;
    }

    @Override
    public Object visitConstantExpression(ConstantExpression constantExpression, Object arg)
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception
    {
        // Store recently evaluated RVALUE in local vartable, shrinking stack by 1   
        Dec declaration = identX.getDec();
        if( declaration.getClass() == ParamDec.class)
        {
            // stack: this, val
            mv.visitIntInsn(ALOAD, 0);
            // stack: val, this
            mv.visitInsn(SWAP);
            // putfield needs value to come before object ref
            mv.visitFieldInsn(PUTFIELD, className, identX.getText(), identX.getTypeName().getJVMTypeDesc());
        }
        else
        {

            // Bound to be local at this point
            mv.visitVarInsn(ISTORE, declaration.getSlot());
        }
        return null;
    }

    @Override
    public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception
    {
        Dec dec = identExpression.getDec();
        if(dec.getClass() == ParamDec.class)
        {
            switch( identExpression.getType() )
            {
                case INTEGER:
                case BOOLEAN:
                    mv.visitIntInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc());
                    break;
                default:
                    break;
            }
        }
        else
        {
            switch( identExpression.getType() )
            {
                case INTEGER:
                case BOOLEAN:
                    mv.visitIntInsn(ILOAD, identExpression.getDec().getSlot());
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception
    {
        Token op = binaryExpression.getOp();
        Expression e0 = binaryExpression.getE0();
        Expression e1 = binaryExpression.getE1();
        
        e0.visit(this, arg);
        e1.visit(this, arg);

        // If ladder to handle all specified type combinations 
        // (except comparisons, where both operands are loaded on to the stack and comparison
        // is deferred to the statement where they must be compared.
        // This will work since comparison
        if( e0.getType() == TypeName.INTEGER && e1.getType() == TypeName.INTEGER )
        {
            switch( op.kind )
            {
                case PLUS:
                    // pop: e1, e0
                    // result: e1+e0
                    mv.visitInsn(IADD);
                    break;
                case MINUS:
                    // pop: e1, e0
                    // result: e0-e1
                    mv.visitInsn(ISUB);
                    break;
                case TIMES:
                    // pop: e1, e0
                    // result: e1*e0
                    mv.visitInsn(IMUL);
                    break;
                case DIV:
                    // pop: e1, e0
                    // result: e0/e1
                    mv.visitInsn(IDIV);
                    break;
                case MOD:
                    // pop e1, e0
                    // result: e0 % e1
                    mv.visitInsn(IREM);
                    break;
                case LT:
                    // pop e1, e0
                    // compare (if_icmpge) = not(e0 >= e1)
                    Label fsLT = new Label();
                    Label guardLT = new Label();
                    mv.visitJumpInsn(IF_ICMPGE, guardLT);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsLT);
                    mv.visitLabel(guardLT);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsLT);
                    break;
                case GT:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsGT = new Label();
                    Label guardGT = new Label();
                    mv.visitJumpInsn(IF_ICMPLE, guardGT);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsGT);
                    mv.visitLabel(guardGT);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsGT);
                    break;
                case LE:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsLE = new Label();
                    Label guardLE = new Label();
                    mv.visitJumpInsn(IF_ICMPGT, guardLE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsLE);
                    mv.visitLabel(guardLE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsLE);
                    break;
                case GE:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsGE = new Label();
                    Label guardGE = new Label();
                    mv.visitJumpInsn(IF_ICMPLT, guardGE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsGE);
                    mv.visitLabel(guardGE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsGE);
                    break;
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ICMPNE, guardE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsE);
                    mv.visitLabel(guardE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsE);
                    break;
                case NOTEQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsNE = new Label();
                    Label guardNE = new Label();
                    mv.visitJumpInsn(IF_ICMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    break;
            }
        }
        if( e0.getType() == TypeName.BOOLEAN && e1.getType() == TypeName.BOOLEAN )
        {
            switch( op.kind )
            {
                case LT:
                    // pop e1, e0
                    // compare (if_icmpge) = not(e0 >= e1)
                    Label fsLT = new Label();
                    Label guardLT = new Label();
                    mv.visitJumpInsn(IF_ICMPGE, guardLT);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsLT);
                    mv.visitLabel(guardLT);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsLT);
                    break;
                case GT:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsGT = new Label();
                    Label guardGT = new Label();
                    mv.visitJumpInsn(IF_ICMPLE, guardGT);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsGT);
                    mv.visitLabel(guardGT);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsGT);
                    break;
                case LE:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsLE = new Label();
                    Label guardLE = new Label();
                    mv.visitJumpInsn(IF_ICMPGT, guardLE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsLE);
                    mv.visitLabel(guardLE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsLE);
                    break;
                case GE:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsGE = new Label();
                    Label guardGE = new Label();
                    mv.visitJumpInsn(IF_ICMPLT, guardGE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsGE);
                    mv.visitLabel(guardGE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsGE);
                    break;
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ICMPNE, guardE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsE);
                    mv.visitLabel(guardE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsE);
                    break;
                case NOTEQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsNE = new Label();
                    Label guardNE = new Label();
                    mv.visitJumpInsn(IF_ICMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception
    {
        CodeGenUtils.genPrint(DEVEL, mv, "\nIfStmt\t:");
        Block b = ifStatement.getB();
        Expression e = ifStatement.getE();
        Label fs = new Label();
        
        e.visit(this, arg);
        
        mv.visitJumpInsn(IFEQ, fs);
        b.visit(this, arg);
        mv.visitLabel(fs);
        
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception
    {
        CodeGenUtils.genPrint(DEVEL, mv, "\nWhileStmt:");
        Block b = whileStatement.getB();
        Expression e = whileStatement.getE();
        
        Label l3 = new Label();
        Label l4 = new Label();
        
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l4);
        b.visit(this, arg);
        mv.visitLabel(l3);
        e.visit(this, arg);
        mv.visitJumpInsn(IFNE, l4);
        
        return null;
    }

    @Override
    public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitTuple(Tuple tuple, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

}
