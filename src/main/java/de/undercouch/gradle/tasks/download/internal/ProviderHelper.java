package de.undercouch.gradle.tasks.download.internal;

import org.gradle.util.GradleVersion;

/**
 * Helper methods to access {@code org.gradle.api.provider.Provider}
 * @author Michel Kraemer
 */
public class ProviderHelper {
    /**
     * If the given object is a org.gradle.api.provider.Provider, get the
     * provider's value and return it. Otherwise, just return the object.
     * @param obj the object
     * @return the provider's value or the object
     */
    public static Object tryGetProvider(Object obj) {
        if (obj == null) {
            return null;
        }

        //Provider class is only available in Gradle 4.0 or higher
        if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) > 0 && ProviderGet.isProvider(obj)) {
            return ProviderGet.getOrNull(obj);
        } else {
            return obj;
        }
    }
}
