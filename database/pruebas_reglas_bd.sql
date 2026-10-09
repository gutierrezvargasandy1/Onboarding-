-- =============================================================================
--  BANCO · ONBOARDING  |  Pruebas de reglas de negocio en la base de datos
-- =============================================================================
--  Comprueba que PostgreSQL rechaza por sí mismo los datos inválidos, aunque
--  alguien se salte la API y escriba directo con SQL. Ejecutar DESPUÉS de schema.sql:
--
--    psql -U postgres -d banco_onboarding -v ON_ERROR_STOP=1 -f database/pruebas_reglas_bd.sql
--
--  Todo corre dentro de una transacción que al final se revierte (ROLLBACK):
--  no deja datos. Imprime OK/FALLA por prueba y termina con error si alguna falla.
--  (Las secuencias sí avanzan: los siguientes id y números de cuenta serán mayores.)
-- =============================================================================

SET client_encoding = 'UTF8';
SET client_min_messages = notice;

BEGIN;
SET LOCAL search_path = onboarding, pg_temp;

-- ----------------------------------------------------------------------------
-- Utilidades de prueba (viven en pg_temp y desaparecen al cerrar la sesión)
-- ----------------------------------------------------------------------------
CREATE TEMP TABLE resultados (n serial, ok boolean, descripcion text) ON COMMIT DROP;
GRANT ALL ON resultados TO PUBLIC;
GRANT ALL ON SEQUENCE resultados_n_seq TO PUBLIC;

CREATE FUNCTION pg_temp.registrar(p_ok boolean, p_descripcion text, p_detalle text) RETURNS void
    LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO pg_temp.resultados (ok, descripcion) VALUES (p_ok, p_descripcion);
    RAISE NOTICE '% %  %', CASE WHEN p_ok THEN 'OK    ' ELSE 'FALLA ' END, rpad(p_descripcion, 66), p_detalle;
END $$;

-- La sentencia debe ser rechazada por la restricción (o SQLSTATE) indicada.
-- Si por error se ejecuta, se revierte igual (excepción centinela P0999).
CREATE FUNCTION pg_temp.debe_fallar(p_descripcion text, p_sql text, p_esperado text) RETURNS void
    LANGUAGE plpgsql AS $$
DECLARE
    v_restriccion text;
    v_estado      text;
    v_mensaje     text;
BEGIN
    BEGIN
        EXECUTE p_sql;
        RAISE EXCEPTION 'sin error' USING ERRCODE = 'P0999';
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS v_restriccion = CONSTRAINT_NAME,
                                v_estado      = RETURNED_SQLSTATE,
                                v_mensaje     = MESSAGE_TEXT;
    END;
    IF v_estado = 'P0999' THEN
        PERFORM pg_temp.registrar(false, p_descripcion, 'NO fue rechazada (se esperaba ' || p_esperado || ')');
    ELSIF v_restriccion = p_esperado OR v_estado = p_esperado THEN
        PERFORM pg_temp.registrar(true, p_descripcion, 'rechazada por ' || p_esperado);
    ELSE
        PERFORM pg_temp.registrar(false, p_descripcion,
            format('se esperaba %s y llegó %s/%s: %s', p_esperado, coalesce(nullif(v_restriccion, ''), '-'), v_estado, v_mensaje));
    END IF;
END $$;

-- La sentencia debe ejecutarse sin error (y se revierte para no afectar otras pruebas).
CREATE FUNCTION pg_temp.debe_funcionar(p_descripcion text, p_sql text) RETURNS void
    LANGUAGE plpgsql AS $$
DECLARE
    v_estado  text;
    v_mensaje text;
BEGIN
    BEGIN
        EXECUTE p_sql;
        RAISE EXCEPTION 'sin error' USING ERRCODE = 'P0999';
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS v_estado = RETURNED_SQLSTATE, v_mensaje = MESSAGE_TEXT;
    END;
    IF v_estado = 'P0999' THEN
        PERFORM pg_temp.registrar(true, p_descripcion, 'aceptada');
    ELSE
        PERFORM pg_temp.registrar(false, p_descripcion, format('error %s: %s', v_estado, v_mensaje));
    END IF;
END $$;

CREATE FUNCTION pg_temp.verificar(p_descripcion text, p_condicion boolean) RETURNS void
    LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_temp.registrar(coalesce(p_condicion, false), p_descripcion, CASE WHEN p_condicion THEN 'cumple' ELSE 'NO cumple' END);
END $$;

-- INSERT de un cliente válido (Carlos Alberto Gómez Mendoza) con campos sustituibles.
CREATE FUNCTION pg_temp.sql_cliente(
    p_curp      text    DEFAULT 'GOMC900515HQTMNR08',
    p_rfc       text    DEFAULT 'GOMC900515AB7',
    p_correo    text    DEFAULT 'carlos.gomez@ejemplo.com',
    p_fecha     date    DEFAULT DATE '1990-05-15',
    p_nombre    text    DEFAULT 'Carlos',
    p_telefono  text    DEFAULT '4421234567',
    p_ingreso   numeric DEFAULT 28000,
    p_activo    boolean DEFAULT true
) RETURNS text LANGUAGE sql AS $$
    SELECT format($f$
        INSERT INTO onboarding.clientes (nombre, segundo_nombre, apellido_paterno, apellido_materno,
            fecha_nacimiento, curp, rfc, sexo_id, nacionalidad, estado_civil_id, correo,
            telefono_movil, ocupacion, empresa, ingreso_mensual, activo)
        VALUES (%L, 'Alberto', 'Gómez', 'Mendoza', %L, %L, %L, 1, 'MX', 2, %L, %L,
                'Contador', 'Despacho Gómez', %s, %L)$f$,
        p_nombre, p_fecha, p_curp, p_rfc, p_correo, p_telefono, p_ingreso, p_activo)
$$;

-- Alta completa (cliente + domicilio + cuenta + usuario) para probar permisos del rol de aplicación.
CREATE FUNCTION pg_temp.registrar_como_aplicacion() RETURNS void
    LANGUAGE plpgsql AS $$
DECLARE
    v_id integer;
BEGIN
    EXECUTE pg_temp.sql_cliente(p_curp => 'RAHJ880310HDFMRN02', p_rfc => 'RAHJ880310XY1',
                                p_fecha => DATE '1988-03-10', p_correo => 'juan.ramirez@ejemplo.com');
    SELECT id INTO v_id FROM onboarding.clientes WHERE curp = 'RAHJ880310HDFMRN02';
    INSERT INTO onboarding.domicilios (cliente_id, calle, numero_exterior, colonia, municipio, estado_id, codigo_postal)
    VALUES (v_id, 'Av. Reforma', '500', 'Juárez', 'Cuauhtémoc', 9, '06600');
    INSERT INTO onboarding.cuentas (cliente_id, saldo) VALUES (v_id, 0);
    INSERT INTO onboarding.usuarios (cliente_id, correo, password)
    VALUES (v_id, 'juan.ramirez@ejemplo.com', '$2a$10$Aw804GDHNiYin/e3R3lIQepN9iJbGACq1E.nV5eXjH02d1VwN3.Ae');
END $$;

-- ----------------------------------------------------------------------------
-- Datos base: una clienta válida con domicilio, cuenta y usuario
-- ----------------------------------------------------------------------------
INSERT INTO clientes (nombre, segundo_nombre, apellido_paterno, apellido_materno, fecha_nacimiento,
                      curp, rfc, sexo_id, nacionalidad, estado_civil_id, correo, telefono_movil,
                      ocupacion, empresa, ingreso_mensual)
VALUES ('  María ', 'Fernanda', 'López', 'Hernández', DATE '1995-08-21',
        'lohf950821mgtprr08', 'lohf950821qk5', 2, 'mx', 1, '  Maria.Lopez@Ejemplo.COM ', '4731234567',
        'Ingeniera de software', 'Tecnologías del Bajío', 35000.00);

INSERT INTO domicilios (cliente_id, calle, numero_exterior, colonia, municipio, estado_id, codigo_postal)
SELECT id, 'Calle Hidalgo', '123', 'Centro', 'Guanajuato', 11, '36000' FROM clientes WHERE curp = 'LOHF950821MGTPRR08';

INSERT INTO cuentas (cliente_id, saldo) SELECT id, 1000.00 FROM clientes WHERE curp = 'LOHF950821MGTPRR08';

INSERT INTO usuarios (cliente_id, correo, password)
SELECT id, correo, '$2a$10$Aw804GDHNiYin/e3R3lIQepN9iJbGACq1E.nV5eXjH02d1VwN3.Ae'
  FROM clientes WHERE curp = 'LOHF950821MGTPRR08';

-- ----------------------------------------------------------------------------
-- Pruebas
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_hoy         date := (now() AT TIME ZONE 'America/Mexico_City')::date;
    v_fecha       date;
    v_cliente     integer;
    v_cuenta      record;
    v_usuario     record;
    v_cli         record;
BEGIN
    SELECT * INTO v_cli FROM onboarding.clientes WHERE curp = 'LOHF950821MGTPRR08';
    v_cliente := v_cli.id;

    RAISE NOTICE '--- Normalización y valores generados ---';
    PERFORM pg_temp.verificar('CURP y RFC se guardan en mayúsculas', v_cli.curp = 'LOHF950821MGTPRR08' AND v_cli.rfc = 'LOHF950821QK5');
    PERFORM pg_temp.verificar('Correo se guarda sin espacios y en minúsculas', v_cli.correo = 'maria.lopez@ejemplo.com');
    PERFORM pg_temp.verificar('Nombre sin espacios sobrantes; nacionalidad en mayúsculas', v_cli.nombre = 'María' AND v_cli.nacionalidad = 'MX');
    PERFORM pg_temp.verificar('Cliente nuevo queda activo y sin fecha de baja', v_cli.activo AND v_cli.fecha_baja IS NULL);

    SELECT * INTO v_cuenta FROM onboarding.cuentas WHERE cliente_id = v_cliente;
    PERFORM pg_temp.verificar('Cuenta: número de 10 dígitos generado por la BD', v_cuenta.numero_cuenta ~ '^[0-9]{10}$');
    PERFORM pg_temp.verificar('Cuenta: dígito verificador Luhn correcto', onboarding.fn_numero_cuenta_valido(v_cuenta.numero_cuenta));
    PERFORM pg_temp.verificar('Cuenta: estatus inicial ACTIVA (1) y moneda MXN', v_cuenta.estatus_id = 1 AND v_cuenta.moneda = 'MXN');
    PERFORM pg_temp.verificar('Luhn conocido: 7992739871 -> 3', onboarding.fn_digito_luhn('7992739871') = 3);

    RAISE NOTICE '--- Validaciones de clientes ---';
    PERFORM pg_temp.debe_funcionar('Cliente válido se acepta', pg_temp.sql_cliente());
    v_fecha := (v_hoy - INTERVAL '18 years')::date;
    PERFORM pg_temp.debe_funcionar('Cumple 18 años hoy: se acepta',
        pg_temp.sql_cliente(p_fecha => v_fecha, p_curp => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'HQTMNRA8',
                            p_rfc => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'AB7'));
    v_fecha := v_fecha + 1;
    PERFORM pg_temp.debe_fallar('Cumple 18 años mañana: menor de edad',
        pg_temp.sql_cliente(p_fecha => v_fecha, p_curp => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'HQTMNRA8',
                            p_rfc => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'AB7'),
        'tg_clientes_mayor_edad');
    v_fecha := v_hoy + 1;
    PERFORM pg_temp.debe_fallar('Fecha de nacimiento futura',
        pg_temp.sql_cliente(p_fecha => v_fecha, p_curp => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'HQTMNRA8',
                            p_rfc => 'GOMC' || onboarding.fn_fecha_aammdd(v_fecha) || 'AB7'),
        'tg_clientes_fecha_futura');
    PERFORM pg_temp.debe_fallar('Fecha anterior a 1900',
        pg_temp.sql_cliente(p_fecha => DATE '1899-05-15', p_curp => 'GOMC990515HQTMNR08', p_rfc => 'GOMC990515AB7'),
        'ck_clientes_fecha_nacimiento');
    PERFORM pg_temp.debe_fallar('Nombre con números', pg_temp.sql_cliente(p_nombre => 'Carl0s'), 'ck_clientes_nombre');
    PERFORM pg_temp.debe_fallar('Nombre de 1 carácter', pg_temp.sql_cliente(p_nombre => 'C'), 'ck_clientes_nombre');
    PERFORM pg_temp.debe_fallar('Nombre de 51 caracteres', pg_temp.sql_cliente(p_nombre => repeat('a', 51)), '22001');
    PERFORM pg_temp.debe_fallar('CURP con formato inválido', pg_temp.sql_cliente(p_curp => 'GOMC900515HXXMNR08'), 'ck_clientes_curp');
    PERFORM pg_temp.debe_fallar('CURP de 17 caracteres', pg_temp.sql_cliente(p_curp => 'GOMC900515HQTMNR0'), 'ck_clientes_curp');
    PERFORM pg_temp.debe_fallar('CURP con fecha distinta a la de nacimiento',
        pg_temp.sql_cliente(p_curp => 'GOMC900516HQTMNR08'), 'ck_clientes_curp_fecha');
    PERFORM pg_temp.debe_fallar('RFC con formato inválido (mes 13)', pg_temp.sql_cliente(p_rfc => 'GOMC901315AB7'), 'ck_clientes_rfc');
    PERFORM pg_temp.debe_fallar('RFC de 11 caracteres', pg_temp.sql_cliente(p_rfc => 'GOMC900515A'), 'ck_clientes_rfc');
    PERFORM pg_temp.debe_fallar('RFC de 13 con fecha distinta', pg_temp.sql_cliente(p_rfc => 'GOMC900516AB7'), 'ck_clientes_rfc_fecha');
    PERFORM pg_temp.debe_funcionar('RFC de 12 caracteres se acepta', pg_temp.sql_cliente(p_rfc => 'GOM900515AB7'));
    PERFORM pg_temp.debe_fallar('Correo sin dominio', pg_temp.sql_cliente(p_correo => 'carlos@ejemplo'), 'ck_clientes_correo');
    PERFORM pg_temp.debe_fallar('Correo con puntos consecutivos', pg_temp.sql_cliente(p_correo => 'carlos..gomez@ejemplo.com'), 'ck_clientes_correo');
    PERFORM pg_temp.debe_fallar('Correo de más de 100 caracteres',
        pg_temp.sql_cliente(p_correo => repeat('a', 95) || '@x.com'), '22001');
    PERFORM pg_temp.debe_fallar('Teléfono de 9 dígitos', pg_temp.sql_cliente(p_telefono => '442123456'), 'ck_clientes_telefono_movil');
    PERFORM pg_temp.debe_fallar('Teléfono con letras', pg_temp.sql_cliente(p_telefono => '44212345ab'), 'ck_clientes_telefono_movil');
    PERFORM pg_temp.debe_fallar('Ingreso mensual en cero', pg_temp.sql_cliente(p_ingreso => 0), 'ck_clientes_ingreso');
    PERFORM pg_temp.debe_fallar('Ingreso mensual negativo', pg_temp.sql_cliente(p_ingreso => -1), 'ck_clientes_ingreso');

    RAISE NOTICE '--- Unicidad ---';
    PERFORM pg_temp.debe_fallar('CURP duplicada',
        pg_temp.sql_cliente(p_curp => 'LOHF950821MGTPRR08', p_fecha => DATE '1995-08-21', p_rfc => 'GOMC950821AB7'), 'uq_clientes_curp');
    PERFORM pg_temp.debe_fallar('RFC duplicado',
        pg_temp.sql_cliente(p_rfc => 'LOHF950821QK5', p_fecha => DATE '1995-08-21', p_curp => 'GOMC950821HQTMNR08'), 'uq_clientes_rfc');
    PERFORM pg_temp.debe_fallar('Correo duplicado (aunque cambie mayúsculas)',
        pg_temp.sql_cliente(p_correo => 'MARIA.LOPEZ@ejemplo.com'), 'uq_clientes_correo');

    RAISE NOTICE '--- Domicilio ---';
    PERFORM pg_temp.debe_fallar('Código postal de 4 dígitos',
        format('UPDATE onboarding.domicilios SET codigo_postal = %L WHERE cliente_id = %s', '3600', v_cliente), 'ck_domicilios_cp');
    PERFORM pg_temp.debe_fallar('Domicilio fuera de México',
        format('UPDATE onboarding.domicilios SET pais = %L WHERE cliente_id = %s', 'US', v_cliente), 'ck_domicilios_pais_mx');
    PERFORM pg_temp.debe_fallar('Segundo domicilio para el mismo cliente',
        format($f$INSERT INTO onboarding.domicilios (cliente_id, calle, numero_exterior, colonia, municipio, estado_id, codigo_postal)
                  VALUES (%s, 'Otra', '1', 'Centro', 'León', 11, '37000')$f$, v_cliente), 'pk_domicilios');
    PERFORM pg_temp.debe_fallar('Estado inexistente (33)',
        format('UPDATE onboarding.domicilios SET estado_id = 33 WHERE cliente_id = %s', v_cliente), 'fk_domicilios_estado');

    RAISE NOTICE '--- Cuentas ---';
    PERFORM pg_temp.debe_fallar('Saldo negativo',
        format('UPDATE onboarding.cuentas SET saldo = -0.01 WHERE cliente_id = %s', v_cliente), 'ck_cuentas_saldo');
    PERFORM pg_temp.debe_fallar('Número de cuenta con dígito verificador incorrecto',
        format($f$INSERT INTO onboarding.cuentas (cliente_id, saldo, numero_cuenta) VALUES (%s, 0, '1000000010')$f$, v_cliente),
        'ck_cuentas_numero');
    PERFORM pg_temp.debe_fallar('Número de cuenta duplicado',
        format($f$INSERT INTO onboarding.cuentas (cliente_id, saldo, numero_cuenta) VALUES (%s, 0, %L)$f$, v_cliente, v_cuenta.numero_cuenta),
        'uq_cuentas_numero');
    PERFORM pg_temp.debe_fallar('Modificar el número de cuenta',
        format($f$UPDATE onboarding.cuentas SET numero_cuenta = '1000000028' WHERE id = %s$f$, v_cuenta.id), 'tg_cuentas_numero_inmutable');
    PERFORM pg_temp.debe_funcionar('Un cliente puede tener varias cuentas (1:N)',
        format('INSERT INTO onboarding.cuentas (cliente_id, saldo) VALUES (%s, 0)', v_cliente));

    RAISE NOTICE '--- CURP y RFC inmutables ---';
    PERFORM pg_temp.debe_fallar('Modificar la CURP',
        format($f$UPDATE onboarding.clientes SET curp = 'LOHF950821MGTPRR17' WHERE id = %s$f$, v_cliente), 'tg_clientes_curp_inmutable');
    PERFORM pg_temp.debe_fallar('Modificar el RFC',
        format($f$UPDATE onboarding.clientes SET rfc = 'LOHF950821QK6' WHERE id = %s$f$, v_cliente), 'tg_clientes_rfc_inmutable');

    RAISE NOTICE '--- Usuarios ---';
    PERFORM pg_temp.debe_fallar('Contraseña en texto plano',
        format($f$UPDATE onboarding.usuarios SET password = 'Segura#2026' WHERE cliente_id = %s$f$, v_cliente), 'ck_usuarios_password_bcrypt');
    PERFORM pg_temp.debe_fallar('Segundo usuario para el mismo cliente',
        format($f$INSERT INTO onboarding.usuarios (cliente_id, correo, password)
                  VALUES (%s, 'maria.lopez@ejemplo.com', '$2a$10$Aw804GDHNiYin/e3R3lIQepN9iJbGACq1E.nV5eXjH02d1VwN3.Ae')$f$, v_cliente),
        'uq_usuarios_cliente');
    PERFORM pg_temp.debe_fallar('Usuario con correo distinto al del cliente',
        format($f$UPDATE onboarding.usuarios SET correo = 'otra@ejemplo.com' WHERE cliente_id = %s$f$, v_cliente), 'tg_usuarios_correo_cliente');

    UPDATE onboarding.clientes SET correo = 'mafer.lopez@ejemplo.com' WHERE id = v_cliente;
    SELECT * INTO v_usuario FROM onboarding.usuarios WHERE cliente_id = v_cliente;
    PERFORM pg_temp.verificar('Cambiar el correo del cliente actualiza su usuario', v_usuario.correo = 'mafer.lopez@ejemplo.com');

    UPDATE onboarding.usuarios SET password = '$2a$10$GRmqp8ADhE4fNWGprgchKOoPcXSQN0LEyKmWqqJzYRXbqM7lsfSNu' WHERE cliente_id = v_cliente;
    PERFORM pg_temp.verificar('Cambiar la contraseña sube version_credenciales',
        (SELECT version_credenciales FROM onboarding.usuarios WHERE cliente_id = v_cliente) = v_usuario.version_credenciales + 1);

    RAISE NOTICE '--- Baja lógica ---';
    UPDATE onboarding.clientes SET activo = FALSE WHERE id = v_cliente;
    SELECT * INTO v_cli FROM onboarding.clientes WHERE id = v_cliente;
    PERFORM pg_temp.verificar('Baja lógica registra fecha_baja', NOT v_cli.activo AND v_cli.fecha_baja IS NOT NULL);
    PERFORM pg_temp.verificar('Baja lógica inactiva al usuario',
        NOT (SELECT activo FROM onboarding.usuarios WHERE cliente_id = v_cliente));
    PERFORM pg_temp.verificar('Baja lógica pasa sus cuentas a INACTIVA',
        NOT EXISTS (SELECT 1 FROM onboarding.cuentas WHERE cliente_id = v_cliente AND estatus_id = 1));
    PERFORM pg_temp.verificar('El cliente sigue existiendo (no hay borrado físico)',
        EXISTS (SELECT 1 FROM onboarding.clientes WHERE id = v_cliente));
    PERFORM pg_temp.debe_fallar('Cuenta ACTIVA para cliente inactivo',
        format('INSERT INTO onboarding.cuentas (cliente_id, saldo) VALUES (%s, 0)', v_cliente), 'tg_cuentas_cliente_inactivo');
    PERFORM pg_temp.debe_fallar('Reactivar una cuenta de cliente inactivo',
        format('UPDATE onboarding.cuentas SET estatus_id = 1 WHERE cliente_id = %s', v_cliente), 'tg_cuentas_cliente_inactivo');
    PERFORM pg_temp.debe_fallar('Reactivar el usuario de un cliente inactivo',
        format('UPDATE onboarding.usuarios SET activo = TRUE WHERE cliente_id = %s', v_cliente), 'tg_usuarios_cliente_inactivo');
    PERFORM pg_temp.debe_fallar('DELETE físico de un cliente',
        format('DELETE FROM onboarding.clientes WHERE id = %s', v_cliente), 'tg_baja_logica');
    PERFORM pg_temp.debe_fallar('DELETE físico de una cuenta',
        format('DELETE FROM onboarding.cuentas WHERE cliente_id = %s', v_cliente), 'tg_baja_logica');
END $$;

-- ----------------------------------------------------------------------------
-- Mínimo privilegio: el rol de la aplicación no puede borrar
-- ----------------------------------------------------------------------------
SET LOCAL ROLE rol_app_onboarding;
DO $$
BEGIN
    RAISE NOTICE '--- Permisos del rol de aplicación ---';
    PERFORM pg_temp.debe_funcionar('Rol de aplicación: registra cliente, domicilio, cuenta y usuario',
        'SELECT pg_temp.registrar_como_aplicacion()');
    PERFORM pg_temp.debe_fallar('Rol de aplicación: DELETE denegado',
        'DELETE FROM onboarding.clientes', '42501');
    PERFORM pg_temp.debe_fallar('Rol de aplicación: TRUNCATE denegado',
        'TRUNCATE onboarding.cuentas', '42501');
    PERFORM pg_temp.debe_fallar('Rol de aplicación: no puede modificar catálogos',
        $q$UPDATE onboarding.cat_estado SET nombre = 'X' WHERE id = 1$q$, '42501');
END $$;
RESET ROLE;

-- ----------------------------------------------------------------------------
-- Resumen
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_total  integer;
    v_fallas integer;
BEGIN
    SELECT count(*), count(*) FILTER (WHERE NOT ok) INTO v_total, v_fallas FROM pg_temp.resultados;
    RAISE NOTICE '==== % pruebas, % correctas, % fallidas ====', v_total, v_total - v_fallas, v_fallas;
    IF v_fallas > 0 THEN
        RAISE EXCEPTION 'Hay % prueba(s) de base de datos fallida(s)', v_fallas;
    END IF;
END $$;

ROLLBACK;
