package ru.mainplay_tg.ms2ext.file_downloader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import ru.mainplay_tg.ms2ext.file_downloader.App.RequestConfig;

public class Downloader {
  public static class DownloadResult {
    public final int statusCode;
    public final long downloadedSize;
    public final HttpHeaders headers;

    public DownloadResult(int a, HttpHeaders b, long c) {
      this.statusCode = a;
      this.headers = b;
      this.downloadedSize = c;
    }

    public Map<String, Object> to_map() {
      Map<String, Object> result = new HashMap<>();
      result.put("downloadedSize", this.downloadedSize);
      result.put("headers", this.headers.map());
      result.put("statusCode", this.statusCode);
      return result;
    }
  }

  public static DownloadResult download(RequestConfig config) throws Exception {
    // Подготовка
    Boolean to_stdout = "-".equals(config.path);
    HttpClient client = buildClient(config);
    HttpRequest req = buildRequest(config);
    Long sizeLimit = config.sizeLimit;
    Path path = to_stdout ? null : Paths.get(config.path).toAbsolutePath();
    // Отправка запроса
    var respFuture = client.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream());
    var resp = respFuture.get();
    // Проверка статуса
    int statusCode = resp.statusCode();
    if (config.checkStatus && statusCode >= 400) {
      throw new RuntimeException("HTTP error: " + statusCode);
    }
    // Проверка размера файла в заголовке
    if (sizeLimit != null) {
      String contentLengthStr = resp.headers().firstValue("Content-Length").orElse(null);
      if (contentLengthStr != null) {
        try {
          long contentLength = Long.parseLong(contentLengthStr);
          if (contentLength > sizeLimit) {
            throw new RuntimeException("File too large: " + contentLength + " > " + sizeLimit);
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
    // Загрузка файла
    long downloadedSize = 0;
    try (OutputStream out = to_stdout ? System.out : Files.newOutputStream(path); InputStream in = resp.body()) {
      byte[] buffer = new byte[config.bufSize];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        downloadedSize += bytesRead;
        // Проверка фактического размера файла
        if (sizeLimit != null && sizeLimit < downloadedSize) {
          throw new RuntimeException("File too large: " + downloadedSize + " > " + sizeLimit);
        }
        out.write(buffer, 0, bytesRead);
      }
      out.flush();
    }
    return new DownloadResult(statusCode, resp.headers(), downloadedSize);
  }

  private static HttpClient buildClient(RequestConfig config) throws Exception {
    HttpClient.Builder builder = HttpClient.newBuilder();
    builder.followRedirects(config.followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
    if (!config.verify) {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[] { TRUST_ALL }, null);
      builder.sslContext(ctx);
    }
    return builder.build();
  }

  private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  };

  private static HttpRequest buildRequest(RequestConfig config) throws Exception {
    String baseUrl = config.url;
    // Query
    if (config.query != null && !config.query.isEmpty()) {
      StringBuilder queryBuilder = new StringBuilder();
      for (Map.Entry<String, String> entry : config.query.entrySet()) {
        if (queryBuilder.length() > 0) {
          queryBuilder.append("&");
        }
        queryBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
            .append("=")
            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
      }
      baseUrl += (baseUrl.contains("?") ? "&" : "?") + queryBuilder.toString();
    }
    // Сборка запроса
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl));
    // Метод
    builder.method(config.method.toUpperCase(Locale.ROOT), HttpRequest.BodyPublishers.noBody());
    // Заголовки
    if (config.headers != null) {
      for (Map.Entry<String, String> entry : config.headers.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }
    // Завершение
    return builder.build();
  }
}
