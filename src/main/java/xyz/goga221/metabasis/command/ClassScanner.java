package xyz.goga221.metabasis.command;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Lists every class under a package inside the plugin's shaded jar, so {@link CommandGroupScanner} can find {@link BaseCommand} leaves without a manual registry. */
final class ClassScanner {

    private ClassScanner() {
    }

    static List<Class<?>> getClassesInCurrentJar(String packageName) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');

        ClassLoader classLoader = ClassScanner.class.getClassLoader();
        if (classLoader == null) {
            throw new IllegalStateException("Class loader is null!");
        }

        Enumeration<URL> resources = classLoader.getResources(packagePath);
        if (!resources.hasMoreElements()) {
            throw new IOException("Package not found: " + packageName);
        }

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            if (!"jar".equals(resource.getProtocol())) {
                throw new IOException("Unsupported protocol: " + resource.getProtocol());
            }

            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            JarFile jarFile = connection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) && entryName.endsWith(".class") && !entry.isDirectory()) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }

        return classes;
    }
}
