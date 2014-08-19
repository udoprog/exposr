package eu.toolchain.exposr.taskmanager;

import lombok.Data;

@Data
public class TaskOutput {
    private final TaskOutputType type;
    private final String text;

    public TaskOutput(TaskOutputType type, String text) {
        this.type = type;
        this.text = text;
    }
}
