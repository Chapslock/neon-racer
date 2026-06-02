# Neon Racer

A 3D game engine and racing game built from scratch in Java with LWJGL 3/OpenGL.

## Build & Run

```bash
./gradlew build              # Build both modules
./gradlew :app:run           # Run the game (requires display/GPU)
./gradlew :app:shadowJar     # Create fat JAR
./gradlew :core:test         # Run core unit tests
./gradlew test               # Run all tests
```

## Project Structure

```
neon-racer/
  core/   — Reusable game engine library (org.chapzlock.core)
  app/    — The neon-racer game (org.chapzlock.app)
```

## Engine Architecture

### Core Systems

```mermaid
graph TD
    App[Application] --> EC[EngineContext]
    EC --> CR[ComponentRegistry]
    EC --> EB[EventBus]
    EC --> MS[MeshSystem]
    EC --> TS[TextureSystem]
    EC --> SS[ShaderSystem]
    EC --> CS[CameraSystem]
    EC --> SM[SceneManager]
    EC --> Win[Window]
    EC --> GAR[GltfAssetRegistry]

    MS --> RC[RefCounted]
    TS --> RC
    SS --> RC

    SM --> SI[SceneInstantiator]
    SI --> DR[DescriptorResolver]
    SI --> GI[GltfSceneInstantiator]
    SI --> CV[ComponentRule]
```

### Rendering Pipeline

```mermaid
graph LR
    RS[RenderSystem] --> RCtx[RenderContext]
    RCtx --> OP[OpaquePass]
    RCtx --> SP[SpritePass]
    OP --> PBR[PbrMaterialRenderer]
    SP --> SR[SpriteRenderer]
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
    SB[SceneBuilder] --> BP[EntityBlueprint]
    SB --> SC[Scene]
    SC --> SM[SceneManager]
    SM --> SI[SceneInstantiator]
    SI --> DR[DescriptorResolver]
    SI --> GI[GltfSceneInstantiator]
    SI --> VAL[ComponentRule Validation]
    SI --> INST[SceneInstance]
```

### Engine Lifecycle

```mermaid
sequenceDiagram
    participant App as Application
    participant EC as EngineContext
    participant SM as SceneManager
    participant SI as SceneInstantiator
    participant ES as EngineSystems

    Note over App: STARTUP
    App->>EC: createSubsystems
    App->>SM: new SceneManager
    App->>ES: onInit
    App->>SM: loadImmediate
    SM->>SI: instantiate
    SI-->>SM: SceneInstance

    Note over App: MAIN LOOP
    loop Every Frame
        App->>ES: onUpdate
        App->>ES: onRender
        App->>ES: onDebugRender
        App->>SM: processPendingTransitions
    end

    Note over App: SHUTDOWN
    App->>SM: unloadAll
    App->>ES: onDestroy
    App->>EC: dispose
```

### System Execution Order

```mermaid
graph LR
    I[INPUT] --> L[LOGIC]
    L --> P[PHYSICS]
    P --> R[RENDER]
```

| Phase | Systems |
|-------|---------|
| INPUT | InputMap.frameReset |
| LOGIC | TransformSystem, AnimationSystem, game systems |
| PHYSICS | PhysicsSystem |
| RENDER | RenderSystem |

### ECS

- **Entities** are plain integer IDs (`Entity.create(registry)`)
- **Components** implement `Component` — data holders (Transform, Mesh, PbrMaterial, PointLight, etc.)
- **Tags** implement `Tag` — empty markers for query filtering (OpaqueLayer, SpriteLayer, PlayerTag)
- **Systems** implement `GameSystem` — logic that operates on entities via queries

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

- Java 21+, Lombok
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML (vectors, matrices, quaternions)
- JBullet (physics)
- fastutil (primitive collections)
