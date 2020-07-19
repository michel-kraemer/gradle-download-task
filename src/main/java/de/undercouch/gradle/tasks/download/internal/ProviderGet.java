package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.provider.Provider;

public class ProviderGet {
    public static boolean isProvider(Object destObject) {
        return destObject instanceof Provider;
    }

    public static Object getOrNull(Object destObject) {
        return ((Provider) destObject).getOrNull();
    }
}
