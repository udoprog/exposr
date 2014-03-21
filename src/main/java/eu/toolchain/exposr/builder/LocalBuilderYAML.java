package eu.toolchain.exposr.builder;


public class LocalBuilderYAML implements BuilderYAML {
    public static final String TYPE = "!local-builder";

    @Override
    public Builder build(String context) {
        return new LocalBuilder();
    }
}