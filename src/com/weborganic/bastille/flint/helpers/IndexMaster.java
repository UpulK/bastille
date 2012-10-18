/*
 * This file is part of the Bastille library.
 *
 * Available under a commercial licence, contact Weborganic.
 *
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package com.weborganic.bastille.flint.helpers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.flint.Index;
import org.weborganic.flint.IndexConfig;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.IndexJob.Priority;
import org.weborganic.flint.IndexManager;
import org.weborganic.flint.Requester;
import org.weborganic.flint.content.AutoXMLTranslatorFactory;
import org.weborganic.flint.content.Content;
import org.weborganic.flint.content.ContentFetcher;
import org.weborganic.flint.content.ContentId;
import org.weborganic.flint.local.LocalFileContentId;
import org.weborganic.flint.local.LocalFileContentType;
import org.weborganic.flint.local.LocalIndex;
import org.weborganic.flint.log.SLF4JListener;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchQuery;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.query.SuggestionQuery;
import org.weborganic.flint.search.Facet;
import org.weborganic.flint.search.FieldFacet;
import org.weborganic.flint.util.Terms;

import com.weborganic.bastille.psml.PSMLConfig;

/**
 * Centralises all the indexing and searching function using Flint for one index.
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 * @version 0.7.4 - 17 October 2012
 * @since 0.6.0
 */
public final class IndexMaster {

  /**
   * A logger for this class and to provide for Flint.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexMaster.class);

  /**
   * The requester is always the index master.
   */
  private static final Requester REQUESTER = new Requester() {
    @Override
    public String getRequesterID() {
      return "IndexMaster";
    }
  };

  private static AnalyzerProvider analyzerProvider = null;

  /**
   * The IXML templates used for processing XML in this index.
   */
  private final URI itemplates;

  /**
   * Where the private files are.
   */
  private volatile IndexManager manager = null;

  /**
   * The underlying index used by this generator.
   */
  private volatile LocalIndex index = null;

  /**
   * The index config used by this generator.
   */
  private volatile IndexConfig config = null;

  /**
   * The last time this index was modified
   */
  private volatile long lastModified = -1;

  /**
   * Sets up the index master.
   *
   * @param indexDir the index directory
   */
  public IndexMaster(File indexDir) {
    this(indexDir, null);
  }

  /**
   * Sets up the index master.
   *
   * @param indexDir the index directory
   * @param xslt     the location of the XSLT generating the IXML.
   */
  public IndexMaster(File indexDir, File xslt) {

    ContentFetcher fetcher = new ContentFetcher() {
      @Override
      public Content getContent(ContentId id) {
        LocalFileContentId fid = (LocalFileContentId)id;
        return new FileContent(fid.file());
      }
    };

    // Create the index
    this.index = new LocalIndex(indexDir, getNewAnalyzer());

    // Get the last modified from the index
    this.lastModified = this.index.getLastModified();

    // Create a Manager
    this.manager = new IndexManager(fetcher, new SLF4JListener(LOGGER));
    List<String> psml = Collections.singletonList(PSMLConfig.MEDIATYPE);
    this.manager.registerTranslatorFactory(new AutoXMLTranslatorFactory(psml));

    // Setup the configuration
    this.config = new IndexConfig();
    this.itemplates = xslt.toURI();

    this.config.setTemplates(LocalFileContentType.SINGLETON, "text/xml", this.itemplates);
    this.config.setTemplates(LocalFileContentType.SINGLETON, PSMLConfig.MEDIATYPE, this.itemplates);

    // Start the index manager
    this.manager.start();
  }

  /**
   * Set the new Analyzer Provider.
   * @param providore the new Analyzer Provider.
   */
  public static void setAnalyzerProvider(AnalyzerProvider providore) {
    analyzerProvider = providore;
  }

  /**
   * @return an analyzer similar to the one used in all indexes
   */
  public static Analyzer getNewAnalyzer() {
    if (analyzerProvider == null)
      return new StandardAnalyzer(Version.LUCENE_30);
    return analyzerProvider.getAnalyzer();
  }

  /**
   * Returns the index instance this class operates on.
   *
   * @return the index instance this class operates on.
   */
  public Index index() {
    return this.index;
  }

  /**
   * Returns the index config this class operates on.
   *
   * @return the index config this class operates on.
   */
  public IndexConfig config() {
    return this.config;
  }

  /**
   * Returns the underlying Index Manager.
   *
   * @return the underlying Index Manager.
   */
  public IndexManager manager() {
    return this.manager;
  }

  /**
   * Index the specified file with the given parameters.
   *
   * @param file       The file to index.
   * @param parameters The parameters to pass to the stylesheet.
   */
  public void index(File file, Map<String, String> parameters) {
    ContentId cid = new LocalFileContentId(file);
    this.lastModified = System.currentTimeMillis();
    this.manager.index(cid, this.index, this.config, REQUESTER, Priority.HIGH, parameters);
  }

  /**
   * Clears the content of the wrapped index.
   */
  public void clear() {
    this.manager.clear(this.index, REQUESTER, Priority.HIGH);
  }

  /**
   * Makes a query to the wrapped index.
   *
   * @param query The query to make
   * @return The search results
   *
   * @throws IndexException Should any error while the query is made.
   */
  public SearchResults query(SearchQuery query) throws IndexException {
    return this.manager.query(this.index, query);
  }

  /**
   * Makes a query to the wrapped index.
   *
   * @param query  The query to make
   * @param paging The paging configuration
   *
   * @return The search results
   *
   * @throws IndexException Should any error while the query is made.
   */
  public SearchResults query(SearchQuery query, SearchPaging paging) throws IndexException {
    return this.manager.query(this.index, query, paging);
  }

  /**
   * Returns the last time an index job was requested or if none was requested the last time
   * the index was updated.
   *
   * @return the last time an index job was requested.
   */
  public long lastModified() {
    return this.lastModified;
  }

  /**
   * Reload the IXML templates.
   */
  public void reloadTemplates() {
    this.config.setTemplates(LocalFileContentType.SINGLETON, "text/xml", this.itemplates);
    this.config.setTemplates(LocalFileContentType.SINGLETON, "application/vnd.pageseeder.psml+xml", this.itemplates);
  }

  /**
   * Returns the list of term and how frequently they are used by performing a fuzzy match on the
   * specified term.
   *
   * @param field  the field to use as a facet
   * @param upTo   the max number of values to return
   * @param query  a predicate to apply on the facet (can be null or empty)
   *
   * @throws IndexException if there was an error reading the index or creating the condition query
   */
  public Facet getFacet(String field, int upTo, Query query) throws IndexException, IOException {
    FieldFacet facet = null;
    IndexReader reader = null;
    IndexSearcher searcher = null;
    try {
      // Retrieve all terms for the field
      reader = this.manager.grabReader(this.index);
      facet = FieldFacet.newFacet(field, reader);

      // search
      searcher = this.manager.grabSearcher(this.index);
      facet.compute(searcher, query, upTo);

    } finally {
      this.manager.releaseQuietly(this.index, reader);
      this.manager.releaseQuietly(this.index, searcher);
    }
    return facet;
  }

  /**
   * Return an index reader on the index.
   *
   * @return The index will need to be released.
   */
  public IndexReader grabReader() throws IndexException {
    return this.manager.grabReader(this.index);
  }

  /**
   * Return an index searcher on the index.
   *
   * @return The index searcher that will need to be released.
   */
  public IndexSearcher grabSearcher() throws IndexException {
    return this.manager.grabSearcher(this.index);
  }

  /**
   * Returns the list of files containing ??
   */
  public List<File> list(Term t) throws IOException {
    List<File> files = new ArrayList<File>();
    IndexReader reader = null;
    try {
      reader = this.manager.grabReader(this.index);
      for (int i = 0; i < reader.numDocs(); i++) {
        File f = FilePathRule.toFile(reader.document(i));
        files.add(f);
      }
    } catch (IndexException ex) {
      throw new IOException(ex);
    } finally {
      this.manager.releaseQuietly(this.index, reader);
    }
    return files;
  }

  /**
   * Suggests results for the given fields and text.
   *
   * @param fields    The list of fields to use.
   * @param texts     The list of term texts to use.
   * @param max       The maximum number of suggested results.
   * @param predicate By default, assumes that it is the document type.
   *
   * @return the suggestions in the form of search results.
   */
  public SearchResults getSuggestions(List<String> fields, List<String> texts, int max, String predicate) throws IOException, IndexException {
    // Generate the list of terms
    List<Term> terms = Terms.terms(fields, texts);

    // Parse the condition query
    Query condition = toQuery(predicate);

    // Creates the suggestion query
    SuggestionQuery query = new SuggestionQuery(terms, condition);
    IndexReader reader = null;
    try {
      reader = this.manager.grabReader(this.index);
      query.compute(reader);
    } finally {
      this.releaseSilently(reader);
    }

    // Pages
    SearchPaging pages = new SearchPaging();
    if (max > 0) pages.setHitsPerPage(max);
    return this.manager.query(this.index, query, pages);
  }

  /**
   * Returns the query for the specified predicate using the Query Parser.
   *
   * @param predicate The predicate to parse
   * @return the corresponding query object or <code>null</code>.
   * @throws IndexException should any error occur
   */
  public static Query toQuery(String predicate) throws IndexException {
    // Parse the condition query
    QueryParser parser = new QueryParser(IndexManager.LUCENE_VERSION, "type", getNewAnalyzer());
    Query condition = null;
    if (predicate != null && !"".equals(predicate)) {
      try {
        condition = parser.parse(predicate);
      } catch (ParseException ex) {
        throw new IndexException("Condition for the suggestion could not be parsed.", ex);
      }
    }
    return condition;
  }

  /**
   * Releases this reader for use by other threads silently (any exception will be ignored).
   *
   * <p>Provided for convenience when used inside a <code>finally</code> block.
   *
   * @param reader The Lucene index reader to release.
   */
  public void releaseSilently(IndexReader reader) {
    this.manager.releaseQuietly(this.index, reader);
  }

  /**
   * Releases this searcher for use by other threads silently (any exception will be ignored).
   *
   * <p>Provided for convenience when used inside a <code>finally</code> block.
   *
   * @param searcher The Lucene index searcher to release.
   */
  public void releaseSilently(IndexSearcher searcher) {
    this.manager.releaseQuietly(this.index, searcher);
  }

}
