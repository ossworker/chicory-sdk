package org.extism.chicory.sdk.http.okhttp3.cookie;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.List;

/**
 * @author workoss
 */
public class DefaultCookieJar implements CookieJar {
    private CookieStore cookieStore;

    public DefaultCookieJar(CookieStore cookieStore) {
        if (cookieStore == null) {
            throw new NullPointerException("CookieStore may not be null.");
        }
        this.cookieStore = cookieStore;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookieStore.add(url, cookies);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return cookieStore.get(url);
    }
}
