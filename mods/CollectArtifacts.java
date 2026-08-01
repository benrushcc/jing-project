import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Collect module artifacts and required dependencies (JUnit, JMH) into the mods directory.
 * This script is meant to be placed inside the mods directory.
 * It uses `mvn dependency:get` to download dependencies from Maven Central.
 * No output is produced on success; errors are thrown as exceptions.
 */
public class CollectArtifacts {

    private static final Map<String, String> DEPENDENCIES = new LinkedHashMap<>();
    static {
        DEPENDENCIES.put("org.junit.jupiter:junit-jupiter-api", "junit.version");
        DEPENDENCIES.put("org.openjdk.jmh:jmh-core", "jmh.version");
    }

    public static void main(String[] args) throws Exception {
        Path rootDir = Paths.get("..").toRealPath();
        Path modsDir = Paths.get(".").toRealPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path entry : stream) {
                Files.deleteIfExists(entry);
                System.out.println("removing : " + entry.getFileName());
            }
        }
        Path rootPom = rootDir.resolve("pom.xml");
        if (!Files.exists(rootPom)) {
            throw new RuntimeException("root pom.xml not found at " + rootPom);
        }
        Map<String, String> versions = readVersionsFromPom(rootPom);
        for (Map.Entry<String, String> entry : DEPENDENCIES.entrySet()) {
            String gav = entry.getKey();
            String versionProperty = entry.getValue();
            String version = versions.get(versionProperty);
            if (version == null || version.isEmpty()) {
                throw new RuntimeException("version property " + versionProperty + " not found in root pom.xml");
            }
            String[] parts = gav.split(":");
            if (parts.length != 2) {
                throw new RuntimeException("invalid GAV format: " + gav);
            }
            String groupId = parts[0];
            String artifactId = parts[1];
            downloadDependency(groupId, artifactId, version, modsDir);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDir, entry -> Files.isDirectory(entry))) {
            for (Path subDir : stream) {
                if (subDir.getFileName().toString().equals("mods")) {
                    continue;
                }
                Path pomFile = subDir.resolve("pom.xml");
                Path targetDir = subDir.resolve("target");
                if (Files.exists(pomFile) && Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                    copyModuleJars(targetDir, modsDir);
                }
            }
        }
    }

    private static Map<String, String> readVersionsFromPom(Path pomPath) {
        try {
            Map<String, String> versions = new HashMap<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomPath.toFile());
            NodeList propertiesNodes = doc.getElementsByTagName("properties");
            if (propertiesNodes.getLength() == 0) {
                return versions;
            }
            Element propertiesElem = (Element) propertiesNodes.item(0);
            NodeList children = propertiesElem.getChildNodes();
            Set<String> wantedProps = new HashSet<>(DEPENDENCIES.values());
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String tag = child.getNodeName();
                    if (wantedProps.contains(tag)) {
                        versions.put(tag, child.getTextContent().trim());
                    }
                }
            }
            return versions;
        } catch (Exception e) {
            throw new RuntimeException("failed to read verisons from root pom.xml", e);
        }
    }

    private static void downloadDependency(String groupId, String artifactId, String version, Path destDir) throws Exception {
        String jarName = artifactId + "-" + version + ".jar";
        Path targetFile = destDir.resolve(jarName);
        System.out.println("downloading dependency : " + jarName);
        ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd": "mvn",
                "dependency:get",
                "-DgroupId=" + groupId,
                "-DartifactId=" + artifactId,
                "-Dversion=" + version,
                "-Ddest=" + targetFile.toAbsolutePath().toString()
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                throw new RuntimeException("Failed to download dependency " + groupId + ":" + artifactId + ":" + version +
                        ", exit code " + exitCode + "\n" + sb);
            }
        }
    }

    private static void copyModuleJars(Path targetDir, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir, "*.jar")) {
            for (Path jar : stream) {
                String name = jar.getFileName().toString();
                if (name.contains("-sources") || name.contains("-javadoc")) {
                    continue;
                }
                Path target = destDir.resolve(name);
                System.out.println("copying module jar : " + name);
                Files.copy(jar, target);
            }
        }
    }
}