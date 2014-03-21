package eu.toolchain.exposr.project.reporter;


public class MemoryProjectReporterYAML implements
        ProjectReporterYAML {
    public static final String TYPE = "!memory-project-reporter";

    @Override
    public ProjectReporter build(String context) {
        return new MemoryProjectReporter();
    }
}