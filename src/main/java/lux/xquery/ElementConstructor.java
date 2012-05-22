package lux.xquery;

import lux.ExpressionVisitor;
import lux.xpath.AbstractExpression;
import lux.xpath.Namespace;
import lux.xpath.QName;

public class ElementConstructor extends AbstractExpression {

    private final QName qname;
    private final Namespace[] namespaces;
    
    
    public ElementConstructor(QName qname, Namespace[] namespaces, AbstractExpression content) {
        super(Type.Element);
        this.qname = qname;
        this.subs = new AbstractExpression[] { content };
        this.namespaces = namespaces;
    }

    public AbstractExpression accept(ExpressionVisitor visitor) {
        acceptSubs(visitor);
        return visitor.visit(this);
    }

    @Override
    public void toString(StringBuilder buf) {
        buf.append ("element ");
        qname.toString(buf);
        buf.append (" { ");
        if (namespaces != null && namespaces.length > 0) {
            appendNamespace(namespaces[0], buf);
            for (int i = 1; i < namespaces.length; i++) {
                buf.append (", ");
                appendNamespace(namespaces[i], buf);
            }
        }
        if (getContent() != null) {
            if (namespaces != null && namespaces.length > 0) {
                buf.append (", ");
            }
            getContent() .toString(buf);
        }
        buf.append (" }");
    }
    
    private AbstractExpression getContent() {
        return subs[0];
    }

    private void appendNamespace (Namespace ns, StringBuilder buf) {
        buf.append ("attribute xmlns");
        if (!ns.getPrefix().isEmpty()) {
            buf.append (':');
            buf.append (ns.getPrefix());
        }
        buf.append (" { \"");
        buf.append (ns.getNamespace());
        buf.append ("\" }");        
    }

}
