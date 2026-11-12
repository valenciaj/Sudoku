# Sudoku (Android)

App de Sudoku en Kotlin + Jetpack Compose, sin internet ni anuncios.

## Niveles

La dificultad se mide por las técnicas que hacen falta para resolverlo:

| Nivel | Técnicas necesarias |
|---|---|
| Fácil | Solo singles (desnudos y ocultos) |
| Intermedio | + pares apuntadores, reducción caja-línea, pares desnudos/ocultos |
| Difícil | + tríos desnudos/ocultos, X-Wing |
| Demencial | Más allá de lo anterior |

Todos los sudokus tienen solución única.

## Estructura

- `core/`: motor en Kotlin puro (generador, resolvedor, clasificador de dificultad). Con tests.
- `app/`: la app Android (Compose).

## Compilar

- Tests del motor: `./gradlew :core:test`
- APK: `./gradlew :app:assembleDebug` (o abrir el proyecto en Android Studio).

Cada push compila el APK en GitHub Actions y lo deja como artefacto descargable.

## Plan

1. ✅ Base: tablero 9x9, teclado, menú de niveles.
2. ✅ Motor: generador con solución única y 4 niveles.
3. Juego: notas a lápiz, deshacer, cronómetro, guardar partida.
4. Extras: estadísticas, publicación en Google Play.
