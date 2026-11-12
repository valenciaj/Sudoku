# Publicar en Google Play

## 1. Crear la clave de firma (una sola vez)

```bash
keytool -genkey -v -keystore sudoku-release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias sudoku
```

Guarda el `.jks` y las contraseñas en un lugar seguro. **Si los pierdes no podrás actualizar la app** (salvo que uses Play App Signing, recomendado).

## 2. Firmar

**En local:** crea `keystore.properties` en la raíz (está en `.gitignore`):

```
storeFile=sudoku-release.jks
storePassword=...
keyAlias=sudoku
keyPassword=...
```

y ejecuta `./gradlew :app:bundleRelease`.

**En GitHub Actions:** añade estos secretos en Settings → Secrets and variables → Actions:

| Secreto | Valor |
|---|---|
| `SUDOKU_KEYSTORE_BASE64` | `base64 -w0 sudoku-release.jks` |
| `SUDOKU_KEYSTORE_PASSWORD` | contraseña del keystore |
| `SUDOKU_KEY_ALIAS` | `sudoku` |
| `SUDOKU_KEY_PASSWORD` | contraseña de la clave |

Cada build deja el `.aab` firmado en el artefacto `sudoku-release-aab`.

## 3. Play Console

1. Cuenta de desarrollador (pago único de 25 $).
2. Crear app → nombre "Sudoku", idioma español, gratuita.
3. Activar **Play App Signing** y subir el `.aab` a "Prueba interna".
4. Rellenar la ficha (textos abajo). Gráficos listos en `play/`: icono 512x512 y gráfico destacado 1024x500 (se regeneran con `python3 tools/make_icons.py`). Capturas: artefacto `sudoku-screenshots` de GitHub Actions (o `./gradlew :app:recordPaparazziDebug`).
5. Cuestionarios: clasificación de contenido (apta para todos), "Seguridad de los datos" (la app no recoge datos; la copia opcional va a la cuenta de GitHub del usuario), público objetivo.
6. Política de privacidad: publicar el texto de abajo (por ejemplo con GitHub Pages) y poner la URL.
7. Antes de cada versión nueva, subir `versionCode` en `app/build.gradle.kts`.

## Textos de la ficha

**Descripción breve (máx. 80):**
Sudoku clásico con 4 niveles, de Fácil a Demencial. Sin anuncios.

**Descripción completa:**
Sudokus generados al momento, siempre con solución única, en cuatro niveles:

• Fácil: para relajarse.
• Intermedio: requiere algo de lógica.
• Difícil: técnicas como X-Wing.
• Demencial: solo para valientes.

Modos Clásico, Contrarreloj y Killer. Incluye sudoku diario con rachas, pistas que explican el siguiente paso, logros, notas a lápiz, deshacer, cronómetro, guardado automático, estadísticas y tema claro u oscuro con varios colores.
Sin anuncios y sin recoger datos. Copia en la nube opcional en tu propia cuenta de GitHub.

## Política de privacidad

> Esta app no recoge ni comparte datos personales con el desarrollador ni con terceros. La partida y las estadísticas se guardan en tu dispositivo. Si activas la copia en la nube (opcional), se guardan también en un Gist privado de tu propia cuenta de GitHub, al que solo tú tienes acceso; puedes borrarlo cuando quieras desde GitHub.
