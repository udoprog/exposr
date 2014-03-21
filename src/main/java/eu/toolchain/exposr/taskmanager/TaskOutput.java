package eu.toolchain.exposr.taskmanager;

import lombok.Getter;

public class TaskOutput {
    @Getter
    private final TaskOutputType type;
    @Getter
    private final String text;

    public TaskOutput(TaskOutputType type, String text) {
        this.type = type;
        this.text = text;
    }
}
