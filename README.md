# appremove

App de escritorio para Windows, minimalista (frontend), que:
1. Remueve/reemplaza el fondo de imágenes usando un modelo de IA local (sin cuotas, sin internet).
2. Reduce el tamaño de imágenes (compresión/resize).

## Estado

🚧 En desarrollo. Resize de imágenes con ancho/alto personalizado: listo. Remoción de fondo (ONNX + IS-Net): en progreso.

## Stack

- Kotlin + Compose Multiplatform (Desktop)
- ONNX Runtime (Java) + modelo IS-Net para remoción de fondo
- Gradle Wrapper (versión fijada en el repo, sin instalación global)
- GitHub Actions para CI/CD

## Modelo de IA (Git LFS)

El modelo de remoción de fondo (`core-ml/src/main/resources/models/isnet-general-use.onnx`, ~170MB) se versiona con **Git LFS**. Para clonar el repo con el modelo real:

```
git lfs install
git clone <repo>
```

Fuente del modelo: [rembg](https://github.com/danielgatis/rembg) (release `v0.0.0`), pesos bajo licencia OpenRAIL. El CI **no** descarga el contenido real de LFS (solo el puntero) para no consumir la cuota de ancho de banda del repo en cada corrida — no hace falta el modelo real para compilar, lintear ni correr los tests que no dependen de él.

## Módulos

- `app` — UI (Compose Desktop) + ViewModels (MVVM)
- `domain` — casos de uso y contratos (Strategy), Kotlin puro sin dependencias de framework
- `data` — repositorios (filesystem, carga de modelo)
- `core-image` — algoritmos de compresión/resize
- `core-ml` — inferencia ONNX (remoción de fondo)

## Licencia

MIT — ver [LICENSE](LICENSE).
