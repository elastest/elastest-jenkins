/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.elastest.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Processes test reports to send to ElasTest
 * 
 * @author Francisco R. Díaz
 * @since 0.0.1
 */
public class ParseResultCallable
        extends MasterToSlaveFileCallable<List<String>> {

    private static final long serialVersionUID = 1L;
    private final long buildTime;
    private final String testResults;
    private final long nowMaster;

    public ParseResultCallable(String testResults, long buildTime,
            long nowMaster) {
        this.buildTime = buildTime;
        this.testResults = testResults;
        this.nowMaster = nowMaster;
    }

    public List<String> invoke(File ws, VirtualChannel channel)
            throws IOException {
        final long nowSlave = System.currentTimeMillis();

        FileManager fm = new FileManager();

        FileSet fs = fm.createFileSet(ws, testResults, null);
        DirectoryScanner ds = fs.getDirectoryScanner();
        List<String> result = null;

        String[] files = ds.getIncludedFiles();
        if (files.length > 0) {
            result = parse(buildTime + (nowSlave - nowMaster), ds.getBasedir(),
                    ds.getIncludedFiles(), fm);
        }
        return result;
    }

    public List<String> parse(long buildTime, File baseDir,
            String[] reportFiles, FileManager fm) throws IOException {
        List<String> testReportsAsString = new ArrayList<>();
        boolean parsed = false;

        for (String value : reportFiles) {
            File reportFile = new File(baseDir, value);
            // only count files that were actually updated during this build
            if (buildTime - 3000 <= reportFile.lastModified()) {
                testReportsAsString.add(fm.readFile(reportFile));
            }
        }
        return testReportsAsString;
    }

}