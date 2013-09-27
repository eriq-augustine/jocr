package com.eriqaugustine.ocr.translate;

import com.eriqaugustine.ocr.utils.Props;
import com.eriqaugustine.ocr.utils.WebUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * A simple translator that is a thin wrapper around Google Translate.
 * This will probably later be replaced with more complicated systems.
 */
public class Translator {
   private static Logger logger = LogManager.getLogger(Translator.class.getName());

   private final String fromLanguage;
   private final String targetLanguage;

   private static final String BASE_URL = "https://www.googleapis.com/language/translate/v2";

   /**
    * A testing main.
    */
   public static void main(String[] args) {
      String toTranslate = "こんにちは世界";
      String fromLang = "ja";
      String toLang = "en";

      Translator trans = new Translator(fromLang, toLang);
      String res = trans.translate(toTranslate);

      System.out.println("Base:  " + toTranslate);
      System.out.println("Trans: " + res);
   }

   public Translator(String fromLanguage, String targetLanguage) {
      assert(Props.has("GOOGLE_API_KEY"));

      this.fromLanguage = fromLanguage;
      this.targetLanguage = targetLanguage;
   }

   public String translate(String text) {
      return fetchTranslation(text);
   }

   private String fetchTranslation(String text) {
      // Try to encode the text. If it does not encode, just leave it as is.
      try {
         text = URLEncoder.encode(text, "UTF-8");
      } catch (Exception ex) {
      }

      String translation = null;
      String requestUrl = String.format("%s?key=%s&source=%s&target=%s&q=%s",
                                        BASE_URL,
                                        Props.getString("GOOGLE_API_KEY"),
                                        fromLanguage,
                                        targetLanguage,
                                        text);

      String response = WebUtils.fetchPageAsString(requestUrl);
      if (response == null) {
         logger.error("Error fetching translation.");
         return null;
      }

      final JSONObject jsonResponse;
      try {
         jsonResponse = new JSONObject(response);
      } catch (JSONException ex) {
         logger.error("Bad json response for '" + text + "'.", ex);
         return null;
      }

      try {
         translation = jsonResponse.getJSONObject("data").
                                    getJSONArray("translations").
                                    getJSONObject(0).
                                    getString("translatedText");
      } catch (JSONException ex) {
         logger.error("JSON is not formatted as expected.", ex);
         return null;
      }

      logger.debug(translation);

      return translation;
   }
}
