# appremove

App de escritorio para Windows, minimalista (frontend), que:
1. Remueve/reemplaza el fondo de imágenes usando un modelo de IA local (sin cuotas, sin internet).
2. Reduce el tamaño de imágenes (compresión/resize).

## Estado

🚧 Bloque 0 — esqueleto del proyecto (estructura Gradle multi-módulo, sin lógica de negocio todavía).

## Stack

- Kotlin + Compose Multiplatform (Desktop)
- ONNX Runtime (Java) + modelo IS-Net para remoción de fondo
- Gradle Wrapper (versión fijada en el repo, sin instalación global)
- GitHub Actions para CI/CD

## Módulos

- `app` — UI (Compose Desktop) + ViewModels (MVVM)
- `domain` — casos de uso y contratos (Strategy), Kotlin puro sin dependencias de framework
- `data` — repositorios (filesystem, carga de modelo)
- `core-image` — algoritmos de compresión/resize
- `core-ml` — inferencia ONNX (remoción de fondo)

## Licencia

MIT — ver [LICENSE](LICENSE).
