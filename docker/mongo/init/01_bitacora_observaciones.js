db = db.getSiblingDB("sira");

db.createCollection("bitacora_observaciones", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: [
        "ejecucionId",
        "participanteId",
        "rutinaId",
        "autor",
        "fecha",
        "tipo",
        "nota",
        "etiquetas",
        "creadoEn"
      ],
      properties: {
        ejecucionId: {
          bsonType: ["int", "long"],
          description: "Referencia a ejecucion.id en PostgreSQL"
        },
        participanteId: {
          bsonType: ["int", "long"],
          description: "Referencia a participante.id en PostgreSQL"
        },
        rutinaId: {
          bsonType: ["int", "long"],
          description: "Referencia a rutina.id en PostgreSQL"
        },
        autor: {
          bsonType: "object",
          required: ["usuarioId", "nombre", "rol"],
          properties: {
            usuarioId: { bsonType: ["int", "long"] },
            nombre: { bsonType: "string" },
            rol: { enum: ["PROFESIONAL", "ENCARGADO"] }
          }
        },
        fecha: { bsonType: "date" },
        tipo: { enum: ["OBSERVACION_CASA", "OBSERVACION_PROFESIONAL", "AJUSTE_RUTINA"] },
        nota: { bsonType: "string" },
        etiquetas: {
          bsonType: "array",
          items: { bsonType: "string" }
        },
        contexto: { bsonType: "object" },
        creadoEn: { bsonType: "date" }
      }
    }
  }
});

db.bitacora_observaciones.createIndex({ ejecucionId: 1 });
db.bitacora_observaciones.createIndex({ participanteId: 1, fecha: -1 });
db.bitacora_observaciones.createIndex({ etiquetas: 1 });
db.bitacora_observaciones.createIndex({ "autor.usuarioId": 1, fecha: -1 });

db.bitacora_observaciones.insertMany([
  {
    ejecucionId: NumberLong(1),
    participanteId: NumberLong(1),
    rutinaId: NumberLong(1),
    autor: {
      usuarioId: NumberLong(3),
      nombre: "Laura Mendez Rojas",
      rol: "ENCARGADO"
    },
    fecha: ISODate("2026-08-24T12:00:00-06:00"),
    tipo: "OBSERVACION_CASA",
    nota: "Mateo inicio tranquilo. El temporizador visual ayudo especialmente en el cepillado.",
    etiquetas: ["temporizador", "higiene", "avance"],
    contexto: {
      ambiente: "casa",
      apoyoUsado: "recordatorio verbal",
      nivelAnsiedadPercibido: "bajo"
    },
    creadoEn: ISODate("2026-08-24T06:56:00-06:00")
  },
  {
    ejecucionId: NumberLong(2),
    participanteId: NumberLong(1),
    rutinaId: NumberLong(1),
    autor: {
      usuarioId: NumberLong(3),
      nombre: "Laura Mendez Rojas",
      rol: "ENCARGADO"
    },
    fecha: ISODate("2026-08-25T12:00:00-06:00"),
    tipo: "OBSERVACION_CASA",
    nota: "Hubo resistencia al final de la rutina. Conviene probar una tarjeta de cierre mas clara.",
    etiquetas: ["resistencia", "cierre", "higiene"],
    contexto: {
      ambiente: "casa",
      cambioDetectado: "durmio menos de lo habitual",
      pasoCritico: "guardar cepillo y secarse"
    },
    creadoEn: ISODate("2026-08-25T06:54:00-06:00")
  },
  {
    ejecucionId: NumberLong(3),
    participanteId: NumberLong(1),
    rutinaId: NumberLong(2),
    autor: {
      usuarioId: NumberLong(1),
      nombre: "Mariana Vargas Soto",
      rol: "PROFESIONAL"
    },
    fecha: ISODate("2026-08-24T12:00:00-06:00"),
    tipo: "AJUSTE_RUTINA",
    nota: "Se recomienda mantener la lista visual y revisar si la merienda queda lista antes de iniciar.",
    etiquetas: ["mochila", "recomendacion", "lista-visual"],
    contexto: {
      origen: "revision semanal",
      sugerencia: {
        mantenerPictograma: true,
        revisarOrdenPasos: false
      }
    },
    creadoEn: ISODate("2026-08-24T20:10:00-06:00")
  }
]);
