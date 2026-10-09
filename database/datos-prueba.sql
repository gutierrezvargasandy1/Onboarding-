-- =============================================================================
--  BANCO · ONBOARDING  |  Datos de ejemplo (OPCIONAL)
-- =============================================================================
--  Cuatro clientes con domicilio, cuenta y usuario para explorar la API o las
--  consultas sin registrar a mano. Ejecutar después de schema.sql (o de arrancar
--  la aplicación una vez, que crea el esquema con Flyway):
--
--    psql -U postgres -d banco_onboarding -v ON_ERROR_STOP=1 -f database/datos-prueba.sql
--
--  Todos los usuarios tienen la contraseña  Segura#2026  (guardada como hash BCrypt).
--  Lucía Pérez queda dada de baja para probar la baja lógica.
-- =============================================================================

SET client_encoding = 'UTF8';

BEGIN;
SET LOCAL search_path = onboarding, pg_temp;

CREATE FUNCTION pg_temp.alta_cliente(
    p_nombre text, p_segundo text, p_paterno text, p_materno text, p_nacimiento date,
    p_curp text, p_rfc text, p_sexo smallint, p_estado_civil smallint, p_correo text, p_telefono text,
    p_ocupacion text, p_empresa text, p_ingreso numeric,
    p_calle text, p_num_ext text, p_colonia text, p_municipio text, p_estado smallint, p_cp text
) RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
    v_id integer;
BEGIN
    INSERT INTO onboarding.clientes (nombre, segundo_nombre, apellido_paterno, apellido_materno, fecha_nacimiento,
                                     curp, rfc, sexo_id, nacionalidad, estado_civil_id, correo, telefono_movil,
                                     ocupacion, empresa, ingreso_mensual)
    VALUES (p_nombre, p_segundo, p_paterno, p_materno, p_nacimiento, p_curp, p_rfc, p_sexo, 'MX', p_estado_civil,
            p_correo, p_telefono, p_ocupacion, p_empresa, p_ingreso)
    RETURNING id INTO v_id;

    INSERT INTO onboarding.domicilios (cliente_id, calle, numero_exterior, colonia, municipio, estado_id, codigo_postal)
    VALUES (v_id, p_calle, p_num_ext, p_colonia, p_municipio, p_estado, p_cp);

    INSERT INTO onboarding.cuentas (cliente_id, saldo) VALUES (v_id, 1000.00);

    -- Hash BCrypt de "Segura#2026"
    INSERT INTO onboarding.usuarios (cliente_id, correo, password)
    VALUES (v_id, p_correo, '$2a$10$Aw804GDHNiYin/e3R3lIQepN9iJbGACq1E.nV5eXjH02d1VwN3.Ae');
    RETURN v_id;
END $$;

SELECT pg_temp.alta_cliente('María', 'Fernanda', 'López', 'Hernández', DATE '1995-08-21',
    'LOHF950821MGTPRR08', 'LOHF950821QK5', 2::smallint, 1::smallint, 'maria.lopez@ejemplo.com', '4731234567',
    'Ingeniera de software', 'Tecnologías del Bajío SA de CV', 35000.00,
    'Calle Hidalgo', '123', 'Centro', 'Guanajuato', 11::smallint, '36000');

SELECT pg_temp.alta_cliente('Carlos', 'Alberto', 'Gómez', 'Mendoza', DATE '1990-05-15',
    'GOMC900515HQTMNR08', 'GOMC900515AB7', 1::smallint, 2::smallint, 'carlos.gomez@ejemplo.com', '4421234567',
    'Contador', 'Despacho Gómez', 28000.00,
    'Av. Constituyentes', '100', 'Centro', 'Querétaro', 22::smallint, '76000');

SELECT pg_temp.alta_cliente('Juan', NULL, 'Ramírez', 'Hernández', DATE '1988-03-10',
    'RAHJ880310HDFMRN02', 'RAHJ880310XY1', 1::smallint, 3::smallint, 'juan.ramirez@ejemplo.com', '5512345678',
    'Analista financiero', 'Banco del Centro', 42000.50,
    'Paseo de la Reforma', '500', 'Juárez', 'Cuauhtémoc', 9::smallint, '06600');

SELECT pg_temp.alta_cliente('Lucía', NULL, 'Pérez', 'Sánchez', DATE '2002-02-14',
    'PESL020214MJCRNCA2', 'PESL020214HG7', 2::smallint, 1::smallint, 'lucia.perez@ejemplo.com', '3312345678',
    'Diseñadora', 'Estudio Creativo', 18500.00,
    'Av. Chapultepec', '250', 'Americana', 'Guadalajara', 14::smallint, '44160');

-- Baja lógica de Lucía: los triggers inactivan su usuario y su cuenta
UPDATE clientes SET activo = FALSE WHERE curp = 'PESL020214MJCRNCA2';

COMMIT;

SELECT c.id, c.nombre || ' ' || c.apellido_paterno AS cliente, c.activo, cu.numero_cuenta, ec.clave AS estatus_cuenta, u.activo AS usuario_activo
  FROM onboarding.clientes c
  JOIN onboarding.cuentas cu ON cu.cliente_id = c.id
  JOIN onboarding.cat_estatus_cuenta ec ON ec.id = cu.estatus_id
  JOIN onboarding.usuarios u ON u.cliente_id = c.id
 ORDER BY c.id;
