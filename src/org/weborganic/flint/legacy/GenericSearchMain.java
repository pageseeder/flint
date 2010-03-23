package org.weborganic.flint.legacy;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.lucene.queryParser.ParseException;
import org.weborganic.flint.query.GenericSearchQuery;
import org.weborganic.flint.query.SearchPaging;
import org.weborganic.flint.query.SearchResults;
import org.weborganic.flint.query.SearchTermParameter;
import org.weborganic.flint.util.CommandLine;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * Perform a Lucene index search using the arguments
 * received from the command-line
 * 
 * Possible arguments are:
 *     -index       [index files to use]     (compulsory)
 *     -field       [index field to search]  (compulsory)
 *     -value       [index value to search]  (compulsory)
 *     -paging      [result hits per page]   (optional,default '20')
 *
 * @author  Jin Zhou (Allette Systems)
 * 
 * @version 9 November 2009
 */
public final class GenericSearchMain {

  /**
   * Prevent creation of instances.
   */
  private GenericSearchMain() {
  }

  public static void main(String[] args) {
    // read arguments from the command-line
    String index = CommandLine.getParameter("-index", args); // index to use
      String searchfield = CommandLine.getParameter("-field", args); // search field
      String searchvalue = CommandLine.getParameter("-value", args); // search value
      String paging = CommandLine.getParameter("-paging", args); // hits per page

      // perform the search
      try {
      search(index, searchfield, searchvalue, paging);
    } catch (ParseException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

// methods

  /**
   * Displays the error message plus usage on System.err.
   * 
   * @param error The error thrown
   */
  public static void usage(String error) {
    System.err.println("error: " + error);
    usage();
    System.exit(1);
  }

  /**
   * Displays the usage on System.err.
   * 
   */
  public static void usage() {
    System.err.println("usage: Indexer -index <index> <options>");
    System.err.println("  <index>     Specify the index folder to use");
    System.err.println("possible options include:");
    System.err.println("  -field <search field>   index field to search");
    System.err.println("  -value <search value>   index value to search");
    System.err.println("  -paging <hits per page>  The paging setting (default '20' hits-per-page)");
  }

  private static void search(String index, String searchfield, String searchvalue, String paging) throws ParseException, IOException {

      // do a check on these arguments
      if (index == null)
        usage("you must specify an index folder to read.");

    // locate the index folder on file system
    File indexFile = new File(index);
    // initialise the search
    IndexSearch isearch = new IndexSearch(indexFile);
    // initialise the query
    GenericSearchQuery query = new GenericSearchQuery();
    SearchTermParameter parameter = new SearchTermParameter(searchfield, searchvalue);
    query.add(parameter);
    // initialise the paging
    SearchPaging searchpaging = new SearchPaging();
    if (paging != null) {
        try {
          searchpaging.setHitsPerPage(Integer.parseInt(paging));
        }
        catch(NumberFormatException e) {
          usage("-paging arg must be an integer value");
        }
      }
    // get search result
    SearchResults results = isearch.search(query, searchpaging);
    // initialise the writer
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer,true);
    xml.xmlDecl();
    results.toXML(xml);
    xml.flush();
    // don't forget to close the index to avoid 
    // "too many open file" error
    isearch.closeIndex();
    System.out.println(writer.toString());
  }

}
