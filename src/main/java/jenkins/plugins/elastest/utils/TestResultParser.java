package jenkins.plugins.elastest.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestResultParser {
    private static final Logger LOG = LoggerFactory
            .getLogger(TestResultParser.class);

    public List<String> prepareTestReportsAsString(String workspacePath,
            String testResultFilePattern) throws Exception {
        List<String> testReportsAsString = new ArrayList<>();
        LOG.info("Preparing test reports to be sended to ElasTest");

        try {
            FileManager fm = new FileManager();

            FileSet fs = fm.createFileSet(new File(workspacePath),
                    testResultFilePattern, null);
            DirectoryScanner ds = fs.getDirectoryScanner();

            List<String> files = Arrays.asList(ds.getIncludedFiles());
            for (String file : files) {
                LOG.info("Test result file: " + file);
                String absoluteFilePath = workspacePath + "/" + file;
                LOG.debug("Content of the test results file: "
                        + fm.readFile(new File(absoluteFilePath)));
                testReportsAsString
                        .add(fm.readFile(new File(absoluteFilePath)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error processing test results files.");
        }

        return testReportsAsString;
    }

}
