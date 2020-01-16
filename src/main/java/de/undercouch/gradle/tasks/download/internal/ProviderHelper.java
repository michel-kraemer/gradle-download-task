package de.undercouch.gradle.tasks.download.internal;

import java.lang.reflect.Method;

/**
 * Helper methods to dynamically access {@code org.gradle.api.provider.Provider}
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
        Class<?> providerClass;
        try {
            providerClass = Class.forName("org.gradle.api.provider.Provider");
        } catch (ClassNotFoundException e) {
            return obj;
        }

        if (providerClass == null || !providerClass.isAssignableFrom(obj.getClass())) {
            return obj;
        }

        try {
            Method m = obj.getClass().getMethod("getOrNull");
            m.setAccessible(true);
            obj = m.invoke(obj);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }

        return obj;
    }
}
