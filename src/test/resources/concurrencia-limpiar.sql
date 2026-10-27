-- La suite de concurrencia no corre dentro de una transaccion de prueba, asi que
-- cada metodo parte de una base vacia antes de volver a cargar el fixture comun.
DELETE FROM registro_paso;
DELETE FROM ejecucion;
DELETE FROM paso_rutina;
DELETE FROM rutina_dia_semana;
DELETE FROM rutina;
DELETE FROM participante;
DELETE FROM usuario;
