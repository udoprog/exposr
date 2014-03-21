package eu.toolchain.exposr.project;

public class ProjectBuildException extends ProjectException {
    private static final long serialVersionUID = 928760680376698555L;

    public ProjectBuildException() {
        super();
    }

    public ProjectBuildException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProjectBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectBuildException(String message) {
        super(message);
    }

    public ProjectBuildException(Throwable cause) {
        super(cause);
    }
}
