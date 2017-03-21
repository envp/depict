package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;

public class IdentLValue extends ASTNode
{
    public Dec dec;
    public Type.TypeName typeName;

    public Type.TypeName getTypeName()
    {
        return typeName;
    }

    public void setTypeName(Type.TypeName typeName)
    {
        this.typeName = typeName;
    }

    public Dec getDec()
    {
        return dec;
    }

    public void setDec(Dec dec)
    {
        this.dec = dec;
    }

    public IdentLValue(Token firstToken)
    {
        super(firstToken);
    }

    @Override
    public String toString()
    {
        return "IdentLValue [firstToken=" + firstToken + "]";
    }

    @Override
    public Object visit(ASTVisitor v, Object arg) throws Exception
    {
        return v.visitIdentLValue(this, arg);
    }

    public String getText()
    {
        return firstToken.getText();
    }

}
