package lux.xquery;

import lux.ExpressionVisitor;
import lux.xpath.AbstractExpression;

/**
 * represents xquery conditionals (if, then, else)
 */
public class Conditional extends AbstractExpression {
    
    public Conditional (AbstractExpression condition, AbstractExpression trueAction, AbstractExpression falseAction) {
        super (Type.Conditional);
        subs = new AbstractExpression[] { condition, trueAction, falseAction };
    }
    
    public AbstractExpression accept(ExpressionVisitor visitor) {
        acceptSubs(visitor);
        return visitor.visit(this);
    }

    @Override
    public void toString(StringBuilder buf) {
        buf.append ("if (");
        getCondition().toString(buf);
        buf.append (") then (");
        getTrueAction().toString(buf);
        if (getFalseAction() != null) {
            buf.append(") else (");
            getFalseAction().toString(buf);
        }
        buf.append (")");
    }
    
    public final AbstractExpression getCondition () {
        return subs[0];
    }

    
    public final AbstractExpression getTrueAction() {
        return subs[1];
    }
    
    public final AbstractExpression getFalseAction() {
        return subs[2];
    }
}
