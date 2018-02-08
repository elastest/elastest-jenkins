package jenkins.plugins.elastest.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilsTest {
    
    @Test
    public void isCompatibleVersionsOKTest() {
        assertTrue(Utils.isCompatibleVersions("1.1.0", "1.1.2-beta4"));
        assertTrue(Utils.isCompatibleVersions("1.1.2-beta4", "1.1.2-beta4"));
    }
    
    @Test
    public void isCompatibleVersionsKOTest() {
        assertFalse(Utils.isCompatibleVersions("1.1.4", "1.1.2-beta4"));
    }
    
    @Test
    public void isCompatibleVersionsDevest() {
        assertTrue(Utils.isCompatibleVersions("1.1.4", "dev"));
    }

}
