# Posta — notas del fork

Posta es un fork de [bitchat-android](https://github.com/permissionlesstech/bitchat-android) (GPL-3.0) centrado en emergencias, en español primero y compatible con Bitchat en Android e iPhone.

- Rama de trabajo: `posta`. Remoto `upstream` → Bitchat Android, para incorporar sus mejoras con `git fetch upstream && git merge upstream/main`.
- Base actual: Bitchat Android 2.2.0 (`2fc6f47`).
- `applicationId`: `org.posta.app` (puede convivir con Bitchat instalada).

## Qué añade Posta

| Archivo | Qué hace |
|---|---|
| `app/src/main/java/com/bitchat/android/posta/PostaStatus.kt` | Estados de emergencia: formato de cable bilingüe, estable y compatible (texto normal) |
| `app/src/main/java/com/bitchat/android/posta/PostaStatusBar.kt` | Botones de estado + diálogo de confirmación con destino real y nota opcional |
| `app/src/main/java/com/bitchat/android/posta/PostaIcon.kt` | Marca de Posta dentro de la app |
| `posta/PostaLocation.kt`, `posta/PostaLocationProvider.kt` | Coordenadas GPS opcionales en los estados (lectura única bajo demanda, funciona sin red) |
| `posta/SosPattern.kt`, `posta/SosBeacon.kt`, `posta/SosScreen.kt` | Señales SOS en Morse con linterna, pantalla y tono; botón al 112 |
| `util/ApkDownloadSource.kt`, `util/GitHubReleaseClient.kt`, `gradle.properties` | Actualizaciones desde las versiones de Posta, con el certificado de Posta fijado |
| `app/src/main/res/values*/strings_posta.xml` | Textos de Posta (EN, ES) separados para no chocar con upstream |
| `ui/ChatScreen.kt`, `ui/MeshPeerListSheet.kt`, `ui/ChatUIUtils.kt` | Integración mínima: barra de estados y resaltado |
| `res/values*/strings.xml`, iconos, `hotspot/ApkWebServer.kt` | Marca Posta en textos visibles (no en identificadores de protocolo) |

## Pendiente antes de publicar

1. Validación en teléfonos físicos (Mesh Lab: 2 y 3+ dispositivos, sin cobertura, segundo plano, batería) e interoperabilidad con Bitchat iOS.
2. Versiones reproducibles en CI (`docs/reproducible-builds.md`); hoy se firma en local con la clave de Posta.
3. Activar Wi‑Fi Aware por defecto cuando se haya probado en dispositivos (hoy es experimental, ajuste de depuración).
4. Puente con radios LoRa (Meshtastic) para alcance de kilómetros.
5. Traducciones de `strings_posta.xml` a más idiomas (por ahora caen al inglés).
