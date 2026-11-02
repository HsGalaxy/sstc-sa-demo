package com.sstc.platform.identity.domain;

/**
 * S01 - the shared Level-1 role taxonomy of the platform.
 *
 * <p>{@code TEAM_MEMBER} = regular employee, {@code SENIOR_MANAGER} = senior manager,
 * {@code OVERALL_LEAD} = boss / overall lead.</p>
 */
public enum RoleType {

    TEAM_MEMBER("Team Member", "Team member - daily tasks, chat, files, toolbox"),
    SENIOR_MANAGER("Senior Manager", "Senior manager - task review, OKR, project files"),
    OVERALL_LEAD("Overall Lead", "Overall lead - identities, roles and permission policies");

    private final String label;
    private final String description;

    RoleType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
