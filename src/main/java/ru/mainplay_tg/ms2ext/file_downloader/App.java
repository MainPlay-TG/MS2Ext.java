package ru.mainplay_tg.ms2ext.file_downloader;

import ru.mainplay_tg.ms2ext.file_downloader.Downloader.DownloadResult;
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
    RequestConfig config = Util.read_config(RequestConfig.class);
    if (config == null) {
      return;
    }
    if (config.path == null) {
      Util.exit_exc("Missing save path");
      return;
    }
    if (config.url == null) {
      Util.exit_exc("Missing URL");
      return;
    }
    DownloadResult result = Downloader.download(config);
    String json_result = Util.get_gson().toJson(result.to_map());
    System.err.println(json_result);
  }

  static class RequestConfig {
    Boolean checkStatus = true;
    Boolean followRedirects = true;
    Boolean verify = true;
    int bufSize = 1024 * 32;
    java.util.Map<String, String> headers = new java.util.HashMap<>();
    java.util.Map<String, String> query = new java.util.HashMap<>();
    Long sizeLimit;
    String method = "GET";
    String path;
    String url;

    public RequestConfig() {
    }
  }
}
