package jenkins.plugins.elastest.utils;

import com.github.zafarkhaja.semver.Version;

public class Utils {

    public static boolean isCompatibleVersions(String storedVersion,
            String comparableVersion) {
        boolean result = true;
        if (!comparableVersion.equals("dev")) {
            Version initialVersion = Version.valueOf(storedVersion);
            Version actualVersion = Version.valueOf(comparableVersion);
            result = initialVersion.lessThanOrEqualTo(actualVersion);
        }
        return result;
    }

}
