// Copyright 2013-2019 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.gradle.tasks.download.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.gradle.api.logging.Logger;

/**
 * Wraps around Gradle's internal progress logger. Uses reflection
 * to provide as much compatibility to different Gradle versions
 * as possible. Note that Gradle's progress logger does not belong
 * to its public API.
 * @author Michel Kraemer
 */
public class ProgressLoggerWrapper {
    private final Logger logger;
    private final Object progressLogger;
    
    /**
     * Create a progress logger wrapper
     * @param logger the Gradle logger
     * @param servicesOwner the Gradle services owner
     * @param src the URL to the file to be downloaded
     * @throws ClassNotFoundException if one of Gradle's internal classes
     * could not be found
     * @throws NoSuchMethodException if the interface of one of Gradle's
     * internal classes has changed
     * @throws InvocationTargetException if a method from one of Gradle's
     * internal classes could not be invoked
     * @throws IllegalAccessException if a method from one of Gradle's
     * internal classes is not accessible
     */
    public ProgressLoggerWrapper(Logger logger, Object servicesOwner, String src)
            throws ClassNotFoundException, NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
        this.logger = logger;

        // we are about to access an internal class. Use reflection here to provide
        // as much compatibility to different Gradle versions as possible
        
        // get ProgressLoggerFactory class
        Class<?> progressLoggerFactoryClass = Class.forName(
                "org.gradle.internal.logging.progress.ProgressLoggerFactory");

        //get ProgressLoggerFactory service
        Object serviceFactory = invoke(servicesOwner, "getServices");
        Object progressLoggerFactory = invoke(serviceFactory, "get",
                progressLoggerFactoryClass);
        
        //get actual progress logger
        progressLogger = invoke(progressLoggerFactory, "newOperation", getClass());
        
        //configure progress logger
        String desc = "Download " + src;
        invoke(progressLogger, "setDescription", desc);
        try {
            // prior to Gradle 6.0
            invoke(progressLogger, "setLoggingHeader", desc);
        } catch (ReflectiveOperationException e) {
            logger.lifecycle(desc);
        }
    }
    
    /**
     * Invoke a method using reflection
     * @param obj the object whose method should be invoked
     * @param method the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return the method's return value
     * @throws NoSuchMethodException if the method was not found
     * @throws InvocationTargetException if the method could not be invoked
     * @throws IllegalAccessException if the method could not be accessed
     */
    private static Object invoke(Object obj, String method, Object... args)
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Class<?>[] argumentTypes = new Class[args.length];
        for (int i = 0; i < args.length; ++i) {
            argumentTypes[i] = args[i].getClass();
        }
        Method m = findMethod(obj, method, argumentTypes);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }

    /**
     * Uses reflection to find a method with the given name and argument types
     * from the given object or its superclasses.
     * @param obj the object
     * @param methodName the name of the method to return
     * @param argumentTypes the method's argument types
     * @return the method object
     * @throws NoSuchMethodException if the method could not be found
     */
    private static Method findMethod(Object obj, String methodName,
            Class<?>[] argumentTypes) throws NoSuchMethodException {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) &&
                        Arrays.equals(method.getParameterTypes(), argumentTypes)) {
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException("Method " + methodName + "(" +
                Arrays.toString(argumentTypes) + ") on " + obj.getClass());
    }

    /**
     * Invoke a method using reflection but don't throw any exceptions.
     * Just log errors instead.
     * @param obj the object whose method should be invoked
     * @param method the name of the method to invoke
     * @param args the arguments to pass to the method
     */
    private void invokeIgnoreExceptions(Object obj, String method,
            Object... args) {
        try {
            invoke(obj, method, args);
        } catch (ReflectiveOperationException e) {
            logger.trace("Unable to log progress", e);
        }
    }
    
    /**
     * Start on operation
     */
    public void started() {
        invokeIgnoreExceptions(progressLogger, "started");
    }
    
    /**
     * Complete an operation
     */
    public void completed() {
        invokeIgnoreExceptions(progressLogger, "completed");
    }
    
    /**
     * Set the current operation's progress
     * @param msg the progress message
     */
    public void progress(String msg) {
        invokeIgnoreExceptions(progressLogger, "progress", msg);
    }
}
