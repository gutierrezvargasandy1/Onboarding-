-- =============================================================================
-- V1 · Esquema de onboarding de clientes personas físicas
-- =============================================================================
-- Tipos de datos y uso de memoria (PostgreSQL)
--  * Llaves INTEGER (4 bytes). El dominio está acotado: México tiene ~130 millones
--    de habitantes e INTEGER admite 2,147 millones. BIGINT gastaría 4 bytes más
--    por fila y por cada llave foránea, y también en los índices compuestos.
--  * Catálogos pequeños y cerrados (sexo, estado civil, estatus de cuenta,
--    entidad federativa) en SMALLINT (2 bytes) con llave foránea, en vez de
--    repetir texto en cada fila.
--  * Texto en VARCHAR(n) con CHECK de formato. En PostgreSQL CHAR(n) ocupa lo
--    mismo que VARCHAR(n), rellena con espacios y complica JPA, por eso no se usa.
--    Teléfono, código postal, CURP, RFC y número de cuenta son texto: pueden
--    llevar ceros a la izquierda o letras y nunca se opera aritméticamente con ellos.
--  * Dinero en NUMERIC (exacto). Nunca REAL/DOUBLE (binarios, con redondeo).
--  * Instantes en TIMESTAMPTZ (8 bytes, guardado en UTC); fecha de nacimiento en DATE (4 bytes).
--  * Columnas de ancho fijo ordenadas por alineación (8 -> 4 -> 2 -> 1 bytes) y
--    al final las de longitud variable: así no se desperdician bytes de relleno.
--    Los VARCHAR cortos (< 127 bytes) usan encabezado de 1 byte y no se alinean,
--    por lo que su orden no afecta el tamaño de la fila.
--  * Restricciones con nombre explícito: la API las traduce a mensajes claros.
--  * Las reglas de negocio se validan en Java y la base de datos las garantiza
--    otra vez (CHECK + triggers), aunque alguien escriba directo con SQL.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS onboarding;

-- -----------------------------------------------------------------------------
-- 1. FUNCIONES AUXILIARES (IMMUTABLE: se pueden usar dentro de CHECK)
-- -----------------------------------------------------------------------------

-- Nombres y apellidos: solo letras (incluye acentos, diéresis y ñ) y espacios
-- simples entre palabras; de 2 a 50 caracteres.
CREATE FUNCTION fn_nombre_valido(p_texto text) RETURNS boolean
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
AS $$
    SELECT char_length(p_texto) BETWEEN 2 AND 50
       AND p_texto ~ '^[A-Za-zÀ-ÖØ-öø-ÿ]+( [A-Za-zÀ-ÖØ-öø-ÿ]+)*$'
$$;

-- Fecha en formato AAMMDD, tal como aparece dentro de la CURP y del RFC.
CREATE FUNCTION fn_fecha_aammdd(p_fecha date) RETURNS text
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
AS $$
    SELECT lpad((extract(year FROM p_fecha)::integer % 100)::text, 2, '0')
        || lpad(extract(month FROM p_fecha)::integer::text, 2, '0')
        || lpad(extract(day FROM p_fecha)::integer::text, 2, '0')
$$;

-- Dígito verificador Luhn (módulo 10) de una cadena de dígitos.
CREATE FUNCTION fn_digito_luhn(p_digitos text) RETURNS smallint
    LANGUAGE plpgsql IMMUTABLE STRICT PARALLEL SAFE
AS $$
DECLARE
    v_suma  integer := 0;
    v_valor integer;
    v_doble boolean := true;   -- se duplica desde el dígito más a la derecha
BEGIN
    IF p_digitos !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    FOR i IN REVERSE char_length(p_digitos)..1 LOOP
        v_valor := substr(p_digitos, i, 1)::integer;
        IF v_doble THEN
            v_valor := v_valor * 2;
            IF v_valor > 9 THEN
                v_valor := v_valor - 9;
            END IF;
        END IF;
        v_suma  := v_suma + v_valor;
        v_doble := NOT v_doble;
    END LOOP;
    RETURN ((10 - v_suma % 10) % 10)::smallint;
END $$;

-- Número de cuenta: 10 dígitos = 9 de secuencia + 1 dígito verificador Luhn.
CREATE FUNCTION fn_numero_cuenta_valido(p_numero text) RETURNS boolean
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
    SET search_path = onboarding, pg_temp
AS $$
    SELECT p_numero ~ '^[0-9]{10}$'
       AND right(p_numero, 1)::smallint = fn_digito_luhn(left(p_numero, 9))
$$;

-- -----------------------------------------------------------------------------
-- 2. CATÁLOGOS
-- -----------------------------------------------------------------------------
CREATE TABLE cat_pais (
    codigo_iso2 VARCHAR(2)  NOT NULL,          -- ISO 3166-1 alfa-2
    nombre      VARCHAR(80) NOT NULL,
    CONSTRAINT pk_cat_pais        PRIMARY KEY (codigo_iso2),
    CONSTRAINT uq_cat_pais_nombre UNIQUE (nombre),
    CONSTRAINT ck_cat_pais_codigo CHECK (codigo_iso2 ~ '^[A-Z]{2}$')
);

CREATE TABLE cat_estado (
    id     SMALLINT    NOT NULL,               -- clave INEGI de la entidad federativa (1-32)
    nombre VARCHAR(60) NOT NULL,
    CONSTRAINT pk_cat_estado        PRIMARY KEY (id),
    CONSTRAINT uq_cat_estado_nombre UNIQUE (nombre),
    CONSTRAINT ck_cat_estado_id     CHECK (id BETWEEN 1 AND 32)
);

CREATE TABLE cat_sexo (
    id          SMALLINT    NOT NULL,
    clave       VARCHAR(1)  NOT NULL,
    descripcion VARCHAR(30) NOT NULL,
    CONSTRAINT pk_cat_sexo       PRIMARY KEY (id),
    CONSTRAINT uq_cat_sexo_clave UNIQUE (clave)
);

CREATE TABLE cat_estado_civil (
    id          SMALLINT    NOT NULL,
    clave       VARCHAR(20) NOT NULL,
    descripcion VARCHAR(30) NOT NULL,
    CONSTRAINT pk_cat_estado_civil       PRIMARY KEY (id),
    CONSTRAINT uq_cat_estado_civil_clave UNIQUE (clave)
);

CREATE TABLE cat_estatus_cuenta (
    id          SMALLINT    NOT NULL,
    clave       VARCHAR(20) NOT NULL,
    descripcion VARCHAR(30) NOT NULL,
    CONSTRAINT pk_cat_estatus_cuenta       PRIMARY KEY (id),
    CONSTRAINT uq_cat_estatus_cuenta_clave UNIQUE (clave)
);

-- -----------------------------------------------------------------------------
-- 3. CLIENTES (datos personales, de contacto e información laboral)
-- -----------------------------------------------------------------------------
CREATE TABLE clientes (
    -- 4 + 4 bytes (juntos completan 8 y dejan alineadas las fechas)
    id                  INTEGER       GENERATED ALWAYS AS IDENTITY,
    version             INTEGER       NOT NULL DEFAULT 0,          -- bloqueo optimista (JPA @Version)
    -- 8 bytes
    fecha_registro      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_baja          TIMESTAMPTZ,
    -- 4 bytes
    fecha_nacimiento    DATE          NOT NULL,
    -- 2 bytes
    sexo_id             SMALLINT      NOT NULL,
    estado_civil_id     SMALLINT      NOT NULL,
    -- 1 byte
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    -- longitud variable
    nombre              VARCHAR(50)   NOT NULL,
    segundo_nombre      VARCHAR(50),
    apellido_paterno    VARCHAR(50)   NOT NULL,
    apellido_materno    VARCHAR(50)   NOT NULL,
    curp                VARCHAR(18)   NOT NULL,
    rfc                 VARCHAR(13)   NOT NULL,
    nacionalidad        VARCHAR(2)    NOT NULL,
    correo              VARCHAR(100)  NOT NULL,
    telefono_movil      VARCHAR(10)   NOT NULL,
    telefono_alterno    VARCHAR(10),
    ocupacion           VARCHAR(100)  NOT NULL,
    empresa             VARCHAR(100)  NOT NULL,
    ingreso_mensual     NUMERIC(12,2) NOT NULL,                    -- hasta 9,999,999,999.99

    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT fk_clientes_sexo         FOREIGN KEY (sexo_id)         REFERENCES cat_sexo (id),
    CONSTRAINT fk_clientes_estado_civil FOREIGN KEY (estado_civil_id) REFERENCES cat_estado_civil (id),
    CONSTRAINT fk_clientes_nacionalidad FOREIGN KEY (nacionalidad)    REFERENCES cat_pais (codigo_iso2),

    CONSTRAINT uq_clientes_curp   UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),

    CONSTRAINT ck_clientes_nombre           CHECK (fn_nombre_valido(nombre)),
    CONSTRAINT ck_clientes_segundo_nombre   CHECK (segundo_nombre IS NULL OR fn_nombre_valido(segundo_nombre)),
    CONSTRAINT ck_clientes_apellido_paterno CHECK (fn_nombre_valido(apellido_paterno)),
    CONSTRAINT ck_clientes_apellido_materno CHECK (fn_nombre_valido(apellido_materno)),
    -- CURP: 18 caracteres exactos (los garantiza el patrón anclado de 18 posiciones)
    CONSTRAINT ck_clientes_curp CHECK (curp ~ '^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HMX](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$'),
    -- RFC: 13 caracteres (persona física) o 12 (persona moral), como pide la tarea
    CONSTRAINT ck_clientes_rfc CHECK (rfc ~ '^[A-ZÑ&]{3,4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{2}[0-9A]$'),
    -- La fecha de nacimiento debe coincidir con la que va dentro de la CURP y del RFC de 13
    CONSTRAINT ck_clientes_curp_fecha CHECK (substr(curp, 5, 6) = fn_fecha_aammdd(fecha_nacimiento)),
    CONSTRAINT ck_clientes_rfc_fecha  CHECK (char_length(rfc) = 12 OR substr(rfc, 5, 6) = fn_fecha_aammdd(fecha_nacimiento)),
    CONSTRAINT ck_clientes_fecha_nacimiento CHECK (fecha_nacimiento >= DATE '1900-01-01'),
    CONSTRAINT ck_clientes_correo CHECK (
        char_length(correo) <= 100
        AND correo = lower(correo)
        AND correo ~ '^[a-z0-9_%+-]+(\.[a-z0-9_%+-]+)*@([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,}$'
    ),
    CONSTRAINT ck_clientes_telefono_movil   CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_telefono_alterno CHECK (telefono_alterno IS NULL OR telefono_alterno ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_ocupacion CHECK (btrim(ocupacion) <> ''),
    CONSTRAINT ck_clientes_empresa   CHECK (btrim(empresa) <> ''),
    CONSTRAINT ck_clientes_ingreso   CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_baja      CHECK ((activo AND fecha_baja IS NULL) OR (NOT activo AND fecha_baja IS NOT NULL))
);

-- -----------------------------------------------------------------------------
-- 4. DOMICILIOS (1 cliente : 1 domicilio)
-- -----------------------------------------------------------------------------
-- La llave primaria ES el id del cliente (llave compartida): garantiza la
-- relación 1:1 sin una columna id extra ni un segundo índice único.
CREATE TABLE domicilios (
    -- 8 bytes
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- 4 bytes
    cliente_id          INTEGER      NOT NULL,
    -- 2 bytes
    estado_id           SMALLINT     NOT NULL,
    -- longitud variable
    calle               VARCHAR(100) NOT NULL,
    numero_exterior     VARCHAR(10)  NOT NULL,
    numero_interior     VARCHAR(10),
    colonia             VARCHAR(100) NOT NULL,
    municipio           VARCHAR(100) NOT NULL,
    codigo_postal       VARCHAR(5)   NOT NULL,
    pais                VARCHAR(2)   NOT NULL DEFAULT 'MX',

    CONSTRAINT pk_domicilios         PRIMARY KEY (cliente_id),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_domicilios_estado  FOREIGN KEY (estado_id)  REFERENCES cat_estado (id),
    CONSTRAINT fk_domicilios_pais    FOREIGN KEY (pais)       REFERENCES cat_pais (codigo_iso2),
    -- El catálogo de estados (INEGI) y el CP de 5 dígitos son de México
    CONSTRAINT ck_domicilios_pais_mx   CHECK (pais = 'MX'),
    CONSTRAINT ck_domicilios_cp        CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT ck_domicilios_calle     CHECK (btrim(calle) <> ''),
    CONSTRAINT ck_domicilios_num_ext   CHECK (btrim(numero_exterior) <> ''),
    CONSTRAINT ck_domicilios_num_int   CHECK (numero_interior IS NULL OR btrim(numero_interior) <> ''),
    CONSTRAINT ck_domicilios_colonia   CHECK (btrim(colonia) <> ''),
    CONSTRAINT ck_domicilios_municipio CHECK (btrim(municipio) <> '')
);

-- -----------------------------------------------------------------------------
-- 5. CUENTAS (1 cliente : N cuentas)
-- -----------------------------------------------------------------------------
-- Secuencia de 9 dígitos (cabe en INTEGER); el décimo dígito es el verificador.
CREATE SEQUENCE seq_numero_cuenta AS integer
    START WITH 100000001 MINVALUE 100000001 MAXVALUE 999999999 NO CYCLE;

CREATE FUNCTION fn_generar_numero_cuenta() RETURNS varchar
    LANGUAGE sql VOLATILE
    SET search_path = onboarding, pg_temp
AS $$
    SELECT base || fn_digito_luhn(base)
      FROM (SELECT nextval('seq_numero_cuenta')::text AS base) s
$$;

CREATE TABLE cuentas (
    -- 4 + 4 bytes
    id                  INTEGER       GENERATED ALWAYS AS IDENTITY,
    cliente_id          INTEGER       NOT NULL,
    -- 8 bytes
    fecha_apertura      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- 4 bytes
    version             INTEGER       NOT NULL DEFAULT 0,          -- evita actualizaciones perdidas del saldo
    -- 2 bytes
    estatus_id          SMALLINT      NOT NULL DEFAULT 1,          -- 1 = ACTIVA
    -- longitud variable
    numero_cuenta       VARCHAR(10)   NOT NULL DEFAULT fn_generar_numero_cuenta(),
    saldo               NUMERIC(14,2) NOT NULL DEFAULT 0,
    moneda              VARCHAR(3)    NOT NULL DEFAULT 'MXN',      -- ISO 4217

    CONSTRAINT pk_cuentas         PRIMARY KEY (id),
    CONSTRAINT uq_cuentas_numero  UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuentas_estatus FOREIGN KEY (estatus_id) REFERENCES cat_estatus_cuenta (id),
    CONSTRAINT ck_cuentas_numero  CHECK (fn_numero_cuenta_valido(numero_cuenta)),
    CONSTRAINT ck_cuentas_saldo   CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_moneda  CHECK (moneda ~ '^[A-Z]{3}$')
);
ALTER SEQUENCE seq_numero_cuenta OWNED BY cuentas.numero_cuenta;

-- -----------------------------------------------------------------------------
-- 6. USUARIOS (1 cliente : 1 usuario de acceso; el correo es el nombre de usuario)
-- -----------------------------------------------------------------------------
CREATE TABLE usuarios (
    -- 4 + 4 bytes
    id                   INTEGER      GENERATED ALWAYS AS IDENTITY,
    cliente_id           INTEGER      NOT NULL,
    -- 8 bytes
    fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- 4 bytes
    version_credenciales INTEGER      NOT NULL DEFAULT 0,          -- sube al cambiar la contraseña: invalida tokens previos
    -- 1 byte
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    -- longitud variable
    correo               VARCHAR(100) NOT NULL,
    password             VARCHAR(100) NOT NULL,                    -- hash BCrypt (60 caracteres), nunca texto plano

    CONSTRAINT pk_usuarios         PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),            -- cada cliente tiene un solo usuario
    CONSTRAINT uq_usuarios_correo  UNIQUE (correo),
    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE RESTRICT,
    -- Rechaza texto plano: solo acepta un hash BCrypt ($2a$, $2b$ o $2y$ + costo + 53 caracteres),
    -- con o sin el prefijo {bcrypt} de Spring Security.
    CONSTRAINT ck_usuarios_password_bcrypt CHECK (password ~ '^(\{bcrypt\})?\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),
    CONSTRAINT ck_usuarios_correo  CHECK (correo = lower(correo) AND char_length(correo) <= 100),
    CONSTRAINT ck_usuarios_version CHECK (version_credenciales >= 0)
);

-- -----------------------------------------------------------------------------
-- 7. ÍNDICES
-- -----------------------------------------------------------------------------
-- Las PK y UNIQUE ya crean su índice: clientes(curp), clientes(rfc), clientes(correo),
-- cuentas(numero_cuenta), usuarios(correo), usuarios(cliente_id), domicilios(cliente_id).
-- Las FK hacia catálogos no se indexan: los catálogos no se borran ni se consulta por ellas.

-- FK sin índice automático en PostgreSQL: cuentas de un cliente (consulta y baja en cascada)
CREATE INDEX idx_cuentas_cliente_id ON cuentas (cliente_id);
-- Consulta de clientes registrados en un rango de fechas
CREATE INDEX idx_clientes_fecha_registro ON clientes (fecha_registro);
-- Índices parciales: solo guardan las filas activas (más pequeños que un índice completo)
CREATE INDEX idx_clientes_activos ON clientes (id) WHERE activo;
CREATE INDEX idx_cuentas_activas  ON cuentas (id)  WHERE estatus_id = 1;

-- -----------------------------------------------------------------------------
-- 8. TRIGGERS: reglas de negocio garantizadas por la base de datos
-- -----------------------------------------------------------------------------
-- PostgreSQL ejecuta los triggers de una tabla en orden alfabético; el número en
-- el nombre fija ese orden (0 = bloqueo de borrado, 1 = validación, 9 = fecha).

-- 8.1 fecha_actualizacion automática
CREATE FUNCTION fn_set_fecha_actualizacion() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.fecha_actualizacion := now();
    RETURN NEW;
END $$;

CREATE TRIGGER trg_clientes_9_fecha   BEFORE UPDATE ON clientes   FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();
CREATE TRIGGER trg_domicilios_9_fecha BEFORE UPDATE ON domicilios FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();
CREATE TRIGGER trg_cuentas_9_fecha    BEFORE UPDATE ON cuentas    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();
CREATE TRIGGER trg_usuarios_9_fecha   BEFORE UPDATE ON usuarios   FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- 8.2 Baja lógica: nadie puede borrar físicamente clientes, domicilios, cuentas ni usuarios
CREATE FUNCTION fn_bloquear_borrado() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'No se permite eliminar registros de %: la baja es lógica', TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation', CONSTRAINT = 'tg_baja_logica';
END $$;

CREATE TRIGGER trg_clientes_0_sin_borrado   BEFORE DELETE ON clientes   FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado();
CREATE TRIGGER trg_domicilios_0_sin_borrado BEFORE DELETE ON domicilios FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado();
CREATE TRIGGER trg_cuentas_0_sin_borrado    BEFORE DELETE ON cuentas    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado();
CREATE TRIGGER trg_usuarios_0_sin_borrado   BEFORE DELETE ON usuarios   FOR EACH ROW EXECUTE FUNCTION fn_bloquear_borrado();

-- 8.3 Clientes: normalización, CURP/RFC inmutables, fecha no futura, mayoría de edad y fecha de baja
CREATE FUNCTION fn_clientes_validar() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = onboarding, pg_temp
AS $$
DECLARE
    v_hoy date := (now() AT TIME ZONE 'America/Mexico_City')::date;
BEGIN
    -- Normalización (por si alguien inserta directo con SQL)
    NEW.nombre           := regexp_replace(btrim(NEW.nombre), '\s+', ' ', 'g');
    NEW.segundo_nombre   := NULLIF(regexp_replace(btrim(NEW.segundo_nombre), '\s+', ' ', 'g'), '');
    NEW.apellido_paterno := regexp_replace(btrim(NEW.apellido_paterno), '\s+', ' ', 'g');
    NEW.apellido_materno := regexp_replace(btrim(NEW.apellido_materno), '\s+', ' ', 'g');
    NEW.curp             := upper(btrim(NEW.curp));
    NEW.rfc              := upper(btrim(NEW.rfc));
    NEW.nacionalidad     := upper(btrim(NEW.nacionalidad));
    NEW.correo           := lower(btrim(NEW.correo));
    NEW.telefono_movil   := btrim(NEW.telefono_movil);
    NEW.telefono_alterno := NULLIF(btrim(NEW.telefono_alterno), '');
    NEW.ocupacion        := btrim(NEW.ocupacion);
    NEW.empresa          := btrim(NEW.empresa);

    IF TG_OP = 'UPDATE' THEN
        IF NEW.curp IS DISTINCT FROM OLD.curp THEN
            RAISE EXCEPTION 'La CURP no puede modificarse'
                USING ERRCODE = 'integrity_constraint_violation', CONSTRAINT = 'tg_clientes_curp_inmutable';
        END IF;
        IF NEW.rfc IS DISTINCT FROM OLD.rfc THEN
            RAISE EXCEPTION 'El RFC no puede modificarse'
                USING ERRCODE = 'integrity_constraint_violation', CONSTRAINT = 'tg_clientes_rfc_inmutable';
        END IF;
    END IF;

    -- Fecha de nacimiento: se valida al insertar o cuando cambia
    IF TG_OP = 'INSERT' OR NEW.fecha_nacimiento IS DISTINCT FROM OLD.fecha_nacimiento THEN
        IF NEW.fecha_nacimiento > v_hoy THEN
            RAISE EXCEPTION 'La fecha de nacimiento no puede ser futura'
                USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_clientes_fecha_futura';
        END IF;
        IF NEW.fecha_nacimiento > (v_hoy - INTERVAL '18 years')::date THEN
            RAISE EXCEPTION 'El cliente debe ser mayor de edad (18 años o más)'
                USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_clientes_mayor_edad';
        END IF;
    END IF;

    -- Baja lógica: fecha_baja siempre consistente con activo
    IF NEW.activo THEN
        NEW.fecha_baja := NULL;
    ELSIF TG_OP = 'INSERT' OR OLD.activo THEN
        NEW.fecha_baja := now();
    ELSE
        NEW.fecha_baja := OLD.fecha_baja;
    END IF;

    RETURN NEW;
END $$;

CREATE TRIGGER trg_clientes_1_validar
    BEFORE INSERT OR UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION fn_clientes_validar();

-- 8.4 Baja lógica en cascada: usuario inactivo y cuentas ACTIVA (1) -> INACTIVA (2)
CREATE FUNCTION fn_clientes_baja_cascada() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = onboarding, pg_temp
AS $$
BEGIN
    UPDATE usuarios SET activo = FALSE WHERE cliente_id = NEW.id AND activo;
    UPDATE cuentas  SET estatus_id = 2 WHERE cliente_id = NEW.id AND estatus_id = 1;
    RETURN NULL;
END $$;

CREATE TRIGGER trg_clientes_baja_cascada
    AFTER UPDATE OF activo ON clientes
    FOR EACH ROW WHEN (OLD.activo AND NOT NEW.activo)
    EXECUTE FUNCTION fn_clientes_baja_cascada();

-- 8.5 Si cambia el correo del cliente se sincroniza su usuario (el correo ES el nombre de usuario)
CREATE FUNCTION fn_clientes_sync_correo() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = onboarding, pg_temp
AS $$
BEGIN
    UPDATE usuarios SET correo = NEW.correo WHERE cliente_id = NEW.id AND correo IS DISTINCT FROM NEW.correo;
    RETURN NULL;
END $$;

CREATE TRIGGER trg_clientes_sync_correo
    AFTER UPDATE OF correo ON clientes
    FOR EACH ROW WHEN (OLD.correo IS DISTINCT FROM NEW.correo)
    EXECUTE FUNCTION fn_clientes_sync_correo();

-- 8.6 Cuentas: número y cliente inmutables; solo clientes activos pueden tener cuentas ACTIVAS (1)
CREATE FUNCTION fn_cuentas_validar() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = onboarding, pg_temp
AS $$
DECLARE
    v_cliente_activo boolean;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.numero_cuenta IS DISTINCT FROM OLD.numero_cuenta THEN
            RAISE EXCEPTION 'El número de cuenta no puede modificarse'
                USING ERRCODE = 'integrity_constraint_violation', CONSTRAINT = 'tg_cuentas_numero_inmutable';
        END IF;
        IF NEW.cliente_id IS DISTINCT FROM OLD.cliente_id THEN
            RAISE EXCEPTION 'La cuenta no puede reasignarse a otro cliente'
                USING ERRCODE = 'integrity_constraint_violation', CONSTRAINT = 'tg_cuentas_cliente_inmutable';
        END IF;
    END IF;

    IF NEW.estatus_id = 1 THEN
        SELECT activo INTO v_cliente_activo FROM clientes WHERE id = NEW.cliente_id FOR SHARE;
        IF FOUND AND NOT v_cliente_activo THEN
            RAISE EXCEPTION 'Solo los clientes activos pueden tener cuentas activas'
                USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_cuentas_cliente_inactivo';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_cuentas_1_validar
    BEFORE INSERT OR UPDATE ON cuentas
    FOR EACH ROW EXECUTE FUNCTION fn_cuentas_validar();

-- 8.7 Usuarios: mismo correo que su cliente, solo clientes activos tienen usuario activo,
--     cliente inmutable y versión de credenciales que sube al cambiar la contraseña
CREATE FUNCTION fn_usuarios_validar() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path = onboarding, pg_temp
AS $$
DECLARE
    v_activo boolean;
    v_correo varchar;
BEGIN
    NEW.correo := lower(btrim(NEW.correo));

    IF TG_OP = 'UPDATE' THEN
        IF NEW.cliente_id IS DISTINCT FROM OLD.cliente_id THEN
            RAISE EXCEPTION 'El usuario no puede reasignarse a otro cliente'
                USING ERRCODE = 'integrity_constraint_violation', CONSTRAINT = 'tg_usuarios_cliente_inmutable';
        END IF;
        IF NEW.password IS DISTINCT FROM OLD.password
           AND NEW.version_credenciales <= OLD.version_credenciales THEN
            NEW.version_credenciales := OLD.version_credenciales + 1;
        END IF;
    END IF;

    SELECT activo, correo INTO v_activo, v_correo FROM clientes WHERE id = NEW.cliente_id FOR SHARE;
    IF FOUND THEN
        IF NEW.correo IS DISTINCT FROM v_correo THEN
            RAISE EXCEPTION 'El usuario debe usar el mismo correo registrado por el cliente'
                USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_usuarios_correo_cliente';
        END IF;
        IF NEW.activo AND NOT v_activo THEN
            RAISE EXCEPTION 'Un cliente dado de baja no puede tener usuario activo'
                USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_usuarios_cliente_inactivo';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_usuarios_1_validar
    BEFORE INSERT OR UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_usuarios_validar();

-- -----------------------------------------------------------------------------
-- 9. DOCUMENTACIÓN EN EL CATÁLOGO DE LA BASE DE DATOS
-- -----------------------------------------------------------------------------
COMMENT ON SCHEMA onboarding IS 'Onboarding de clientes personas físicas';

-- Catálogos
COMMENT ON TABLE  cat_pais IS 'Países ISO 3166-1 (nacionalidad del cliente y país del domicilio).';
COMMENT ON COLUMN cat_pais.codigo_iso2 IS 'Código ISO 3166-1 alfa-2 (MX, US...).';
COMMENT ON COLUMN cat_pais.nombre      IS 'Nombre del país en español.';
COMMENT ON TABLE  cat_estado IS 'Entidades federativas de México con su clave INEGI.';
COMMENT ON COLUMN cat_estado.id     IS 'Clave INEGI de la entidad (1-32).';
COMMENT ON COLUMN cat_estado.nombre IS 'Nombre oficial de la entidad federativa.';
COMMENT ON TABLE  cat_sexo IS 'Sexo, como en la CURP: H, M o X.';
COMMENT ON COLUMN cat_sexo.id          IS 'Identificador (1=H, 2=M, 3=X).';
COMMENT ON COLUMN cat_sexo.clave       IS 'Clave que usa la API.';
COMMENT ON COLUMN cat_sexo.descripcion IS 'Texto para mostrar.';
COMMENT ON TABLE  cat_estado_civil IS 'Estados civiles.';
COMMENT ON COLUMN cat_estado_civil.id          IS 'Identificador.';
COMMENT ON COLUMN cat_estado_civil.clave       IS 'Clave que usa la API (SOLTERO, CASADO...).';
COMMENT ON COLUMN cat_estado_civil.descripcion IS 'Texto para mostrar.';
COMMENT ON TABLE  cat_estatus_cuenta IS 'Estatus de una cuenta. Toda cuenta nueva inicia ACTIVA.';
COMMENT ON COLUMN cat_estatus_cuenta.id          IS 'Identificador (1=ACTIVA, 2=INACTIVA, 3=BLOQUEADA, 4=CANCELADA).';
COMMENT ON COLUMN cat_estatus_cuenta.clave       IS 'Clave que usa la API.';
COMMENT ON COLUMN cat_estatus_cuenta.descripcion IS 'Texto para mostrar.';

-- Clientes
COMMENT ON TABLE  clientes IS 'Datos personales, de contacto y laborales. Baja lógica con activo/fecha_baja.';
COMMENT ON COLUMN clientes.id                  IS 'Llave primaria generada (IDENTITY).';
COMMENT ON COLUMN clientes.version             IS 'Control de concurrencia optimista (JPA @Version).';
COMMENT ON COLUMN clientes.fecha_registro      IS 'Instante del alta (UTC).';
COMMENT ON COLUMN clientes.fecha_actualizacion IS 'Último cambio; la actualiza un trigger.';
COMMENT ON COLUMN clientes.fecha_baja          IS 'Se llena automáticamente al pasar activo a FALSE.';
COMMENT ON COLUMN clientes.fecha_nacimiento    IS 'Debe coincidir con la fecha de la CURP y del RFC; mayor de 18 años.';
COMMENT ON COLUMN clientes.sexo_id             IS '1=H 2=M 3=X (cat_sexo).';
COMMENT ON COLUMN clientes.estado_civil_id     IS 'Ver cat_estado_civil.';
COMMENT ON COLUMN clientes.activo              IS 'FALSE = dado de baja (baja lógica); nunca se borra el registro.';
COMMENT ON COLUMN clientes.nombre              IS 'Primer nombre: solo letras y espacios, 2 a 50 caracteres.';
COMMENT ON COLUMN clientes.segundo_nombre      IS 'Segundo nombre (opcional), mismas reglas que nombre.';
COMMENT ON COLUMN clientes.apellido_paterno    IS 'Solo letras y espacios, 2 a 50 caracteres.';
COMMENT ON COLUMN clientes.apellido_materno    IS 'Solo letras y espacios, 2 a 50 caracteres.';
COMMENT ON COLUMN clientes.curp                IS 'CURP de 18 caracteres, única e inmutable.';
COMMENT ON COLUMN clientes.rfc                 IS 'RFC de 12 o 13 caracteres, único e inmutable.';
COMMENT ON COLUMN clientes.nacionalidad        IS 'Código ISO 3166-1 alfa-2 (cat_pais).';
COMMENT ON COLUMN clientes.correo              IS 'Correo en minúsculas, único; también es el nombre de usuario.';
COMMENT ON COLUMN clientes.telefono_movil      IS 'Exactamente 10 dígitos.';
COMMENT ON COLUMN clientes.telefono_alterno    IS 'Opcional; exactamente 10 dígitos.';
COMMENT ON COLUMN clientes.ocupacion           IS 'Información laboral: ocupación.';
COMMENT ON COLUMN clientes.empresa             IS 'Información laboral: empresa o negocio.';
COMMENT ON COLUMN clientes.ingreso_mensual     IS 'Ingreso mensual en MXN; mayor a cero.';

-- Domicilios
COMMENT ON TABLE  domicilios IS 'Domicilio del cliente (1:1, llave primaria compartida con clientes).';
COMMENT ON COLUMN domicilios.fecha_actualizacion IS 'Último cambio; la actualiza un trigger.';
COMMENT ON COLUMN domicilios.cliente_id          IS 'Llave primaria y foránea a clientes (relación 1:1).';
COMMENT ON COLUMN domicilios.estado_id           IS 'Clave INEGI de la entidad federativa (cat_estado).';
COMMENT ON COLUMN domicilios.calle               IS 'Calle.';
COMMENT ON COLUMN domicilios.numero_exterior     IS 'Número exterior (texto: admite 12-B, S/N...).';
COMMENT ON COLUMN domicilios.numero_interior     IS 'Número interior (opcional).';
COMMENT ON COLUMN domicilios.colonia             IS 'Colonia.';
COMMENT ON COLUMN domicilios.municipio           IS 'Municipio o alcaldía.';
COMMENT ON COLUMN domicilios.codigo_postal       IS 'Código postal de 5 dígitos (texto: puede iniciar en 0).';
COMMENT ON COLUMN domicilios.pais                IS 'Por ahora solo MX.';

-- Cuentas
COMMENT ON TABLE  cuentas IS 'Cuentas bancarias del cliente (1:N).';
COMMENT ON COLUMN cuentas.id                  IS 'Llave primaria generada (IDENTITY).';
COMMENT ON COLUMN cuentas.cliente_id          IS 'Cliente dueño de la cuenta; no se puede reasignar.';
COMMENT ON COLUMN cuentas.fecha_apertura      IS 'Instante de apertura (UTC).';
COMMENT ON COLUMN cuentas.fecha_actualizacion IS 'Último cambio; la actualiza un trigger.';
COMMENT ON COLUMN cuentas.version             IS 'Control de concurrencia optimista (JPA @Version).';
COMMENT ON COLUMN cuentas.estatus_id          IS '1=ACTIVA 2=INACTIVA 3=BLOQUEADA 4=CANCELADA (cat_estatus_cuenta).';
COMMENT ON COLUMN cuentas.numero_cuenta       IS '10 dígitos: 9 de seq_numero_cuenta + dígito verificador Luhn. Único e inmutable.';
COMMENT ON COLUMN cuentas.saldo               IS 'Saldo exacto; nunca negativo. El inicial lo define el sistema.';
COMMENT ON COLUMN cuentas.moneda              IS 'Código ISO 4217 (MXN por defecto).';

-- Usuarios
COMMENT ON TABLE  usuarios IS 'Credenciales de acceso (1:1 con clientes). El nombre de usuario es el correo.';
COMMENT ON COLUMN usuarios.id                   IS 'Llave primaria generada (IDENTITY).';
COMMENT ON COLUMN usuarios.cliente_id           IS 'Cliente dueño del usuario (único: un usuario por cliente).';
COMMENT ON COLUMN usuarios.fecha_creacion       IS 'Instante de creación (UTC).';
COMMENT ON COLUMN usuarios.fecha_actualizacion  IS 'Último cambio; la actualiza un trigger.';
COMMENT ON COLUMN usuarios.version_credenciales IS 'Aumenta al cambiar la contraseña; los JWT emitidos con una versión anterior se rechazan.';
COMMENT ON COLUMN usuarios.activo               IS 'FALSE cuando el cliente se da de baja: ya no puede iniciar sesión.';
COMMENT ON COLUMN usuarios.correo               IS 'Nombre de usuario: el mismo correo del cliente.';
COMMENT ON COLUMN usuarios.password             IS 'Hash BCrypt. Nunca texto plano.';

-- Secuencia y funciones
COMMENT ON SEQUENCE seq_numero_cuenta        IS 'Parte consecutiva (9 dígitos) del número de cuenta.';
COMMENT ON FUNCTION fn_generar_numero_cuenta() IS 'Siguiente número de cuenta: secuencia + dígito Luhn.';
COMMENT ON FUNCTION fn_numero_cuenta_valido(text) IS 'TRUE si son 10 dígitos con dígito verificador Luhn correcto.';
COMMENT ON FUNCTION fn_nombre_valido(text)   IS 'TRUE si solo tiene letras y espacios simples (2 a 50 caracteres).';
COMMENT ON FUNCTION fn_digito_luhn(text)     IS 'Dígito verificador Luhn (módulo 10) de una cadena de dígitos.';
COMMENT ON FUNCTION fn_fecha_aammdd(date)    IS 'Fecha en formato AAMMDD, como aparece dentro de la CURP y del RFC.';
