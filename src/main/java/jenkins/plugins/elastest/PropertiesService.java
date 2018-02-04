package jenkins.plugins.elastest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesService {

    public static String getCompatibleElasTestVersion() throws IOException {
        Properties properties = new Properties();
        try (final InputStream stream = PropertiesService.class.getClassLoader()
                .getResourceAsStream("compatible-versions.properties")) {
            properties.load(stream);
            return (String)properties.get("elastes.version");
        }
    }
}
