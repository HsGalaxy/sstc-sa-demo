package com.sstc.platform.toolbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S04 - configuration of the simulated external integrations of the Toolbox. The values
 * mirror the "external services" row of the Level-1 context diagram.
 */
@Component
@ConfigurationProperties(prefix = "sstc.toolbox")
public class ToolboxProperties {

    private Office office = new Office();

    private Git git = new Git();

    private Container container = new Container();

    private Storage storage = new Storage();

    public static class Office {

        private String provider = "Microsoft 365 (simulated)";

        private int quotaPerDay = 200;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public int getQuotaPerDay() {
            return quotaPerDay;
        }

        public void setQuotaPerDay(int quotaPerDay) {
            this.quotaPerDay = quotaPerDay;
        }
    }

    public static class Git {

        private String provider = "GitHub (simulated)";

        private String defaultBranch = "main";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDefaultBranch() {
            return defaultBranch;
        }

        public void setDefaultBranch(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }
    }

    public static class Container {

        private String runtime = "Docker (simulated)";

        private int maxEnvironments = 12;

        public String getRuntime() {
            return runtime;
        }

        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }

        public int getMaxEnvironments() {
            return maxEnvironments;
        }

        public void setMaxEnvironments(int maxEnvironments) {
            this.maxEnvironments = maxEnvironments;
        }
    }

    public static class Storage {

        private String provider = "Local object storage (simulated)";

        private int maxUploadMb = 25;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public int getMaxUploadMb() {
            return maxUploadMb;
        }

        public void setMaxUploadMb(int maxUploadMb) {
            this.maxUploadMb = maxUploadMb;
        }
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
