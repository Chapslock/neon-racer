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
- `application/` — Application, EngineContext, ApplicationSpecification, Disposable, ApplicationUtil
- `scene/` — Scene, SceneManager, SceneBuilder, EntityBlueprint, EntityBlueprintBuilder, ComponentFactory, MaterialType, SceneLoadedEvent, SceneUnloadedEvent
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

**Contents:** ComponentRegistry, EventBus, MeshSystem, TextureSystem, ShaderSystem, MaterialSystem, MaterialRendererRegistry, CameraSystem, Window, SceneManager.

**Lifecycle:** Application creates the context at startup, passes it to systems and SceneManager. Application calls `ctx.dispose()` on shutdown.

**Testing:** `EngineContext.testContext()` creates a minimal context with fresh ComponentRegistry + EventBus (GPU systems null) for unit tests.

### Scene System

A `Scene` is an immutable blueprint describing entities (via `EntityBlueprint` + `ComponentFactory` lambdas) and game-logic systems. Scenes are built via a fluent builder: `Scene.builder("name").entity("x", e -> e.mesh(...).texture(...)).systems(...).build()`.

`SceneManager` manages scene lifecycle: one primary scene plus zero or more additive scenes. Engine systems (PhysicsSystem, RenderSystem) are always active; scene systems are added/removed with their scene. `getActiveSystems()` returns engine + scene systems sorted by phase.

**Transitions:** `loadImmediate(scene)` for initial load (synchronous). `load(scene)` queues a deferred transition (old scene unloaded, new scene instantiated at end-of-frame via `processPendingTransitions()`). `loadAdditive(scene)` adds a persistent overlay scene. `unload(scene)` removes an additive scene.

**Asset ownership:** GPU asset systems (MeshSystem, TextureSystem, ShaderSystem) use reference counting (`retain()`/`release()`). SceneManager retains assets when instantiating a scene and releases them on unload. Shared assets (same path across scenes) survive until all referencing scenes are unloaded.

**Material defaults:** `MaterialType` enum (ENTITY, TERRAIN, UI) maps to pre-registered `MaterialRenderer` instances via `MaterialRendererRegistry.registerDefault()`. The scene builder auto-selects the appropriate default based on entity setup (e.g., `.mesh() + .texture()` → ENTITY).

### Application Lifecycle

`Application.create(spec)` builds the application. `ApplicationSpecification` declares `engineSystems` (always-active infrastructure) and `initialScene` (first scene to load). `run()` creates `EngineContext`, registers default material renderers, creates `SceneManager`, inits engine systems, then loads the initial scene.

**Main loop order per frame:** `onUpdate(delta)` → `onRender(delta)` → `onDebugRender(delta)` on all active systems (sorted by SystemPhase), then `sceneManager.processPendingTransitions()`.

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

GPU systems (MeshSystem, TextureSystem, ShaderSystem) implement `Disposable`. They track all allocated OpenGL resources and free them in `dispose()`. `EngineContext.dispose()` cascades to all disposable subsystems on shutdown. Reference counting (`retain()`/`release()`) handles per-scene asset ownership during transitions; `dispose()` frees everything unconditionally at shutdown.

## Tech Stack

- Java 21+, Lombok for boilerplate reduction
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML for math (vectors, matrices, quaternions)
- JBullet for physics
- fastutil for primitive collections
