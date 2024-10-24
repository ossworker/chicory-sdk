package org.extism.chicory.sdk.http.okhttp3.cookie;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.util.List;

/**
 * @author workoss
 */
public interface CookieStore {

    /**
     * add
     * @param uri
     * @param cookies
     */
    void add(HttpUrl uri, List<Cookie> cookies);

    /**
     * get
     * @param uri
     * @return
     */
    List<Cookie> get(HttpUrl uri);

    /**
     * get all
     * @return
     */
    List<Cookie> getCookies();

    /**
     * remove
     * @param uri
     * @param cookie
     * @return
     */
    boolean remove(HttpUrl uri, Cookie cookie);

    /**
     * remove all
     * @return
     */
    boolean removeAll();
}
