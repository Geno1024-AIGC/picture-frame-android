# 相框 (PictureFrame)

An Android app for framing photos and giving them a background, then saving the
result back to the gallery.

## Features

- **镜框 / Frame** — no frame, solid, bevel, inset, double line, drop shadow, or
  polaroid. Adjustable width, corner radius, shadow strength, plus an inner mat
  board and its own colour.
- **背景 / Background** — transparent, solid colour, linear or radial gradient,
  or a blurred copy of the photo itself. Gradients have an angle; any background
  can be dimmed.
- **画布 / Canvas** — aspect ratio (original, 1:1, 4:5, 3:4, 9:16, 16:9),
  output resolution, and margin around the framed picture.
- **导出 / Export** — JPEG or PNG at up to 4096 px on the long edge, saved into
  `Pictures/PictureFrame`.
- One-tap **looks** for the common cases (白边, 拍立得, 暗夜, 虚化, 暖阳, 双线, 留白).

## Design notes

Every measurement in the editor is a fraction of the canvas min-dimension, not a
pixel count. `Layout.stage` turns that into rectangles, and both the on-screen
preview and the exported bitmap run through the same `drawFramed` on the same
`DrawScope`. The result is that the preview is not an approximation of the
export — it is the export, drawn smaller.

Shadows are stacked translucent rounded rectangles rather than a platform blur,
and the blurred background is a separable box blur over a 128 px copy. Both were
chosen so the output does not change with the Android version.

Source photos are decoded with EXIF orientation applied and downsampled to 3072 px
on the long edge, which bounds memory while staying sharp for every export size
offered.

## Build

Gradle KTS, Kotlin, Jetpack Compose, AGP 9.4, JDK 25.

```bash
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # layout engine tests
./gradlew lintDebug
```

`local.properties` must point at an Android SDK with platform 37 and build-tools
36. GitHub Actions runs the same three tasks on every push and uploads the debug
APK as an artifact.

## Canary updates

The `Build` workflow publishes every master build to a rolling pre-release under
the fixed `canary` tag. The app can update itself from that tag, which is useful
when GitHub is slow or blocked: the update button in the top bar checks for a
newer build and, if there is one, downloads the APK through a mirror and hands
it to the system installer.

Because `versionName` is static across canaries, freshness is decided by the
workflow run number. CI passes `-PcanaryRunNumber` into the build so the APK
knows what it is, and publishes a `canary.properties` manifest next to the APK
carrying the run number, the size and the sha256.

The manifest is fetched from GitHub directly first, and is only a few hundred
bytes so a slow link can still afford it. The mirrors act as a fallback for it
too. Only the APK digest is checked against the manifest, which catches
corruption, truncated files and a mirror serving a stale build. If the manifest
itself came from a mirror, that mirror also chose the expected digest, so at
that point the digest is no longer a defence against a hostile mirror — what
actually prevents one from installing its own APK is that Android refuses an
update signed with a different key. The realistic worst case is a failed or
stuck update, not a compromised install.

Mirrors are community-run and go down without warning, so the list is
user-selectable, persisted, and falls back through the remaining presets when
one fails. A custom prefix can be typed in for domains that are not built in.
Installing requires the "unknown sources" permission, which is only requested
once an APK is actually waiting to be installed.
## Layout

```
app/src/main/java/com/geno1024/pictureframe/
├── MainActivity.kt
├── EditorViewModel.kt        editor state, load/export actions
├── model/EditorModels.kt     state, enums, look presets, swatches
├── render/
│   ├── Layout.kt             aspect/resolution sizing, frame geometry
│   ├── FrameRenderer.kt      the shared DrawScope renderer
│   ├── Blur.kt               box blur for the blurred background
│   └── FrameExporter.kt      renders the state to a full-size Bitmap
├── io/                       photo loading and gallery saving
├── update/                   canary manifest, mirrors, download, install
└── ui/                       screen, panels, shared controls
```
