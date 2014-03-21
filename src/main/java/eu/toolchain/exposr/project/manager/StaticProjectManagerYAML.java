package eu.toolchain.exposr.project.manager;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.project.ProjectAuth;
import eu.toolchain.exposr.project.Project;
import eu.toolchain.exposr.project.ProjectAuthYAML;
import eu.toolchain.exposr.project.ProjectYAML;

public class StaticProjectManagerYAML implements ProjectManagerYAML {
    public static final String TYPE = "!static-project-manager";

    @Getter
    @Setter
    private List<ProjectYAML> projects;

    @Getter
    @Setter
    private ProjectAuthYAML auth;

    @Override
    public ProjectManager build(String context) {
        notEmpty(context + ".projects", this.projects);

        final List<Project> projects = new ArrayList<Project>();

        ProjectAuth auth = null;

        if (this.auth != null)
            auth = this.auth.build();

        int i = 0;

        for (final ProjectYAML project : this.projects) {
            projects.add(project.build("projectManager.projects[" + i++
                    + "]", auth));
        }

        return new StaticProjectManager(projects);
    }
}