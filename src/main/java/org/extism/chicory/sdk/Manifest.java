package org.extism.chicory.sdk;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class Manifest {

    public enum Validation {
        Import, Type, All;
    }

    public static class Options {
        boolean aot;
        EnumSet<Validation> validationFlags = EnumSet.noneOf(Validation.class);
        Map<String, String> config;

        public Options withAoT() {
            this.aot = true;
            return this;
        }

        public Options withConfig(Map<String, String> config) {
            this.config = config;
            return this;
        }

        public Options withValidation(Validation... vs) {
            this.validationFlags.addAll(List.of(vs));
            return this;
        }
    }


    public static Builder ofWasms(ManifestWasm... wasms) {
        return new Builder(wasms);
    }

    public static class Builder {
        final ManifestWasm[] wasms;
        private Options options;
        private String name;
        private List<String> allowHosts;

        private Builder(ManifestWasm[] manifestWasms) {
            this.wasms = manifestWasms;
        }

        public Builder withOptions(Options opts) {
            this.options = opts;
            return this;
        }

        public Builder withAllowHosts(List<String> allowHosts) {
            this.allowHosts = allowHosts;
            return this;
        }

        public Manifest build() {
            return new Manifest(wasms, name, options, allowHosts);
        }

    }

    final ManifestWasm[] wasms;
    final Manifest.Options options;
    List<String> allowHosts;

    Manifest(ManifestWasm[] wasms, String name, Manifest.Options opts) {
        this.wasms = wasms;
        this.options = opts;
    }

    Manifest(ManifestWasm[] wasms, String name, Manifest.Options opts, List<String> allowHosts) {
        this.wasms = wasms;
        this.options = opts;
        this.allowHosts = allowHosts;
    }

    public List<String> getAllowHosts() {
        return allowHosts;
    }
}
