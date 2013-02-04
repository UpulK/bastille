/*
 * This file is part of the Bastille library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.bastille.recaptcha;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.servlet.HttpContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A simple object encapsulating the ReCaptcha configuration.
 *
 * @author Christophe Lauret
 * @version Bastille 0.8.4 - 1 Feb 2013
 */
public final class ReCaptcha {

  /**
   * A logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ReCaptcha.class);

  /**
   * The default URL to connect to.
   */
  public static final String DEFAULT_HTTP_SERVER = "http://api.recaptcha.net";

  /**
   * The default secure URL to connect to.
   */
  public static final String DEFAULT_HTTPS_SERVER = "https://api-secure.recaptcha.net";

  /**
   * The verify URL to use.
   */
  public static final String VERIFY_URL = "http://api-verify.recaptcha.net/verify";

  /**
   * ReCaptcha public key.
   */
  private final String _publicKey;

  /**
   * ReCaptcha private key.
   */
  private final String _privateKey;

  /**
   * The URL of the reCaptcha server.
   */
  private final String _server;

  /**
   * Create a new ReCaptcha instance.
   *
   * @param publicKey  The public key.
   * @param privateKey THe private key.
   */
  private ReCaptcha(String publicKey, String privateKey, boolean secure) {
    this._publicKey = publicKey;
    this._privateKey = privateKey;
    this._server = secure? DEFAULT_HTTPS_SERVER : DEFAULT_HTTP_SERVER;
  }

  /**
   * Create a new ReCaptcha instance.
   *
   * @param publicKey  The public key.
   * @param privateKey The private key.
   * @param server     The URL of the reCaptcha server
   */
  private ReCaptcha(String publicKey, String privateKey, String server) {
    this._publicKey = publicKey;
    this._privateKey = privateKey;
    this._server = server;
  }

  /**
   * Returns the public key to connect to the ReCapatcha server..
   *
   * @return the public key to connect to the ReCapatcha server..
   */
  public String publicKey() {
    return this._publicKey;
  }

  /**
   * Returns the private key to connect to the ReCapatcha server.
   *
   * @return the private key to connect to the ReCapatcha server..
   */
  public String privateKey() {
    return this._privateKey;
  }

  /**
   * Returns the URL of the reCaptcha server.
   *
   * @return the URL of the reCaptcha server
   */
  public String server() {
    return this._server;
  }

  /**
   * Invoke the reCaptcha Verify API to check whether the challenge has been passed by the user.
   *
   * @param remoteAddr
   * @param challenge
   * @param response
   *
   * @return The results of the reCaptcha challenge
   *
   * @throws ReCaptchaException
   */
  public ReCaptchaResult verify(String remoteAddr, String challenge, String response) throws ReCaptchaException {
    if (challenge == null)
      return new ReCaptchaResult(false, "recaptcha-missing-challenge");
    if (remoteAddr == null)
      return new ReCaptchaResult(false, "recaptcha-missing-remoteaddress");
    if (response == null)
      return new ReCaptchaResult(false, "recaptcha-missing-response");
    String postParameters = "privatekey=" + encode(this._privateKey)
                          + "&remoteip=" + encode(remoteAddr)
                          + "&challenge=" + encode(challenge)
                          + "&response=" + encode(response);
    LOGGER.debug("Verifying response '{}' on server {} ", response, VERIFY_URL);
    String message = getDataFromPost(VERIFY_URL, postParameters);
    ReCaptchaResult result = ReCaptchaResult.parse(message);
    LOGGER.debug("Server replied {}:{} ", result.isValid(), result.message());
    return result;
  }

  /**
   * Writes the form to display the captcha as XHTML
   *
   * @param errorMessage
   * @return
   */
  public void toXHTMLForm(XMLWriter xml, String message) throws IOException {

    String errorPart = message == null ? "" : "&amp;error=" + encode(message);

    // JavaScript
    xml.openElement("script");
    xml.attribute("type", "text/javascript");
    xml.attribute("src", this._server + "/challenge?k=" + this._publicKey + errorPart);
    xml.closeElement();

    // NoJavaScript
    xml.openElement("noscript");
    xml.openElement("iframe");
    xml.attribute("src", this._server+"/noscript?k="+this._publicKey + errorPart);
    xml.attribute("height", "300");
    xml.attribute("width", "500");
    xml.attribute("frameborder", "0");
    xml.closeElement();
    xml.emptyElement("br");
    xml.openElement("textarea");
    xml.attribute("name", "recaptcha_challenge_field");
    xml.attribute("rows", "3");
    xml.attribute("cols", "40");
    xml.closeElement();
    xml.openElement("input");
    xml.attribute("type", "hidden");
    xml.attribute("name", "recaptcha_response_field");
    xml.attribute("value", "manual_challenge");
    xml.closeElement();
    xml.closeElement();
  }

  // Factory methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates a new ReCaptcha instance from the Bastille properties.
   *
   * <ul>
   *  <li><code>bastille.recaptcha.public-key</code></li>
   *  <li><code>bastille.recaptcha.private-key</code></li>
   *  <li><code>bastille.recaptcha.server</code></li>
   *  <li><code>bastille.recaptcha.secure</code></li>
   * </ul>
   *
   * @return a new ReCaptcha instance
   *
   * @throws ReCaptchaException If the server has not been configured properly
   */
  public static ReCaptcha newReCaptcha() throws ReCaptchaException {
    String publicKey = GlobalSettings.get("bastille.recaptcha.public-key");
    String privateKey = GlobalSettings.get("bastille.recaptcha.private-key");
    if (publicKey == null)
      throw new ReCaptchaException("Property 'bastille.recaptcha.public-key' is null");
    if (privateKey == null)
      throw new ReCaptchaException("Property 'bastille.recaptcha.private-key' is null");
    String server = GlobalSettings.get("bastille.recaptcha.server");
    ReCaptcha recaptcha;
    if (server != null) {
      recaptcha = new ReCaptcha(publicKey, privateKey, server);
    } else {
      boolean secure = GlobalSettings.get("bastille.recaptcha.secure", false);
      recaptcha = new ReCaptcha(publicKey, privateKey, secure);
    }
    return recaptcha;
  }

  /**
   * Automatically create a ReCaptcha instance and verify the standard paramaters send
   *
   * @param req The content request.
   *
   * @return a new ReCaptcha instance
   *
   * @throws ReCaptchaException If the server has not been configured properly
   */
  public static ReCaptchaResult verify(ContentRequest req) throws ReCaptchaException {
    ReCaptcha recaptcha = newReCaptcha();
    String challenge = req.getParameter("recaptcha_challenge_field");
    String response = req.getParameter("recaptcha_response_field");
    if (req instanceof HttpContentRequest) {
      HttpContentRequest http = (HttpContentRequest)req;
      return recaptcha.verify(http.getHttpRequest().getRemoteAddr(), challenge, response);
    } else {
      throw new ReCaptchaException("Unable to get remote IP");
    }
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Make an HTTP Post request to the specified URL.
   *
   * @param urlS
   * @param postdata The POST data
   *
   * @return The corresponding data
   *
   * @throws ReCaptchaException
   */
  private static String getDataFromPost(String urlS, String postdata) throws ReCaptchaException {
    InputStream in = null;
    URLConnection connection = null;
    try {
      // Initialise connection
      URL url = new URL(urlS);
      connection = url.openConnection();
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setReadTimeout(10000);
      connection.setConnectTimeout(10000);

      // Write POST data
      OutputStream out = connection.getOutputStream();
      out.write(postdata.getBytes());
      out.flush();

      in = connection.getInputStream();

      // Read response
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      while (true) {
        int rc = in.read(buf);
        if (rc <= 0)
          break;
        else
          bout.write(buf, 0, rc);
      }

      out.close();

      return bout.toString();
    } catch (IOException ex) {
      LOGGER.warn("Cannot load URL", ex);
      throw new ReCaptchaException("Cannot load URL: " + ex.getMessage(), ex);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Encodes the specified string for the URL
   *
   * @param s The string to encode
   *
   * @return The encoded string.
   */
  private static String encode(String s) {
    try {
      return URLEncoder.encode(s, "utf-8");
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

}