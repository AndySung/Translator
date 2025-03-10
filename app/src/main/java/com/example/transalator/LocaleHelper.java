package com.example.transalator;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {
    public static Context setLocale(Context context, String language) {
        Locale locale;
        if (language.isEmpty()) {
            locale = new Locale("zh");
        } else {
            locale = new Locale(language);
        }
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }
}