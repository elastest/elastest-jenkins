package jenkins.plugins.elastest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

public class FileManager {
    
    public FileSet createFileSet(@Nonnull File baseDir, @Nonnull String includes, @CheckForNull String excludes) {
        FileSet fs = new FileSet();
        fs.setDir(baseDir);
        fs.setProject(new Project());

        StringTokenizer tokens;

        tokens = new StringTokenizer(includes,",");
        while(tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            fs.createInclude().setName(token);
        }
        if(excludes!=null) {
            tokens = new StringTokenizer(excludes,",");
            while(tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                fs.createExclude().setName(token);
            }
        }
        return fs;
    }
    
    public String readFile(File file) throws IOException {
        String content = null;

        if (!file.isDirectory()) {
            try {
                content = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {                
                throw e;
            }
        }

        return content;
    }

}
