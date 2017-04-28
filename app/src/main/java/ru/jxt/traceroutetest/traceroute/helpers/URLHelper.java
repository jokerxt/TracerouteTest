package ru.jxt.traceroutetest.traceroute.helpers;

import android.webkit.URLUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/*
* Помощник в работе с URL
* Дает возможность получить из строки валидный URL
* в противном случае выкидывает исключение UrlNotValidException
* Проверка с использованием регэкспов, дает возможность
* исключить лишние знаки, которые разрешены в URL
*/

public final class URLHelper {

    private static final Pattern VALID_HOST_REGEX;
    private static final String VALID_HOST_STRING = "^(([a-zA-Zа-яА-Я]|[a-zA-Zа-яА-Я0-9][a-zA-Zа-яА-Я0-9\\-]*[a-zA-Zа-яА-Я0-9])\\.)*([a-zA-Zа-яА-Я]|[a-zA-Zа-яА-Я][a-zA-Zа-яА-Я0-9\\-]*[a-zA-Zа-яА-Я0-9])$";
    private static final Pattern VALID_IP_REGEX;
    private static final String VALID_IP_STRING = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    static {
        VALID_IP_REGEX = Pattern.compile(VALID_IP_STRING);
        VALID_HOST_REGEX = Pattern.compile(VALID_HOST_STRING);
    }

    public static class UrlNotValidException extends Exception {
        public UrlNotValidException() {
            super();
        }
    }

    public static URL getValidURLFromString(String strurl) throws URLHelper.UrlNotValidException {
        //проверяем содержит ли полученный url тип протокола Https или Http
        if(URLUtil.isHttpsUrl(strurl) || URLUtil.isHttpUrl(strurl)) {
            try {
                URL url = new URL(strurl);
                String host = url.getHost();
                if(VALID_IP_REGEX.matcher(host).matches() | VALID_HOST_REGEX.matcher(host).matches())
                    return url;
            }
            catch (MalformedURLException ignored) {}
        }
        throw new URLHelper.UrlNotValidException();
    }
}
