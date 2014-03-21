package eu.toolchain.exposr.yaml;

import lombok.Getter;
import lombok.Setter;

import eu.toolchain.exposr.project.LocalRepository;
import eu.toolchain.exposr.project.ProjectManager;
import eu.toolchain.exposr.project.github.GithubProjectManager;

public class ExposrConfigYAML {
    public static interface ProjectManagerYAML {
        public ProjectManager build();
    }

    public static class GithubProjectManagerYAML implements ProjectManagerYAML {
        public static interface AuthYAML {
            public GithubProjectManager.Auth build();
        }

        public static class BasicAuthYAML implements AuthYAML {
            @Getter
            @Setter
            private String username;

            @Getter
            @Setter
            private String password;

            @Override
            public GithubProjectManager.Auth build() {
                if (username == null || username.isEmpty()) {
                    throw new RuntimeException(
                            "'username' must be defined and non-empty");
                }

                if (password == null || password.isEmpty()) {
                    throw new RuntimeException(
                            "'username' must be defined and non-empty");
                }

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
            if (user == null || user.isEmpty()) {
                throw new RuntimeException(
                        "'user' must be defined and non-empty");
            }

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

        @Getter
        @Setter
        private String publish;

        public LocalRepository build() {
            if (path == null || path.isEmpty()) {
                throw new RuntimeException(
                        "'path' must be defined and non-empty");
            }

            if (publish == null || publish.isEmpty()) {
                throw new RuntimeException(
                        "'publish' must be defined and non-empty");
            }

            return new LocalRepository(path, publish);
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
    private String target;
}
