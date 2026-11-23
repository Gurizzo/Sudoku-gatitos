# Sudoku Gatitos 🐱

Sudoku para Android **sin anuncios** donde, en vez de números, se juega con 9 gatitos distintos.

## Características
- 4 dificultades: Fácil, Medio, Difícil y Experto, con récord de tiempo por nivel.
- Cada partida es un tablero **aleatorio y siempre resoluble**:
  - todos los tableros tienen una única solución;
  - Fácil y Medio se pueden terminar solo con lógica, sin tener que adivinar.
- Si te equivocas, el teléfono vibra, la casilla se sacude y el gatito queda en gris. Tienes 3 vidas.
- Notas, deshacer, borrar y 3 pistas por partida.
- Opción para mostrar el número en cada gatito.

## Sonidos
Grabaciones reales con licencia **CC0 (dominio público)** de Freesound, recortadas y normalizadas a OGG
(`app/src/main/res/raw/`):

| Archivo | Cuándo suena | Fuente |
|---|---|---|
| `ronroneo.ogg` | al perder una vida | [Cat purring — cubilon](https://freesound.org/people/cubilon/sounds/130968/) (tramo 17.5–19.5 s) |
| `grunido.ogg` | al perder la partida | [angry cat.wav — patchytherat](https://freesound.org/people/patchytherat/sounds/532226/) (tramo 8.6–10.6 s) |
| `maullido.ogg` | al ganar | [Cat Meow — Mafon2](https://freesound.org/people/Mafon2/sounds/436541/) |

Se recortaron con:
```
ffmpeg -ss INICIO -t DURACION -i original.mp3 -af "aformat=channel_layouts=mono,loudnorm=I=-16:TP=-1.5:LRA=11,afade=t=in:d=0.08,afade=t=out:st=FIN-0.3:d=0.3" -c:a libvorbis -q:a 4 salida.ogg
```
Sonidos y vibración se pueden desactivar desde el menú.

## Compilar
Requiere el Android SDK (API 35) y JDK 17 o superior.

```
./gradlew assembleDebug        # app de pruebas "Gatitos DEBUG" (com.gatitos.sudoku.debug)
./gradlew assembleRelease      # APK firmado en app/build/outputs/apk/release/
```

### Firma del release
`assembleRelease` usa `keystore.properties` en la raíz (no se sube a git):

```
storeFile=C:/ruta/a/sudoku-gatitos-release.jks
storePassword=...
keyAlias=gatitos
keyPassword=...
```

Si no existe, el release se firma con la clave debug.

## Pruebas
```
./gradlew testDebugUnitTest                        # genera 160 tableros y comprueba que todos sean resolubles
./gradlew installDebug installDebugAndroidTest     # instala la app de pruebas en el celular
adb shell am instrument -w com.gatitos.sudoku.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Las pruebas en el celular recorren el menú, las 4 dificultades, colocar gatitos bien y mal
(vibración + gris), quedarse sin vidas, notas/deshacer/borrar/pista y ganar una partida.
Guardan capturas en `/sdcard/Android/data/com.gatitos.sudoku.debug/files/capturas/`.
