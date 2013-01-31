/*
 * This file is part of the Bastille library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.bastille.cache.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * A interface for cached resources.
 *
 * @author Christophe Lauret
 *
 * @version Bastille 0.8.3 - 30 January 2013
 */
public interface CachedResource {

  /**
   * @return the content type of the response.
   */
  String getContentType();

  /**
   * @return the HTTP status code of the response.
   */
  int getStatusCode();

  /**
   * Returns the body content as bytes.
   *
   * <p>If the content is gzippable, this method will unzip the content on demand.
   *
   * <p>If the content is not gzippable, this method will trown a
   *
   * @param gzipped <code>true</code> to request the gzipped content;
   *                <code>false</code> to get the raw content.
   *
   * @return the ungzipped version of the body.
   *
   * @throws IOException if thrown while ungzipping the content.
   */
  byte[] getBody(boolean gzipped) throws IOException;

  /**
   * @return <code>true</code> if the body is not <code>null</code> and would not result in a 0-length content.
   */
  boolean hasContent();

  /**
   * @return <code>true</code> if there is a non <code>null</code> gzipped body
   */
  boolean hasGzippedBody();

  /**
   * Returns <code>true</code> if the response is OK (200).
   *
   * @return <code>true</code> if the status code is 200;
   *         <code>false</code> for any other code.
   */
  boolean isOK();

  /**
   * Returns the last modified date for this resource.
   *
   * @return the last modified date if there is an "Last-Modified" header defined; -1 otherwise.
   */
  long getLastModified();

  /**
   * Returns the etag for this resource.
   *
   * @param gzipped <code>true</code> to get the etag for a gzipped content;
   *                <code>false</code> to get the etag for raw content.
   *
   * @return the etag if there is an "Etag" header defined; <code>null</code> otherwise.
   */
  String getETag(boolean gzipped);

  /**
   * @return All of the headers set on the page.
   * @param gzipped      <code>true</code> if the content is sent gzipped;
   *                     <code>false</code> otherwise.
   */
  List<HttpHeader<? extends Serializable>> getHeaders(boolean gzipped);

  /**
   * Copy the headers to the HTTP servlet response.
   *
   * @param res          The HTTP servlet response where the headers should be copied.
   * @param gzipped      <code>true</code> if the content is sent gzipped;
   *                     <code>false</code> otherwise.
   */
  void copyHeadersTo(HttpServletResponse res, boolean gzipped);

}
