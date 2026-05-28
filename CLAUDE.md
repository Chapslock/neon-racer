# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build              # Build both modules
./gradlew :app:run           # Run the game (requires display/GPU)
./gradlew :app:shadowJar     # Create fat JAR at app/build/libs/neon-racer-<version>.jar
./gradlew :core:test         # Run core unit tests (JUnit 5)
./gradlew :app:test          # Run app unit tests
./gradlew test               # Run all tests
```

JVM flag `--enable-native-access=ALL-UNNAMED` is required at runtime (configured in build.gradle.kts).

## Project Structure

Two Gradle modules (Kotlin DSL):
- **core** — Reusable game engine library (`org.chapzlock.core`)
- **app** — The neon-racer game built on top of core (`org.chapzlock.app`)

### Core Packages

- `ecs/` — GameSystem, SystemPhase, Component, Tag, Entity, EntityView, ComponentRegistry, ComponentStore
- `application/` — Application, Layer, EngineContext, Disposable, ApplicationUtil
- `render/` — RenderSystem, CameraSystem, MaterialSystem, MeshSystem, TextureSystem, ShaderSystem, MaterialRendererRegistry, MaterialRenderer impls, ShaderProps, GL utilities
- `component/` — All data components (Camera, Transform, Mesh, Material, PhysicsBody, etc.)
- `physics/` — PhysicsSystem and all physics utilities
- `event/` — EventBus, Event, lifecycle events
- `input/` — Keyboard/mouse event classes
- `window/` — Window, WindowSpecification
- `asset/` — FileUtils, RawMeshData, RawMeshDataFactory
- `logging/` — Log

## Architecture

### ECS (Entity-Component-System)

Entities are plain integer IDs (`Entity.create(registry)`). Components are marker-interface data holders (`implements Component`). Tags are empty components used for querying (`implements Tag`). Systems implement `GameSystem` and receive dependencies via `onInit(EngineContext ctx)`. They operate on entities via `registry.view(ComponentA.class, ComponentB.class, ...)` which returns entities possessing all listed components.

`ComponentRegistry` stores components in typed `ComponentStore` maps, indexed by entity ID. It uses bitset masks for fast archetype queries. Query results are cached internally and invalidated on component mutations — `view()` is O(1) on stable frames.

### EngineContext

`EngineContext` is a plain object created by `Application` that holds all engine subsystems. No singletons exist — all dependencies flow through the context.

**Contents:** ComponentRegistry, EventBus, MeshSystem, TextureSystem, ShaderSystem, MaterialSystem, MaterialRendererRegistry, Window.

**Lifecycle:** Application creates the context at startup, passes it to layers via `onInit(EngineContext ctx)`, layers pass it to systems, and Application calls `ctx.dispose()` on shutdown.

**Testing:** `EngineContext.testContext()` creates a minimal context with fresh ComponentRegistry + EventBus (GPU systems null) for unit tests.

### Application Lifecycle

`Application.create(spec)` builds the application. `run()` creates the `EngineContext`, collects systems from all layers, sorts them by `SystemPhase` (INPUT → LOGIC → PHYSICS → RENDER, stable sort preserves declaration order within a phase), and enters the main loop.

Layers are declarative: `systems()` returns the list of systems, `onInit(ctx)` sets up entities. The engine drives system lifecycle (init, update, render, destroy) — layers do not forward calls manually.

**Init order:** Systems are initialized first (so they can subscribe to lifecycle events), then layers (which create entities that fire those events).

**Main loop order per frame:** `onUpdate(delta)` on all sorted systems, then `onRender(delta)` on all sorted systems.

### SystemPhase

Systems declare their execution phase via `default SystemPhase phase()`. The enum values are: `INPUT`, `LOGIC` (default), `PHYSICS`, `RENDER`. Core systems (PhysicsSystem, RenderSystem) declare their own phase; game-specific systems default to LOGIC.

### Rendering Pipeline

Materials pair a `Shader` (vertex+fragment GLSL paths) with textures and properties. Each material type has a corresponding `MaterialRenderer` implementation that knows how to bind uniforms and textures. New material types require:
1. A `*ShaderProps` class pointing to GLSL files
2. A `*MaterialRenderer` implementing `MaterialRenderer`
3. Registration via `MaterialSystem.registerNewMaterial(material, renderer)`

`RenderSystem` batches entities by material to minimize shader switches.

### Physics

JBullet (Bullet physics Java port). `PhysicsSystem` owns a `DiscreteDynamicsWorld`. Entities with `PhysicsBody` + `Transform` components are registered into the physics world via lifecycle events. Transform synchronization is handled via `EntityMotionState`.

### Event System

`EventBus` with typed publish/subscribe. Listeners return `true` to stop propagation. Input events (key/mouse) flow through this bus. Subscribe returns a `Subscription` handle for cleanup. Each EngineContext owns its own EventBus instance.

### Resource Cleanup

GPU systems (MeshSystem, TextureSystem, ShaderSystem) implement `Disposable`. They track all allocated OpenGL resources and free them in `dispose()`. `EngineContext.dispose()` cascades to all disposable subsystems on shutdown.

## Tech Stack

- Java 21+, Lombok for boilerplate reduction
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML for math (vectors, matrices, quaternions)
- JBullet for physics
- fastutil for primitive collections
