package ru.mainplay_tg.ms2ext.file_hasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.mainplay_tg.ms2ext.Util;

public class AppV2 {
  public static void main(String[] argv) {
    try {
      main_real(argv);
    } catch (Exception exc) {
      Util.exit_exc("Unhandled exception", exc);
    }
  }

  private static void main_real(String[] argv) throws Exception {
    Config config = Util.read_config(Config.class);
    if (config == null) {
      return;
    }
    if (config.algs == null || config.algs.isEmpty()) {
      Util.exit_exc("No algorithm is specified");
      return;
    }
    if (config.paths == null || config.paths.isEmpty()) {
      Util.exit_exc("No paths is specified");
      return;
    }
    Map<String, MessageDigest> digests = new HashMap<>();
    for (String alg : config.algs) {
      try {
        digests.put(alg, MessageDigest.getInstance(alg));
      } catch (NoSuchAlgorithmException exc) {
        Util.exit_exc("No such algorithm: " + alg);
        return;
      }
    }
    Map<String, Map<String, String>> results = new HashMap<>();
    for (String path : config.paths) {
      results.put(path, App.hash_file(path, digests, config.bufSize));
    }
    System.err.println(Util.to_json(results));
  }

  static class Config {
    int bufSize = 1024 * 1024 * 4;
    List<String> algs;
    List<String> paths;
  }
}
