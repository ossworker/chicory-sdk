package org.extism.chicory.sdk;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import com.google.common.net.MediaType;
import com.workoss.boot.util.Lazy;
import com.workoss.boot.util.collection.CollectionUtils;
import com.workoss.boot.util.json.JsonMapper;
import com.workoss.boot.util.reflect.ClassUtils;
import okhttp3.Response;
import org.extism.chicory.sdk.http.HttpUtils;
import org.extism.chicory.sdk.http.ManifestHttpRequest;
import org.extism.chicory.sdk.http.ManifestHttpResponse;
import org.extism.chicory.sdk.http.okhttp3.OkHttpUtil;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.extism.chicory.sdk.Kernel.IMPORT_MODULE_NAME;

public class CustomHostFunction {

    private static final Lazy<Boolean> CHECK_OKHTTP = Lazy.of(
            () -> ClassUtils.isPresent("okhttp3.OkHttpClient", CustomHostFunction.class.getClassLoader()));

    public static ExtismHostFunction[] newHostFunctions(Manifest manifest,Logger logger) {
        ManifestHttpResponse lastResp = new ManifestHttpResponse(200, Map.of(), new byte[]{});
        return new ExtismHostFunction[]{toHttpRequestFun(manifest,lastResp),
                toHttpStatusCodeFun(lastResp),
                toHttpHeaderFun(lastResp),
                toGetLoggerLevel(logger)
        };
    }


    static ExtismHostFunction toHttpStatusCodeFun(ManifestHttpResponse lastResp) {
        return ExtismHostFunction.of(IMPORT_MODULE_NAME, "http_status_code", (currentPlugin, values) -> {
                                         Integer statusCode = Optional.ofNullable(lastResp)
                                                 .map(ManifestHttpResponse::getStatusCode)
                                                 .orElse(200);
                                         return new Value[]{Value.i32(statusCode)};
                                     },
                                     List.of(),
                                     List.of(ValueType.I32));
    }

    static ExtismHostFunction toHttpRequestFun(Manifest manifest,ManifestHttpResponse lastResp) {
        return ExtismHostFunction.of(IMPORT_MODULE_NAME, "http_request", (currentPlugin, values) -> {
                                         ManifestHttpRequest request = JsonMapper.parseObject(currentPlugin.memory().readBytes(values[0].asLong()),
                                                                                              ManifestHttpRequest.class);

                                         byte[] body = currentPlugin.memory().readBytes(values[1].asLong());
                                         if (body == null) {
                                             body = new byte[0];
                                         }

                                         ManifestHttpResponse response = httpRequest(manifest,request, body);
                                         lastResp.setBody(response.getBody());

                                         long ptr = currentPlugin.memory()
                                                 .writeBytes(response.getBody());

                                         return new Value[]{Value.i64(ptr)};


                                     },
                                     List.of(ValueType.I64, ValueType.I64),
                                     List.of(ValueType.I64));
    }

    static ExtismHostFunction toGetLoggerLevel(Logger logger) {
        return ExtismHostFunction.of(IMPORT_MODULE_NAME, "get_log_level", (currentPlugin, values) ->
                                             new Value[]{Value.i32(getLogLevel(logger))},
                                     List.of(),
                                     List.of(ValueType.I32));
    }

    static int getLogLevel(Logger logger){
        int levelNum = 0;
        if (logger == null) {
            return levelNum;
        }
        for (Logger.Level level : Logger.Level.values()) {
            if (level.getSeverity() < 0) {
                continue;
            }
            if (logger.isLoggable(level)){
                return levelNum;
            }
            levelNum++;

        }
        return levelNum;
    }


    static ManifestHttpResponse httpRequest(Manifest manifest,ManifestHttpRequest request, byte[] body) {
        // 域名拦截
        boolean checkDomain = Optional.ofNullable(manifest.getAllowHosts()).orElse(List.of())
                .stream().anyMatch(allowHost -> URI.create(request.getUrl()).getHost().equalsIgnoreCase(allowHost));
        if (!checkDomain) {
            return new ManifestHttpResponse(403, Map.of(), new byte[]{});
        }
        //优先 okhttp3
        if (Boolean.TRUE.equals(CHECK_OKHTTP.get())) {
            try (Response response = OkHttpUtil.execute(request.getUrl(), null, request.getMethod(),
                                                        request.getHeaders(),
                                                        MediaType.PLAIN_TEXT_UTF_8, body)) {
                Map<String, String> headers = response.headers().toMultimap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, v -> {
                            List<String> value = v.getValue();
                            return CollectionUtils.isNotEmpty(value) ? value.get(0) : null;
                        }));
                byte[] bytes = OkHttpUtil.handleResponse(response);
                return new ManifestHttpResponse(response.code(), headers, bytes);
            }
        }
        HttpResponse<InputStream> response = HttpUtils.executeWithResponse(request.getUrl(),
                                                                           HttpUtils.HttpMehtod.valueOf(
                                                                                   request.getMethod()
                                                                                           .toUpperCase()),
                                                                           request.getHeaders(),
                                                                           body,
                                                                           Duration.ofSeconds(60));
        HttpHeaders headers = response.headers();
        Map<String, String> respHeader = headers.map().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> {
                    List<String> value = v.getValue();
                    return CollectionUtils.isNotEmpty(value) ? value.get(0) : null;
                }));
        return new ManifestHttpResponse(response.statusCode(), respHeader, HttpUtils.handleResponse(response));
    }

    static ExtismHostFunction toHttpHeaderFun(ManifestHttpResponse lastResp) {
        return ExtismHostFunction.of(IMPORT_MODULE_NAME, "http_headers", (currentPlugin, values) -> {
                                         Map<String, String> headers = Optional.ofNullable(lastResp)
                                                 .map(ManifestHttpResponse::getHeader)
                                                 .orElse(Map.of());
                                         long ptr = currentPlugin.memory()
                                                 .writeBytes(JsonMapper.toJSONBytes(headers));
                                         return new Value[]{Value.i64(ptr)};
                                     },
                                     List.of(),
                                     List.of(ValueType.I64));
    }

}
