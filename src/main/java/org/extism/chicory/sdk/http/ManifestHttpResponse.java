package org.extism.chicory.sdk.http;

import java.util.Map;

public class ManifestHttpResponse {


    private  Integer statusCode;

    private  Map<String, String> header;

    private byte[] body;


    public ManifestHttpResponse(Integer statusCode, Map<String, String> header, byte[] body) {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }


}
