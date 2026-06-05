package org.chapzlock.core.component;

import org.chapzlock.core.ecs.Component;

/**
 * Marker component that triggers glTF model loading during entity instantiation.
 *
 * <p><strong>What is glTF?</strong> GL Transmission Format — an open standard for 3D models
 * that bundles geometry, materials, textures, animations, and skeleton rigs into a single
 * file (.glb binary or .gltf JSON + separate buffers).
 *
 * <p>When an entity with this component is instantiated, the {@link org.chapzlock.core.asset.gltf.GltfAssetManager}
 * expands it into a hierarchy of child entities — one per mesh node in the glTF scene graph.
 * Each child gets its own {@link Transform}, {@link Mesh}, {@link PbrMaterial}, and optionally
 * {@link Skeleton} and animation components.
 *
 * <p>After expansion, this component is consumed (removed from the entity). The entity
 * becomes the root of the loaded model hierarchy.
 *
 * @param resourcePath classpath-relative path to the .glb or .gltf file
 * @see org.chapzlock.core.asset.gltf.GltfAssetManager
 */
public record GltfModel(String resourcePath) implements Component {}
