# Neon Racer

A 3D racing game built on a custom Java game engine. The engine provides an Entity-Component-System (ECS) architecture, PBR rendering, physics simulation, skeletal animation, scene management, and a 2D sprite overlay — all powered by LWJGL 3 and OpenGL.

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

## Engine TODO

* save/load feature
* UI/HUD enhancements
* audio

## Engine Architecture

### Core Systems

```mermaid
graph TD
    App[Application] --> Engine[Engine]
    Engine --> CR[ComponentRegistry]
    Engine --> EB[EventBus]
    Engine --> MR[MeshRegistry]
    Engine --> TR[TextureRegistry]
    Engine --> SR[ShaderRegistry]
    Engine --> CP[CameraProjection]
    Engine --> SM[SceneManager]
    Engine --> Win[Window]
    Engine --> GAM[GltfAssetManager]

    MR --> RC[RefCounted]
    TR --> RC
    SR --> RC

    SM --> EF[EntityFactory]
    EF --> CV[ComponentRule]
```

### Rendering Pipeline

```mermaid
graph LR
    RS[RenderSystem] --> RCtx[RenderContext]
    RCtx --> OP[OpaquePass]
    RCtx --> SP[SpritePass]
    OP --> PBR[PbrMaterialRenderer]
    SP --> SRen[SpriteRenderer]
    OP -.-> MR[MeshRegistry]
    OP -.-> TR[TextureRegistry]
    OP -.-> SR[ShaderRegistry]
```

Per frame:

```mermaid
sequenceDiagram
    participant RS as RenderSystem
    participant RC as RenderContext
    participant OP as OpaquePass
    participant SP as SpritePass

    RS->>RC: beginFrame
    Note over RC: Collect camera, lights, fog
    RS->>OP: collectVisibles
    Note over OP: Query OpaqueLayer + PbrMaterial + Mesh + Transform
    RS->>OP: execute
    Note over OP: Batch by PbrMaterial, draw
    RS->>SP: collectVisibles
    Note over SP: Query SpriteLayer + SpriteMaterial + Mesh + Transform
    RS->>SP: execute
    Note over SP: Batch by SpriteMaterial, draw
```

### Scene System

```mermaid
graph TD
    SB[SceneBuilder] --> ER[EntityRecipe]
    SB --> SC[Scene]
    SC --> SM[SceneManager]
    SM --> EF[EntityFactory]
    EF --> VAL[ComponentRule Validation]
    SM --> GAM[GltfAssetManager]
    SM --> INST[SceneInstance]
```

### Engine Lifecycle

```mermaid
sequenceDiagram
    participant App as Application
    participant Eng as Engine
    participant SM as SceneManager
    participant SS as SystemScheduler

    Note over App: STARTUP
    App->>Eng: Engine.create(spec)
    Note over Eng: Build all subsystems:<br/>ComponentRegistry, EventBus, Window,<br/>MeshRegistry, TextureRegistry, ShaderRegistry,<br/>GltfAssetManager, CameraProjection, EntityFactory
    Eng->>SS: Register permanent systems
    Note over SS: TweenSystem, AnimationSystem,<br/>TransformSystem, PhysicsSystem, RenderSystem
    App->>SM: loadImmediate(initialScene)
    SM-->>App: SceneInstance

    Note over App: MAIN LOOP
    loop Every Frame
        App->>SS: onUpdate (all systems sorted by phase)
        App->>SS: onRender
        App->>SS: onDebugRender
        App->>SM: processPendingTransitions
    end

    Note over App: SHUTDOWN
    App->>SM: unloadAll
    App->>SS: onDestroy (all systems)
    App->>Eng: dispose
```

### System Execution Order

```mermaid
graph LR
    I[INPUT] --> L[LOGIC]
    L --> P[PHYSICS]
    P --> R[RENDER]
```

| Phase | Systems | Managed By |
|-------|---------|-----------|
| INPUT | *(reserved for future input system)* | Engine |
| LOGIC | TweenSystem, AnimationSystem, TransformSystem, *scene systems* | Engine + Scene |
| PHYSICS | PhysicsSystem | Engine |
| RENDER | RenderSystem | Engine |

Engine systems are registered as permanent systems by `Engine.create()`. Scene-provided systems (user-defined) run in LOGIC phase alongside engine systems, sorted by `SystemOrdering`.

### System Ownership

```mermaid
graph TD
    subgraph Engine-Managed
        TS[TweenSystem<br/>LOGIC]
        AS[AnimationSystem<br/>LOGIC]
        TrS[TransformSystem<br/>LOGIC]
        PS[PhysicsSystem<br/>PHYSICS]
        RS[RenderSystem<br/>RENDER]
    end

    subgraph Scene-Provided
        GS1[Game System 1<br/>LOGIC]
        GS2[Game System 2<br/>LOGIC]
        GSN[...<br/>LOGIC]
    end

    Eng[Engine.create] -->|permanent systems| TS
    Eng -->|permanent systems| AS
    Eng -->|permanent systems| TrS
    Eng -->|permanent systems| PS
    Eng -->|permanent systems| RS

    SM[SceneManager] -->|scene.systems| GS1
    SM -->|scene.systems| GS2
    SM -->|scene.systems| GSN
```

### Configuration API

```mermaid
graph TD
    AS[ApplicationSpecification] --> WS[windowSpec]
    AS --> PC[physicsConfig]
    AS --> IS[initialScene]

    WS --> Win[Window]
    WS --> CP[CameraProjection]
    PC --> PS[PhysicsSystem]
    IS --> SM[SceneManager.loadImmediate]

    Note1[All engine systems<br/>auto-created internally]
```

Usage:

```java
Application.run(ApplicationSpecification.builder()
    .windowSpec(new WindowSpecification("Neon Racer", 1920, 1080))
    .physicsConfig(PhysicsWorldConfig.defaults())  // optional
    .initialScene(myScene)
    .build()
);
```

### Package Organization

```mermaid
graph TD
    subgraph ecs
        Component
        Tag
        Entity
        ComponentRegistry
        GameSystem
        SystemPhase
        SystemOrdering
        SystemScheduler
        TransformSystem
        EntityFactory
        EntityBuilder
    end

    subgraph render
        RenderSystem
        CameraProjection
        MeshRegistry
        TextureRegistry
        ShaderRegistry
        OpaquePass
        SpritePass
        RenderContext
        PbrMaterialRenderer
        SpriteRenderer
    end

    subgraph physics
        PhysicsSystem
        PhysicsWorld
        Physics
        CollisionShapeFactory
    end

    subgraph scene
        Scene
        SceneManager
        SceneBuilder
        EntityRecipe
        SceneInstance
    end

    subgraph animation
        AnimationSystem
        Animation
        Channel
    end

    subgraph application
        Application
        ApplicationSpecification
        Engine
        Time
    end
```

### ECS (Entity-Component-System)

ECS is a data-oriented architecture pattern where game objects are decomposed into:

- **Entities** — plain integer IDs (`Entity.create(registry)`). Not objects — just unique numbers that index into component storage.
- **Components** — data-only classes implementing `Component` (Transform, Mesh, PbrMaterial, PointLight, etc.). Hold state but no logic.
- **Tags** — empty components implementing `Tag` (OpaqueLayer, SpriteLayer, PlayerTag). Used purely for filtering in queries.
- **Systems** — logic classes implementing `GameSystem` that operate on entities via queries each frame.

This separation means data is stored contiguously by type (cache-friendly), systems are decoupled from each other, and new behaviors can be added by composing existing components rather than inheriting from base classes.

Query API:
```java
// Zero-alloc iteration with typed components
registry.forEach(Transform.class, Velocity.class, (entityId, transform, vel) -> {
    transform.getPosition().add(vel.dx() * delta, vel.dy() * delta, 0);
});

// Singleton access
Camera camera = registry.getSingleton(Camera.class).orElse(null);

// Name-based lookup
int player = registry.getEntityByName("player");
```

### Component Validation

Scene instantiation validates component dependencies:

| Component | Requires |
|-----------|----------|
| Mesh | Transform |
| PhysicsBody | Transform |
| PointLight | Transform |
| SpriteMaterial | Mesh |
| PbrMaterial | Mesh |
| Skeleton | Mesh |

Entities violating these rules are skipped with a clear error message.

## Tech Stack

- Java 21+, Lombok for boilerplate reduction
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML (vectors, matrices, quaternions)
- JBullet (physics)
- fastutil (primitive collections)
