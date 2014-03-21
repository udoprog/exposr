package eu.toolchain.exposr.tasks;

import java.util.List;

import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.taskmanager.Task;
import eu.toolchain.exposr.taskmanager.TaskState;

public class RefreshTask implements Task<List<Project>> {
    private final ProjectManager projectManager;

    public RefreshTask(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @Override
    public List<Project> run(TaskState state) throws Exception {
        return projectManager.fetchProjects();
    }
}