-- =============================================================================
-- V2 · Datos de catálogos
-- =============================================================================
-- Los ids de sexo, estado civil y estatus de cuenta son fijos: Java los usa como
-- enums (AttributeConverter) y una prueba verifica que coincidan con estas filas.

INSERT INTO cat_sexo (id, clave, descripcion) VALUES
    (1, 'H', 'Hombre'),
    (2, 'M', 'Mujer'),
    (3, 'X', 'No binario');

INSERT INTO cat_estado_civil (id, clave, descripcion) VALUES
    (1, 'SOLTERO', 'Soltero(a)'),
    (2, 'CASADO', 'Casado(a)'),
    (3, 'UNION_LIBRE', 'Unión libre'),
    (4, 'DIVORCIADO', 'Divorciado(a)'),
    (5, 'VIUDO', 'Viudo(a)'),
    (6, 'SEPARADO', 'Separado(a)');

INSERT INTO cat_estatus_cuenta (id, clave, descripcion) VALUES
    (1, 'ACTIVA', 'Activa'),
    (2, 'INACTIVA', 'Inactiva'),
    (3, 'BLOQUEADA', 'Bloqueada'),
    (4, 'CANCELADA', 'Cancelada');

-- Entidades federativas con su clave INEGI
INSERT INTO cat_estado (id, nombre) VALUES
    (1, 'Aguascalientes'), (2, 'Baja California'), (3, 'Baja California Sur'), (4, 'Campeche'),
    (5, 'Coahuila de Zaragoza'), (6, 'Colima'), (7, 'Chiapas'), (8, 'Chihuahua'),
    (9, 'Ciudad de México'), (10, 'Durango'), (11, 'Guanajuato'), (12, 'Guerrero'),
    (13, 'Hidalgo'), (14, 'Jalisco'), (15, 'México'), (16, 'Michoacán de Ocampo'),
    (17, 'Morelos'), (18, 'Nayarit'), (19, 'Nuevo León'), (20, 'Oaxaca'),
    (21, 'Puebla'), (22, 'Querétaro'), (23, 'Quintana Roo'), (24, 'San Luis Potosí'),
    (25, 'Sinaloa'), (26, 'Sonora'), (27, 'Tabasco'), (28, 'Tamaulipas'),
    (29, 'Tlaxcala'), (30, 'Veracruz de Ignacio de la Llave'), (31, 'Yucatán'), (32, 'Zacatecas');

-- Países: los 249 códigos ISO 3166-1 alfa-2 con su nombre en español
INSERT INTO cat_pais (codigo_iso2, nombre) VALUES
    ('AD', 'Andorra'), ('AE', 'Emiratos Árabes Unidos'), ('AF', 'Afganistán'), ('AG', 'Antigua y Barbuda'),
    ('AI', 'Anguila'), ('AL', 'Albania'), ('AM', 'Armenia'), ('AO', 'Angola'),
    ('AQ', 'Antártida'), ('AR', 'Argentina'), ('AS', 'Samoa Americana'), ('AT', 'Austria'),
    ('AU', 'Australia'), ('AW', 'Aruba'), ('AX', 'Islas Åland'), ('AZ', 'Azerbaiyán'),
    ('BA', 'Bosnia y Herzegovina'), ('BB', 'Barbados'), ('BD', 'Bangladés'), ('BE', 'Bélgica'),
    ('BF', 'Burkina Faso'), ('BG', 'Bulgaria'), ('BH', 'Baréin'), ('BI', 'Burundi'),
    ('BJ', 'Benín'), ('BL', 'San Bartolomé'), ('BM', 'Bermudas'), ('BN', 'Brunéi'),
    ('BO', 'Bolivia'), ('BQ', 'Caribe neerlandés'), ('BR', 'Brasil'), ('BS', 'Bahamas'),
    ('BT', 'Bután'), ('BV', 'Isla Bouvet'), ('BW', 'Botsuana'), ('BY', 'Bielorrusia'),
    ('BZ', 'Belice'), ('CA', 'Canadá'), ('CC', 'Islas Cocos'), ('CD', 'República Democrática del Congo'),
    ('CF', 'República Centroafricana'), ('CG', 'República del Congo'), ('CH', 'Suiza'), ('CI', 'Costa de Marfil'),
    ('CK', 'Islas Cook'), ('CL', 'Chile'), ('CM', 'Camerún'), ('CN', 'China'),
    ('CO', 'Colombia'), ('CR', 'Costa Rica'), ('CU', 'Cuba'), ('CV', 'Cabo Verde'),
    ('CW', 'Curazao'), ('CX', 'Isla de Navidad'), ('CY', 'Chipre'), ('CZ', 'Chequia'),
    ('DE', 'Alemania'), ('DJ', 'Yibuti'), ('DK', 'Dinamarca'), ('DM', 'Dominica'),
    ('DO', 'República Dominicana'), ('DZ', 'Argelia'), ('EC', 'Ecuador'), ('EE', 'Estonia'),
    ('EG', 'Egipto'), ('EH', 'Sáhara Occidental'), ('ER', 'Eritrea'), ('ES', 'España'),
    ('ET', 'Etiopía'), ('FI', 'Finlandia'), ('FJ', 'Fiyi'), ('FK', 'Islas Malvinas'),
    ('FM', 'Micronesia'), ('FO', 'Islas Feroe'), ('FR', 'Francia'), ('GA', 'Gabón'),
    ('GB', 'Reino Unido'), ('GD', 'Granada'), ('GE', 'Georgia'), ('GF', 'Guayana Francesa'),
    ('GG', 'Guernsey'), ('GH', 'Ghana'), ('GI', 'Gibraltar'), ('GL', 'Groenlandia'),
    ('GM', 'Gambia'), ('GN', 'Guinea'), ('GP', 'Guadalupe'), ('GQ', 'Guinea Ecuatorial'),
    ('GR', 'Grecia'), ('GS', 'Islas Georgia del Sur y Sándwich del Sur'), ('GT', 'Guatemala'), ('GU', 'Guam'),
    ('GW', 'Guinea-Bisáu'), ('GY', 'Guyana'), ('HK', 'Hong Kong'), ('HM', 'Islas Heard y McDonald'),
    ('HN', 'Honduras'), ('HR', 'Croacia'), ('HT', 'Haití'), ('HU', 'Hungría'),
    ('ID', 'Indonesia'), ('IE', 'Irlanda'), ('IL', 'Israel'), ('IM', 'Isla de Man'),
    ('IN', 'India'), ('IO', 'Territorio Británico del Océano Índico'), ('IQ', 'Irak'), ('IR', 'Irán'),
    ('IS', 'Islandia'), ('IT', 'Italia'), ('JE', 'Jersey'), ('JM', 'Jamaica'),
    ('JO', 'Jordania'), ('JP', 'Japón'), ('KE', 'Kenia'), ('KG', 'Kirguistán'),
    ('KH', 'Camboya'), ('KI', 'Kiribati'), ('KM', 'Comoras'), ('KN', 'San Cristóbal y Nieves'),
    ('KP', 'Corea del Norte'), ('KR', 'Corea del Sur'), ('KW', 'Kuwait'), ('KY', 'Islas Caimán'),
    ('KZ', 'Kazajistán'), ('LA', 'Laos'), ('LB', 'Líbano'), ('LC', 'Santa Lucía'),
    ('LI', 'Liechtenstein'), ('LK', 'Sri Lanka'), ('LR', 'Liberia'), ('LS', 'Lesoto'),
    ('LT', 'Lituania'), ('LU', 'Luxemburgo'), ('LV', 'Letonia'), ('LY', 'Libia'),
    ('MA', 'Marruecos'), ('MC', 'Mónaco'), ('MD', 'Moldavia'), ('ME', 'Montenegro'),
    ('MF', 'San Martín'), ('MG', 'Madagascar'), ('MH', 'Islas Marshall'), ('MK', 'Macedonia del Norte'),
    ('ML', 'Mali'), ('MM', 'Myanmar'), ('MN', 'Mongolia'), ('MO', 'Macao'),
    ('MP', 'Islas Marianas del Norte'), ('MQ', 'Martinica'), ('MR', 'Mauritania'), ('MS', 'Montserrat'),
    ('MT', 'Malta'), ('MU', 'Mauricio'), ('MV', 'Maldivas'), ('MW', 'Malaui'),
    ('MX', 'México'), ('MY', 'Malasia'), ('MZ', 'Mozambique'), ('NA', 'Namibia'),
    ('NC', 'Nueva Caledonia'), ('NE', 'Níger'), ('NF', 'Isla Norfolk'), ('NG', 'Nigeria'),
    ('NI', 'Nicaragua'), ('NL', 'Países Bajos'), ('NO', 'Noruega'), ('NP', 'Nepal'),
    ('NR', 'Nauru'), ('NU', 'Niue'), ('NZ', 'Nueva Zelanda'), ('OM', 'Omán'),
    ('PA', 'Panamá'), ('PE', 'Perú'), ('PF', 'Polinesia Francesa'), ('PG', 'Papúa Nueva Guinea'),
    ('PH', 'Filipinas'), ('PK', 'Pakistán'), ('PL', 'Polonia'), ('PM', 'San Pedro y Miquelón'),
    ('PN', 'Islas Pitcairn'), ('PR', 'Puerto Rico'), ('PS', 'Palestina'), ('PT', 'Portugal'),
    ('PW', 'Palaos'), ('PY', 'Paraguay'), ('QA', 'Catar'), ('RE', 'Reunión'),
    ('RO', 'Rumania'), ('RS', 'Serbia'), ('RU', 'Rusia'), ('RW', 'Ruanda'),
    ('SA', 'Arabia Saudita'), ('SB', 'Islas Salomón'), ('SC', 'Seychelles'), ('SD', 'Sudán'),
    ('SE', 'Suecia'), ('SG', 'Singapur'), ('SH', 'Santa Elena'), ('SI', 'Eslovenia'),
    ('SJ', 'Svalbard y Jan Mayen'), ('SK', 'Eslovaquia'), ('SL', 'Sierra Leona'), ('SM', 'San Marino'),
    ('SN', 'Senegal'), ('SO', 'Somalia'), ('SR', 'Surinam'), ('SS', 'Sudán del Sur'),
    ('ST', 'Santo Tomé y Príncipe'), ('SV', 'El Salvador'), ('SX', 'Sint Maarten'), ('SY', 'Siria'),
    ('SZ', 'Eswatini'), ('TC', 'Islas Turcas y Caicos'), ('TD', 'Chad'), ('TF', 'Territorios Australes Franceses'),
    ('TG', 'Togo'), ('TH', 'Tailandia'), ('TJ', 'Tayikistán'), ('TK', 'Tokelau'),
    ('TL', 'Timor Oriental'), ('TM', 'Turkmenistán'), ('TN', 'Túnez'), ('TO', 'Tonga'),
    ('TR', 'Turquía'), ('TT', 'Trinidad y Tobago'), ('TV', 'Tuvalu'), ('TW', 'Taiwán'),
    ('TZ', 'Tanzania'), ('UA', 'Ucrania'), ('UG', 'Uganda'), ('UM', 'Islas menores alejadas de EE. UU.'),
    ('US', 'Estados Unidos'), ('UY', 'Uruguay'), ('UZ', 'Uzbekistán'), ('VA', 'Ciudad del Vaticano'),
    ('VC', 'San Vicente y las Granadinas'), ('VE', 'Venezuela'), ('VG', 'Islas Vírgenes Británicas'), ('VI', 'Islas Vírgenes de EE. UU.'),
    ('VN', 'Vietnam'), ('VU', 'Vanuatu'), ('WF', 'Wallis y Futuna'), ('WS', 'Samoa'),
    ('YE', 'Yemen'), ('YT', 'Mayotte'), ('ZA', 'Sudáfrica'), ('ZM', 'Zambia'),
    ('ZW', 'Zimbabue');
