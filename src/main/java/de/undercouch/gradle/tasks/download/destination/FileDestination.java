package de.undercouch.gradle.tasks.download.destination;

import java.io.File;

/**
 * Created by berndfarka on 11.11.14.
 */
public class FileDestination extends AbstractDestination {

    private final File dest;


    public FileDestination(File dest) {
        this.dest = dest;
    }

    public FileDestination(CharSequence dest){
        this(new File(dest.toString()));
    }

    public File getFile(){
        return dest;
    }

    @Override
    public Object getValue() {
        return dest;
    }
}
