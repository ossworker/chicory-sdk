
package org.extism.chicory.sdk.http;
import com.workoss.boot.util.Lazy;
import com.workoss.boot.util.StringUtils;
import com.workoss.boot.util.json.JsonMapper;
import com.workoss.boot.util.text.EscapeUtil;
import com.workoss.boot.util.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * @author workoss
 */
@SuppressWarnings("ALL")
public class HttpUtils {

	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String APPLICATION_JSON_VALUE = "application/json";

	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	public static final String TEXT_PLAIN_VALUE = "text/plain";

	public static final String TEXT_HTML_VALUE = "text/html";

	public static final String TEXT_XML_VALUE = "text/xml";

	private static final int DEFAULT_TIMEOUT = 30 * 1000;

	private static final Lazy<HttpClient> HTTP_CLIENT_LAZY = Lazy.of(() -> getClient());

	private HttpUtils() {
	}

	private static HttpClient getClient() {
		return getClient(null, null, -1, -1, HttpClient.Version.HTTP_2, HttpClient.Redirect.NEVER,
				Duration.ofMillis(DEFAULT_TIMEOUT));
	}

	public static HttpClient getClient(SslContexts.SslConfig sslConfig, String poolName, int corePoolSize,
									   int queueSize, HttpClient.Version version, HttpClient.Redirect redirect, Duration timeout) {
		if (StringUtils.isBlank(poolName)) {
			poolName = "HTTP-POOL-";
		}
		if (sslConfig == null) {
			sslConfig = SslContexts.parse(null, null, null, null);
		}
		if (corePoolSize <= 0) {
			corePoolSize = Runtime.getRuntime().availableProcessors();
		}
		if (queueSize <= 0) {
			queueSize = 10240;
		}
		String finalPoolName = poolName;
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, corePoolSize * 4, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queueSize), new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, finalPoolName + r.hashCode());
					}

				}, new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						throw new HttpException("HTTP-POOL,is EXHAUSTED!");
					}
				});

		return HttpClient.newBuilder()
				.version(version)
				.connectTimeout(timeout)
				.sslContext(sslConfig.getSslContext())
				.followRedirects(redirect)
				.executor(threadPoolExecutor)
				.build();
	}

	public static byte[] executePostJson(String url, Map<String, String> headers, Object body, Duration timeout) {
		if (headers == null) {
			headers = new HashMap<>(4);
		}
		headers.put(CONTENT_TYPE, APPLICATION_JSON_VALUE);
		return execute(url, HttpMehtod.POST, headers, body, timeout);
	}

	public static byte[] executePostForm(String url, Map<String, String> headers, Map<String, Object> body,
			Duration timeout) {
		return execute(url, HttpMehtod.POST, headers, body, timeout);
	}

	public static void executeAsyncPostForm(String url, Map<String, String> headers, Map<String, Object> body,
			Duration timeout, Consumer<byte[]> consumer) {
		executeAsync(url, HttpMehtod.POST, headers, body, timeout, consumer);
	}

	public static byte[] execute(String url, HttpMehtod mehtod, Map<String, String> headers, Object body,
			Duration timeout) {
			return handleResponse(executeWithResponse(url,mehtod,headers,body,timeout));
	}

	public static HttpResponse<InputStream> executeWithResponse(String url, HttpMehtod mehtod, Map<String, String> headers, Object body,
								 Duration timeout) {
		if (headers == null) {
			headers = new HashMap<>(4);
		}
		String contentType = headers.getOrDefault(CONTENT_TYPE, APPLICATION_JSON_VALUE);

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
				.method(mehtod.name(), formatBody(contentType, body))
				.timeout(timeout == null ? Duration.ofMillis(DEFAULT_TIMEOUT) : timeout);

		headers.put(CONTENT_TYPE, contentType);
		headers.entrySet().forEach(
				stringStringEntry -> requestBuilder.header(stringStringEntry.getKey(), stringStringEntry.getValue()));

		try {
			return HTTP_CLIENT_LAZY.get().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
		}
		catch (java.net.http.HttpTimeoutException timeoutException) {
			throw new HttpException(timeoutException);
		}
		catch (IOException | InterruptedException e) {
			throw new HttpException(e);
		}
	}

	public static void executeAsync(String url, HttpMehtod mehtod, Map<String, String> headers, Object body,
			Duration timeout, Consumer<byte[]> consumer) {
		executeAsync(url, mehtod, headers, body, timeout).thenAccept(consumer).exceptionally(throwable -> {
			if (throwable instanceof java.net.http.HttpTimeoutException) {
				throw new HttpException(throwable);
			}
			throw new HttpException(throwable);
		}).join();
	}

	public static CompletableFuture<byte[]> executeAsync(String url, HttpMehtod mehtod, Map<String, String> headers,
			Object body, Duration timeout) {
		if (headers == null) {
			headers = new HashMap<>(4);
		}
		String contentType = headers.getOrDefault(CONTENT_TYPE, APPLICATION_JSON_VALUE);

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url))
				.method(mehtod.name(), formatBody(contentType, body))
				.timeout(timeout == null ? Duration.ofMillis(DEFAULT_TIMEOUT) : timeout);

		headers.put(CONTENT_TYPE, contentType);
		headers.entrySet().forEach(
				stringStringEntry -> requestBuilder.header(stringStringEntry.getKey(), stringStringEntry.getValue()));

		return HTTP_CLIENT_LAZY.get().sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream())
				.thenApply(response -> handleResponse(response));
	}

	public static byte[] handleResponse(HttpResponse<InputStream> response) {
		byte[] respBody = null;
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			InputStream inputStream = response.body();
			if (inputStream != null || inputStream.available() > 0) {
				String contentEncoding = response.headers().firstValue("Content-Encoding").orElse(StringUtils.EMPTY);
				if (contentEncoding.equalsIgnoreCase("gzip")) {
					// 解压
					try (InputStream gzipInputStream = new GZIPInputStream(inputStream)) {
						gzipInputStream.transferTo(byteArrayOutputStream);
					}
				}
				else {
					inputStream.transferTo(byteArrayOutputStream);
				}
			}
			respBody = byteArrayOutputStream.toByteArray();
		}
		catch (IOException e) {
			throw new HttpException(e);
		}
		if (response.statusCode() < 200 && response.statusCode() >= 400) {
			throw new HttpException(String.valueOf(response.statusCode()),
					new String(respBody, StandardCharsets.UTF_8));
		}
		return respBody;
	}

	private static HttpRequest.BodyPublisher formatBody(String contentType, Object body) {
		if (body == null) {
			return HttpRequest.BodyPublishers.noBody();
		}
		if (body instanceof String) {
			return HttpRequest.BodyPublishers.ofString((String) body);
		}
		if (StringUtils.isBlank(contentType)) {
			contentType = TEXT_HTML_VALUE;
		}
		byte[] reqBody;

		HttpRequest.BodyPublisher bodyPublisher = null;
		switch (contentType) {
		case APPLICATION_FORM_URLENCODED_VALUE:
			if (body instanceof Map) {
				bodyPublisher = ofFormData((Map<String, Object>) body);
			}
			else {
				throw new HttpException("application/x-www-form-urlencoded 暂时只支持map body入参");
			}
			break;
		case MULTIPART_FORM_DATA_VALUE:
			if (body instanceof Map) {
				bodyPublisher = ofFormMultipartData(null, (Map<String, Object>) body);
			}
			else {
				throw new HttpException("multipart/form-data 暂时只支持map body入参");
			}
			break;
		case TEXT_XML_VALUE:
			if (body instanceof byte[]) {
				bodyPublisher = HttpRequest.BodyPublishers.ofByteArray((byte[]) body);
			}else{
				bodyPublisher = HttpRequest.BodyPublishers.ofString(XmlMapper.toXmlString(body), StandardCharsets.UTF_8);
			}
			break;
		case TEXT_PLAIN_VALUE:
		case APPLICATION_JSON_VALUE:
		default:
			if (body instanceof byte[]) {
				bodyPublisher = HttpRequest.BodyPublishers.ofByteArray((byte[]) body);
			}else{
				bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(JsonMapper.toJSONBytes(body));
			}
			break;
		}

		return bodyPublisher;
	}

	private static HttpRequest.BodyPublisher ofFormData(Map<String, Object> body) {
		StringJoiner urlJoiner = new StringJoiner("&");
		for (Map.Entry<String, Object> stringObjectEntry : body.entrySet()) {
			String key = stringObjectEntry.getKey();
			Object value = stringObjectEntry.getValue();
			if (StringUtils.isBlank(key)) {
				continue;
			}
			if (!Objects.isNull(value)) {

				urlJoiner.add(EscapeUtil.urlEncode(key, false)).add("=")
						.add(EscapeUtil.urlEncode(value.toString(), false));
			}
		}
		return HttpRequest.BodyPublishers.ofString(urlJoiner.toString(), StandardCharsets.UTF_8);
	}

	private static HttpRequest.BodyPublisher ofFormMultipartData(String boundary, Map<String, Object> body) {
		try {
			List<byte[]> byteArrays = new ArrayList<>();
			if (StringUtils.isBlank(boundary)) {
				boundary = new BigInteger(256, new Random()).toString();
			}
			byte[] seperator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
					.getBytes(StandardCharsets.UTF_8);
			for (Map.Entry<String, Object> stringObjectEntry : body.entrySet()) {
				byteArrays.add(seperator);
				String key = stringObjectEntry.getKey();
				Object value = stringObjectEntry.getValue();
				if (value == null) {
					continue;
				}
				if (value instanceof Path) {
					Path path = (Path) value;
					String mimeType = Files.probeContentType(path);
					byteArrays.add(("\"" + key + "\"; filename=\"" + path.getFileName() + "\"\r\nContent-Type: "
							+ mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
					byteArrays.add(Files.readAllBytes(path));
					byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
				}
				else {
					byteArrays.add(
							("\"" + key + "\"\r\n\r\n" + value.toString() + "\r\n").getBytes(StandardCharsets.UTF_8));
				}
			}
			byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
			return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
		}
		catch (IOException e) {
			throw new HttpException(e);
		}
	}

	public enum HttpMehtod {

		GET, POST, HEAD, OPTIONS, PUT, DELETE

	}

}
