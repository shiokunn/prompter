# Gemma 4 E2B GPU backend (2026-10-30)

- Google documents the same `gemma-4-E2B-it.litertlm` model for Android CPU and GPU; a separate GPU model is not required. Its example measurements are on an S26 Ultra and do not establish performance or compatibility on arrows Alpha.
- The Kotlin API uses `Backend.GPU()` and requests optional `libvndksupport.so` and `libOpenCL.so` through the application manifest. Both declarations already existed in this project.
- Sources: [LiteRT-LM Android guide](https://developers.google.com/edge/litert-lm/android), [Gemma 4 model guide](https://developers.google.com/edge/litert-lm/models/gemma-4), [model card](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm).
- Implementation selects CPU or GPU explicitly and keeps the existing model file and CPU default; GPU generation does not silently fall back to CPU. A separate GPU result-cache key prevents a CPU cached response from being presented as GPU generation.
- Actual GPU initialization, inference, speed, and compatibility on the target device are unverified; user permission is required before an ADB interaction test.
