package ru.mainplay_tg.ms2ext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Util {
  /**
   * Прочитать весь stdin
   */
  public static String read_stdin() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    StringBuilder input = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      input.append(line);
    }
    return input.toString();
  }

  public static Gson get_gson() {
    return new GsonBuilder().setLenient().create();
  }

  /**
   * Парсить JSON в указанный класс
   */
  public static <T> T from_json(String json, Class<T> classOfT) throws JsonSyntaxException {
    return get_gson().fromJson(json, classOfT);
  }

  /**
   * Закодировать объект в JSON
   */
  public static String to_json(Object data) {
    return get_gson().toJson(data);
  }

  /**
   * Прочитать конфиг из stdin в виде JSON
   * 
   * @param configClass Класс конфига
   */
  public static <T> T read_config(Class<T> configClass) {
    String input;
    try {
      input = read_stdin();
    } catch (IOException exc) {
      exit_exc("Failed to read input JSON", exc);
      return null;
    }
    try {
      return from_json(input, configClass);
    } catch (JsonSyntaxException exc) {
      exit_exc("Failed to parse input JSON", exc);
    }
    return null;
  }

  /**
   * Написать ошибку и закрыть процесс
   * 
   * @param msg Текст ошибки
   * @param exc Объект ошибки
   */
  public static void exit_exc(String msg, Exception exc) {
    Map<String, Object> result = new HashMap<>();
    if (msg != null) {
      result.put("message", msg);
    }
    if (exc != null) {
      result.put("exception", exc.getMessage());
      result.put("stacktrace", get_stacktrace(exc));
    }
    System.err.println(to_json(result));
    System.exit(1);
  }

  public static String get_stacktrace(Exception exc) {
    try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
      exc.printStackTrace(pw);
      return sw.toString();
    } catch (IOException e) {
    }
    return "";
  }

  /**
   * Написать ошибку и закрыть процесс
   * 
   * @param msg Текст ошибки
   */
  public static void exit_exc(String msg) {
    exit_exc(msg, null);
  }
}
