package com.weborganic.bastille.flint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.util.ISO8601;
import org.weborganic.flint.util.FileCollector;

import com.topologi.diffx.xml.XMLWriter;
import com.weborganic.bastille.flint.helpers.FilePathRule;
import com.weborganic.bastille.flint.helpers.IndexMaster;
import com.weborganic.bastille.flint.helpers.IndexUpdateFilter;
import com.weborganic.bastille.flint.helpers.IndexUpdateFilter.Action;

/**
 * List the files corresponding to the specified directory.
 * 
 * <p>This content generator is not cacheable because it causes the index to be updated using a
 * separate thread.
 * 
 * <p>The index must be located in the '/index' directory. 
 * The IXML stylesheet must be 'ixml/default.xsl'.

 * 
 * <p>Note: access to this is generator should be made secured in the Web descriptor.   
 * 
 * @author Christophe Lauret 
 * @version 26 July 2010
 */
public class GenerateIndex extends ContentGeneratorBase implements ContentGenerator  {

  /**
   * Logger for debugging
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndex.class);

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    // Getting the index
    final Environment env = req.getEnvironment();
    IndexMaster master = IndexMaster.getInstance();
    long modified = 0;
    List<File> indexed = new ArrayList<File>();
    
    // Set up the Index
    if (!master.isSetup()) {
      LOGGER.debug("Setting up Index.");
      master.setup(env.getPrivateFile("index"), env.getPrivateFile("ixml/default.xsl"));
    } else {
      modified = master.lastModified();
      indexed.addAll(master.list(new Term("visibility", "private"), "path"));
    }

    // Scanning the directory
    File root = env.getPrivateFile("xml");
    LOGGER.debug("Scanning XML directory.");

    IndexUpdateFilter filter = new IndexUpdateFilter(modified, indexed);
    int updated = FileCollector.list(root, filter).size();
    Map<File, Action> files = filter.getActions();

    // Send the files for indexing
    xml.openElement("index-job", true);
    xml.attribute("last-modified", ISO8601.format(modified, ISO8601.DATETIME));
    for (Entry<File, Action> entry : files.entrySet()) {
      File f = entry.getKey();
      Action action = entry.getValue();
      String path = FilePathRule.toPath(entry.getKey());
      toXML(xml, path, ISO8601.format(f.lastModified(), ISO8601.DATETIME), action.toString());
      // Parameters send to iXML
      if (action == Action.INSERT || action == Action.UPDATE) {
        Map<String, String> p = new HashMap<String, String>();
        p.put("path", path);
        p.put("visibility", "private");
        p.put("last-modified", ISO8601.format(f.lastModified(), ISO8601.DATETIME));
        master.index(f, p);
      } else if (action == Action.DELETE) {
        Map<String, String> p = Collections.emptyMap();
        master.index(f, p);
      }
    }
    LOGGER.debug("{} files queued for indexing", updated);
    xml.closeElement();
  }

  /**
   * XML for a file to be indexed 
   * @param xml  the XML writer
   * @param path the path to the file.
   * @throws IOException If thrown by the xml writer
   */
  public void toXML(XMLWriter xml, String path, String modified, String action) throws IOException {
    xml.openElement("file");
    xml.attribute("path", path);
    xml.attribute("last-modified", modified);
    xml.attribute("action", action);
    xml.closeElement();
  }

  public List<File> toFiles(File root, List<String> paths) {
    List<File> files = new ArrayList<File>();
    for (String path : paths) {
      files.add(new File(root, path));
    }
    return files;
  }

  /**
   * Returns the specified date as ISO 8601 format.
   *  
   * @deprecated Use {@link ISO8601#format(long, ISO8601)} instead.
   *  
   * @param date the specified date.
   * @return the date formatted using ISO 8601.
   */
  public static String toISO8601(long date) {
    return ISO8601.format(date, ISO8601.DATETIME);
  }

}
