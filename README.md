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
