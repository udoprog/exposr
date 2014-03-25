package eu.toolchain.exposr.yaml;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.TypeDescription;

public final class UtilsYAML {
    public static void notEmpty(String context, String string)
            throws ValidationException {
        if (string == null || string.isEmpty())
            throw new ValidationException(context
                    + ": must be defined and non-empty");
    }

    public static <T> List<T> notEmpty(String context, List<T> list)
            throws ValidationException {
        if (list == null || list.isEmpty())
            throw new ValidationException(context
                    + ": must be a non-empty list");

        return list;
    }

    public static URI toURI(String context, String url)
            throws ValidationException {
        notEmpty(context, url);

        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new ValidationException(context + ": must be a valid URL");
        }
    }

    public static Path toPath(String context, String path)
            throws ValidationException {
        notEmpty(context, path);
        return Paths.get(path);
    }

    public static Path toDirectory(String context, String path)
            throws ValidationException {
        return toDirectory(context, path, null);
    }

    public static Path toDirectory(String context, String path, Path defaultPath)
            throws ValidationException {
        if (defaultPath != null && (path == null || path.isEmpty()))
            return defaultPath;

        notEmpty(context, path);

        final Path p = Paths.get(path);

        if (!Files.isDirectory(p))
            throw new ValidationException(context
                    + ": must be an existing directory");

        return p;
    }

    public static <T> List<T> toList(String context, List<T> list) {
        if (list == null)
            return new ArrayList<T>();

        return list;
    }

    public static TypeDescription makeType(Class<?> clazz) {
        final Field field;

        try {
            field = clazz.getField("TYPE");
        } catch (Exception e) {
            throw new RuntimeException("Invalid field 'TYPE' on class " + clazz);
        }

        final Object type;

        try {
            type = field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to access field 'TYPE' on class " + clazz);
        }

        final String stringType;

        try {
            stringType = (String) type;
        } catch (ClassCastException e) {
            throw new RuntimeException("Type field 'TYPE' of class " + clazz
                    + " must be a String");
        }

        return new TypeDescription(clazz, stringType);
    }
}
