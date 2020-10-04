package de.undercouch.gradle.tasks.download.internal;

import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;

/**
 * Helper methods to access {@link Provider}
 * @author Michel Kraemer
 */
@SuppressWarnings({"UnstableApiUsage", "rawtypes"})
public class ProviderHelper {
    /**
     * If the given object is a {@link Provider}, get the provider's value and
     * return it. Otherwise, just return the object.
     * @param obj the object
     * @return the provider's value or the object
     */
    public static Object tryGetProvider(Object obj) {
        if (obj == null) {
            return null;
        }

        // Provider class is only available in Gradle 4.0 or higher
        if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) > 0 &&
                obj instanceof Provider) {
            return ((Provider)obj).getOrNull();
        } else {
            return obj;
        }
    }
}
