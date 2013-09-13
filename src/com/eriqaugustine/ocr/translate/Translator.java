package com.eriqaugustine.ocr.translate;

import com.eriqaugustine.ocr.utils.Props;
import com.eriqaugustine.ocr.utils.WebUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple translator that is a thin wrapper around Google Translate.
 * This will probably later be replaced with more complicated systems.
 */
public class Translator {
   private final String fromLanguage;
   private final String targetLanguage;

   private static final String BASE_URL = "https://www.googleapis.com/language/translate/v2";

   public Translator(String fromLanguage, String targetLanguage) {
      assert(Props.has("GOOGLE_API_KEY"));

      this.fromLanguage = fromLanguage;
      this.targetLanguage = targetLanguage;
   }

   public String translate(String text) {
      return fetchTranslation(text);
   }

   private String fetchTranslation(String text) {
      String translation = null;
      String requestUrl = String.format("%s?key=%s&source=%s&target=%s&q=%s",
                                        BASE_URL,
                                        Props.getString("GOOGLE_API_KEY"),
                                        fromLanguage,
                                        targetLanguage,
                                        text);

      String response = WebUtils.fetchPageAsString(requestUrl);
      if (response == null) {
         // TODO(eriq): Log
         System.err.println("Error fetching translation.");
         return null;
      }

      final JSONObject jsonResponse;
      try {
         jsonResponse = new JSONObject(response);
      } catch (JSONException ex) {
         System.err.println("Bad json response for '" + text + "'.");
         return null;
      }

      try {
         translation = jsonResponse.getJSONObject("data").
                                    getJSONArray("translations").
                                    getJSONObject(0).
                                    getString("translatedText");
      } catch (JSONException ex) {
         // TODO(eriq): Logs.
         System.err.println("JSON is not formatted as expected.");
         return null;
      }

      return translation;
   }
}
