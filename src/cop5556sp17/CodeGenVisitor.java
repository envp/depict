package cop5556sp17;

import cop5556sp17.AST.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import cop5556sp17.AST.Type.TypeName;

import java.util.ArrayList;
import java.util.stream.Stream;

//import org.objectweb.asm.Type;

public class CodeGenVisitor implements ASTVisitor, Opcodes
{

    /**
     * @param DEVEL          used as parameter to genPrint and genPrintTOS
     * @param GRADE          used as parameter to genPrint and genPrintTOS
     * @param sourceFileName name of source file, may be null.
     */
    public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName)
    {
        super();
        this.DEVEL = DEVEL;
        this.GRADE = GRADE;
        this.sourceFileName = sourceFileName;
    }

    ClassWriter cw;
    MethodVisitor mv;
    String className;
    String classDesc;
    String sourceFileName;

    /**
     * Indicates whether genPrint and genPrintTOS should generate code.
     */
    final boolean DEVEL;
    final boolean GRADE;

    final String[] INTERFACES = Stream.of(
        new Class[]{Runnable.class}
    ).map(Type::getInternalName).toArray(String[]::new);

    final static String SUPER_NAME = Type.getInternalName(Object.class);
    final static String JNI_CONS_NAME = "<init>";
    final static String JNI_MAIN_NAME = "main";

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception
    {
        String sourceFileName = ( String ) arg;
        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        this.className = program.getName();
        this.classDesc = "L" + className + ";";

        cw.visit(
            52,
            ACC_PUBLIC + ACC_SUPER,
            className,
            null,
            SUPER_NAME,
            INTERFACES
        );

        cw.visitSource(sourceFileName, null);

        // generate constructor code
        // get a MethodVisitor
        mv = cw.visitMethod(
            ACC_PUBLIC,
            JNI_CONS_NAME,
            CodeGenUtils.methodDescriptor(void.class, String[].class),
            null,
            null
        );

        mv.visitCode();

        // Create labels for start and end of code
        Label constructorStart = new Label();
        Label constructorEnd = new Label();
        mv.visitLabel(constructorStart);

        CodeGenUtils.genPrint(DEVEL, mv, "\nentering " + JNI_CONS_NAME);

        // generate code to call superclass constructor
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(
            INVOKESPECIAL,
            Type.getInternalName(Object.class),
            JNI_CONS_NAME,
            CodeGenUtils.methodDescriptor(void.class),
            false
        );

        // visit parameter decs to add each as field to the class
        // pass in mv so decs can add their initialization code to the
        // constructor.
        ArrayList<ParamDec> params = program.getParams();

        for( ParamDec dec : params )
        {
            dec.visit(this, mv);
        }

        mv.visitInsn(RETURN);

        // create label at end of code
        mv.visitLabel(constructorEnd);

        // finish up by visiting local vars of constructor
        // the fourth and fifth arguments are the region of code where the local
        // variable is defined as represented by the labels we inserted.
        mv.visitLocalVariable(
            "this",
            classDesc,
            null,
            constructorStart,
            constructorEnd,
            0
        );
        mv.visitLocalVariable(
            "args",
            Type.getInternalName(String[].class),
            null,
            constructorStart,
            constructorEnd,
            1
        );

        // indicates the max stack size for the method.
        // because we used the COMPUTE_FRAMES parameter in the classwriter
        // constructor, asm
        // will do this for us. The parameters to visitMaxs don't matter, but
        // the method must
        // be called.
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        // end of constructor

        // create main method which does the following
        // 1. instantiate an instance of the class being generated, passing the
        // String[] with command line arguments
        // 2. invoke the run method.
        mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC,
            JNI_MAIN_NAME,
            CodeGenUtils.methodDescriptor(void.class, String[].class),
            null,
            null
        );
        mv.visitCode();
        Label mainStart = new Label();
        Label mainEnd = new Label();

        mv.visitLabel(mainStart);
        CodeGenUtils.genPrint(DEVEL, mv, "\nentering " + JNI_MAIN_NAME);
        mv.visitTypeInsn(NEW, className);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(
            INVOKESPECIAL,
            className,
            JNI_CONS_NAME,
            CodeGenUtils.methodDescriptor(void.class, String[].class),
            false
        );
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitLabel(mainEnd);
        mv.visitLocalVariable(
            "args",
            Type.getInternalName(String[].class),
            null,
            mainStart, mainEnd,
            0
        );
        mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // create run method
        mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        mv.visitCode();

        Label startRun = new Label();
        Label endRun = new Label();

        mv.visitLabel(startRun);
        CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
        program.getB().visit(this, null);
        mv.visitInsn(RETURN);

        mv.visitLabel(endRun);
        mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
        //TODO  visit the local variables
        mv.visitMaxs(1, 1);
        mv.visitEnd(); // end of run method


        cw.visitEnd();//end of class

        //generate classfile and return it
        return cw.toByteArray();
    }

    @Override
    public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception
    {
        //TODO Implement this
    	TypeName typeName = paramDec.getTypeName();
    	String varName = paramDec.getIdent().getText();
    	FieldVisitor fv;
        
        switch( typeName )
        {
            case INTEGER:
            	fv = cw.visitField(ACC_PUBLIC, varName, Type.getInternalName(int.class), null, 0);
            case BOOLEAN:
            	fv = cw.visitField(ACC_PUBLIC, varName, Type.getInternalName(boolean.class), null, false);
            	break;
            default:
                // Not supported currently
                // TODO: Assignment 6
                break;
        }
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception
    {
        assignStatement.getE().visit(this, arg);
        CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
        CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getType());
        assignStatement.getVar().visit(this, arg);
        return null;
    }

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception
    {
        //TODO  Implement this
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws Exception
    {
        //TODO  Implement this
        return null;
    }

    @Override
    public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception
    {
        //TODO Implement this
        return null;
    }

    @Override
    public Object visitConstantExpression(ConstantExpression constantExpression, Object arg)
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitDec(Dec declaration, Object arg) throws Exception
    {
        //TODO Implement this
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
    public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception
    {
        //TODO Implement this
        return null;
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception
    {
        //TODO Implement this
        return null;

    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception
    {
        //TODO Implement this
        return null;
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception
    {
        assert false : "not yet implemented";
        return null;
    }

    @Override
    public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception
    {
        //TODO Implement this
        return null;
    }

    @Override
    public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception
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

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception
    {
        //TODO Implement this
        return null;
    }

}
