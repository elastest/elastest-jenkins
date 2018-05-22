package jenkins.plugins.elastest.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

public class ParseResultCallable extends MasterToSlaveFileCallable<List<String>>{

    private static final long serialVersionUID = 1L;
    private final long buildTime;
    private final String testResults;
    private final long nowMaster;
    

    public ParseResultCallable(String testResults, long buildTime, long nowMaster) {
        this.buildTime = buildTime;
        this.testResults = testResults;
        this.nowMaster = nowMaster;
    }

    public List<String> invoke(File ws, VirtualChannel channel) throws IOException {
        final long nowSlave = System.currentTimeMillis();
        
        FileManager fm = new FileManager();

        FileSet fs = fm.createFileSet(ws, testResults, null);
        DirectoryScanner ds = fs.getDirectoryScanner();
        List<String> result = null;

        String[] files = ds.getIncludedFiles();
        if (files.length > 0) {
            result = parse(buildTime + (nowSlave - nowMaster), ds.getBasedir(), ds.getIncludedFiles(), fm);
        }        
        return result;
    }
    
    public List<String> parse(long buildTime, File baseDir, String[] reportFiles, FileManager fm) throws IOException {

        List<String> testReportsAsString = new ArrayList<>();
        boolean parsed=false;

        for (String value : reportFiles) {
            File reportFile = new File(baseDir, value);
            // only count files that were actually updated during this build
            if (buildTime-3000/*error margin*/ <= reportFile.lastModified()) {
                testReportsAsString
                .add(fm.readFile(reportFile));
            }
        }
        return testReportsAsString;
    }

}