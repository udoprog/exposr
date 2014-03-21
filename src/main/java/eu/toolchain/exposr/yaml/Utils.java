package eu.toolchain.exposr.yaml;

import java.util.List;

public final class Utils {
    public static void notEmpty(String name, String string) {
        if (string == null || string.isEmpty()) {
            throw new RuntimeException("'" + name
                    + "' must be defined and non-empty");
        }
    }

    public static void notEmpty(String name, List<?> list) {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("'" + name
                    + "' must be defined and non-empty");
        }
    }
}
