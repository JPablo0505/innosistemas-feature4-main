package com.udea.innosistemas.service;

import com.udea.innosistemas.dto.TeamMember;
import com.udea.innosistemas.dto.UserInfo;
import com.udea.innosistemas.dto.UserPermissions;
import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;
import com.udea.innosistemas.exception.AuthenticationException;
import com.udea.innosistemas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar queries de usuario, permisos y equipos.
 * Proporciona métodos para obtener información del usuario actual,
 * sus permisos y miembros de su equipo.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Service
public class UserQueryService {

    private static final Logger logger = LoggerFactory.getLogger(UserQueryService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Obtiene la información del usuario actualmente autenticado.
     *
     * @return UserInfo con los datos del usuario actual
     * @throws AuthenticationException si no hay usuario autenticado
     */
    public UserInfo getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (username == null || username.equals("anonymousUser")) {
                logger.warn("Attempted to get current user without authentication");
                throw new AuthenticationException("No hay usuario autenticado");
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            logger.info("Retrieved current user info for: {}", username);
            return new UserInfo(user);

        } catch (UsernameNotFoundException e) {
            logger.warn("Current user not found in database");
            throw new AuthenticationException("Usuario no encontrado");
        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage());
            throw new AuthenticationException("Error al obtener información del usuario");
        }
    }

    /**
     * Obtiene los permisos del usuario actualmente autenticado.
     *
     * @return UserPermissions con los permisos calculados basados en el rol
     * @throws AuthenticationException si no hay usuario autenticado
     */
    public UserPermissions getUserPermissions() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (username == null || username.equals("anonymousUser")) {
                logger.warn("Attempted to get permissions without authentication");
                throw new AuthenticationException("No hay usuario autenticado");
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            // Generar lista de permisos basados en el rol
            List<String> permissions = generatePermissionsList(user.getRole());

            UserPermissions userPermissions = new UserPermissions(user.getId(), user.getRole(), permissions);
            userPermissions.setTeamId(user.getTeamId());
            userPermissions.setCourseId(user.getCourseId());

            logger.info("Retrieved permissions for user: {}, role: {}", username, user.getRole());
            return userPermissions;

        } catch (UsernameNotFoundException e) {
            logger.warn("User not found when getting permissions");
            throw new AuthenticationException("Usuario no encontrado");
        } catch (Exception e) {
            logger.error("Error getting user permissions: {}", e.getMessage());
            throw new AuthenticationException("Error al obtener permisos del usuario");
        }
    }

    /**
     * Obtiene los miembros de un equipo específico.
     * Los estudiantes solo pueden ver su propio equipo.
     * Los profesores y admins pueden ver cualquier equipo.
     *
     * @param teamId ID del equipo
     * @return Lista de TeamMember
     * @throws AuthenticationException si el usuario no tiene permisos
     */
    public List<TeamMember> getTeamMembers(Long teamId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if (username == null || username.equals("anonymousUser")) {
                logger.warn("Attempted to get team members without authentication");
                throw new AuthenticationException("No hay usuario autenticado");
            }

            User currentUser = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            // Validar permisos: estudiantes solo pueden ver su propio equipo
            if (currentUser.getRole() == UserRole.STUDENT) {
                if (currentUser.getTeamId() == null) {
                    logger.warn("Student {} has no team assigned", username);
                    throw new AuthenticationException("No perteneces a ningún equipo");
                }
                if (!currentUser.getTeamId().equals(teamId)) {
                    logger.warn("Student {} attempted to access team {} but belongs to team {}",
                            username, teamId, currentUser.getTeamId());
                    throw new AuthenticationException("No tienes permiso para ver este equipo");
                }
            }

            // Buscar miembros del equipo
            List<User> teamMembers = userRepository.findByTeamId(teamId);

            logger.info("Retrieved {} members for team {}", teamMembers.size(), teamId);

            return teamMembers.stream()
                    .map(TeamMember::new)
                    .collect(Collectors.toList());

        } catch (UsernameNotFoundException e) {
            logger.warn("User not found when getting team members");
            throw new AuthenticationException("Usuario no encontrado");
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting team members: {}", e.getMessage());
            throw new AuthenticationException("Error al obtener miembros del equipo");
        }
    }

    /**
     * Genera la lista de permisos basados en el rol del usuario.
     *
     * @param role Rol del usuario
     * @return Lista de strings con los permisos
     */
    private List<String> generatePermissionsList(UserRole role) {
        List<String> permissions = new ArrayList<>();

        switch (role) {
            case ADMIN:
                permissions.add("user:create");
                permissions.add("user:read");
                permissions.add("user:update");
                permissions.add("user:delete");
                permissions.add("team:create");
                permissions.add("team:read");
                permissions.add("team:update");
                permissions.add("team:delete");
                permissions.add("course:create");
                permissions.add("course:read");
                permissions.add("course:update");
                permissions.add("course:delete");
                permissions.add("notification:send");
                permissions.add("system:configure");
                break;

            case PROFESSOR:
                permissions.add("user:read");
                permissions.add("team:read");
                permissions.add("team:update");
                permissions.add("course:create");
                permissions.add("course:read");
                permissions.add("course:update");
                permissions.add("notification:send");
                permissions.add("grade:assign");
                break;

            case TA:
                permissions.add("user:read");
                permissions.add("team:read");
                permissions.add("course:read");
                permissions.add("notification:send");
                permissions.add("grade:view");
                break;

            case STUDENT:
                permissions.add("team:read");
                permissions.add("course:read");
                permissions.add("project:submit");
                permissions.add("grade:view");
                break;

            default:
                logger.warn("Unknown role: {}", role);
                break;
        }

        return permissions;
    }
}
