package eu.toolchain.exposr.project;

public class ProjectPublishException extends ProjectException {
    private static final long serialVersionUID = 928760680376698555L;

    public ProjectPublishException() {
        super();
    }

    public ProjectPublishException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProjectPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectPublishException(String message) {
        super(message);
    }

    public ProjectPublishException(Throwable cause) {
        super(cause);
    }
}
