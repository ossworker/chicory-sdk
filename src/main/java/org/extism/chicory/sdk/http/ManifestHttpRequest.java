package org.extism.chicory.sdk.http;

import java.util.Map;

public class ManifestHttpRequest {

    private String url;
    private Map<String, String> headers;
    private String method;

    public ManifestHttpRequest() {
    }

    public ManifestHttpRequest(String url, Map<String, String> headers, String method) {
        this.url = url;
        this.headers = headers;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
