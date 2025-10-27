package com.udea.innosistemas.dto;

import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;

/**
 * DTO con información del usuario para respuestas de autenticación.
 * Incluye userId, email, role, teamId, courseId y nombre completo.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 2.0.0
 */
public class UserInfo {

    private Long id;
    private String email;
    private UserRole role;
    private Long teamId;
    private Long courseId;
    private String firstName;
    private String lastName;
    private String fullName;

    public UserInfo() {
    }

    public UserInfo(Long id, String email, UserRole role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public UserInfo(Long id, String email, UserRole role, Long teamId, Long courseId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.teamId = teamId;
        this.courseId = courseId;
    }

    public UserInfo(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.teamId = user.getTeamId();
        this.courseId = user.getCourseId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.fullName = user.getFullName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}