package de.undercouch.gradle.tasks.download;

import groovy.lang.Closure;
import kotlin.jvm.functions.Function0;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies a file's integrity by calculating its checksum.
 * @author Michel Kraemer
 */
public class VerifyAction implements VerifySpec {
    private final ProjectLayout projectLayout;
    private File src;
    private String algorithm = "MD5";
    private String checksum;
    
    /**
     * Creates a new verify action
     * @param project the project to be built
     */
    public VerifyAction(Project project) {
        this.projectLayout = project.getLayout();
    }

    private String toHex(byte[] barr) {
        StringBuilder result = new StringBuilder();
        for (byte b : barr) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    /**
     * Starts verifying
     * @throws IOException if the file could not verified
     * @throws NoSuchAlgorithmException if the given algorithm is not available
     */
    public void execute() throws IOException, NoSuchAlgorithmException {
        if (src == null) {
            throw new IllegalArgumentException("Please provide a file to verify");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("Please provide the algorithm to "
                    + "use to calculate the checksum");
        }
        if (checksum == null) {
            throw new IllegalArgumentException("Please provide a checksum to verify against");
        }
        
        // calculate file's checksum
        MessageDigest md = MessageDigest.getInstance(algorithm);
        String calculatedChecksum;
        try (FileInputStream fis = new FileInputStream(src)) {
            byte[] buf = new byte[1024];
            int read;
            while ((read = fis.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
            calculatedChecksum = toHex(md.digest());
        }
        
        // verify checksum
        if (!calculatedChecksum.equalsIgnoreCase(checksum)) {
            throw new GradleException("Invalid checksum for file '" +
                    src.getName() + "'. Expected " + checksum.toLowerCase() + 
                    " but got " + calculatedChecksum.toLowerCase() + ".");
        }
    }
    
    @Override
    public void src(Object src) {
        if (src instanceof Function0) {
            // lazily evaluate Kotlin function
            Function0<?> function = (Function0<?>)src;
            src = function.invoke();
        }
        if (src instanceof Closure) {
            //lazily evaluate closure
            Closure<?> closure = (Closure<?>)src;
            src = closure.call();
        }
        if (src instanceof Provider) {
            src = ((Provider<?>)src).getOrNull();
        }
        if (src instanceof CharSequence) {
            src = projectLayout.getProjectDirectory().file(src.toString()).getAsFile();
        }
        if (src instanceof File) {
            this.src = (File)src;
        } else {
            throw new IllegalArgumentException("Verification source must "
                    + "either be a CharSequence or a File");
        }
    }
    
    @Override
    public void algorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void checksum(String checksum) {
        this.checksum = checksum;
    }
    
    public File getSrc() {
        return src;
    }

    public String getAlgorithm() {
        return algorithm;
    }
    
    public String getChecksum() {
        return checksum;
    }
}
