package eu.toolchain.exposr.project;

import static eu.toolchain.exposr.yaml.Utils.notEmpty;
import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.yaml.ValidationException;

public class ProjectYAML {
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private String ref = "HEAD";

    @Getter
    @Setter
    private ProjectAuthYAML auth;

    public Project build(String context, ProjectAuth auth)
            throws ValidationException {
        notEmpty(context + ".name", name);
        notEmpty(context + ".url", url);
        notEmpty(context + ".ref", ref);

        if (this.auth != null)
            auth = this.auth.build();

        return new Project(name, url, ref, auth);
    }
}