package eu.toolchain.exposr.project.manager;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectAuth;
import eu.toolchain.exposr.yaml.ConfigUtils;
import eu.toolchain.exposr.yaml.ValidationException;

public class StaticProjectManager implements ProjectManager {
    @Data
    @NoArgsConstructor
    public static class YAML implements ProjectManager.YAML {
        public static final String TYPE = "!static-project-manager";

        private List<Project.YAML> projects;
        private ProjectAuth.YAML auth;

        @Override
        public ProjectManager build(String context) throws ValidationException {
            ConfigUtils.notEmpty(context + ".projects", this.projects);

            final List<Project> projects = new ArrayList<Project>();

            ProjectAuth auth = null;

            if (this.auth != null)
                auth = this.auth.build();

            int i = 0;

            for (final Project.YAML project : this.projects) {
                projects.add(project.build("projectManager.projects[" + i++
                        + "]", auth));
            }

            return new StaticProjectManager(projects);
        }
    }

    private List<Project> projects = new ArrayList<Project>();

    public StaticProjectManager(List<Project> projects) {
        this.projects = projects;
    }

    @Override
    public synchronized List<Project> getProjects() {
        return projects;
    }

    @Override
    public Project getProjectByName(String name) {
        final List<Project> projects = getProjects();

        for (Project project : projects) {
            if (project.getName().equals(name)) {
                return project;
            }
        }

        return null;
    }
}
