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
 * Implementación de la directiva @requiresCourse para GraphQL.
 * Valida que el usuario pertenezca a un curso específico o tenga
 * permisos de profesor/admin para ver otros cursos.
 *
 * Autor: Fábrica-Escuela de Software UdeA
 * Versión: 1.0.0
 */
@Component
public class RequiresCourseDirective implements SchemaDirectiveWiring {

    private static final Logger logger = LoggerFactory.getLogger(RequiresCourseDirective.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition field = environment.getElement();
        DataFetcher<?> originalDataFetcher = environment.getCodeRegistry().getDataFetcher(
                environment.getFieldsContainer(),
                field
        );

        DataFetcher<?> courseDataFetcher = dataFetchingEnvironment -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
                logger.warn("@requiresCourse directive blocked unauthenticated access to field: {}", field.getName());
                throw new AccessDeniedException("Debe estar autenticado para acceder a este campo");
            }

            String username = authentication.getName();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));

            // Profesores y admins pueden ver cualquier curso
            if (user.getRole() == UserRole.PROFESSOR || user.getRole() == UserRole.ADMIN) {
                logger.debug("@requiresCourse directive allowed access for {} with role {}", username, user.getRole());
                return originalDataFetcher.get(dataFetchingEnvironment);
            }

            // Estudiantes y TAs solo pueden ver su propio curso
            Long requestedCourseId = dataFetchingEnvironment.getArgument("courseId");
            if (requestedCourseId != null) {
                // Convertir a Long si viene como String
                Long courseIdLong = Long.parseLong(requestedCourseId.toString());

                if (user.getCourseId() == null) {
                    logger.warn("User {} has no course assigned", username);
                    throw new AccessDeniedException("No perteneces a ningún curso");
                }

                if (!user.getCourseId().equals(courseIdLong)) {
                    logger.warn("User {} attempted to access course {} but belongs to course {}",
                            username, courseIdLong, user.getCourseId());
                    throw new AccessDeniedException("Solo puedes ver tu propio curso");
                }
            }

            logger.debug("@requiresCourse directive validated user {} for field: {}", username, field.getName());
            return originalDataFetcher.get(dataFetchingEnvironment);
        };

        environment.getCodeRegistry().dataFetcher(
                environment.getFieldsContainer(),
                field,
                courseDataFetcher
        );

        return field;
    }
}
