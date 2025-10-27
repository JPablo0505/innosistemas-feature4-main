-- Migración V2: Agregar campos team_id, course_id, first_name, last_name a tabla users
-- Autor: Fábrica-Escuela de Software UdeA
-- Fecha: 2025-10-21
-- Descripción: Agrega soporte para equipos, cursos y nombres de usuario según tasking

-- Agregar columna team_id (nullable porque no todos los usuarios tienen equipo)
ALTER TABLE users ADD COLUMN IF NOT EXISTS team_id BIGINT;

-- Agregar columna course_id (nullable porque no todos los usuarios tienen curso)
ALTER TABLE users ADD COLUMN IF NOT EXISTS course_id BIGINT;

-- Agregar columna first_name
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);

-- Agregar columna last_name
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- Crear índice en team_id para búsquedas rápidas por equipo
CREATE INDEX IF NOT EXISTS idx_users_team_id ON users(team_id);

-- Crear índice en course_id para búsquedas rápidas por curso
CREATE INDEX IF NOT EXISTS idx_users_course_id ON users(course_id);

-- Comentarios en las columnas
COMMENT ON COLUMN users.team_id IS 'ID del equipo al que pertenece el usuario';
COMMENT ON COLUMN users.course_id IS 'ID del curso asociado al usuario (para profesores y estudiantes)';
COMMENT ON COLUMN users.first_name IS 'Primer nombre del usuario';
COMMENT ON COLUMN users.last_name IS 'Apellido del usuario';
