package com.eriqaugustine.ocr.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A class for general web centered utilities.
 */
public class WebUtils {
   private static Logger logger = LogManager.getLogger(WebUtils.class.getName());

   private static final String WEB_CACHE_PREFIX = "web_cache";

   /**
    * Testing main.
    */
   public static void main(String[] args) {
      String apiKey = Props.getString("GOOGLE_API_KEY");
      String targetLang = "ja";
      String targetText = "Hello%20world";

      if (apiKey == null) {
         System.err.println("NO KEY: GOOGLE_API_KEY");
      }

      String url = String.format("https://www.googleapis.com/language/translate/v2?" +
                                 "key=%s&source=en&target=%s&q=%s",
                                 apiKey, targetLang, targetText);

      String page = fetchPageAsString(url, false);
      System.out.println(page);
   }

   public static Reader fetchPageAsReader(String address) {
      return fetchPageAsReader(address, true);
   }

   public static Reader fetchPageAsReader(String address, boolean cache) {
      final String page = fetchPageImpl(address, cache);
      if (page != null) {
         return new StringReader(page);
      }

      return null;
   }

   public static String fetchPageAsString(String address) {
      return fetchPageAsString(address, true);
   }

   public static String fetchPageAsString(String address, boolean cache) {
      return fetchPageImpl(address, cache);
   }

   /**
    * Get a web page.
    * TODO(eriq): Don't just error on all non-200 status and return the status.
    * @return A Reader that the caller owns on success, null on failure.
    */
   private static String fetchPageImpl(String address, boolean cache) {
      String page = null;
      File cacheFile = null;
      final URL url;

      try {
         url = new URL(address);
      } catch (java.net.MalformedURLException ex) {
         logger.error("Bad URL.", ex);
         return null;
      }

      // Check the cache for the page.
      if (cache) {
         cacheFile = getCacheFile(url);
         page = fetchCache(cacheFile);
         if (page != null) {
            return page;
         }
      }

      final URLConnection conn;
      try {
         conn = url.openConnection();
      } catch (Exception ex) {
         logger.warn("Unable to open connection.", ex);
         return null;
      }

      final String encoding = conn.getContentEncoding();

      Charset charset = Charset.forName("UTF-8");
      if (encoding != null) {
         try {
            charset = Charset.forName(encoding);
         } catch (UnsupportedCharsetException e) {
            // Just use the default encoding.
         }
      }

      try {
         final InputStream is = conn.getInputStream();
         final InputStreamReader reader = new InputStreamReader(is, charset);
         page = readerToString(reader);
      } catch (IOException ioEx) {
         logger.error("Unable to read GET stream.", ioEx);
         return null;
      }

      // Put in the cache
      if (cache) {
         if (page != null) {
            putCache(cacheFile, page);
         }
      }

      return page;
   }

   private static String readerToString(Reader reader) {
      String rtn = "";

      if (reader == null) {
         return null;
      }

      BufferedReader bufferedReader = null;
      try {
         bufferedReader = new BufferedReader(reader);
         String line = null;
         while ((line = bufferedReader.readLine()) != null) {
            rtn += line + "\n";
         }
      } catch (Exception ex) {
         return null;
      } finally {
         try {
            if (bufferedReader != null) {
               bufferedReader.close();
            }
            reader.close();
         } catch (Exception ex) {
         }
      }

      return rtn;
   }

   private static File getCacheFile(URL url) {
      final String hash = DigestUtils.sha1Hex(url.toString());
      final String cachePath = Props.getString("CACHE_DIR") + File.separator +
                               WEB_CACHE_PREFIX + "_" + hash;
      return new File(cachePath);
   }

   private static void putCache(File cacheFile, String page) {
      try {
         FileUtils.writeStringToFile(cacheFile, page, "UTF-8");
      } catch (Exception ex) {
         logger.error("Error creating cache.", ex);
         cacheFile.delete();
      }
   }

   private static String fetchCache(File cacheFile) {
      try {
         if (!cacheFile.exists()) {
            return null;
         }

         return FileUtils.readFileToString(cacheFile, "UTF-8");
      } catch (Exception ex) {
         logger.error("Error fetching cache.", ex);
         return null;
      }
   }
}
