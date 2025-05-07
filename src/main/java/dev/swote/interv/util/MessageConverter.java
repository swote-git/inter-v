package dev.swote.interv.util;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

public class MessageConverter {
    public static ResourceBundleMessageSource message = new ResourceBundleMessageSource();

    private static final Locale locale = LocaleContextHolder.getLocale();

    static {
        message.setDefaultEncoding("UTF-8");
        message.setBasenames("messages/error");
    }

    public static String getMessage(String code, Object... args) {
        return message.getMessage(code, args, locale);
    }
}