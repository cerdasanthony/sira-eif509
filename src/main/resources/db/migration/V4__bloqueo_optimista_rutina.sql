-- La rutina es la fila que dos sesiones del profesional pueden guardar a la vez:
-- el formulario de edicion y la publicacion trabajan sobre el mismo borrador.
-- La columna version permite que Hibernate detecte la actualizacion perdida.
ALTER TABLE rutina ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
