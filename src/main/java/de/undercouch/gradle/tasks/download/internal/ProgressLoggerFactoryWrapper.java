package de.undercouch.gradle.tasks.download.internal;


import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import java.lang.reflect.InvocationTargetException;

public class ProgressLoggerFactoryWrapper {

    private final Logger logger;
    private final Object hasServices;

    public ProgressLoggerFactoryWrapper(Logger logger, Task task) {
        this(logger, (Object) task);
    }

    public ProgressLoggerFactoryWrapper(Logger logger, Project project) {
        this(logger, (Object) project);
    }

    private ProgressLoggerFactoryWrapper(Logger logger, Object hasServices) {
        this.logger = logger;
        this.hasServices = hasServices;
    }

    public ProgressLoggerWrapper newInstance(String src)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        return new ProgressLoggerWrapper(logger, hasServices, src);
    }
}
