---
name: platform-guard
description: >
  Prevents Qwen Code from changing the platform, framework, or build system
  of the current project without explicit user permission. Use this skill
  whenever the user asks to modify, refactor, or add features to an existing
  project. Especially important for native mobile apps (Android/iOS), desktop
  apps, or any project with a defined target platform. Blocks attempts to
  convert an app to a web app, migrate frameworks, or replace the build system.
---

# Platform Guard

## Purpose

You are working on a project with an **established platform and build system**.
Your default behavior must be to **preserve** that platform, not to replace it.

## Core Rule

**NEVER change the platform, framework, or build system of the project
unless the user has explicitly asked you to do so in the current session.**

If you believe a platform change is beneficial, you must:
1. Explain why you think it is needed.
2. Ask the user for explicit confirmation.
3. Wait for a clear "yes" before making any changes.

## What counts as a platform change

- Converting a native app (Android/iOS) to a web app (React, Vue, plain HTML/JS).
- Adding a cross-platform framework (Flutter, React Native, Kotlin Multiplatform).
- Replacing the build system (Gradle → Maven, Xcode project → Swift Package Manager, etc.).
- Changing the primary language of the project (Kotlin → JavaScript, Swift → TypeScript).
- Removing or replacing platform-specific manifest files (AndroidManifest.xml, Info.plist, etc.).
- Adding web tooling (`package.json`, `vite.config.js`, `webpack.config.js`, `next.config.js`) to a non-web project.

## What to do instead

- Work **within** the existing platform.
- If the user asks for a feature that seems to require a platform change, first
  propose how to implement it **inside** the current platform.
- Only if that is truly impossible, ask the user whether they want to change platforms.

## Protected files

Do not delete, rename, or move these files unless the user explicitly asks:

- Android: `app/build.gradle`, `settings.gradle`, `AndroidManifest.xml`, `gradle/`
- iOS: `*.xcodeproj`, `*.xcworkspace`, `Info.plist`, `Podfile`
- Desktop: `CMakeLists.txt`, `*.csproj`, `*.vcxproj`
- Any file that defines the build target or platform configuration.

## Confirmation phrase

If you are about to make a platform change and the user has not explicitly
asked for it, stop and ask:

> "This action would change the platform of the project. Do you want me to
> proceed with this platform change? (yes/no)"