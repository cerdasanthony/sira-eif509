-- Los usuarios existentes quedan sin acceso hasta configurar credenciales.
ALTER TABLE usuario ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE usuario ADD COLUMN creado_por_id BIGINT REFERENCES usuario(id);
CREATE UNIQUE INDEX uk_usuario_correo_minusculas ON usuario (lower(correo));
