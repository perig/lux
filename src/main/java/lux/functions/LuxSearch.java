package lux.functions;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import lux.index.XmlIndexer;
import lux.index.field.XmlField;
import lux.saxon.Config;
import lux.saxon.ResultIterator;
import lux.saxon.Saxon;
import lux.xpath.FunCall;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;

import org.apache.lucene.queryParser.surround.parser.ParseException;
import org.apache.lucene.queryParser.surround.parser.QueryParser;
import org.apache.lucene.queryParser.surround.query.BasicQueryFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.xmlparser.CoreParser;
import org.apache.lucene.xmlparser.ParserException;

/**
 * Executes a Lucene search query and returns documents.
 * 
 */
public class LuxSearch extends ExtensionFunctionDefinition {
    
    private BasicQueryFactory queryFactory;
    private QueryParser surroundQueryParser;
    private org.apache.lucene.queryParser.QueryParser queryParser;
    private CoreParser xmlQueryParser;
    
    public LuxSearch () {
    }
    
    private Query parseQuery(String queryString, long facts, Saxon saxon) throws ParseException, org.apache.lucene.queryParser.ParseException, ParserException {
        Query query;
        XmlIndexer indexer = saxon.getIndexer();
        if (indexer.isOption(XmlIndexer.INDEX_PATHS)) {
            /*
            SrndQuery q = getSurroundQueryParser().parse2(queryString);
            query = q.makeLuceneQueryFieldNoBoost(XmlField.PATH.getName(), getQueryFactory());
            */
            query = getXmlQueryParser().parse(new ByteArrayInputStream(queryString.getBytes()));
        } else {
            query = getQueryParser().parse(queryString);
        }
        return query;
    }
    
    @Override
    public int getMinimumNumberOfArguments() {
        return 1;
    }
    
    @Override
    public int getMaximumNumberOfArguments() {
        return 2;
    }
    
    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[] { 
                SequenceType.SINGLE_STRING,
                SequenceType.OPTIONAL_INTEGER
                };
    }
    
    @Override
    public StructuredQName getFunctionQName() {
        return new StructuredQName("lux", FunCall.LUX_NAMESPACE, "search");
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.makeSequenceType(NodeKindTest.DOCUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new LuxSearchCall ();
    }
    

    /**
     * Iterate over the search results
     *
     * @param query the query to execute
     * @param saxon 
     * @return an iterator with the results of executing the query and applying the
     * expression to its result.
     * @throws XPathException
     */        
    @SuppressWarnings("rawtypes")
    public SequenceIterator<Item> iterate(final Query query, Saxon saxon, long facts) throws XPathException {        
        try {
            return new ResultIterator (saxon, query);
        } catch (IOException e) {
            throw new XPathException (e);
        }
    }
    
    class LuxSearchCall extends ExtensionFunctionCall {
        
        @SuppressWarnings("rawtypes") @Override
        public SequenceIterator<? extends Item> call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            
            if (arguments.length == 0 || arguments.length > 2) {
                throw new XPathException ("wrong number of arguments");
            }
            Item arg = arguments[0].next();        
            if (arg == null) {
                throw new XPathException ("search arg is null");
            }
            String queryString = arg.getStringValue();
            long facts=0;
            if (arguments.length >= 2) {
                IntegerValue num  = (IntegerValue) arguments[1].next();
                facts = num.longValue();
            }
            Saxon saxon = ((Config)context.getConfiguration()).getSaxon();
            Query query;
            try {
                query = parseQuery(queryString, facts, saxon);
            } catch (ParseException e) {
                throw new XPathException ("Failed to parse surround query " + queryString, e);
            } catch (org.apache.lucene.queryParser.ParseException e) {
                throw new XPathException ("Failed to parse lucene query " + queryString, e);
            } catch (ParserException e) {
                throw new XPathException ("Failed to parse xml query " + queryString, e);
            }
            //System.out.println ("executing xpath query: " + query);
            return iterate (query, saxon, facts);
        }
        
    }
    
    protected BasicQueryFactory getQueryFactory () {
        if (queryFactory == null) {
            queryFactory = new BasicQueryFactory();
        }
        return queryFactory;
    }
    
    protected QueryParser getSurroundQueryParser () {
        if (surroundQueryParser == null) {
            surroundQueryParser = new QueryParser ();//Version.LUCENE_34, null, new WhitespaceAnalyzer(Version.LUCENE_34));
        }
        return surroundQueryParser;
    }
    
    protected org.apache.lucene.queryParser.QueryParser getQueryParser () {
        if (queryParser == null) {
            queryParser = new org.apache.lucene.queryParser.QueryParser (XmlIndexer.LUCENE_VERSION, null, XmlField.FULL_TEXT.getAnalyzer());
        }
        return queryParser;
    }
    
    protected CoreParser getXmlQueryParser () {
        if (xmlQueryParser == null) {
            xmlQueryParser = new CoreParser("", XmlField.FULL_TEXT.getAnalyzer());
        }
        return xmlQueryParser;
    }
    
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
