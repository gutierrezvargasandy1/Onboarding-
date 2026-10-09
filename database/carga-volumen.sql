-- =============================================================================
--  BANCO · ONBOARDING  |  Carga de volumen (OPCIONAL, solo para medir)
-- =============================================================================
--  Genera 20,000 clientes válidos (cada fila pasa todas las restricciones y
--  triggers) para revisar los planes de ejecución y el espacio que ocupa cada
--  tabla. Usar en una base de datos de pruebas, nunca en producción:
--
--    createdb -T template0 -E UTF8 banco_volumen
--    psql -d banco_volumen -v ON_ERROR_STOP=1 -f database/schema.sql
--    psql -d banco_volumen -v ON_ERROR_STOP=1 -f database/carga-volumen.sql
-- =============================================================================

SET client_encoding = 'UTF8';

BEGIN;
SET LOCAL search_path = onboarding, pg_temp;

-- Tres consonantes distintas por número (posiciones 14-16 de la CURP)
CREATE FUNCTION pg_temp.consonantes(n integer) RETURNS text LANGUAGE sql IMMUTABLE AS $$
    SELECT substr('BCDFGHJKLMNPQRSTVWXYZ', (n / 441) % 21 + 1, 1)
        || substr('BCDFGHJKLMNPQRSTVWXYZ', (n / 21) % 21 + 1, 1)
        || substr('BCDFGHJKLMNPQRSTVWXYZ', n % 21 + 1, 1)
$$;

CREATE TEMP TABLE semilla ON COMMIT DROP AS
SELECT i,
       DATE '1950-01-01' + (i % 18000)                                   AS nacimiento,
       (ARRAY['Ana','Luis','María','José','Carmen','Jorge','Laura','Pedro'])[i % 8 + 1]   AS nombre,
       (ARRAY['García','López','Martínez','Hernández','Pérez','Sánchez','Ramírez','Torres'])[i % 8 + 1] AS paterno,
       (ARRAY['Flores','Rivera','Gómez','Díaz','Cruz','Morales','Reyes','Ortiz'])[(i / 8) % 8 + 1]    AS materno,
       now() - make_interval(days => i % 365)                            AS registro
  FROM generate_series(1, 20000) AS g(i);

INSERT INTO clientes (nombre, apellido_paterno, apellido_materno, fecha_nacimiento, curp, rfc, sexo_id,
                      nacionalidad, estado_civil_id, correo, telefono_movil, ocupacion, empresa,
                      ingreso_mensual, fecha_registro)
SELECT nombre, paterno, materno, nacimiento,
       'GOMC' || fn_fecha_aammdd(nacimiento) || 'HQT' || pg_temp.consonantes(i / 18000) || '0' || (i % 10),
       'GOMC' || fn_fecha_aammdd(nacimiento) || substr(pg_temp.consonantes(i / 18000 + 100), 2, 2) || (i % 10),
       (1 + i % 2)::smallint, 'MX', (1 + i % 6)::smallint,
       'cliente' || i || '@volumen.mx', (4420000000 + i)::text, 'Empleado', 'Empresa de prueba',
       10000 + (i % 50000), registro
  FROM semilla;

INSERT INTO domicilios (cliente_id, calle, numero_exterior, colonia, municipio, estado_id, codigo_postal)
SELECT id, 'Calle ' || id, (id % 900 + 1)::text, 'Centro', 'Querétaro', 22, '76000' FROM clientes;

INSERT INTO cuentas (cliente_id, saldo) SELECT id, 1000.00 FROM clientes;

INSERT INTO usuarios (cliente_id, correo, password)
SELECT id, correo, '$2a$10$Aw804GDHNiYin/e3R3lIQepN9iJbGACq1E.nV5eXjH02d1VwN3.Ae' FROM clientes;

-- 10 % dados de baja (los triggers inactivan sus cuentas y usuarios)
UPDATE clientes SET activo = FALSE WHERE id % 10 = 0;

COMMIT;

ANALYZE onboarding.clientes, onboarding.domicilios, onboarding.cuentas, onboarding.usuarios;
