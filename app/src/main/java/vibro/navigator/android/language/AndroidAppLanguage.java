package vibro.navigator.android.language;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.os.ConfigurationCompat;

import java.util.Locale;

import vibro.navigator.settings.AppLanguageSettings;

public final class AndroidAppLanguage {
    private AndroidAppLanguage() {
    }

    @NonNull
    public static String systemLanguageTag() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        return matchSystemLanguages(ConfigurationCompat.getLocales(configuration).toLanguageTags());
    }

    @NonNull
    public static String matchSystemLanguages(@NonNull String languageTags) {
        for (String tag : languageTags.split(",")) {
            String language = Locale.forLanguageTag(tag).getLanguage();
            for (String available : AppLanguageSettings.availableLanguageTags()) {
                if (available.equals(language)) {
                    return available;
                }
            }
        }
        return "en";
    }

    @NonNull
    public static String effectiveLanguageTag(@NonNull Context context) {
        return AppLanguageSettings.getEffectiveLanguageTag(context, systemLanguageTag());
    }

    @NonNull
    public static String getString(@NonNull Context context, @StringRes int resId, Object... formatArgs) {
        return new AndroidLanguageContext(context).getString(resId, formatArgs);
    }

    @NonNull
    static Configuration configuration(@NonNull Context context) {
        Configuration localized = new Configuration(context.getResources().getConfiguration());
        Locale locale = Locale.forLanguageTag(effectiveLanguageTag(context));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            localized.setLocales(new LocaleList(locale));
        } else {
            localized.setLocale(locale);
        }
        localized.setLayoutDirection(locale);
        return localized;
    }

    @NonNull
    static Context activityContext(@NonNull Context context) {
        return context.createConfigurationContext(configuration(context));
    }
}
