package org.extism.chicory.sdk.http.okhttp3;

import com.workoss.boot.util.StreamUtils;
import com.workoss.boot.util.StringUtils;
import com.workoss.boot.util.collection.CollectionUtils;
import com.workoss.boot.util.json.JsonMapper;
import com.workoss.boot.util.text.BaseEncodeUtil;
import okhttp3.Authenticator;
import okhttp3.Callback;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import org.extism.chicory.sdk.http.HttpException;
import org.extism.chicory.sdk.http.okhttp3.cookie.DefaultCookieJar;
import org.extism.chicory.sdk.http.okhttp3.cookie.MemoryCookieStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * okhttp util
 *
 * @author workoss
 */
@SuppressWarnings("ALL")
public class OkHttpUtil {
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = LoggerFactory.getLogger(OkHttpUtil.class);
    private static final OkHttpClient.Builder BUILD = new OkHttpClient.Builder();

    public static MediaType XML_UTF_8 = MediaType.parse("text/xml; charset=utf-8");
    public static MediaType JSON_UTF_8 = MediaType.parse("application/json; charset=utf-8");
    public static MediaType PLAIN_TEXT_UTF_8 = MediaType.parse("text/plain; charset=utf-8");


    private static OkHttpClient client;

    private OkHttpUtil() {

    }


    public static OkHttpClient newInstance(String username, String password, Interceptor... interceptors) {
        CookieJar cookieJar = new DefaultCookieJar(new MemoryCookieStore());
        SslContexts.SslConfig sslConfig = SslContexts.parse(null, null, null, null);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder newBuild = new OkHttpClient.Builder();
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            newBuild = newBuild.authenticator(new Authenticator() {
                public Request authenticate(Route route, Response response) throws IOException {
                    Request.Builder requestBuilder = response.request().newBuilder();
                    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                        requestBuilder.header("Authorization", "Basic " + BaseEncodeUtil.encodeBase64(
                                (username + ":" + password).getBytes(StandardCharsets.UTF_8)));
                    }
                    return requestBuilder.build();
                }
            });
        }
        if (interceptors != null) {
            for (Interceptor interceptor : interceptors) {
                newBuild.addInterceptor(interceptor);
            }
        }
        OkHttpClient okHttpClient = newBuild
                .cookieJar(cookieJar)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                })
                .sslSocketFactory(sslConfig.getSslSocketFactory(), sslConfig.getX509TrustManager())
//                            .addNetworkInterceptor(loggingInterceptor)
//                            .eventListenerFactory(PrintEventListener.FACTORY)
                .build();
        okHttpClient.dispatcher().setMaxRequestsPerHost(1000);
        okHttpClient.dispatcher().setMaxRequests(10000);
        return okHttpClient;
    }


    public static OkHttpClient getInstance() {
        if (client == null) {
            synchronized (OkHttpUtil.class) {
                if (client == null) {
                    CookieJar cookieJar = new DefaultCookieJar(new MemoryCookieStore());
                    SslContexts.SslConfig sslConfig = SslContexts.parse(null, null, null, null);
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    client = BUILD
                            .cookieJar(cookieJar)
                            .connectTimeout(300, TimeUnit.SECONDS)
                            .readTimeout(300, TimeUnit.SECONDS)
                            .writeTimeout(300, TimeUnit.SECONDS)
                            .hostnameVerifier(new HostnameVerifier() {
                                @Override
                                public boolean verify(String s, SSLSession sslSession) {
                                    return true;
                                }
                            })
                            .sslSocketFactory(sslConfig.getSslSocketFactory(), sslConfig.getX509TrustManager())
//                            .addNetworkInterceptor(loggingInterceptor)
//                            .eventListenerFactory(PrintEventListener.FACTORY)
                            .build();

                    client.dispatcher().setMaxRequestsPerHost(1000);
                    client.dispatcher().setMaxRequests(10000);
                }
            }
        }
        return client;
    }


    public static Response execute(String url, Map<String, String> uriVariables, String method,
                                   Map<String, String> headers, MediaType mediaType, Object paramBody) {
        try {
            if ("POST".equalsIgnoreCase(method)) {
                return post(url, uriVariables, headers, mediaType, paramBody);
            }else if ("GET".equalsIgnoreCase(method)){
                return getResponse(url,uriVariables,headers);
            }
        } catch (IOException e) {
            throw new HttpException(e);
        }
        throw new HttpException("Unsupported HTTP method: " + method);
    }

    public static byte[] handleResponse(Response response) {
        if (response.body()==null){
            return new byte[0];
        }
        byte[] respBody = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            InputStream inputStream = response.body().byteStream();
            if (inputStream != null || inputStream.available() > 0) {
                String contentEncoding = response.headers().get("Content-Encoding");
                if (contentEncoding!=null && contentEncoding.equalsIgnoreCase("gzip")) {
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
        if (response.code() < 200 && response.code() >= 400) {
            throw new HttpException(String.valueOf(response.code()),
                                    new String(respBody, StandardCharsets.UTF_8));
        }
        return respBody;
    }

    public static String get(String url, Map<String, String> uriVariables) throws IOException {
        Response response = getResponse(url, uriVariables,null);
        if (response != null && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    public static Response postJSONResponse(String url, Map<String, String> uriVariables, Object jsonObject)
            throws IOException {
        return postJSONResponse(url, uriVariables, null, jsonObject);
    }

    public static Response postJSONResponse(String url, Map<String, String> uriVariables, Map<String, String> headers,
                                            Object jsonObject) throws IOException {

        Response response = post(url, uriVariables, headers, JSON_UTF_8,
                                 formatterParams(jsonObject, JSON_UTF_8));
        if (response != null && response.body() != null) {
            return response;
        }
        return null;
    }

    public static void postJSON(String url, Map<String, String> uriVariables, Object jsonObject, Callback callback)
            throws IOException {
        postJSON(url, uriVariables, null, jsonObject, callback);
    }

    public static void postJSON(String url, Map<String, String> uriVariables, Map<String, String> headers,
                                Object jsonObject, Callback callback) throws IOException {
        InputStream inputStream = formatterParams(jsonObject, JSON_UTF_8);
        post(url, uriVariables, headers, JSON_UTF_8, inputStream, callback);
    }

    public static String postJSON(String url, Map<String, String> uriVariables, Object jsonObject) throws IOException {
        return postJSON(url, uriVariables, null, jsonObject);
    }

    public static String postJSON(String url, Map<String, String> uriVariables, Map<String, String> headers,
                                  Object jsonObject) throws IOException {
        Response response = post(url, uriVariables, headers, JSON_UTF_8,
                                 formatterParams(jsonObject, JSON_UTF_8));
        if (response != null && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    public static String postXml(OkHttpClient client, String url, Map<String, String> uriVariables, Object xmlObject)
            throws IOException {

        InputStream inputStream = formatterParams(xmlObject, XML_UTF_8);
        RequestBody requestBody = OkHttpUtil.generateRequestBody(XML_UTF_8,xmlObject);
        Request request = new Request.Builder()
                .url(buildUrl(url, uriVariables))
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.body() != null) {
            return response.body().string();
        }
        return null;
    }

    private static InputStream formatterParams(Object paramObject, MediaType mediaType) throws IOException {
        if (paramObject == null) {
            return new ByteArrayInputStream(null);
        }
        if (mediaType == null) {
            mediaType = PLAIN_TEXT_UTF_8;
        }
        Charset charset = mediaType.charset(CHARSET);
        byte[] bytes = null;
        if (paramObject instanceof String) {
            bytes = ((String) paramObject).getBytes(charset);
        } else if (paramObject instanceof byte[]) {
            bytes = (byte[]) paramObject;
        } else {
            String subtype = mediaType.subtype();
            if ("json".equalsIgnoreCase(subtype)) {
                //序列化成json
                bytes = JsonMapper.build().getMapper().writeValueAsBytes(paramObject);
            } else if ("xml".equalsIgnoreCase(subtype)) {

            }
        }
        return new ByteArrayInputStream(bytes);

    }


    public static Response post(String url, Map<String, String> uriVariables, Map<String, String> headers,
                                MediaType mediaType, Object param) throws IOException {
        RequestBody requestBody = OkHttpUtil.generateRequestBody(mediaType, param);
        Request.Builder requestBuilder = new Request.Builder()
                .url(buildUrl(url, uriVariables))
                .post(requestBody);
        if (CollectionUtils.isNotEmpty(headers)) {
            requestBuilder.headers(Headers.of(headers));
        }
        return getInstance().newCall(requestBuilder.build()).execute();
    }

    public static Response formData(String url, Map<String, String> uriVariables, MultipartBody multipartBody)
            throws IOException {
        Request request = new Request.Builder()
                .url(buildUrl(url, uriVariables))
                .post(multipartBody)
                .build();
        return getInstance().newCall(request).execute();
    }


    public static void post(String url, Map<String, String> uriVariables, Map<String, String> headers,
                            MediaType mediaType, Object param, Callback callback) throws IOException {
        RequestBody requestBody = OkHttpUtil.generateRequestBody(mediaType, mediaType);
        Request request = new Request.Builder()
                .url(buildUrl(url, uriVariables))
                .post(requestBody)
                .build();
        getInstance().newCall(request).enqueue(callback);
    }

    public static Response getResponse(String url, Map<String, String> uriVariables,Map<String, String> headers) throws IOException {
        Request request = new Request.Builder()
                .url(OkHttpUtil.buildUrl(url, uriVariables))
                .get()
                .build();
        return getInstance().newCall(request).execute();
    }


    public static String buildUrl(String url, Map<String, String> uriVariables) {
        if (CollectionUtils.isNotEmpty(uriVariables)) {
            url = StringUtils.renderString(url, uriVariables);
        }
        return url;
    }


    static RequestBody generateRequestBody(final MediaType mediaType, final Object object) {
        if (object == null) {
            return RequestBody.create(new byte[0]);
        }
        byte[] bytes = new byte[0];
        if (object instanceof InputStream) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                if (StreamUtils.copy((InputStream) object, outputStream) == -1) {
                    throw new IOException("Copy failed");
                }
                bytes = outputStream.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Reading stream failed->", e);
            } finally {
                closeQuietly((InputStream) object);
            }
        } else if (object instanceof byte[]) {
            bytes = (byte[]) object;
        }
        if (mediaType == null) {
            return RequestBody.create(bytes);
        }
        return RequestBody.create(bytes, okhttp3.MediaType.parse(mediaType.toString()));
    }

    protected static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException var2) {
        }

    }

}
