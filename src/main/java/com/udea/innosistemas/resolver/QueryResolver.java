package com.udea.innosistemas.resolver;

import com.udea.innosistemas.dto.TeamMember;
import com.udea.innosistemas.dto.UserInfo;
import com.udea.innosistemas.dto.UserPermissions;
import com.udea.innosistemas.service.UserQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Resolver para queries GraphQL relacionadas con usuarios, permisos y equipos.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 2.0.0
 */
@Controller
public class QueryResolver {

    @Autowired
    private UserQueryService userQueryService;

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public String hello() {
        return "Hello from InnoSistemas GraphQL API!";
    }

    /**
     * Obtiene la información del usuario actualmente autenticado.
     * Requiere autenticación.
     *
     * @return UserInfo con los datos del usuario actual
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserInfo getCurrentUser() {
        return userQueryService.getCurrentUser();
    }

    /**
     * Obtiene los permisos del usuario actualmente autenticado.
     * Requiere autenticación.
     *
     * @return UserPermissions con los permisos calculados
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserPermissions getUserPermissions() {
        return userQueryService.getUserPermissions();
    }

    /**
     * Obtiene los miembros de un equipo específico.
     * Los estudiantes solo pueden ver su propio equipo.
     * Los profesores y admins pueden ver cualquier equipo.
     *
     * @param teamId ID del equipo
     * @return Lista de TeamMember
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<TeamMember> getTeamMembers(@Argument Long teamId) {
        return userQueryService.getTeamMembers(teamId);
    }
}