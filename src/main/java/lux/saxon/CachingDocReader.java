package lux.saxon;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lux.index.XmlIndexer;
import lux.index.field.XmlField;
import lux.saxon.Saxon.SaxonBuilder;
import net.sf.saxon.s9api.XdmNode;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexReader;

/**
 * Keeps an association from docID->document.  This should be dropped when the reader is.
 * Later on, we might want a longer-lived cache, but for now this is really only useful
 * for a single query.  There is no expiration - once a document is cached, it is retained for 
 * the lifetime of the cache. This is needed to ensure document identity is preserved within the
 * context of a query.
 *
 */
public class CachingDocReader {
    private final HashMap <Integer, XdmNode> cache = new HashMap<Integer, XdmNode>();
    private final String xmlFieldName;
    private final String uriFieldName;
    private final FieldSelector fieldSelector;
    private final IndexReader reader;
    private final SaxonBuilder builder;
    private int cacheHits=0;
    private int cacheMisses=0;
    private long buildTime=0;
    
    public CachingDocReader (IndexReader reader, SaxonBuilder builder, XmlIndexer indexer) {
        this.reader = reader;
        this.builder = builder;
        this.xmlFieldName = XmlField.XML_STORE.getName();
        this.uriFieldName = XmlField.URI.getName();
        HashSet<String> fieldNames = new HashSet<String>();
        fieldNames.add(xmlFieldName);
        fieldNames.add(uriFieldName);
        Set<String> empty = Collections.emptySet();
        fieldSelector = new SetBasedFieldSelector(fieldNames, empty);
    }
    
    public XdmNode get (int docID) throws IOException {
        if (cache.containsKey(docID)) {
            ++cacheHits;
            return cache.get(docID);
        }
        Document document;
        document = reader.document(docID, fieldSelector);
        String xml = document.get(xmlFieldName);
        String uri = document.get(uriFieldName);
        long t0 = System.nanoTime();
        XdmNode node = (XdmNode) builder.build(new StringReader (xml), uri, docID);
        buildTime += (System.nanoTime() - t0);
        if (node != null) {
            cache.put(docID, node);
        }
        ++cacheMisses;
        return node;
    }
    
    public final boolean isCached (final int docID) {
        return cache.containsKey(docID);
    }

    /**
     * @return the number of items retrieved from the cache
     */
    public int getCacheHits() {
        return cacheHits;
    }
    /**
     * @return the number of items retrieved and added to the cache
     */
    public int getCacheMisses() {
        return cacheMisses;
    }
    
    /**
     * @return the total time spent building documents (in nanoseconds).  This includes time spent
     * parsing and constructing a Saxon NodeInfo/XdmNode.
     */
    public long getBuildTime() {
        return buildTime;
    }

    public void clear() {
        cache.clear();
    }

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
