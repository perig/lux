package lux.xpath;

import java.util.ArrayList;

import lux.SearchResultIterator;
import lux.compiler.XPathQuery;
import lux.index.IndexConfiguration;
import lux.query.BooleanPQuery;
import lux.xml.ValueType;
import lux.xquery.ElementConstructor;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.SortField;

/**
 * A special search function call; this holds a query that is used to accumulate constraints
 * while optimizing.  The function arguments are inferred from the supplied XPathQuery so as to
 * result in the same query when evaluated at run time.
 * TODO: rename either this or the runtime class (lux.functions.SearchBase$SearchCall) of the same name!
 */
public class SearchCall extends FunCall {

    private AbstractExpression queryArg;
    private XPathQuery query; // for facts and sortFields only
    private boolean fnCollection;
    private final boolean generated; // records whether this search call was generated by the optimizer
    
    /**
     * creates a call to lux:search that encodes information provided by the optimizer, enabling combination
     * with additional filters and sorting criteria
     * @param query containing the information compiled by the optimizer
     * @param config used to determine the default field name
     */
    public SearchCall(XPathQuery query, IndexConfiguration config) {
        this (query.getFullQuery().toXmlNode(config.getDefaultFieldName(), config), query.getFacts(), query.getResultType(), query.getSortFields(), true);
    }

    /** used to convert a generic lux:search FunCall into a SearchCall 
     * @param abstractExpression the query, as a string to be parsed by {@link lux.query.parser.LuxQueryParser},
     * or as a node to be parsed by {@link lux.query.parser.XmlQueryParser}.
     */
    public SearchCall(AbstractExpression abstractExpression) {
        this (abstractExpression, XPathQuery.MINIMAL|XPathQuery.SINGULAR, ValueType.VALUE, null, false);
    }
    
    private SearchCall(AbstractExpression queryArg, long facts, ValueType resultType, SortField[] sortFields, boolean isGenerated) {
        super(FunCall.LUX_SEARCH, resultType);
        this.queryArg = queryArg;
        fnCollection = false;
        query = XPathQuery.getQuery(null, null, facts, resultType, null, sortFields);
        this.generated = isGenerated;
        generateArguments();
    }
   
    public void combineQuery(XPathQuery additionalQuery, IndexConfiguration config) {
        ElementConstructor additional = additionalQuery.getFullQuery().toXmlNode(config.getDefaultFieldName(), config);
        if (! additional.getName().getLocalPart().equals("MatchAllDocsQuery")) {
            if (queryArg.getType() == Type.ELEMENT) {
                ElementConstructor addClause = new ElementConstructor(BooleanPQuery.CLAUSE_QNAME, additional, BooleanPQuery.MUST_OCCUR_ATT);
                ElementConstructor thisClause = new ElementConstructor(BooleanPQuery.CLAUSE_QNAME, queryArg, BooleanPQuery.MUST_OCCUR_ATT);
                ElementConstructor combined = new ElementConstructor(BooleanPQuery.BOOLEAN_QUERY_QNAME, new Sequence (thisClause, addClause));
                queryArg = combined;
            }
        }
        // TODO: combine optimizer constraints with user-defined (string) queries 
        this.query = this.query.combineBooleanQueries(Occur.MUST, additionalQuery, Occur.MUST, this.query.getResultType(), config);
        generateArguments();
    }
    
    private void generateArguments () {
        ArrayList<AbstractExpression> args = new ArrayList<AbstractExpression>();
        args.add (queryArg);
        SortField[] sortFields = query.getSortFields();
        if (sortFields != null) {
            args.add(createSortArgument(sortFields));
        } else if (! generated) {
            // if this is an explicit function call that has no explicit ordering, order by relevance
            args.add(new LiteralExpression ("lux:score descending"));
        }
        subs = args.toArray(new AbstractExpression[args.size()]);
    }

    /**
     * @return a string describing sort options to be passed as an argument to search
     */
    private AbstractExpression createSortArgument (SortField[] sort) {
        if (sort == null || sort.length == 0) {
            return LiteralExpression.EMPTY;
        }
        if (sort.length == 1) {
            return new LiteralExpression (formatSortCriterion(sort[0]));
        }
        AbstractExpression [] criteria = new AbstractExpression[sort.length];
        for (int i = 0; i < sort.length; i++) {
            criteria[i] = new LiteralExpression (formatSortCriterion(sort[i]));
        }
        return new Sequence(criteria);
    }

    private String formatSortCriterion(SortField sortField) {
        StringBuilder buf = new StringBuilder();
        buf.append (sortField.getField());
        if (sortField.getReverse()) {
            buf.append (" descending");
        }
        if (SearchResultIterator.MISSING_LAST.equals(sortField.getComparatorSource())) {
            buf.append (" empty greatest");
        }
        switch (sortField.getType()) {
        case INT: buf.append(" int"); break;
        case LONG: buf.append(" long"); break;
        default: // default is string
        }
        return buf.toString();
    }

    /**
     * @return whether this function call will be represented by fn:collection("lux:" + query)
     */
    public boolean isFnCollection() {
        return fnCollection;
    }

    public void setFnCollection(boolean isFnCollection) {
        this.fnCollection = isFnCollection;
    }
    
    @Override
    public SearchCall getRoot() {
        return this;
    }

    public XPathQuery getQuery () {
    	return query;
    }
}
