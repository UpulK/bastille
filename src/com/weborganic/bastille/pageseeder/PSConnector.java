package com.weborganic.bastille.pageseeder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topologi.diffx.xml.XMLWriter;
import com.weborganic.bastille.pageseeder.PSConnection.Type;
import com.weborganic.bastille.security.ps.PageSeederUser;

/**
 * Represents a request made to the PageSeeder Server.
 * 
 * <p>By default the request is made anonymously. In order to make a request on behalf of a 
 * PageSeeder user, use the {@link #setUser(PageSeederUser)} method - this is required for any page
 * that needs login access.
 * 
 * <p>For simple PageSeeder connections via GET or POST, this class provides convenience methods
 * which will open and close the connections and capture any error in XML.
 * 
 * <p>For example:</p>
 * <pre>
 * PSConnector connector = new PSConnector(PSResourceType.SERVICE, "/groups/123/members");
 * connector.setUser(user);
 * boolean ok = connector.get(xml);
 * </pre>
 * 
 * <p>For more complex connections, involving multipart queries or if any of the default properties 
 * of the connection need to be changed, this class can be used to create the connection to 
 * PageSeeder, for example:</p>
 * <pre>
 * PSConnector connector = new PSConnector(PSResourceType.SERVICE, "/groups/123/upload");
 * connector.setUser(user);
 * 
 * PSConnection connection = connector.connect(Type.MULTIPART);
 * connection.addXMLPart(xml1);
 * connection.addXMLPart(xml2);
 * connection.addXMLPart(xml3);
 * connection.disconnect();
 * </pre>
 * 
 * @author Christophe Lauret
 * @version 30 May 2011
 */
public final class PSConnector {

  /** Logger for this class */
  private static final Logger LOGGER = LoggerFactory.getLogger(PSConnector.class);

  /**
   * The type of resource accessed.
   */
  private final PSResource.Builder _resource;

  /**
   * If specified, the request will be made on behalf of that user.
   */
  private PageSeederUser _user = null;

  /**
   * Creates a new connection to the specified resource.
   * 
   * @param type     The type of resource.
   * @param resource The 
   */
  public PSConnector(PSResourceType type, String resource) {
    this._resource = new PSResource.Builder(type, resource);
  }

  /**
   * Sets the user for this request.
   * @param user the user for this request.
   */
  public void setUser(PageSeederUser user) {
    this._user = user;
  }

  /**
   * Add a parameter to this request.
   * 
   * @param name  The name of the parameter
   * @param value The value of the parameter
   */
  public void addParameter(String name, String value) {
    this._resource.addParameter(name, value);
  }

  // Connection
  // ----------------------------------------------------------------------------------------------

  /**
   * Connect to PageSeeder using the specified method. 
   * 
   * @param type  The connection type using the specified method
   * 
   * @return The PS connection created as a result.
   * @throws IOException If thrown while trying to open the connection or if the URL for the 
   *                     underlying resource is malformed.
   */
  public PSConnection connect(Type type) throws IOException {
    PSResource r = this._resource.build();
    return PSConnection.connect(r, type, this._user);
  }

  // Simple requests
  // ----------------------------------------------------------------------------------------------

  /**
   * Connect to PageSeeder via GET.
   * 
   * @param xml the XML to copy from PageSeeder
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean get(XMLWriter xml) throws IOException {
    return simple(xml, Type.GET, null, null);
  }

  /**
   * Connect to PageSeeder and fetch the XML using the GET method. 
   * 
   * <p>If the handler is not specified, the xml writer receives a copy of the PageSeeder XML.
   * 
   * @param xml     the XML to copy from PageSeeder
   * @param handler the handler for the XML (can be used to rewrite the XML) 
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean get(XMLWriter xml, PSHandler handler) throws IOException {
    return simple(xml, Type.GET, handler, null);
  }

  /**
   * Connect to PageSeeder and fetch the XML using the GET method. 
   * 
   * <p>Templates can be specified to transform the XML. 
   * 
   * @param xml       The XML to copy from PageSeeder
   * @param templates A set of templates to process the XML (optional)
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean get(XMLWriter xml, Templates templates) throws IOException {
    return simple(xml, Type.GET, null, templates);
  }

  /**
   * Connect to PageSeeder via POST.
   * 
   * @param xml the XML to copy from PageSeeder
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean post(XMLWriter xml) throws IOException {
    return simple(xml, Type.POST, null, null);
  }

  /**
   * Connect to PageSeeder and fetch the XML using the POST method. 
   * 
   * <p>If the handler is not specified, the xml writer receives a copy of the PageSeeder XML.
   * 
   * @param xml     the XML to copy from PageSeeder
   * @param handler the handler for the XML (can be used to rewrite the XML) 
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean post(XMLWriter xml, PSHandler handler) throws IOException {
    return simple(xml, Type.POST, handler, null);
  }

  /**
   * Connect to PageSeeder and fetch the XML using the POST method. 
   * 
   * <p>Templates can be specified to transform the XML. 
   * 
   * @param xml       The XML to copy from PageSeeder
   * @param templates A set of templates to process the XML (optional)
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  public boolean post(XMLWriter xml, Templates templates) throws IOException {
    return simple(xml, Type.POST, null, templates);
  }

  /**
   * Connect to PageSeeder and fetch the XML using the GET method. 
   * 
   * <p>If the handler is not specified, the xml writer receives a copy of the PageSeeder XML.
   * 
   * <p>If templates are specified they take precedence over the handler.
   * 
   * @param xml       The XML to copy from PageSeeder
   * @param type      The type of connection
   * @param handler   The handler for the XML (can be used to rewrite the XML)
   * @param templates A set of templates to process the XML (optional)
   * 
   * @throws IOException If an error occurs when trying to write the XML.
   * 
   * @return <code>true</code> if the request was processed without errors;
   *         <code>false</code> otherwise.
   */
  private boolean simple(XMLWriter xml, Type type, PSHandler handler, Templates templates) throws IOException {
    // Build the resource
    PSResource r = this._resource.build();

    // Create the connection and catch errors
    PSConnection connection = null;
    try {
      connection = PSConnection.connect(r, type, this._user);
    } catch (MalformedURLException ex) {
      xml.openElement("ps-"+r.type().toString().toLowerCase(), true);
      xml.attribute("resource", r.name());
      LOGGER.warn("Malformed URL: {}", ex.getMessage());
      error(xml, "malformed-url", ex.getLocalizedMessage());
      xml.closeElement();
      return false;
    }

    // Process the content
    boolean ok = connection.process(xml, handler, templates);

    // Disconnect
    if (connection != null) connection.disconnect();

    // Return the final status
    return ok;
  }

  /**
   * Adds the attributes for when error occurs
   * 
   * @param xml     The XML output.
   * @param error   The error code.
   * @param message The error message. 
   * 
   * @throws IOException If thrown while writing the XML.
   */
  private static void error(XMLWriter xml, String error, String message) throws IOException {
    xml.attribute("error", error);
    xml.attribute("message", message);
  }
}