# Instrucciones para GitHub Copilot — Posta Android

Este repositorio es **Posta**, un fork GPL-3.0 de `permissionlesstech/bitchat-android` orientado a emergencias. Revisa en español y con criterio estricto. Lee también `AGENTS.md` (reglas de privacidad obligatorias) y `POSTA.md`.

## Alcance de la revisión

Revisa con especial atención el código propio de Posta (`app/src/main/java/com/bitchat/android/posta/`, `res/values*/strings_posta.xml`) y cualquier cambio sobre el código heredado. El código de upstream solo se toca para integrar Posta; señala cualquier modificación de upstream que no sea imprescindible.

## Invariantes (bloquea el PR si se rompen)

1. **Compatibilidad con Bitchat.** No se cambian formatos de paquete, tipos de mensaje, TTL, contextos criptográficos (`bitchat-verify-v1`…), esquemas de URI (`bitchat://`), nombres de preferencias ni de canales de notificación. Los estados de Posta viajan como texto normal.
2. **Formato de estado estable.** `PostaStatus.header` y `toWire()` no cambian sin versión nueva y tests: otros teléfonos dependen de reconocerlo. El parser solo acepta el prefijo exacto al inicio del mensaje.
3. **El usuario sabe a quién llega.** Antes de enviar un estado, el diálogo debe decir si es privado y cifrado, canal o público para todos los cercanos. Nunca enviar sin confirmación.
4. **Cifrado, transporte e identidad intactos.** Cualquier cambio en `noise/`, `crypto/`, `mesh/`, `protocol/`, `identity/`, `sync/` o almacenamiento exige justificación, tests y validación en Mesh Lab con teléfonos físicos.
5. **Nada de telemetría, analítica ni servidores nuevos.** Ninguna dependencia nueva que haga red.
6. **Textos honestos.** Nada de «garantizado», «100 % seguro», «anónimo» o «auditado» si no es verdad.

## Calidad

- Kotlin oficial, 4 espacios, Compose con estado elevado, sin bloquear el hilo principal.
- Cada regla nueva del modelo, con su test JUnit en `app/src/test/.../posta/`.
- Textos nuevos en `strings_posta.xml` (en inglés en `values/` y español en `values-es/`), nunca literales en el código.
- Accesibilidad: descripciones de contenido, objetivos táctiles ≥ 48 dp, contraste suficiente de los colores de estado en tema claro y oscuro.

## Cómo comentar

**[BLOQUEANTE]** compatibilidad, privacidad, seguridad, afirmaciones falsas · **[IMPORTANTE]** errores de lógica, accesibilidad, tests ausentes · **[SUGERENCIA]** estilo. Indica archivo, línea, escenario concreto y corrección.
