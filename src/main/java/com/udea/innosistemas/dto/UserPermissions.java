package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.UserRole;

import java.util.List;

/**
 * DTO con los permisos del usuario basados en su rol, equipo y curso.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
public class UserPermissions {

    private Long userId;
    private UserRole role;
    private List<String> permissions;
    private Long teamId;
    private Long courseId;
    private boolean canManageTeam;
    private boolean canManageCourse;
    private boolean canViewAllTeams;
    private boolean canSendNotifications;

    public UserPermissions() {
    }

    public UserPermissions(Long userId, UserRole role, List<String> permissions) {
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
        this.calculatePermissions();
    }

    private void calculatePermissions() {
        // Calcular permisos basados en el rol
        switch (role) {
            case ADMIN:
                this.canManageTeam = true;
                this.canManageCourse = true;
                this.canViewAllTeams = true;
                this.canSendNotifications = true;
                break;
            case PROFESSOR:
                this.canManageTeam = false;
                this.canManageCourse = true;
                this.canViewAllTeams = true;
                this.canSendNotifications = true;
                break;
            case TA:
                this.canManageTeam = false;
                this.canManageCourse = false;
                this.canViewAllTeams = false;
                this.canSendNotifications = true;
                break;
            case STUDENT:
            default:
                this.canManageTeam = false;
                this.canManageCourse = false;
                this.canViewAllTeams = false;
                this.canSendNotifications = false;
                break;
        }
    }

    // Getters y Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public boolean isCanManageTeam() {
        return canManageTeam;
    }

    public void setCanManageTeam(boolean canManageTeam) {
        this.canManageTeam = canManageTeam;
    }

    public boolean isCanManageCourse() {
        return canManageCourse;
    }

    public void setCanManageCourse(boolean canManageCourse) {
        this.canManageCourse = canManageCourse;
    }

    public boolean isCanViewAllTeams() {
        return canViewAllTeams;
    }

    public void setCanViewAllTeams(boolean canViewAllTeams) {
        this.canViewAllTeams = canViewAllTeams;
    }

    public boolean isCanSendNotifications() {
        return canSendNotifications;
    }

    public void setCanSendNotifications(boolean canSendNotifications) {
        this.canSendNotifications = canSendNotifications;
    }
}
