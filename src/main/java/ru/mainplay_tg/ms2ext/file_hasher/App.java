package ru.mainplay_tg.ms2ext.file_hasher;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.mainplay_tg.ms2ext.Util;

public class App {
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
    if (config.path == null) {
      Util.exit_exc("Missing file path");
      return;
    }
    Path path = Paths.get(config.path);
    File file = path.toFile();
    if (!file.exists()) {
      Util.exit_exc("File does not exist");
      return;
    }
    if (config.algs == null || config.algs.isEmpty()) {
      Util.exit_exc("No algorithm is specified");
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
    System.err.println(Util.to_json(hash_file(file, digests, config.bufSize)));
  }

  public static Map<String, String> hash_file(File file, Map<String, MessageDigest> digests, int bufSize)
      throws Exception {
    for (MessageDigest dig : digests.values()) {
      dig.reset();
    }
    try (FileInputStream in = new FileInputStream(file)) {
      byte[] buffer = new byte[bufSize];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        for (MessageDigest dig : digests.values()) {
          dig.update(buffer, 0, bytesRead);
        }
      }
    }
    Map<String, String> result = new HashMap<>();
    Base64.Encoder b64 = Base64.getEncoder();
    for (String alg : digests.keySet()) {
      MessageDigest md = digests.get(alg);
      result.put(alg, b64.encodeToString(md.digest()));
    }
    return result;
  }

  public static Map<String, String> hash_file(Path path, Map<String, MessageDigest> digests, int bufSize)
      throws Exception {
    return hash_file(path.toFile(), digests, bufSize);
  }

  public static Map<String, String> hash_file(String path, Map<String, MessageDigest> digests, int bufSize)
      throws Exception {
    return hash_file(Paths.get(path), digests, bufSize);
  }

  static class Config {
    int bufSize = 1024 * 1024 * 4;
    List<String> algs;
    String path;

    public Config() {
    }
  }
}
