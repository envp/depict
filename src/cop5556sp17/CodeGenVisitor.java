package cop5556sp17;

import cop5556sp17.AST.*;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


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
            Pair<K, V> _other = ( Pair<K, V> ) other;
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

    /**
     * Indicates whether genPrint and genPrintTOS should generate code.
     */
    final boolean DEVEL;
    final boolean GRADE;

    // Declaration to start, end label map
    HashMap<Dec, Pair<Label, Label>> locals = new HashMap<>();

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception
    {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        cw = new ClassWriter(0);
        className = program.getName();
        classDesc = "L" + className + ";";
        String sourceFileName = ( String ) arg;
        ArrayList<ParamDec> params = program.getParams();

        cw.visit(
            52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[]{"java/lang/Runnable"}
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
        mv = cw.visitMethod(
            ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, new String[0]);
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
            params.get(i).visit(this, i);
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
        mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
            new String[]{"java/net/MalformedURLException"}
        );
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

        // Pass 1 to indicate the first free local var slot
        program.getB().visit(this, 1);
        mv.visitInsn(RETURN);

        Label endRun = new Label();
        mv.visitLabel(endRun);
        mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);

        for( Dec dec : locals.keySet() )
        {
            mv.visitLocalVariable(
                dec.getIdent().getText(),
                dec.getTypeName().getJVMTypeDesc(),
                null,
                locals.get(dec).getKey(),
                locals.get(dec).getValue(),
                dec.getSlot()
            );
        }

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
        int idx = ( Integer ) arg;

        mv.visitLabel(label);

        // this
        mv.visitVarInsn(ALOAD, 0);
        switch( paramDec.getTypeName() )
        {
            case INTEGER:
                // this
                mv.visitVarInsn(ALOAD, 1);
                mv.visitIntInsn(BIPUSH, idx);
                mv.visitInsn(AALOAD);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
                break;
            case BOOLEAN:
                // this
                mv.visitVarInsn(ALOAD, 1);
                mv.visitIntInsn(BIPUSH, idx);
                mv.visitInsn(AALOAD);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
                break;
            case FILE:
                // Create a reference to the file
                mv.visitTypeInsn(NEW, "java/io/File");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitIntInsn(BIPUSH, idx);
                mv.visitInsn(AALOAD);
                mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
                break;
            case URL:
                mv.visitVarInsn(ALOAD, 1);
                mv.visitIntInsn(BIPUSH, idx);
                mv.visitMethodInsn(
                    INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
                break;
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
        List<Pair<Dec, Label>> startLabels = new ArrayList<>();
        int maxSlot = ( Integer ) arg;

        // Scope start
        Label blockStart = new Label();
        mv.visitLabel(blockStart);

        // Set slot numbers of local vars in order of appearance
        // Slot 0 belongs to 'this' So start indices for local vars at 1
        for( int i = 0; i < decs.size(); ++i )
        {
            decs.get(i).visit(this, maxSlot + i);
        }

        maxSlot += decs.size();

        for( Statement stmt : block.getStatements() )
        {
            stmt.visit(this, Integer.valueOf(maxSlot));
            if( stmt instanceof AssignmentStatement )
            {
                // After each statement, check if we encountered an assignment.
                // If so, add a label for each of these.
                AssignmentStatement as = ( AssignmentStatement ) stmt;
                if( as.getVar().getDec().getClass() != ParamDec.class )
                {
                    Label start = new Label();
                    mv.visitLabel(start);
                    startLabels.add(new Pair<Dec, Label>(as.getVar().getDec(), start));
                }
            }
            if( stmt instanceof BinaryChain )
            {
                // It always leaves a value behind on the stack that we don't want
                mv.visitInsn(POP);
            }
        }
        // End of scope
        Label blockEnd = new Label();
        mv.visitLabel(blockEnd);
        for( Pair<Dec, Label> pair : startLabels )
        {
            locals.putIfAbsent(pair.key, new Pair<Label, Label>(pair.value, blockEnd));
        }
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

        int slot = ( Integer ) arg;
        declaration.setSlot(slot);

        // Set frame and image to null by default
        if( declaration.getTypeName() == TypeName.IMAGE || declaration.getTypeName() == TypeName.FRAME )
        {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, slot);
        }

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
        if( booleanLitExpression.firstToken.isKind(Scanner.Kind.KW_TRUE) )
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
        Token token = constantExpression.firstToken;

        if( token.isKind(Kind.KW_SCREENWIDTH) )
        {
            mv.visitMethodInsn(
                INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth", PLPRuntimeFrame.getScreenWidthSig, false);
        }
        else if( token.isKind(Kind.KW_SCREENHEIGHT) )
        {
            mv.visitMethodInsn(
                INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight", PLPRuntimeFrame.getScreenHeightSig,
                false
            );
        }
        return null;
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception
    {
        // Store recently evaluated RVALUE in local vartable, shrinking stack by
        // 1
        Dec declaration = identX.getDec();
        if( declaration.getClass() == ParamDec.class )
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
            Type.TypeName type = declaration.getTypeName();

            switch( type )
            {
                case INTEGER:
                case BOOLEAN:
                    mv.visitVarInsn(ISTORE, declaration.getSlot());
                    break;
                case FRAME:
                    // Pre: There is a non-null reference to a frame on the stack
                    mv.visitVarInsn(ASTORE, declaration.getSlot());
                    break;
                case IMAGE:
                    // Pre: Expression must have evaluated to something of type:image
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "copyImage", PLPRuntimeImageOps.copyImageSig, false);
                    break;
            }
        }
        return null;
    }

    @Override
    public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception
    {
        Dec dec = identExpression.getDec();
        if( dec.getClass() == ParamDec.class )
        {
            switch( identExpression.getType() )
            {
                case INTEGER:
                case BOOLEAN:
                case FILE:
                case URL:
                    mv.visitIntInsn(ALOAD, 0);
                    mv.visitFieldInsn(
                        GETFIELD, className, dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc()
                    );
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
                    mv.visitVarInsn(ILOAD, identExpression.getDec().getSlot());
                    break;
                case IMAGE:
                case FRAME:
                    mv.visitVarInsn(ALOAD, identExpression.getDec().getSlot());
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
        // (except comparisons, where both operands are loaded on to the stack
        // and comparison
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
                case AND:
                    // pop e1, e0
                    // result: e0 & e1
                    mv.visitInsn(IAND);
                    break;
                case OR:
                    // pop e1, e0
                    // result: e0 | e1
                    mv.visitInsn(IOR);
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
        if( e0.getType() == TypeName.IMAGE && e1.getType() == TypeName.IMAGE )
        {
            switch( op.kind )
            {
                case PLUS:
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig, false);
                    break;
                case MINUS:
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig, false);
                    break;
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ACMPNE, guardE);
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
                    mv.visitJumpInsn(IF_ACMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    // This should've been caught during TypeChecking!
                    break;
            }
        }

        if( e0.getType() == TypeName.IMAGE && e1.getType() == TypeName.INTEGER )
        {
            switch( op.kind )
            {
                case TIMES:
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig, false);
                    break;
                case DIV:
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "div", PLPRuntimeImageOps.divSig, false);
                    break;
                case MOD:
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mod", PLPRuntimeImageOps.modSig, false);
                    break;
                default:
                    // This should've been caught during TypeChecking!
                    break;
            }
        }

        if( e0.getType() == TypeName.INTEGER && e1.getType() == TypeName.IMAGE )
        {
            switch( op.kind )
            {
                case TIMES:
                    // Swap args and copy code
                    mv.visitInsn(SWAP);
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig, false);
                    break;
                default:
                    // This should've been caught during TypeChecking!
                    break;
            }
        }

        if( e0.getType() == TypeName.FRAME && e1.getType() == TypeName.FRAME )
        {
            switch( op.kind )
            {
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ACMPNE, guardE);
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
                    mv.visitJumpInsn(IF_ACMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    // This should've been caught during TypeChecking!
                    break;
            }
        }

        if( e0.getType() == TypeName.FILE && e1.getType() == TypeName.FILE )
        {
            switch( op.kind )
            {
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ACMPNE, guardE);
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
                    mv.visitJumpInsn(IF_ACMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    // This should've been caught during TypeChecking!
                    break;
            }
        }

        if( e0.getType() == TypeName.URL && e1.getType() == TypeName.URL )
        {
            switch( op.kind )
            {
                case EQUAL:
                    // pop e1, e0
                    // compare (if_icmple) not(e0 <= e1)
                    Label fsE = new Label();
                    Label guardE = new Label();
                    mv.visitJumpInsn(IF_ACMPNE, guardE);
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
                    mv.visitJumpInsn(IF_ACMPEQ, guardNE);
                    mv.visitInsn(ICONST_1);
                    mv.visitJumpInsn(GOTO, fsNE);
                    mv.visitLabel(guardNE);
                    mv.visitInsn(ICONST_0);
                    mv.visitLabel(fsNE);
                    break;
                default:
                    // This should've been caught during TypeChecking!
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
        Expression e = sleepStatement.getE();
//        Label start = new Label();
//        Label end = new Label();
//        Label handler = new Label();
//        mv.visitTryCatchBlock(start, end, handler, "java/lang/InterruptedException");

        // Handling code
        // Literally nothing
//        mv.visitLabel(start);

        // Evaludate expression and push to stack
        e.visit(this, arg);
        // Convert op to long
        mv.visitInsn(I2L);
        // Sleep
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
//        mv.visitLabel(end);
        return null;
    }

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception
    {
        Chain ch = binaryChain.getE0();
        ChainElem chElem = binaryChain.getE1();
        Scanner.Token op = binaryChain.getArrow();

        /**
         * Visit left chain, and tell the chain that it's on the right side
         */
        if( ch instanceof IdentChain )
        {
            ch.visit(this, Boolean.valueOf(false));
        }
        else
        {
            //  ( ch instanceof FilterOpChain )
            ch.visit(this, op);
        }


        /**
         * Visit right element and tell the chain that it's on the left side
         */
        if( chElem instanceof IdentChain )
        {
            chElem.visit(this, Boolean.valueOf(true));
        }
        else
        {
            //  ( ch instanceof FilterOpChain )
            chElem.visit(this, op);
        }
        return null;
    }

    @Override
    public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception
    {
        // Does this ident occur on the right side of a binary chain?
        boolean isRight = ( Boolean ) arg;
        Type.TypeName idType = identChain.getTypeName();
        Token var = identChain.getFirstToken();

        if( isRight )
        {
            // We right now
            switch( idType )
            {
                case INTEGER:
                    // Stack top consumed, so DUP to be able to compose chains
                    mv.visitInsn(DUP);
                    if( var.getClass().equals(ParamDec.class) )
                    {
                        mv.visitIntInsn(ALOAD, 0);
                        // Swap to ensure value is on top
                        mv.visitInsn(SWAP);
                        mv.visitFieldInsn(
                            PUTFIELD, className, var.getText(), identChain.getTypeName().getJVMTypeDesc());
                    }
                    else
                    {
                        mv.visitVarInsn(ISTORE, identChain.getDec().getSlot());
                    }
                    break;
                case IMAGE:
                    // Stack top consumed, so DUP to be able to compose chains
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, identChain.getDec().getSlot());
                    break;
                case FRAME:
                    // Requires stack to be shaped as follows:
                    // Stack: (PLPRuntimeFrame | BufferedImage | ...) -> (BufferedImage' | ...)
                    mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame",
                        PLPRuntimeFrame.createOrSetFrameSig, false
                    );
                    // Dup the computed Frame before storing, is the store even necessary?
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ASTORE, identChain.getDec().getSlot());
                    break;
                case FILE:
                    // This node is terminal, but still DUP so that it can be popped outside
                    mv.visitInsn(DUP);
                    // Requires stack to be shaped as follows:
                    // Stack: BufferedImage | PLPRuntimeFrame
                    mv.visitIntInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, var.getText(), identChain.getTypeName().getJVMTypeDesc());
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc, false);
                    break;
                default:
                    // case URL. Sadly we can't upload stuff ( -> <type:url>) isn't allowed
                    // case BOOLEAN . No one cares
                    break;
            }
        }
        else
        {
            // We left now
            switch( idType )
            {
                case INTEGER:
                    if( var.getClass().equals(ParamDec.class) )
                    {
                        mv.visitIntInsn(ALOAD, 0);
                        mv.visitFieldInsn(
                            GETFIELD, className, var.getText(), identChain.getTypeName().getJVMTypeDesc());
                    }
                    else
                    {
                        mv.visitVarInsn(ILOAD, identChain.getDec().getSlot());
                    }
                    break;
                case IMAGE:
                case FRAME:
                    mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
                    break;
                case FILE:
                    // StackTop => File reference | ...
                    mv.visitIntInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, var.getText(), identChain.getTypeName().getJVMTypeDesc());
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile", PLPRuntimeImageIO.readFromFileDesc,
                        false
                    );
                    // StackTop => BufferedImage reference | ...
                    break;
                case URL:
                    // identifiers with type file, url Must be fields
                    mv.visitIntInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, className, var.getText(), identChain.getTypeName().getJVMTypeDesc());
                    mv.visitMethodInsn(
                        INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL", PLPRuntimeImageIO.readFromURLSig,
                        false
                    );
                    break;
                default:
                    // case BOOLEAN: No one cares
                    break;
            }
        }
        return null;
    }

    @Override
    public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception
    {
        // Precondition: This method requires that the stack contains the destination at the top and the source 
        // image object reference be behind it.
        Tuple tuple = filterOpChain.getArg();
        Token tok = filterOpChain.getFirstToken();

        Token op = ( Token ) arg;

        tuple.visit(this, arg);

        switch( tok.kind )
        {
            case OP_BLUR:
                // Pre: stack = dest | source
                mv.visitInsn(ACONST_NULL);
                mv.visitMethodInsn(
                    INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
                break;
            case OP_GRAY:
                // Pre: stack = dest | source
                if( op.isKind(Kind.ARROW) )
                {
                    mv.visitInsn(ACONST_NULL);
                }
                else if( op.isKind(Kind.BARARROW) )
                {
                    mv.visitInsn(DUP);
                }
                mv.visitMethodInsn(
                    INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
                break;
            case OP_CONVOLVE:
                // Pre: stack = dest | source                
                mv.visitInsn(ACONST_NULL);
                mv.visitMethodInsn(
                    INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig, false);
                break;
        }
        return null;
    }

    @Override
    public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception
    {
        Tuple tuple = frameOpChain.getArg();
        Token tok = frameOpChain.getFirstToken();

        tuple.visit(this, arg);
        switch( tok.kind )
        {
            case KW_SHOW:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc, false);
                break;
            case KW_HIDE:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc, false);
                break;
            case KW_MOVE:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc, false);
                break;
            case KW_XLOC:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc, false);
                break;
            case KW_YLOC:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc, false);
                break;
        }
        return null;
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception
    {
        Tuple tuple = imageOpChain.getArg();
        Token tok = imageOpChain.getFirstToken();

        tuple.visit(this, arg);

        switch( tok.kind )
        {
            case OP_WIDTH:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getWidth", PLPRuntimeImageOps.getWidthSig,
                    false
                );
                break;
            case OP_HEIGHT:
                mv.visitMethodInsn(
                    INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getHeight",
                    PLPRuntimeImageOps.getHeightSig, false
                );
                break;
            case KW_SCALE:
                mv.visitMethodInsn(
                    INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
                break;
        }
        return null;
    }

    @Override
    public Object visitTuple(Tuple tuple, Object arg) throws Exception
    {
        List<Expression> es = tuple.getExprList();
        for( Expression e : es )
        {
            e.visit(this, arg);
        }
        return null;
    }

}
