package com.eriqaugustine.ocr.utils;

import com.eriqaugustine.ocr.image.WrapImage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * A class for general network centered utilities.
 * Utilities here will tend to deal with lower level abstractions (lower layers) than WebUtils.
 */
public class NetUtils {
   private static Logger logger = LogManager.getLogger(NetUtils.class.getName());

   /**
    * Get an image from the socket.
    * Data layout:
    *  - Number of image bytes (int)
    *  - Image Data (bytes)
    */
   public static WrapImage getImage(Socket socket) {
      try {
         DataInputStream inStream = new DataInputStream(socket.getInputStream());
         int numImageBytes = inStream.readInt();
         byte[] imageBytes = new byte[numImageBytes];
         inStream.read(imageBytes, 0, numImageBytes);
         return WrapImage.getImageFromBytes(imageBytes);
      } catch (Exception ex) {
         logger.error("Unable to retreive image from socket", ex);
         return null;
      }
   }

   /**
    * Send an image to the socket.
    * Data layout:
    *  - Number of image bytes (int)
    *  - Image Data (bytes)
    */
   public static boolean sendImage(Socket socket, WrapImage image) {
      try {
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         byte[] imageBytes = image.getBytes();
         out.writeInt(imageBytes.length);
         out.write(imageBytes, 0, imageBytes.length);
      } catch (Exception ex) {
         logger.error("Unable to send image on socket", ex);
         return false;
      }

      return true;
   }

   /**
    * Get a String from the socket.
    * Data layout:
    *  - Number of image bytes (int)
    *  - String Data (bytes)
    */
   public static String getString(Socket socket) {
      try {
         DataInputStream inStream = new DataInputStream(socket.getInputStream());
         int numBytes = inStream.readInt();
         byte[] stringBytes = new byte[numBytes];
         inStream.read(stringBytes, 0, numBytes);
         return new String(stringBytes);
      } catch (Exception ex) {
         logger.error("Unable to retreive string from socket", ex);
         return null;
      }
   }

   /**
    * Send a String to the socket.
    * Data layout:
    *  - Number of image bytes (int)
    *  - String Data (bytes)
    */
   public static boolean sendString(Socket socket, String text) {
      try {
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         byte[] stringBytes = text.getBytes();
         out.writeInt(stringBytes.length);
         out.write(stringBytes, 0, stringBytes.length);
      } catch (Exception ex) {
         logger.error("Unable to send string on socket", ex);
         return false;
      }

      return true;
   }
}
