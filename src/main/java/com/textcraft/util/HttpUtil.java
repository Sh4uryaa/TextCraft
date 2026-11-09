package com.textcraft.util;

import java.net.http.HttpClient;
import java.time.Duration;
import javax.net.ssl.SSLParameters;

/** One shared HttpClient, TLS 1.2+ only. */
public final class HttpUtil {
    private static final HttpClient CLIENT;

    static {
        SSLParameters ssl = new SSLParameters();
        ssl.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
        CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslParameters(ssl)
                .build();
    }

    private HttpUtil() { }

    public static HttpClient client() {
        return CLIENT;
    }

    /** Turns a low-level network failure into a short sentence for the user. */
    public static boolean isOffline(Throwable t) {
        while (t != null) {
            if (t instanceof java.net.ConnectException || t instanceof java.net.UnknownHostException
                    || t instanceof java.nio.channels.UnresolvedAddressException
                    || t instanceof java.net.NoRouteToHostException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
