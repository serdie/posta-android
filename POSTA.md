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
| `app/src/main/res/values*/strings_posta.xml` | Textos de Posta (EN, ES) separados para no chocar con upstream |
| `ui/ChatScreen.kt`, `ui/MeshPeerListSheet.kt`, `ui/ChatUIUtils.kt` | Integración mínima: barra de estados y resaltado |
| `res/values*/strings.xml`, iconos, `hotspot/ApkWebServer.kt` | Marca Posta en textos visibles (no en identificadores de protocolo) |

## Pendiente antes de publicar

1. Validación en teléfonos físicos (Mesh Lab: 2 y 3+ dispositivos, sin cobertura, segundo plano, batería) e interoperabilidad con Bitchat iOS.
2. Decidir la fuente de actualizaciones/descarga de APK (`util/ApkDownloadSource.kt` apunta aún a las versiones de Bitchat).
3. Clave de firma propia y versiones reproducibles (`docs/reproducible-builds.md`).
4. Traducciones de `strings_posta.xml` a más idiomas (por ahora caen al inglés).
