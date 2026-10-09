-- =============================================================================
-- V3 · Seguridad: mínimo privilegio para la aplicación
-- =============================================================================
-- La aplicación debe conectarse con un usuario que pertenezca a rol_app_onboarding:
-- puede consultar, insertar y actualizar, pero NO borrar ni truncar (la baja es lógica).
-- Ejemplo (como administrador):
--   CREATE ROLE app_banco LOGIN PASSWORD '<secreto>' IN ROLE rol_app_onboarding;
-- Si quien ejecuta este script no tiene permiso para crear roles, el bloque solo
-- avisa y el rol se puede crear después a mano.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_app_onboarding') THEN
        CREATE ROLE rol_app_onboarding NOLOGIN;
    END IF;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE NOTICE 'Sin permiso para crear roles: crea rol_app_onboarding manualmente y vuelve a otorgar los permisos.';
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'rol_app_onboarding') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA onboarding TO rol_app_onboarding';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON onboarding.clientes, onboarding.domicilios, '
             || 'onboarding.cuentas, onboarding.usuarios TO rol_app_onboarding';
        EXECUTE 'GRANT SELECT ON onboarding.cat_pais, onboarding.cat_estado, onboarding.cat_sexo, '
             || 'onboarding.cat_estado_civil, onboarding.cat_estatus_cuenta TO rol_app_onboarding';
        EXECUTE 'GRANT USAGE ON SEQUENCE onboarding.seq_numero_cuenta TO rol_app_onboarding';
    END IF;
END $$;

REVOKE ALL ON SCHEMA onboarding FROM PUBLIC;
