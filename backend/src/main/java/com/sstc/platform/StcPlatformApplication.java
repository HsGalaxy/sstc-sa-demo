package com.sstc.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the STC (Small Team Collaboration) platform.
 *
 * <p>The platform is the reference implementation of the architecture designed for the
 * "Smart Working Management App (small teams)" case study. It is organised exactly like the
 * architecture model:</p>
 *
 * <ul>
 *   <li>S01 Identity, Role &amp; Access Management - {@code com.sstc.platform.identity}</li>
 *   <li>S02 Task Dashboard - {@code com.sstc.platform.dashboard}</li>
 *   <li>S03 Team Collaboration - {@code com.sstc.platform.collaboration}</li>
 *   <li>S04 Toolbox - {@code com.sstc.platform.toolbox}</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class StcPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(StcPlatformApplication.class, args);
    }
}
