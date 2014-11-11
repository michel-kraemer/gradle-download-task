package de.undercouch.gradle.tasks.download.destination;

import java.io.File;
import java.io.OutputStream;

/**
 * Created by berndfarka on 11.11.14.
 *
 * Abstract class abstracting the destionation
 *
 */
public abstract class AbstractDestination {

    /**
     * creates an
     * @param dest to be downloaded, can be an CharSequence, File or OutputStream
     * @return An instance of a AbstractDestination and never null
     * @throws java.lang.IllegalArgumentException if dest is not supported
     */
    public static AbstractDestination create(Object dest){
        if (dest == null) {
            throw new IllegalArgumentException("Please provide a download destination");
        }

        if (dest instanceof CharSequence) {
            return new FileDestination(dest.toString());
        } else if (dest instanceof File) {
            return new FileDestination((File)dest);
        }else if(dest instanceof OutputStream){
            return new StreamDestination((OutputStream) dest);
        }
            throw new IllegalArgumentException("Download destination must " +
                    "either be a File or a CharSequence");

    }

    public abstract Object getValue();




}
