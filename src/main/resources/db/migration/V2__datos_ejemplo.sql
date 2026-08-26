INSERT INTO usuario (id, nombre, correo, rol, activo) VALUES
    (1, 'Mariana Vargas Soto', 'mariana.vargas@sira.local', 'PROFESIONAL', TRUE),
    (2, 'Carlos Brenes Mora', 'carlos.brenes@sira.local', 'PROFESIONAL', TRUE),
    (3, 'Laura Mendez Rojas', 'laura.mendez@sira.local', 'ENCARGADO', TRUE),
    (4, 'Andres Solano Vega', 'andres.solano@sira.local', 'ENCARGADO', TRUE);

INSERT INTO participante (id, profesional_id, encargado_id, nombre, fecha_nacimiento, activo) VALUES
    (1, 1, 3, 'Mateo Mendez', DATE '2016-04-18', TRUE),
    (2, 2, 4, 'Sofia Solano', DATE '2014-11-03', TRUE);

INSERT INTO rutina (id, participante_id, nombre, hora_inicio, vigencia_desde, vigencia_hasta, estado, publicado_en) VALUES
    (1, 1, 'Rutina de higiene de la manana', TIME '06:30', DATE '2026-08-01', NULL, 'PUBLICADA', TIMESTAMPTZ '2026-08-01 08:00:00-06'),
    (2, 1, 'Preparar mochila escolar', TIME '19:00', DATE '2026-08-05', NULL, 'PUBLICADA', TIMESTAMPTZ '2026-08-05 15:30:00-06'),
    (3, 2, 'Rutina de llegada a casa', TIME '15:30', DATE '2026-08-10', NULL, 'PUBLICADA', TIMESTAMPTZ '2026-08-10 09:15:00-06');

INSERT INTO rutina_dia_semana (rutina_id, dia_semana) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
    (2, 1), (2, 2), (2, 3), (2, 4), (2, 7),
    (3, 1), (3, 2), (3, 3), (3, 4), (3, 5);

INSERT INTO paso_rutina (id, rutina_id, orden, descripcion, duracion_estimada_min, pictograma) VALUES
    (1, 1, 1, 'Entrar al bano y abrir el grifo', 2, 'grifo'),
    (2, 1, 2, 'Lavarse la cara', 4, 'lavar-cara'),
    (3, 1, 3, 'Cepillarse los dientes', 5, 'cepillo'),
    (4, 1, 4, 'Guardar cepillo y secarse', 3, 'toalla'),
    (5, 2, 1, 'Revisar horario visual del dia siguiente', 3, 'calendario'),
    (6, 2, 2, 'Guardar cuadernos necesarios', 5, 'cuadernos'),
    (7, 2, 3, 'Colocar merienda en la mochila', 4, 'merienda'),
    (8, 3, 1, 'Colgar el salveque en su lugar', 2, 'perchero'),
    (9, 3, 2, 'Lavarse las manos', 4, 'lavar-manos'),
    (10, 3, 3, 'Elegir actividad de descanso', 5, 'descanso');

INSERT INTO ejecucion (id, rutina_id, fecha, hora_inicio, hora_fin, adherencia, estado, registrado_por_id, cerrado_en) VALUES
    (1, 1, DATE '2026-08-24', TIME '06:35', TIME '06:52', 87.50, 'CERRADA', 3, TIMESTAMPTZ '2026-08-24 06:55:00-06'),
    (2, 1, DATE '2026-08-25', TIME '06:32', TIME '06:50', 62.50, 'CERRADA', 3, TIMESTAMPTZ '2026-08-25 06:53:00-06'),
    (3, 2, DATE '2026-08-24', TIME '19:05', TIME '19:18', 83.33, 'CERRADA', 3, TIMESTAMPTZ '2026-08-24 19:20:00-06'),
    (4, 3, DATE '2026-08-24', TIME '15:35', TIME '15:48', 100.00, 'CERRADA', 4, TIMESTAMPTZ '2026-08-24 15:50:00-06');

INSERT INTO registro_paso (ejecucion_id, paso_rutina_id, resultado, observacion_corta) VALUES
    (1, 1, 'LOGRADO', NULL),
    (1, 2, 'LOGRADO', NULL),
    (1, 3, 'CON_APOYO', 'Necesito recordatorio verbal.'),
    (1, 4, 'LOGRADO', NULL),
    (2, 1, 'LOGRADO', NULL),
    (2, 2, 'CON_APOYO', 'Le costo iniciar.'),
    (2, 3, 'CON_APOYO', 'Uso temporizador.'),
    (2, 4, 'NO_LOGRADO', 'Se retiro antes de guardar.'),
    (3, 5, 'LOGRADO', NULL),
    (3, 6, 'CON_APOYO', 'Se reviso lista juntos.'),
    (3, 7, 'LOGRADO', NULL),
    (4, 8, 'LOGRADO', NULL),
    (4, 9, 'LOGRADO', NULL),
    (4, 10, 'LOGRADO', 'Eligio rompecabezas.');

SELECT setval(pg_get_serial_sequence('usuario', 'id'), (SELECT max(id) FROM usuario));
SELECT setval(pg_get_serial_sequence('participante', 'id'), (SELECT max(id) FROM participante));
SELECT setval(pg_get_serial_sequence('rutina', 'id'), (SELECT max(id) FROM rutina));
SELECT setval(pg_get_serial_sequence('paso_rutina', 'id'), (SELECT max(id) FROM paso_rutina));
SELECT setval(pg_get_serial_sequence('ejecucion', 'id'), (SELECT max(id) FROM ejecucion));
SELECT setval(pg_get_serial_sequence('registro_paso', 'id'), (SELECT max(id) FROM registro_paso));
