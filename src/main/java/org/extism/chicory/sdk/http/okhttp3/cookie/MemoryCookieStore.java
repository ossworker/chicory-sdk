package org.extism.chicory.sdk.http.okhttp3.cookie;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author workoss
 */
public class MemoryCookieStore implements CookieStore {
    private static Map<String, List<Cookie>> allCookies = new ConcurrentHashMap<>();

    @Override
    public void add(HttpUrl uri, List<Cookie> cookies) {
        if (uri == null) {
            throw new NullPointerException("Uri must not be null.");
        }
        if (!(cookies!=null&&cookies.size()>0)) {
            throw new NullPointerException("Cookies must not be null.");
        }

        allCookies.put(uri.host(),cookies);
    }

    @Override
    public List<Cookie> get(HttpUrl uri) {
        if (uri == null) {
            throw new NullPointerException("Uri must not be null.");
        }
        List<Cookie> cookies = allCookies.get(uri.host());
        if (cookies == null) {
            cookies = new ArrayList<>();
            allCookies.put(uri.host(), cookies);
        }
        return cookies;
    }

    @Override
    public List<Cookie> getCookies() {
        List<Cookie> cookies = new ArrayList<>(20);
        for (String host : allCookies.keySet()) {
            cookies.addAll(allCookies.get(host));
        }
        return cookies;
    }

    @Override
    public boolean remove(HttpUrl uri, Cookie cookie) {
        if (uri == null) {
            throw new NullPointerException("Uri must not be null.");
        }
        if (cookie == null) {
            throw new NullPointerException("Cookie must not be null.");
        }
        return allCookies.remove(uri.host()) != null;
    }

    @Override
    public boolean removeAll() {
        allCookies.clear();
        return true;
    }
}
