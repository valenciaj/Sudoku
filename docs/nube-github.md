# Copia en la nube con GitHub

La app guarda las estadísticas y la partida en curso en un **Gist privado de la cuenta de GitHub del usuario**. No hay servidores propios ni secretos dentro de la app.

## Cómo funciona

- **Inicio de sesión:** OAuth *device flow*. La app muestra un código; el usuario lo escribe en `github.com/login/device`. Solo se pide el permiso `gist`.
- **Gist:** se llama "Sudoku · copia en la nube (no editar)" y contiene:
  - `stats-<dispositivo>.txt`: las estadísticas de cada dispositivo. Cada móvil solo escribe el suyo y la app **suma** todos (como un contador CRDT), así una partida nunca se cuenta dos veces.
  - `partida.txt`: la partida en curso. Gana la más reciente.
- **Cuándo sincroniza:** al abrir la app, al terminar un sudoku y con el botón "Sincronizar ahora" (Ajustes).

## Activarla (una vez)

1. GitHub → Settings → Developer settings → **OAuth Apps** → *New OAuth App*.
   - Name: `Sudoku`
   - Homepage URL: `https://github.com/valenciaj/Sudoku`
   - Callback URL: `https://github.com/valenciaj/Sudoku` (no se usa, pero es obligatoria)
2. En la app creada, marca **Enable Device Flow** y guarda.
3. Copia el **Client ID** (es público; **no** generes ni uses el *client secret*).
4. Ponlo en `gradle.properties`: `githubClientId=Ov23li...` (o como variable `SUDOKU_GITHUB_CLIENT_ID` en CI).

Sin client ID, la sección aparece como "no disponible" y el resto de la app funciona igual.
