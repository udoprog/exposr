package eu.toolchain.exposr.yaml;

import lombok.Getter;
import lombok.Setter;
import eu.toolchain.exposr.builder.Builder;
import eu.toolchain.exposr.builder.LocalBuilder;
import eu.toolchain.exposr.project.LocalRepository;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.project.github.GithubProjectManager;
import eu.toolchain.exposr.publisher.LocalPublisher;
import eu.toolchain.exposr.publisher.Publisher;

public class ExposrConfigYAML {
    public static void notEmpty(String name, String string) {
        if (string == null || string.isEmpty()) {
            throw new RuntimeException("'" + name
                    + "' must be defined and non-empty");
        }
    }

    public static interface ProjectManagerYAML {
        public ProjectManager build();
    }

    public static class GithubProjectManagerYAML implements ProjectManagerYAML {
        public static final String TYPE = "!github-project-manager";

        public static interface AuthYAML {
            public GithubProjectManager.Auth build();
        }

        public static class BasicAuthYAML implements AuthYAML {
            public static final String TYPE = "!basic-auth";

            @Getter
            @Setter
            private String username;

            @Getter
            @Setter
            private String password;

            @Override
            public GithubProjectManager.Auth build() {
                notEmpty("projectManager.auth.username", username);
                notEmpty("projectManager.auth.password", password);
                return new GithubProjectManager.BasicAuth(username, password);
            }
        }

        @Getter
        @Setter
        private String apiUrl = "https://api.github.com";

        @Getter
        @Setter
        private String ref = "refs/heads/master";

        @Getter
        @Setter
        private String user;

        @Getter
        @Setter
        private AuthYAML auth;

        @Override
        public ProjectManager build() {
            notEmpty("projectManager.user", user);
            final GithubProjectManager.Auth auth;

            if (this.auth != null) {
                auth = this.auth.build();
            } else {
                auth = null;
            }

            return new GithubProjectManager(apiUrl, user, ref, auth);
        }
    }

    public static class RepositoryYAML {
        @Getter
        @Setter
        private String path;

        public LocalRepository build() {
            notEmpty("repository.path", path);
            return new LocalRepository(path);
        }
    }

    public static interface PublisherYAML {
        public Publisher build();
    }

    public static class LocalPublisherYAML implements PublisherYAML {
        public static final String TYPE = "!local-publisher";

        @Getter
        @Setter
        private String path;

        @Override
        public Publisher build() {
            notEmpty("publisher.path", path);
            return new LocalPublisher(path);
        }
    }

    public static interface BuilderYAML {
        public Builder build();
    }

    public static class LocalBuilderYAML implements BuilderYAML {
        public static final String TYPE = "!local-builder";

        @Override
        public Builder build() {
            return new LocalBuilder();
        }
    }

    @Getter
    @Setter
    private ProjectManagerYAML projectManager;

    @Getter
    @Setter
    private RepositoryYAML repository;

    @Getter
    @Setter
    private PublisherYAML publisher;

    @Getter
    @Setter
    private BuilderYAML builder;

    @Getter
    @Setter
    private String target;
}
