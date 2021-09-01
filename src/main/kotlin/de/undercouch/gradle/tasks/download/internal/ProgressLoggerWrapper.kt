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
package de.undercouch.gradle.tasks.download.internal

import org.gradle.api.logging.Logger
import java.lang.ClassNotFoundException
import java.lang.IllegalAccessException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

/**
 * Wraps around Gradle's internal progress logger. Uses reflection
 * to provide as much compatibility to different Gradle versions
 * as possible. Note that Gradle's progress logger does not belong
 * to its public API.
 * @author Michel Kraemer
 */
class ProgressLoggerWrapper(private val logger: Logger, servicesOwner: Any, src: String) {

    private val progressLogger: Any

    /**
     * Invoke a method using reflection but don't throw any exceptions.
     * Just log errors instead.
     * @param obj the object whose method should be invoked
     * @param method the name of the method to invoke
     * @param args the arguments to pass to the method
     */
    private fun invokeIgnoreExceptions(obj: Any, method: String,
                                       vararg args: Any) {
        try {
            invoke(obj, method, *args)
        } catch (e: ReflectiveOperationException) {
            logger.trace("Unable to log progress", e)
        }
    }

    /**
     * Start on operation
     */
    fun started() = invokeIgnoreExceptions(progressLogger, "started")

    /**
     * Complete an operation
     */
    fun completed() = invokeIgnoreExceptions(progressLogger, "completed")

    /**
     * Set the current operation's progress
     * @param msg the progress message
     */
    fun progress(msg: String?) = invokeIgnoreExceptions(progressLogger, "progress", msg!!)

    companion object {
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
        @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
        private operator fun invoke(obj: Any, method: String, vararg args: Any): Any {
            val argumentTypes: Array<Class<*>> = Array(args.size) { args[it].javaClass }
            val m = findMethod(obj, method, argumentTypes)
            m.isAccessible = true
            return m.invoke(obj, *args)
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
        @Throws(NoSuchMethodException::class) private fun findMethod(obj: Any, methodName: String,
                                                                     argumentTypes: Array<Class<*>>): Method {
            var clazz: Class<*>? = obj.javaClass
            while (clazz != null) {
                for (method in clazz.declaredMethods)
                    if (method.name == methodName && Arrays.equals(method.parameterTypes, argumentTypes))
                        return method
                clazz = clazz.superclass
            }
            throw NoSuchMethodException("Method $methodName(${argumentTypes.contentToString()}) on ${obj.javaClass}")
        }
    }

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
    init {

        //we are about to access an internal class. Use reflection here to provide
        //as much compatibility to different Gradle versions as possible

        //get ProgressLoggerFactory class
        val progressLoggerFactoryClass: Class<*>? =
            try {
                //Gradle 2.14 and higher
                Class.forName("org.gradle.internal.logging.progress.ProgressLoggerFactory")
            } catch (e: ClassNotFoundException) {
                //prior to Gradle 2.14
                Class.forName("org.gradle.logging.ProgressLoggerFactory")
            }

        //get ProgressLoggerFactory service
        val serviceFactory = invoke(servicesOwner, "getServices")
        val progressLoggerFactory = invoke(serviceFactory, "get", progressLoggerFactoryClass!!)

        //get actual progress logger
        progressLogger = invoke(progressLoggerFactory, "newOperation", javaClass)

        //configure progress logger
        val desc = "Download $src"
        invoke(progressLogger, "setDescription", desc)
        try {
            // prior to Gradle 6.0
            invoke(progressLogger, "setLoggingHeader", desc)
        } catch (e: ReflectiveOperationException) {
            logger.lifecycle(desc)
        }
    }
}