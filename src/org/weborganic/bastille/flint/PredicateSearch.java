/*
 * This file is part of the Bastille library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.bastille.flint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.weborganic.bastille.flint.config.FlintConfig;
import org.weborganic.bastille.flint.helpers.IndexMaster;
import org.weborganic.bastille.flint.helpers.MultiSearchResults;
import org.weborganic.bastille.flint.helpers.MultipleIndex;
import org.weborganic.bastille.util.Errors;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.flint.IndexException;
import org.weborganic.flint.query.PredicateSearchQuery;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchResults;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Performs a search using a Lucene predicate.
 *
 * <p>Note: this generator is not designed to be used for production but can be useful during
 * development to determine which query should be used.
 *
 * @author Christophe Lauret
 * @version 0.6.20 - 26 September 2011
 * @since 0.6.20
 */
@Beta
public final class PredicateSearch implements ContentGenerator  {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // Create a new query object using HTTP parameters
    String predicate = req.getParameter("predicate", "");
    if (predicate.isEmpty()) {
      Errors.noParameter(req, xml, "predicate");
      return;
    }

    // All predicates must include ':'
    boolean valid = true;
    for (String p : predicate.split("\\s+")) {
      if (p.indexOf(':') == -1) {
        valid = false;
      }
    }
    if (!valid) {
      Errors.invalidParameter(req, xml, "predicate");
      return;
    }

    // Create a query from the predicate
    PredicateSearchQuery query =  new PredicateSearchQuery(predicate);
    Query q = query.toQuery();
    if (q == null) {
      Errors.invalidParameter(req, xml, "predicate");
      return;
    }

    // Initialise the paging setting
    SearchPaging paging = new SearchPaging();
    int page = req.getIntParameter("page", 1);
    paging.setPage(page);
    paging.setHitsPerPage(100);

    // Get search result
    try {

      // get index to search on
      String index = req.getParameter("index");
      if (index == null) {
        IndexMaster central = FlintConfig.getMaster();
        SearchResults results = central.query(query, paging);

        // print the result
        xml.openElement("index-search", true);
        query.toXML(xml);
        results.toXML(xml);
        xml.closeElement();

      } else {
        // find all indexes specified
        String[] indexeNames = index.split(",");
        List<File> indexDirectories = new ArrayList<File>();
        for (String ind : indexeNames) {
          indexDirectories.add(req.getEnvironment().getPrivateFile("index/"+ind));
        }
        MultipleIndex indexes = new MultipleIndex(indexDirectories);
        // run query
        MultiSearchResults results = indexes.query(query, paging);

        // print the result
        xml.openElement("index-search", true);
        query.toXML(xml);
        results.toXML(xml);
        xml.closeElement();
      }

    } catch (IndexException ex) {
      throw new BerliozException("Fail to retrieve search result using query: "+query.toString(), ex);
    }
  }

}
