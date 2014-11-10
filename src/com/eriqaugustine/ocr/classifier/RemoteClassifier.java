package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.NetUtils;
import com.eriqaugustine.ocr.utils.Props;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;

/**
 * A wrapper to a classifier on another JVM (possibly server).
 * TODO(eriq): We REALLY need to not make a new connection for each transfer.
 * TODO(eriq): We should be able to request a specific classifier that the server uses.
 * TODO(eriq): We need to extend the OCRClassifier api to accept multiple images
 *  so we don't have to make so many calls.
 */
public class RemoteClassifier implements OCRClassifier {
   private static Logger logger = LogManager.getLogger(RemoteClassifier.class.getName());

   private String server;
   private int port;

   public RemoteClassifier() {
      this(Props.getString("DEFAULT_TRANSLATION_SERVER"));
   }

   public RemoteClassifier(String server) {
      this(server, Props.getInt("DEFAULT_TRANSLATION_SERVER_PORT"));
   }

   public RemoteClassifier(String server, int port) {
      this.server = server;
      this.port = port;
   }

   public String classify(WrapImage image) {
      String rtn = null;
      Socket socket = null;

      try {
         socket = new Socket(server, port);
         NetUtils.sendImage(socket, image);
         rtn = NetUtils.getString(socket);
      } catch (Exception ex) {
         logger.error("Error in remote classification", ex);
         return null;
      } finally {
         if (socket != null) {
            try {
               socket.close();
            }
            catch (Exception ex) {
               // We tried...
            }
         }
      }

      return rtn;
   }
}
