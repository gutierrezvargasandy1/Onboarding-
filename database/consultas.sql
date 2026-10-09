-- =============================================================================
--  BANCO · ONBOARDING  |  Consultas solicitadas en la tarea (SQL equivalente)
-- =============================================================================
--  La API las ejecuta con Spring Data JPA (ClienteRepository / CuentaRepository).
--  Aquí están en SQL para revisarlas o probarlas directo en psql o pgAdmin.
--  Los valores de ejemplo corresponden a database/datos-prueba.sql.
--  (en una base recién creada: si antes se ejecutó pruebas_reglas_bd.sql, los id y los
--  números de cuenta son otros, porque las secuencias no se revierten con ROLLBACK).
-- =============================================================================

SET search_path = onboarding;

-- 1. Buscar cliente por CURP  ->  índice único uq_clientes_curp
SELECT c.id, c.nombre, c.apellido_paterno, c.apellido_materno, c.curp, c.rfc, c.correo, c.activo,
       d.calle, d.numero_exterior, d.colonia, d.municipio, e.nombre AS estado, d.codigo_postal
  FROM clientes c
  JOIN domicilios d ON d.cliente_id = c.id
  JOIN cat_estado e ON e.id = d.estado_id
 WHERE c.curp = 'LOHF950821MGTPRR08';

-- 2. Buscar cliente por RFC  ->  índice único uq_clientes_rfc
SELECT c.id, c.nombre, c.apellido_paterno, c.rfc, c.correo
  FROM clientes c
 WHERE c.rfc = 'LOHF950821QK5';

-- 3. Buscar cliente por correo  ->  índice único uq_clientes_correo
SELECT c.id, c.nombre, c.apellido_paterno, c.correo, c.telefono_movil
  FROM clientes c
 WHERE c.correo = lower('Maria.Lopez@Ejemplo.com');

-- 4. Consultar cuentas activas  ->  índice parcial idx_cuentas_activas
SELECT cu.numero_cuenta, cu.saldo, cu.moneda, cu.fecha_apertura, c.id AS cliente_id,
       c.nombre || ' ' || c.apellido_paterno AS cliente
  FROM cuentas cu
  JOIN clientes c ON c.id = cu.cliente_id
 WHERE cu.estatus_id = 1                       -- 1 = ACTIVA
 ORDER BY cu.id
 LIMIT 20;

-- 5. Consultar clientes activos  ->  índice parcial idx_clientes_activos
SELECT c.id, c.nombre, c.apellido_paterno, c.curp, c.fecha_registro
  FROM clientes c
 WHERE c.activo
 ORDER BY c.id
 LIMIT 20;

-- 6. Consultar el saldo de una cuenta  ->  índice único uq_cuentas_numero
SELECT cu.numero_cuenta, cu.saldo, cu.moneda, ec.clave AS estatus
  FROM cuentas cu
  JOIN cat_estatus_cuenta ec ON ec.id = cu.estatus_id
 WHERE cu.numero_cuenta = '1000000016';

-- 7. Clientes registrados en un rango de fechas (días completos en hora de la Ciudad de México)
--    ->  índice idx_clientes_fecha_registro
SELECT c.id, c.nombre, c.apellido_paterno, c.fecha_registro AT TIME ZONE 'America/Mexico_City' AS registrado
  FROM clientes c
 WHERE c.fecha_registro >= (DATE '2026-01-01') AT TIME ZONE 'America/Mexico_City'
   AND c.fecha_registro <  (DATE '2026-12-31' + 1) AT TIME ZONE 'America/Mexico_City'
 ORDER BY c.fecha_registro;

-- 8. Consultar cliente por número de cuenta  ->  uq_cuentas_numero + llave primaria de clientes
SELECT c.id, c.nombre, c.apellido_paterno, c.curp, cu.numero_cuenta, cu.saldo
  FROM cuentas cu
  JOIN clientes c ON c.id = cu.cliente_id
 WHERE cu.numero_cuenta = '1000000016';

-- 9. Todos los clientes, paginados (página 1 de 20 registros)  ->  llave primaria
SELECT c.id, c.nombre, c.apellido_paterno, c.activo
  FROM clientes c
 ORDER BY c.id
 LIMIT 20 OFFSET 0;
