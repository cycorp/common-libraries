package com.cyc.webservice.client;

/* $Id: GenericRestfulWSClient.java 145917 2013-06-05 14:59:11Z daves $
 *
 * Copyright (c) 2009-10 Cycorp, Inc.  All rights reserved.
 * This software is the proprietary information of Cycorp, Inc.
 * Use is subject to license terms.
 */

//// Internal Imports

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//// External Imports

/**
 * <p/>GenericRestfulWSClient is designed to make communicating with
 * a variety of restful web services as convenient as possible.
 * <p/>
 * Example:
 * <code><pre>
 *    GenericRestfulWSClient client = new GenericRestfulWSClient("http",
 *      "ws.opencyc.org", DEFAULT_HTTP_PORT, "/webservices/concept/find");
 *    String result = client.get("str", "dog",
 *      "searchType", "ANY", "maxResults", 100,
 *      "startingFrom", 0, "uriType", "current",
 *      "conceptDetails", "typical", "isExactMatch", true,
 *      "ignoreCase", true);
 * </pre></code>
 * <p/>
 * To make communications possible with a service that has a self-signed certificate
 * you can either define your own <code>X509TrustManager</code> and pass it to
 * the <code>GenericRestfulWSClient</code> constructor or you can modify the
 * JVMs truststore by doing the following:
 * <pre>
 *   cd <default client's working directory>
 *   cp ${JAVA_HOME}/jre/lib/security/cacerts cacerts.new
 *   keytool -import -alias <alias> -file <x509 cert file> -keystore cacerts.new
 *   **** if asks for a password, it is probably: changeit
 *   **** answer yes to whether you should trust the certificate
 *   **** add following parameters to client:
 *        -Djavax.net.ssl.trustStore=cacerts.new -Djavax.net.ssl.trustStorePassword=changeit
 * </pre>
 * <p/>@todo need to add support PUT and DELETE http requests
 * <p/>Copyright (c) 2009-10 Cycorp, Inc.  All rights reserved.
 * <br/>This software is the proprietary information of Cycorp, Inc.
 * <br/>Use is subject to license terms.
 *
 * Created on : Sep 30, 2009, 5:24:35 PM
 * Author     : tbrussea
 * @version $Id: GenericRestfulWSClient.java 145917 2013-06-05 14:59:11Z daves $
 */
public class GenericRestfulWSClient {

  //// Constructors

  /** Creates a new instance of <code>GenericRestfulWSClient</code>.
   * @param protocol one of "http" or "https"
   * @param host The hostname or ip address of the web service server
   * @param port The port number at which the service is available,
   *        use GenericRestfulWSClient.DEFAULT_HTTP_PORT
   *        if the service uses the standard HTTTP/S ports
   * @param servicePath the web service path
   * @throws IllegalArgumentException if protocol != "http" or "https"
   */
  public GenericRestfulWSClient(String protocol, String host, int port, String servicePath) {
    this(protocol, host, port, servicePath, null);
  }

  /** Creates a new instance of <code>GenericRestfulWSClient</code>.
   * @param protocol one of "http" or "https"
   * @param host The hostname or ip address of the web service server
   * @param port The port number at which the service is available,
   *        use GenericRestfulWSClient.DEFAULT_HTTP_PORT
   *        if the service uses the standard HTTTP/S ports
   * @param servicePath the web service path
   * @param trustManager the X509 trust manager to use, or null if don't care
   * or not communicating over HTTPS
   * @throws IllegalArgumentException if protocol != "http" or "https"
   */
  public GenericRestfulWSClient(String protocol, String host, int port,
      String servicePath, X509TrustManager trustManager) {
    this(protocol, host, port, servicePath, trustManager, null);
  }

  /** Creates a new instance of <code>GenericRestfulWSClient</code>.
   * @param protocol one of "http" or "https"
   * @param host The hostname or ip address of the web service server
   * @param port The port number at which the service is available,
   *        use GenericRestfulWSClient.DEFAULT_HTTP_PORT
   *        if the service uses the standard HTTTP/S ports
   * @param servicePath the web service path
   * @param trustManager the X509 trust manager to use, or null if don't care
   * or not communicating over HTTPS
   * @param cache a map used to cache this web services HTTP requests
   * @throws IllegalArgumentException if protocol != "http" or "https"
   */
  public GenericRestfulWSClient(String protocol, String host, int port,
      String servicePath, X509TrustManager trustManager, Map<WSRequestParams, Object> cache) {
    this(protocol, host, port, servicePath, trustManager, cache, DEFAULT_REQUEST_PROPS);
  }

  /** Creates a new instance of <code>GenericRestfulWSClient</code>.
   * @param protocol one of "http" or "https"
   * @param host The hostname or ip address of the web service server
   * @param port The port number at which the service is available,
   *        use GenericRestfulWSClient.DEFAULT_HTTP_PORT
   *        if the service uses the standard HTTTP/S ports
   * @param servicePath the web service path
   * @param trustManager the X509 trust manager to use, or null if don't care
   * or not communicating over HTTPS
   * @param cache a map used to cache this web services HTTP requests
   * @param requestProps default properties to append to any HTTP request
   * @throws IllegalArgumentException if protocol != "http" or "https"
   */
  public GenericRestfulWSClient(String protocol, String host, int port,
      String servicePath, X509TrustManager trustManager, Map<WSRequestParams, Object> cache, Map<String, String> requestProps) {
    if (!((protocol.equals("http")) || (protocol.equals("https")))) {
      throw new IllegalArgumentException("GenericRestfulWSClient supports 'http' and 'https', got: " + protocol);
    }
    setLoggerLevel(Level.WARNING);
    this.cache = cache;
    StringBuilder buf = new StringBuilder();
    buf.append(protocol).append("://");
    if ((host == null) || ("".equals(host))) {
      throw new IllegalArgumentException("Got invalid host: " + host);
    } else {
      buf.append(host);
    }
    if (port > 0) {
      buf.append(":").append(port);
    }
    urlToPort = buf.toString();
    if ((servicePath == null) || ("".equals(servicePath))) {
      buf.append("/");
    } else {
      if (!servicePath.startsWith(("/"))) {
        buf.append("/");
      }
      buf.append(servicePath);
    }
    urlStarter = buf.toString();
    if (requestProps == null) {
      this.requestProps = new HashMap<String, String>(0);
    } else {
      this.requestProps = new HashMap<String, String>(requestProps);
    }
    if (trustManager != null) {
      TrustManager mytm[] = {trustManager};
      try {
        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, mytm, null);
        SSLSocketFactory socketFactory = ctx.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  //// Public Area

  @Override
/**
 * Provides a human readable URL to the base of the WS, which might be helpful in debugging prints.
 */
  public String toString(){
    return "REST Web Service at "+urlStarter;
  }

  /**
   * The default http port. When passed to the <code>GenericRestfulWSClient</code>
   * constructor, will cause generated requests not to explicitly set the port.
   */
  public static final int DEFAULT_HTTP_PORT = -1;

  /**
   * Returns the amount of time in milliseconds that any HTTP request
   * will wait before timing out. Zero means to wait forever.
   * @return
   */
  public int getTimeoutMsecs() {
    return timeoutMsecs;
  }

  /**
   * Sets the amount of time in milliseconds that any HTTP request
   * will wait before timing out. Zero means to wait forever.
   * @param timeoutMsecs the amount of time in milliseconds that any HTTP request
   * will wait before timing out (0  means forever).
   * @throws  IllegalArgumentException if timeoutMsecs is less than 0
   */
  public void setTimeoutMsecs(int timeoutMsecs) {
    if (timeoutMsecs < 0) {
      throw new IllegalArgumentException("Invalid timout: " + timeoutMsecs + " msecs.");
    }
    this.timeoutMsecs = timeoutMsecs;
  }

  /**
   * Gets the charset for performing HTTP requests - default is UTF-8.
   * @return the charset for performing HTTP requests
   */
  public Charset getCharset() {
    return charSet;
  }

  /**
   * Sets the charset for performing HTTP requests.
   * @param charSet the charSet to use
   */
  public void setCharset(Charset charSet) {
    this.charSet = charSet;
  }

  /**
   * The client's stream buffer size for communicatios with the web service (default is 32k).
   * @return client's stream buffer size for communicatios with the web service
   */
  public int getStreamBufSize() {
    return streamBufSize;
  }

  /**
   * Sets the client's stream buffer size.
   * @param streamBufSize the new streamBufSize to use
   * @throws IllegalArgumentException if <code>streamBufSize</code> is less than 0
   */
  public void setStreamBufSize(int streamBufSize) {
    if (streamBufSize < 0) {
      throw new IllegalArgumentException("Invalid stream size: " + streamBufSize + ".");
    }
    this.streamBufSize = streamBufSize;
  }

  /**
   * Sets the username and password that should be used for doing basic HTTP
   * authentication. One can pass in a <code>userName</code> of <code>null</code>
   * in order to clear out previous settings. By default the userName and
   * password are <code>null</code>.
   * @param userName
   * @param password
   */
  public void setUserPwd(String userName, String password) {
    if ((userName != null) && (password != null)) {
      requestProps.put("Authorization", encodeCredentialsBasic(userName, password));
    } else {
      requestProps.remove("Authorization");
    }
  }

  /**
   * Returns the set of parameter names that should be sent as POST-style
   * parameters. <code>null</code> means send all parameters POST-style
   * when performing a POST HTTP request. The default is <code>null</code>
   * @return the set of parameter names that should be sent as POST-style
   * parameters
   * names that should be sent as POST-style parameters
   */
  public Set<String> getPostableParams() {
    if (postableParams == null) {
      return null;
    }
    return new HashSet<String>(postableParams);
  }

  /**
   * Sets the set of parameter names that should be sent as POST-style
   * parameters. <code>null</code> means send all parameters POST-style
   * when performing a POST HTTP request.
   * @param postableParams
   */
  public void setPostableParams(Set<String> postableParams) {
    if (postableParams == null) {
      this.postableParams = postableParams;
    } else {
      this.postableParams = new HashSet<String>(postableParams);
    }
  }

  /**
   * Set whether to parse the error stream if the HTTP connection throws an exception.
   * By default, error stream parsing is on.
   * @param newVal whether to parse the error stream if the HTTP connection throws an exception
   */
  public void setParseErrorStream(boolean newVal) {
    parseErrorResults = newVal;
  }

  /**
   * Returns whether to parse the error stream if the HTTP connection throws an exception.
   * By default, error stream parsing is on.
   * @return whether to parse the error stream if the HTTP connection throws an exception
   */
  public boolean parseErrorStream() {
    return parseErrorResults;
  }

  /** Returns the URL of the service being accessed, minus any parameters */
  public String getUrlStarter() {
    return urlStarter;
  }

  /** Returns the URL of the service being accessed up to the port number, 
   * but not including the service name or any parameters.
   */
  public String getUrlToPort() {
    return urlToPort;
  }
  private static final double NANO_USECS = 1000;
  private static final double NANO_MSECS = 1000 * NANO_USECS;
  private static final double NANO_SECS = 1000 * NANO_MSECS;
  private static final double NANO_MINS = 60 * NANO_SECS;
  private static final double NANO_HOURS = 60 * NANO_MINS;
  private static final double NANO_DAYS = 24 * NANO_HOURS;

  // @todo move this to a generic library
  public static String getDurationString(long nanoDuration) {
    if (nanoDuration < 0) {
      throw new IllegalArgumentException("'nanoDuration' must be >= 0, got: " + nanoDuration);
    }
    double divisor = 1.0;
    double nsecs, usecs, msecs, secs, mins, hours, days;
    StringBuilder buf = new StringBuilder(1024);
    if ((days = Math.floor(nanoDuration / NANO_DAYS)) >= 1.0) {
      buf.append((long)days).append((days == 1.0) ? " day, " : " days, ");
      hours = Math.floor(nanoDuration / NANO_HOURS) % 24;
      buf.append((long)hours).append((hours == 1.0) ? " hour, " : " hours, ");
      mins = Math.round(nanoDuration / NANO_MINS) % 60;
      buf.append((long)mins).append((mins == 1.0) ? " min" : " mins");
    } else if ((hours = Math.floor(nanoDuration / NANO_HOURS)) >= 1.0) {
      buf.append((long)hours).append((hours == 1.0) ? " hour, " : " hours, ");
      mins = Math.floor(nanoDuration / NANO_MINS) % 60;
      buf.append((long)mins).append((mins == 1.0) ? " min, " : " mins, ");
      secs = Math.round(nanoDuration / NANO_SECS) % 60;
      buf.append((long)secs).append((secs == 1.0) ? " sec" : " secs");
    } else if ((mins = Math.floor(nanoDuration / NANO_MINS)) >= 1.0) {
      buf.append((long)mins).append((mins == 1.0) ? " min, " : " mins, ");
      secs = Math.round(nanoDuration / NANO_SECS) % 60;
      buf.append((long)secs).append((secs == 1.0) ? " sec" : " secs");
    } else if ((secs = Math.floor(nanoDuration / NANO_SECS)) >= 1.0) {
      buf.append((long)secs).append((secs == 1.0) ? " sec, " : " secs, ");
      msecs = Math.round(nanoDuration / NANO_MSECS) % 1000;
      buf.append((long)msecs).append((msecs == 1.0) ? " msec" : " msecs");
    } else if ((msecs = Math.floor(nanoDuration / NANO_MSECS)) >= 1.0) {
      buf.append((long)msecs).append((msecs == 1.0) ? " msec, " : " msecs, ");
      usecs = Math.round(nanoDuration / NANO_USECS) % 1000;
      buf.append((long)usecs).append((usecs == 1.0) ? " usec" : " usecs");
    } else if ((usecs = Math.floor(nanoDuration / NANO_USECS)) >= 1.0) {
      buf.append((long)usecs).append((usecs == 1.0) ? " usec, " : " usecs, ");
      nsecs = nanoDuration % 1000;
      buf.append((long)nsecs).append((nsecs == 1.0) ? " nsec" : " nsecs");
    } else if (nanoDuration >= 1) {
      buf.append(nanoDuration).append((nanoDuration == 1) ? " nsec" : " nsecs");
    } else {
      buf.append("no measurable elapsed time");
    }
    return buf.toString();
  }

  /**
   * Perform a HTTP GET request, returning the entire result as a <code>String</code>.
   * This function is possibly cached if an appropriate cache was passed to the constructor.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the entire result as a <code>String</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public String get(Object... paramNameThenValue)
      throws MalformedURLException, IOException {
    WSRequestParams params = new WSRequestParams("String", "GET", paramNameThenValue);
    String result = (String) getCached(params);
    if (result != null) {
      return result;
    }
    result = getStringInternal(false, paramNameThenValue);
    putCached(params, result);
    long endTime = System.nanoTime();
    return result;
  }

  /**
   * Perform a HTTP GET request, returning the result as an <code>InputStream</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as an <code>InputStream</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public InputStream getAsStream(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getStreamInternal(false, paramNameThenValue);
  }

  /**
   * Perform a HTTP GET request, returning the result as a <code>Reader</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as a <code>Reader</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public Reader getAsReader(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getReaderInternal(false, paramNameThenValue);
  }

  /**
   * Perform a HTTP GET request, returning the result as a XML <code>Document</code>.
   * This function is possibly cached if an appropriate cache was passed to the constructor.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as a XML <code>Document</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   * @throws ParserConfigurationException if the parser is misconfigured
   * @throws SAXException if an XML parsing error occurs
   */
  public Document getAsXml(Object ... paramNameThenValue)
      throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
    WSRequestParams params = new WSRequestParams("Document", "GET", paramNameThenValue);
    Document result = (Document)getCached(params);
    if (result != null) {
      return result;
    }
    String response = get(paramNameThenValue); // @todo use stream processing
    result = convertStringToXml(response);
    putCached(params, result);
    return result;
  }

  /**
   * Perform a HTTP GET request, returning the result as an <code>javax.xml.transform.Source</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as an <code>javax.xml.transform.Source</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public Source getAsSource(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getSourceInternal(false, paramNameThenValue);
  }

  /**
   * Perform a HTTP POST request, returning the entire result as a <code>String</code>.
   * This function is possibly cached if an appropriate cache was passed to the constructor.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the entire result as a <code>String</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public String post(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    WSRequestParams params = new WSRequestParams("String", "POST", paramNameThenValue);
    String result = (String)getCached(params);
    if (result != null) {
      return result;
    }
    result = getStringInternal(true, paramNameThenValue);
    putCached(params, result);
    return result;
  }

  /**
   * Perform a HTTP POST request, returning the result as an <code>InputStream</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as an <code>InputStream</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public InputStream postAsStream(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getStreamInternal(true, paramNameThenValue);
  }

  /**
   * Perform a HTTP POST request, returning the result as a <code>Reader</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as a <code>Reader</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public Reader postAsReader(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getReaderInternal(true, paramNameThenValue);
  }

  /**
   * Perform a HTTP POST request, returning the result as a XML <code>Document</code>.
   * This function is possibly cached if an appropriate cache was passed to the constructor.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as a XML <code>Document</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   * @throws ParserConfigurationException if the parser is misconfigured
   * @throws SAXException if an XML parsing error occurs
   */
  public Document postAsXml(Object ... paramNameThenValue)
      throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
    WSRequestParams params = new WSRequestParams("Document", "POST", paramNameThenValue);
    Document result = (Document)getCached(params);
    if (result != null) {
      return result;
    }
    String response = post(paramNameThenValue); // @todo use stream processing
    result = convertStringToXml(response);
    putCached(params, result);
    return result;
  }

  /**
   * Perform a HTTP GET request, returning the result as an <code>javax.xml.transform.Source</code>.
   * @param paramNameThenValue a sequence of parameter name followed by parameter value
   * @return the result as an <code>javax.xml.transform.Source</code>
   * @throws MalformedURLException if unable to generate a URL from the given parameters
   * @throws IOException if an error occurs with trying to communicate with the server
   */
  public Source postAsSource(Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    return getSourceInternal(true, paramNameThenValue);
  }

  public void setLoggerLevel(Level level) {
    logger.setLevel(level);
  }
  
  // @todo add methods for PUT
  // @todo add methods for DELETE

  /**
   * <p>Encode the specified credentials into a String as required by
   * HTTP Basic Authentication (<a href="http://www.ietf.org/rfc/rfc2617.txt">RFC 2617</a>).</p>
   *
   * @param userName userName to be encoded
   * @param password password to be encoded
   * @return
   */
  public static String encodeCredentialsBasic(String userName, String password) {
    String encode = userName + ":" + password;
    int paddingCount = (3 - (encode.length() % 3)) % 3;
    encode += "\0\0".substring(0, paddingCount);
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < encode.length(); i += 3) {
      int j = (encode.charAt(i) << 16) + (encode.charAt(i + 1) << 8) + encode.charAt(i + 2);
      encoded.append(BASE64_CHARS.charAt((j >> 18) & 0x3f));
      encoded.append(BASE64_CHARS.charAt((j >> 12) & 0x3f));
      encoded.append(BASE64_CHARS.charAt((j >> 6) & 0x3f));
      encoded.append(BASE64_CHARS.charAt(j & 0x3f));
    }
    return "Basic " + encoded.toString();
  }

  /** Clears this web service's cache or does nothing if no cache has been set */
  public void clearCache() {
    if (cache != null) {
      cache.clear();
    }
  }

  /**
   * Returns whether this web service does internal caching
   * @return whether this web service does internal caching
   */
  public boolean isCaching() {
    return (cache != null);
  }

  /**
   * Specify the cache to use for web-service calls.
   * @param cache 
   */
  public void setCache(Map<WSRequestParams, Object> cache) {
      this.cache = cache;
  }

  /**
   * The cache currently being used by this webservice client.
   * @return the cache being used by this webservice client.
   */
  public Map<WSRequestParams, Object> getCache() {
      return this.cache;
  }
  
  //// Protected Area
  
  protected void logDuration(long startTimeNano, long endTimeNano) {
    if (logger.getLevel().intValue() >= Level.INFO.intValue()) {
      logger.log(Level.INFO, "Web service call took: "
          + getDurationString(endTimeNano - startTimeNano));
    }
  }

  /**
   * Returns the cached value for <code>key</code>, or <code>null</code> if
   * the key is not present or this web service is not generally performing
   * caching
   * @param key the key to look up
   * @return the cached value for <code>key</code>, or <code>null</code> if
   * the key is not present or this web service is not generally performing
   * caching
   */
  protected Object getCached(WSRequestParams key) {
    if (cache == null) {
      return null;
    }
    return cache.get(key);
  }

  /**
   * Removes the indicated cached value.
   * @param key the key for the entry that should be removed
   * @return the old value, or <code>null</code> if there was no previous value
   * or this web service is not performing caching.
   */
  protected Object removeCached(WSRequestParams key) {
    if (cache == null) {
      return null;
    }
    return cache.remove(key);
  }
  
  /**
   * Inserts the indicated key/value pair.
   * @param key the key that should be added
   * @param val the value that should be added
   * @return the previous value associated with <code>key</code>, or
   * <code>null</code> if there was no previous value
   * or this web service is not performing caching.
   */
  protected Object putCached(WSRequestParams key, Object val) {
    if (cache == null) {
      return null;
    }
    return cache.put(key, val);
  }

  //// Private Area

  /** This method should be the only one used to get connections to the web service.  */
  private HttpURLConnection getConnection(boolean allowPost, Object... paramNameThenValue)
      throws MalformedURLException, IOException, ProtocolException {
    String url = getURLStringForRequest(allowPost, paramNameThenValue);
    logger.log(Level.INFO, "About to make web service request: {0}", url);
    URL realUrl = new URL(url);
    HttpURLConnection con =  (HttpURLConnection)realUrl.openConnection();
    con.setReadTimeout(timeoutMsecs);
    for (Map.Entry<String, String> entry : requestProps.entrySet()) {
      con.addRequestProperty(entry.getKey(), entry.getValue());
    }
    if (allowPost) {
      configurePost(con, paramNameThenValue);
    }
    return con;
  }

  /** set post data and HTTP headers */
  private void configurePost(HttpURLConnection con, Object... paramNameThenValue)
  throws ProtocolException, IOException {
    con.setDoOutput(true);
    con.setRequestMethod("POST");
    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    con.setUseCaches(false);
    String postableStr = getPostableData(paramNameThenValue);
    byte[] postableData = postableStr.getBytes(getCharset());
    con.setRequestProperty("Content-Length", "" + postableData.length);
    OutputStream outStream = con.getOutputStream();
    try {
      if (streamBufSize > 0) {
        outStream = new BufferedOutputStream(outStream, streamBufSize);
      }
      outStream.write(postableData);
      outStream.flush();
    } finally {
      try {
        outStream.close();
      } catch (Exception e) {
      }
    }
  }

  /** Make a connection and return a stream (possibly buffered). */
  private InputStream getStreamInternal(boolean allowPost, Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    HttpURLConnection con = getConnection(allowPost, paramNameThenValue);
    InputStream is = null;
    try {
       is = con.getInputStream();
    } catch (IOException e) {
      if (parseErrorStream()) {
        is = con.getErrorStream();
      }
      if (is == null) {
        throw e;
      }
    }
    if (streamBufSize > 0) {
      is = new BufferedInputStream(is, streamBufSize);
    }
    return is;
  }

  /** Make a connection and return a reader (possibly buffered). */
  private Reader getReaderInternal(boolean allowPost, Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    InputStream is = getStreamInternal(allowPost, paramNameThenValue);
    return new InputStreamReader(is, charSet);
  }

  /** Make a connection and return a source. */
  private Source getSourceInternal(boolean allowPost, Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    Reader reader = getReaderInternal(allowPost, paramNameThenValue);
    return new StreamSource(reader);
  }

  /** Make a connection and return a String. */
  private String getStringInternal(boolean allowPost, Object ... paramNameThenValue)
      throws MalformedURLException, IOException {
    Reader reader = getReaderInternal(allowPost, paramNameThenValue);
    StringBuilder buf = urlResponseBuffer.get();
    long startTime = System.nanoTime();
    try {
      int nchars = 0;
      buf.delete(0, buf.length());
      char[] streamBuf = streamBuffer.get();
      int bytesRead = -1;
      while ((bytesRead = reader.read(streamBuf, 0, streamBuf.length)) >= 0) {
        buf.append(streamBuf, 0, bytesRead);
      }
    } finally {
      try { reader.close(); } catch (Exception e) {}
      logDuration(startTime, System.nanoTime());
    }
    return buf.toString();
  }

  /** return a string representation of the post parameters */
  private String getPostableData(Object... paramNameThenValue) {
    // @todo break out shared code with getURLStringForRequest
    StringBuilder buf = urlStringBuilder.get();
    try {
      buf.delete(0, buf.length());
      if (paramNameThenValue.length > 1) {
        if ((paramNameThenValue.length % 2) == 1) {
          throw new IllegalArgumentException("Got illegal web service argument list.\n "
              + "There should be an even number of paramaters because each argument\n "
              + "should be named first then the value should be given.");
        }
        boolean isFirst = true;
        for (int i = 0, size = paramNameThenValue.length; i < size;) {
          String paramName = "" + paramNameThenValue[i++];
          if ((postableParams != null) && (!postableParams.contains(paramName))) {
            continue;
          }
          if (!isFirst) {
            buf.append("&");
          }
          isFirst = false;
          buf.append(paramName).append("=").
              append(URLEncoder.encode("" + paramNameThenValue[i++], "UTF-8"));
        }
      }
    } catch (UnsupportedEncodingException e) {
    } //ignore
    String result = buf.toString();
    logger.info("Post data: \n" + result);
    return result;
  }

  /** returns a string representation of the URL to use for making the connection */
  private String getURLStringForRequest(boolean allowPost, Object ... paramNameThenValue) {
    // @todo break out shared code with getPostableData
    StringBuilder buf = urlStringBuilder.get();
    try {
      buf.delete(0, buf.length());
      buf.append(urlStarter);
      if ((postableParams != null) || (!allowPost)) {
        if (paramNameThenValue.length > 1) {
          if ((paramNameThenValue.length % 2) == 1) {
            throw new IllegalArgumentException("Got illegal web service argument list.\n "
                + "There should be an even number of paramaters because each argument\n "
                + "should be named first then the value should be given.");
          }
          boolean isFirst = !urlStarter.contains("?");
          for (int i = 0, size = paramNameThenValue.length; i < size;) {
            String paramName = "" + paramNameThenValue[i++];
            if (allowPost && ((postableParams == null) || postableParams.contains(paramName))) {
              continue;
            }
            if (isFirst) {
              buf.append("?");
            } else {
              buf.append("&");
            }
            isFirst = false;
            buf.append(paramName).append("=").
                append(URLEncoder.encode("" + paramNameThenValue[i++], "UTF-8"));
          }
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (Throwable t) {
        t.printStackTrace();
    }//ignore
    return buf.toString();
  }

  /** default method for converting a string to XML */
  private Document convertStringToXml(String str) throws IOException, ParserConfigurationException, SAXException {
    // @todo make the streamed and protected so that subclasses can override
    str = str.trim();
    // @hack for poorly behaved services that don't emit an xml prolog
    if (!str.startsWith("<?xml")) {
      str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + str;
    }
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    InputSource source = new InputSource(new ByteArrayInputStream(str.getBytes()));
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(source);
    return doc;
  }

  //// Internal Rep

  /** for future use: occurrences of "allowPost" should be replaced by this */
  private static enum RequestType { GET, POST, PUT, DELETE };

  /** Time to wait on HTTP requests in milliseconds before timing out. 0 means to wait forever. */
  private static final int DEFAULT_TIMEOUT_MSECS = 0;

  /** Default map of HTTP request headers that should be added to every request.
   It only contains an "accept"/"application/xml" mapping. */
  public static final Map<String, String> DEFAULT_REQUEST_PROPS = new HashMap<String, String>();
  
  public static final Map<String, String> DEFAULT_HTML_REQUEST_PROPS = new HashMap<String, String>();
  
  public static final Map<String, String> DEFAULT_TEXT_REQUEST_PROPS = new HashMap<String, String>();

  /** default size for stream buffering */
  private static final int DEFAULT_STRING_BUF_SIZE = 1024 * 32;

  /** Time to wait on HTTP requests in milliseconds before timing out. 0 means to wait forever. */
  private int timeoutMsecs = DEFAULT_STRING_BUF_SIZE;

  /** Charset to use for communications. */
  private Charset charSet = Charset.forName("UTF-8");

  /** Stream buffer size. */
  private int streamBufSize = DEFAULT_STRING_BUF_SIZE;

  /** Map of request heards to send. */
  private Map<String, String> requestProps = DEFAULT_REQUEST_PROPS;

  /** The URL for the web service, up to and including the service name */
  private String urlStarter = "";

  /** The URL for the web service, up to but not including the service name */
  private String urlToPort = "";

  /** A set of parameter names that should be sent POST-style when
   * performing a POST request. null means to send all parameters POST-style.
   */
  private Set<String> postableParams = null;

  /** boolean value indicating whether the error stream for failing HTTP URL connections
   * should be parsed.
   */
  private boolean parseErrorResults = true;

  /** cache to use for caching HTTP request, or null if no caching should be performed.
   * By default this value is null.
   */
  private Map<WSRequestParams, Object> cache = null;

  private volatile Logger logger = Logger.getLogger(getClass().getName());
  
  static {
    DEFAULT_REQUEST_PROPS.put("accept", "application/xml");
  };
  
  static {
    DEFAULT_HTML_REQUEST_PROPS.put("accept", "text/html");
  };
  
  static {
    DEFAULT_TEXT_REQUEST_PROPS.put("accept", "text/plain");
  };

  /** Characters to use for base-64 encoding. */
  private static final String BASE64_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
      "abcdefghijklmnopqrstuvwxyz" +
      "0123456789+/";

  /** hack to avoid unnecessary memory allocations */
  private static ThreadLocal<char[]> streamBuffer = new ThreadLocal<char[]>() {
    @Override
    protected synchronized char[] initialValue() {
      return new char[1024 * 32];
    }
  };

  /** hack to avoid unnecessary memory allocations */
  private static ThreadLocal<StringBuilder> urlStringBuilder = new ThreadLocal<StringBuilder>() {
    @Override
    protected synchronized StringBuilder initialValue() {
      return new StringBuilder(1024*32);
    }
  };

  /** hack to avoid unnecessary memory allocations */
  private static ThreadLocal<StringBuilder> urlResponseBuffer = new ThreadLocal<StringBuilder>() {
    @Override
    protected synchronized StringBuilder initialValue() {
      return new StringBuilder(1024*32);
    }
  };

  //// Main

  /**
   * Simple sanity-check main method.
   * @param args not used
   */
  private static void main(String[] args) {
    System.out.println("Starting...");
    System.out.flush();
    try {
      GenericRestfulWSClient client = new GenericRestfulWSClient("http",
        "ws.opencyc.org", DEFAULT_HTTP_PORT, "/webservices/concept/find");
      String result = client.get("str", "dog",
        "searchType", "ANY", "maxResults", 100,
        "startingFrom", 0, "uriType", "current",
        "conceptDetails", "typical", "isExactMatch", true,
        "ignoreCase", true);
      System.out.println("Got result: " + result);
      System.out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      System.err.flush();
      System.out.flush();
    }
    System.out.println("Finished.");
    System.out.flush();
  }
}
