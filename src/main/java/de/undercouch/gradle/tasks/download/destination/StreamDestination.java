package de.undercouch.gradle.tasks.download.destination;

import org.simpleframework.util.buffer.Stream;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by berndfarka on 11.11.14.
 */
public class StreamDestination extends AbstractDestination {

    private final OutputStream destination;

    public StreamDestination(OutputStream destination) {
        this.destination = destination;
    }


    public OutputStream getStream(){
        return destination;
    }

    @Override
    public Object getValue() {
        return destination;
    }
}
