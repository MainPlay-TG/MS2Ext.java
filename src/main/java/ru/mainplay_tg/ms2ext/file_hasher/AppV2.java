package ru.mainplay_tg.ms2ext.file_hasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(config.maxThreads, config.paths.size()));
    Map<String, Map<String, String>> results = new ConcurrentHashMap<>();
    List<CompletableFuture<Void>> futures = config.paths.stream().map(path -> CompletableFuture.supplyAsync(() -> {
      Map<String, String> hashResult;
      try {
        hashResult = App.hash_file(path, digests, config.bufSize);
      } catch (Exception e) {
        hashResult = null;
      }
      return Map.entry(path, hashResult);
    }, executor).thenAccept(result -> results.put(result.getKey(), result.getValue()))).collect(Collectors.toList());
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    System.err.println(Util.to_json(results));
  }

  static class Config {
    int bufSize = 1024 * 1024 * 4;
    int maxThreads = 8;
    List<String> algs;
    List<String> paths;
  }
}
