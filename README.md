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
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # verifica que los tableros generados sean resolubles
```
