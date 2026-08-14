package me.connorjb05.skyblock.util;

import java.util.concurrent.CompletionException;

public class ExceptionUtil {

    public static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException
                && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

}
