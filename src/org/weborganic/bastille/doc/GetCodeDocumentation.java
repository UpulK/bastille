package org.weborganic.bastille.doc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.weborganic.bastille.util.Errors;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.cobble.CobbleException;
import org.weborganic.cobble.XMLGenerator;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the XSLT documentation using the Cobble format
 *
 * @author Christophe Lauret
 *
 */
public final class GetCodeDocumentation implements ContentGenerator, Cacheable {

  @Override
  public String getETag(ContentRequest req) {
    return null;
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    String path = req.getParameter("path");
    if (path == null) {
      Errors.noParameter(req, xml, "path");
      return;
    }

    Environment env = req.getEnvironment();
    File code = env.getPrivateFile(path);

    if (!XMLGenerator.isSupported(path) || !code.exists()) {
      Errors.invalidParameter(req, xml, "path");
      return;
    }

    // Generate the document
    XMLGenerator docgen = new XMLGenerator(code);
    try {
      StringWriter w = new StringWriter();
      docgen.generate(w);
      xml.writeXML(w.toString());
    } catch (CobbleException ex) {
      Errors.error(req, xml, "server", ex.getMessage(), ContentStatus.INTERNAL_SERVER_ERROR);
    }

  }

}