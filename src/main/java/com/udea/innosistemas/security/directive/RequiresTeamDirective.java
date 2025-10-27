package com.udea.innosistemas.security.directive;

import com.udea.innosistemas.entity.User;
import com.udea.innosistemas.entity.UserRole;
import com.udea.innosistemas.repository.UserRepository;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementación de la directiva @requiresTeam para GraphQL.
 * Valida que el usuario pertenezca a un equipo específico o tenga
 * permisos de profesor/admin para ver otros equipos.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class RequiresTeamDirective implements SchemaDirectiveWiring {

    private static final Logger logger = LoggerFactory.getLogger(RequiresTeamDirective.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition field = environment.getElement();
        DataFetcher<?> originalDataFetcher = environment.getCodeRegistry().getDataFetcher(
                environment.getFieldsContainer(),
                field
        );

        DataFetcher<?> teamDataFetcher = dataFetchingEnvironment -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
                logger.warn("@requiresTeam directive blocked unauthenticated access to field: {}", field.getName());
                throw new AccessDeniedException("Debe estar autenticado para acceder a este campo");
            }

            String username = authentication.getName();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));

            // Profesores y admins pueden ver cualquier equipo
            if (user.getRole() == UserRole.PROFESSOR || user.getRole() == UserRole.ADMIN) {
                logger.debug("@requiresTeam directive allowed access for {} with role {}", username, user.getRole());
                return originalDataFetcher.get(dataFetchingEnvironment);
            }

            // Estudiantes solo pueden ver su propio equipo
            Long requestedTeamId = dataFetchingEnvironment.getArgument("teamId");
            if (requestedTeamId != null) {
                // Convertir a Long si viene como String
                Long teamIdLong = Long.parseLong(requestedTeamId.toString());

                if (user.getTeamId() == null) {
                    logger.warn("Student {} has no team assigned", username);
                    throw new AccessDeniedException("No perteneces a ningún equipo");
                }

                if (!user.getTeamId().equals(teamIdLong)) {
                    logger.warn("Student {} attempted to access team {} but belongs to team {}",
                            username, teamIdLong, user.getTeamId());
                    throw new AccessDeniedException("Solo puedes ver tu propio equipo");
                }
            }

            logger.debug("@requiresTeam directive validated user {} for field: {}", username, field.getName());
            return originalDataFetcher.get(dataFetchingEnvironment);
        };

        environment.getCodeRegistry().dataFetcher(
                environment.getFieldsContainer(),
                field,
                teamDataFetcher
        );

        return field;
    }
}
