/*
 * This file is part of the Bastille library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.bastille.cache.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for GZIP compression.
 *
 * @see <a href="http://tools.ietf.org/html/rfc1952">GZIP file format specification version 4.3</a>
 *
 * @author Christophe Lauret
 * @version Bastille 0.8.3 - 27 January 2013
 */
public final class GZIPUtils {

  /** Useful debug info goes here */
  private static final Logger LOGGER = LoggerFactory.getLogger(GZIPUtils.class);

  /** For use as default buffer size. */
  private static final int FOUR_KB = 4196;

  /** The first byte of a GZIP byte array */
  private static final int GZIP_ID1_BYTE = 31;

  /** The second byte of a GZIP byte array */
  private static final int GZIP_ID2_BYTE = -117;

  /**
   * Empty content takes 20 bytes when gzipped (10-byte header, 2 bytes and 8-byte footer with CRC)
   *
   * <p>We use this to determine whether the content will be empty when ungzipped.
   */
  private static final int EMPTY_GZIPPED_CONTENT_SIZE = 20;

  /**
   * Utility class.
   */
  private GZIPUtils() {
  }

  /**
   * Checks the first two bytes of the candidate byte array for the magic number 0x677a.
   *
   * <p>This magic number was obtained from /usr/share/file/magic. The line for gzip is:
   * <pre>
   * >>14    beshort 0x677a          (gzipped)
   * </pre>
   *
   * @param candidate the byte array to check
   * @return <code>true</code> if gzipped;
   *         <code>false</code> if <code>null</code>, less than two bytes or not gzipped.
   */
  public static boolean isGzipped(byte[] candidate) {
    if (candidate == null || candidate.length < 2) {
      return false;
    } else {
      return (candidate[0] == GZIP_ID1_BYTE && candidate[1] == GZIP_ID2_BYTE);
    }
  }

  /**
   * Checks whether a gzipped body is actually empty and should just be zero.
   *
   * <p>When the compressedBytes is {@link #EMPTY_GZIPPED_CONTENT_SIZE} it should be zero.
   *
   * @param compressed the gzipped response body
   * @param request the client HTTP request
   *
   * @return <code>true</code> if the response should be 0, even if it is isn't.
   */
  public static boolean shouldGzippedBodyBeZero(byte[] compressed, HttpServletRequest request) {
    // Check for 0 length body
    if (compressed.length == EMPTY_GZIPPED_CONTENT_SIZE) {
      LOGGER.debug("{} resulted in an empty response.", request.getRequestURL());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Adds the gzip HTTP header to the response to indicates to clients that the response body is compressed.
   *
   * <p>This method checks that the header has actually been included.
   *
   * @param res the response which will have a header added to it
   *
   * @throws ServletException if the response is committed or set header is ignored.
   */
  public static void addGzipHeader(HttpServletResponse res) throws ServletException {
    res.setHeader("Content-Encoding", "gzip");
    boolean containsEncoding = res.containsHeader("Content-Encoding");
    if (!containsEncoding) {
      throw new ServletException("Failure when attempting to set Content-Encoding: gzip");
    }
  }

  /**
   * Gzip the specified content.
   *
   * @param ungzipped the bytes to be gzipped
   *
   * @return gzipped bytes
   *
   * @throws IOException the content was already compressed.
   */
  public static byte[] gzip(byte[] ungzipped) throws IOException {
    if (isGzipped(ungzipped)) {
      throw new IOException("Attempted to gzipped content that is already gzipped.");
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(bytes);
    gzip.write(ungzipped);
    gzip.close();
    return bytes.toByteArray();
  }

  /**
   * A highly performant ungzip implementation.
   *
   * @param gzipped the gzipped content
   *
   * @return an ungzipped byte[]
   *
   * @throws IOException Should an error occur while ungzipping the content
   */
  public static byte[] ungzip(byte[] gzipped) throws IOException {
    ByteArrayOutputStream uncompressed = new ByteArrayOutputStream(gzipped.length);
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipped));
    final byte[] buffer = new byte[FOUR_KB];
    int bytesRead = 0;
    while (bytesRead != -1) {
      bytesRead = gzip.read(buffer, 0, FOUR_KB);
      if (bytesRead != -1) {
        uncompressed.write(buffer, 0, bytesRead);
      }
    }
    byte[] ungzipped = uncompressed.toByteArray();
    gzip.close();
    uncompressed.close();
    return ungzipped;
  }

}
