package com.weborganic.bastille.web;

import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;

/**
 * This generator returns the XML for the Navigation.
 * 
 * <h3>Configuration</h3>
 * <p>The navigation is defined at the PageSeeder level using the website properties. When 
 * the website is published the path to the file to use for the navigation is stored in
 * <code>/WEB-INF/conf/template-config.prp</code> as the <code>navigation</code> property.
 * <p>There is no reason to modify this file directly as it could be overriden when the 
 * Website is published again.
 * 
 * <h3>Parameters</h3>
 * <p>This generator does not require any parameter.<p>
 * <p>The following optional parameter can be used:
 * <table>
 *   <tbody>
 *   <tr><th>reload-conf-properties</th><td>Reloads the properties where the path to the 
 *   website navigation, navigation and navigation is stored in case the website has been re-published
 *   by PageSeeder.</td></tr>
 *   </tbody>
 * </table>
 * 
 * <h3>Returned XML</h3>
 * <p>This generator return the content of the navigation file.</p>
 * <p>The content is wrapped in:</p>
 * <pre>{@code <template-file name="[navigation-filename]" status="ok">
 *
 *   <!-- Content of the template file -->
 *
 * </template-file >}</pre>
 * 
 * <p>Generally, the navigation is a PageSeeder standard document, and would follow the format
 * below:</p>
 * <pre>{@code <root>
 *
 *   <!-- metadata for the document -->
 *   <ps:documentInfo edit="true"> ... </ps:documentInfo>
 *
 *   <!-- for each document section -->
 *   <section id="[id]"> ... </section>
 *   ...
 *
 * </root>}</pre>
 * <p>Refer to the PageSeeder developer Website for more information about the PageSeeder 
 * standard format.</p>
 *
 * <h4>Error handling</h4>
 * <p>TODO</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="com.weborganic.bastille.web.Getnavigation" 
 *            name="[name]" target="[target]"/>}</pre>
 * 
 * @author Christophe Lauret
 * @version 31 May 2010
 */
public final class GetNavigation  extends GetTemplateFile implements ContentGenerator, Cacheable {

  /**
   * Creates a new navigation template file generator.
   * 
   * <p>It extends the {@link GetTemplateFile} class as below:
   * <pre>
   * public GetNavigation() {
   *   super("navigation");
   * }
   * </pre>
   */
  public GetNavigation() {
    super("navigation");
  }
}
