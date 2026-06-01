package org.chapzlock.core.asset.gltf;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chapzlock.core.asset.RawMeshData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;

/**
 * Loads glTF/GLB files and converts them into engine-friendly data structures.
 *
 * <p>This is the main entry point for loading 3D assets. It wraps the jglTF library
 * which handles the complex parsing (binary chunks, buffer views, URI resolution),
 * then converts the jglTF model objects into our engine's data types.
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * GltfLoader loader = new GltfLoader();
 * GltfAsset asset = loader.load("models/car.glb");
 * // asset now contains nodes, meshes, materials ready for scene instantiation
 * }</pre>
 *
 * <p><strong>What is glTF?</strong> "GL Transmission Format" — a JSON-based format
 * (or binary .glb) designed by the Khronos Group as the "JPEG of 3D." It maps
 * directly to GPU concepts: buffer views are VBOs, accessors are vertex attributes,
 * materials are PBR metallic-roughness. This makes it efficient to load.
 *
 * <p><strong>Loading pipeline:</strong>
 * <ol>
 *   <li>jglTF reads the file (handles binary chunks, base64 data URIs, external files)</li>
 *   <li>We walk the jglTF model and extract mesh geometry using {@link AccessorUnpacker}</li>
 *   <li>We extract material properties (PBR factors, texture references)</li>
 *   <li>We extract the node hierarchy (transforms, mesh/skin references, parent-child links)</li>
 *   <li>Everything is packaged into a {@link GltfAsset} ready for scene instantiation</li>
 * </ol>
 *
 * @see GltfAsset
 * @see AccessorUnpacker
 */
public class GltfLoader {

    private final GltfModelReader reader = new GltfModelReader();

    /**
     * Loads a glTF or GLB file from the classpath.
     *
     * <p>This is the primary loading method for game assets. Resources are found via
     * the classloader, which means the file should be in the resources directory
     * (e.g., {@code src/main/resources/models/car.glb}).
     *
     * @param resourcePath path relative to resources root (e.g., "models/car.glb")
     * @return parsed asset data ready for scene instantiation
     * @throws GltfLoadException if the file cannot be found or parsed
     */
    public GltfAsset load(String resourcePath) {
        try {
            URI uri = getClass().getClassLoader().getResource(resourcePath).toURI();
            GltfModel model = reader.read(uri);
            return convertModel(model, resourcePath);
        } catch (Exception e) {
            throw new GltfLoadException("Failed to load glTF asset: " + resourcePath, e);
        }
    }

    /**
     * Loads a glTF or GLB file from an absolute filesystem path.
     *
     * <p>Use this method for loading assets from outside the classpath, such as
     * user-provided mods or assets downloaded at runtime.
     *
     * @param path absolute path to the file
     * @return parsed asset data
     * @throws GltfLoadException if the file cannot be found or parsed
     */
    public GltfAsset loadFromPath(Path path) {
        try {
            GltfModel model = reader.read(path.toUri());
            return convertModel(model, path.toString());
        } catch (Exception e) {
            throw new GltfLoadException("Failed to load glTF asset: " + path, e);
        }
    }

    /**
     * Converts a jglTF model into our engine's asset format.
     *
     * <p>This is the core conversion logic. It walks the jglTF object graph and extracts
     * meshes, materials, and nodes into our flat, index-based data structures.
     */
    private GltfAsset convertModel(GltfModel model, String sourcePath) {
        List<GltfMeshData> meshes = extractMeshes(model);
        List<GltfMaterialData> materials = extractMaterials(model, sourcePath);
        List<GltfNodeData> nodes = extractNodes(model);
        List<Integer> rootNodeIndices = extractRootNodeIndices(model);
        List<GltfSkinData> skins = extractSkins(model);
        List<GltfAnimationData> animations = extractAnimations(model);

        return new GltfAsset(nodes, rootNodeIndices, meshes, materials, skins, animations);
    }

    /**
     * Extracts all meshes from the glTF model.
     *
     * <p>Each glTF mesh can have multiple "primitives" (sub-meshes), each with its own
     * geometry and material. We convert each primitive's accessor data into a
     * {@link RawMeshData} that our GPU upload code understands.
     *
     * <p>Morph targets (blend shapes) are also extracted from each primitive if present.
     * These store per-vertex position/normal deltas that can be blended at runtime using
     * weights to produce facial expressions, corrective shapes, etc.
     */
    private List<GltfMeshData> extractMeshes(GltfModel model) {
        List<GltfMeshData> meshes = new ArrayList<>();
        for (MeshModel meshModel : model.getMeshModels()) {
            List<GltfMeshData.Primitive> primitives = new ArrayList<>();
            for (MeshPrimitiveModel primitive : meshModel.getMeshPrimitiveModels()) {
                RawMeshData rawMesh = extractPrimitiveMesh(primitive);
                int materialIndex = model.getMaterialModels().indexOf(primitive.getMaterialModel());
                List<GltfMeshData.MorphTargetData> morphTargets = extractMorphTargets(primitive);
                primitives.add(new GltfMeshData.Primitive(rawMesh, materialIndex, morphTargets));
            }
            meshes.add(new GltfMeshData(meshModel.getName(), primitives));
        }
        return meshes;
    }

    /**
     * Extracts morph target (blend shape) data from a mesh primitive.
     *
     * <p>In glTF, morph targets are stored as a list of attribute maps on each primitive.
     * Each map represents one target and contains accessors for the vertex attribute deltas
     * (offsets from the base mesh). Common keys are:
     * <ul>
     *   <li>"POSITION" — per-vertex position offsets (vec3), the most common target attribute</li>
     *   <li>"NORMAL" — per-vertex normal offsets (vec3), for correct lighting on morphed shapes</li>
     *   <li>"TANGENT" — per-vertex tangent offsets (vec3, not vec4), rare in practice</li>
     * </ul>
     *
     * <p>We extract position and normal deltas. Tangent deltas are omitted as they are rarely
     * used and can be recomputed from morphed positions/normals if needed.
     *
     * @param primitive the mesh primitive to extract morph targets from
     * @return list of morph target data (empty list if no targets present)
     */
    private List<GltfMeshData.MorphTargetData> extractMorphTargets(MeshPrimitiveModel primitive) {
        List<Map<String, AccessorModel>> targets = primitive.getTargets();
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }

        List<GltfMeshData.MorphTargetData> result = new ArrayList<>();
        for (Map<String, AccessorModel> target : targets) {
            AccessorModel posAccessor = target.get("POSITION");
            float[] posDeltas = posAccessor != null ? readFloatAccessor(posAccessor) : null;

            AccessorModel normalAccessor = target.get("NORMAL");
            float[] normalDeltas = normalAccessor != null ? readFloatAccessor(normalAccessor) : null;

            result.add(new GltfMeshData.MorphTargetData(posDeltas, normalDeltas));
        }
        return result;
    }

    /**
     * Extracts vertex data from a single mesh primitive.
     *
     * <p>A primitive is the atomic unit of renderable geometry in glTF. It contains
     * vertex attributes (positions, normals, UVs, etc.) stored as accessors pointing
     * into binary buffers. This method reads each attribute and packs them into our
     * engine's {@link RawMeshData} format.
     *
     * <p><strong>Required attributes:</strong> Only POSITION is required by the glTF spec.
     * All others (NORMAL, TEXCOORD_0, TANGENT, etc.) are optional. We provide sensible
     * defaults (zero-filled) when optional attributes are missing.
     */
    private RawMeshData extractPrimitiveMesh(MeshPrimitiveModel primitive) {
        var builder = RawMeshData.builder();

        // Positions (required by glTF spec)
        AccessorModel posAccessor = primitive.getAttributes().get("POSITION");
        builder.positions(readFloatAccessor(posAccessor));

        // Texture coordinates (optional — defaults to zeros if missing)
        AccessorModel texCoordAccessor = primitive.getAttributes().get("TEXCOORD_0");
        if (texCoordAccessor != null) {
            builder.texCoords(readFloatAccessor(texCoordAccessor));
        } else {
            int vertexCount = posAccessor.getCount();
            builder.texCoords(new float[vertexCount * 2]);
        }

        // Normals (optional — used for lighting calculations)
        AccessorModel normalAccessor = primitive.getAttributes().get("NORMAL");
        if (normalAccessor != null) {
            builder.normals(readFloatAccessor(normalAccessor));
        }

        // Tangents (optional — needed for normal mapping, vec4 where w = handedness)
        AccessorModel tangentAccessor = primitive.getAttributes().get("TANGENT");
        if (tangentAccessor != null) {
            builder.tangents(readFloatAccessor(tangentAccessor));
        }

        // Joints (optional — bone indices for skeletal animation, typically ivec4)
        AccessorModel jointsAccessor = primitive.getAttributes().get("JOINTS_0");
        if (jointsAccessor != null) {
            builder.joints(readIntAccessor(jointsAccessor));
        }

        // Weights (optional — bone weights for skeletal animation, typically vec4)
        AccessorModel weightsAccessor = primitive.getAttributes().get("WEIGHTS_0");
        if (weightsAccessor != null) {
            builder.weights(readFloatAccessor(weightsAccessor));
        }

        // Vertex colors (optional — per-vertex RGBA colors)
        AccessorModel colorAccessor = primitive.getAttributes().get("COLOR_0");
        if (colorAccessor != null) {
            builder.colors(readFloatAccessor(colorAccessor));
        }

        // Secondary UV set (optional — some assets use two UV sets for lightmaps)
        AccessorModel texCoord1Accessor = primitive.getAttributes().get("TEXCOORD_1");
        if (texCoord1Accessor != null) {
            builder.texCoords1(readFloatAccessor(texCoord1Accessor));
        }

        // Indices (optional — if missing, geometry is non-indexed: draw vertices sequentially)
        AccessorModel indicesAccessor = primitive.getIndices();
        if (indicesAccessor != null) {
            builder.indices(readIntAccessor(indicesAccessor));
        } else {
            // Non-indexed geometry: generate sequential indices (0, 1, 2, 3, ...)
            int vertexCount = posAccessor.getCount();
            int[] indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) indices[i] = i;
            builder.indices(indices);
        }

        return builder.build();
    }

    /**
     * Reads float data from a glTF accessor.
     *
     * <p>This handles the indirection chain: accessor -> buffer view -> buffer.
     * The accessor specifies the byte offset, element count, and element type.
     * The buffer view provides the underlying byte data and optional stride.
     *
     * @param accessor the glTF accessor describing where and how to read the data
     * @return flat float array (e.g., for vec3 positions: [x0, y0, z0, x1, y1, z1, ...])
     */
    private float[] readFloatAccessor(AccessorModel accessor) {
        ByteBuffer buffer = accessor.getBufferViewModel().getBufferViewData();
        int byteOffset = accessor.getByteOffset();
        int byteStride = resolveByteStride(accessor);
        int componentCount = accessor.getElementType().getNumComponents();

        return AccessorUnpacker.unpackFloats(buffer, componentCount, accessor.getCount(),
            byteOffset, byteStride);
    }

    /**
     * Reads integer data from a glTF accessor.
     *
     * <p>glTF stores integer data in different sizes to save space:
     * <ul>
     *   <li>UNSIGNED_BYTE (5121) — 1 byte per value, range 0-255 (joint indices)</li>
     *   <li>UNSIGNED_SHORT (5123) — 2 bytes per value, range 0-65535 (most index buffers)</li>
     *   <li>UNSIGNED_INT (5125) — 4 bytes per value, full range (large meshes)</li>
     * </ul>
     * We detect the component type and use the appropriate unpacker to convert to int[].
     *
     * @param accessor the glTF accessor for integer data
     * @return flat int array of index or joint data
     * @throws GltfLoadException if the component type is not one of the supported integer types
     */
    private int[] readIntAccessor(AccessorModel accessor) {
        ByteBuffer buffer = accessor.getBufferViewModel().getBufferViewData();
        int byteOffset = accessor.getByteOffset();
        int byteStride = resolveByteStride(accessor);
        int componentCount = accessor.getElementType().getNumComponents();
        int componentType = accessor.getComponentType();

        // glTF component types: 5120=BYTE, 5121=UNSIGNED_BYTE, 5122=SHORT,
        // 5123=UNSIGNED_SHORT, 5125=UNSIGNED_INT, 5126=FLOAT
        return switch (componentType) {
            case 5121 -> AccessorUnpacker.unpackUnsignedBytes(buffer, componentCount,
                accessor.getCount(), byteOffset, byteStride);
            case 5123 -> AccessorUnpacker.unpackUnsignedShorts(buffer, componentCount,
                accessor.getCount(), byteOffset, byteStride);
            case 5125 -> AccessorUnpacker.unpackInts(buffer, componentCount,
                accessor.getCount(), byteOffset, byteStride);
            default -> throw new GltfLoadException(
                "Unsupported component type for integer accessor: " + componentType, null);
        };
    }

    /**
     * Resolves the effective byte stride for an accessor.
     *
     * <p>In glTF, the byte stride can come from two places:
     * <ol>
     *   <li>The accessor itself ({@link AccessorModel#getByteStride()}) — if non-zero, use it</li>
     *   <li>The buffer view ({@link BufferViewModel#getByteStride()}) — nullable Integer</li>
     * </ol>
     * If both are zero/null, data is tightly packed (stride = 0, which our unpacker interprets
     * as "use element size").
     *
     * @param accessor the accessor to resolve stride for
     * @return the byte stride, or 0 for tightly packed data
     */
    private int resolveByteStride(AccessorModel accessor) {
        // The accessor's own byte stride takes priority
        int accessorStride = accessor.getByteStride();
        if (accessorStride > 0) {
            return accessorStride;
        }
        // Fall back to buffer view stride (may be null for tightly packed)
        Integer bvStride = accessor.getBufferViewModel().getByteStride();
        return bvStride != null ? bvStride : 0;
    }

    /**
     * Extracts all PBR materials from the glTF model.
     *
     * <p>Each glTF material uses the "metallic-roughness" PBR model with optional textures.
     * We extract all relevant properties and texture paths into our engine's
     * {@link GltfMaterialData} format.
     *
     * @param model      the parsed glTF model
     * @param sourcePath the original file path (used to resolve relative texture paths)
     */
    private List<GltfMaterialData> extractMaterials(GltfModel model, String sourcePath) {
        List<GltfMaterialData> materials = new ArrayList<>();
        String baseDir = sourcePath.contains("/")
            ? sourcePath.substring(0, sourcePath.lastIndexOf('/') + 1) : "";

        Map<ImageModel, Integer> imageIndexMap = new HashMap<>();

        for (MaterialModel mat : model.getMaterialModels()) {
            MaterialModelV2 matV2 = (MaterialModelV2) mat;

            // Extract texture references (null if no texture assigned)
            GltfTextureRef baseColorTex = resolveTextureRef(matV2.getBaseColorTexture(), baseDir, sourcePath, imageIndexMap);
            GltfTextureRef metallicRoughnessTex = resolveTextureRef(
                matV2.getMetallicRoughnessTexture(), baseDir, sourcePath, imageIndexMap);
            GltfTextureRef normalTex = resolveTextureRef(matV2.getNormalTexture(), baseDir, sourcePath, imageIndexMap);
            GltfTextureRef occlusionTex = resolveTextureRef(matV2.getOcclusionTexture(), baseDir, sourcePath, imageIndexMap);
            GltfTextureRef emissiveTex = resolveTextureRef(matV2.getEmissiveTexture(), baseDir, sourcePath, imageIndexMap);

            // Extract factors (with defaults matching glTF spec)
            float[] bcf = matV2.getBaseColorFactor() != null
                ? matV2.getBaseColorFactor() : new float[]{1, 1, 1, 1};
            float metallicFactor = matV2.getMetallicFactor();
            float roughnessFactor = matV2.getRoughnessFactor();
            float[] ef = matV2.getEmissiveFactor() != null
                ? matV2.getEmissiveFactor() : new float[]{0, 0, 0};

            // Alpha mode controls transparency handling
            String alphaMode = matV2.getAlphaMode() != null
                ? matV2.getAlphaMode().name() : "OPAQUE";
            float alphaCutoff = matV2.getAlphaCutoff();
            boolean doubleSided = matV2.isDoubleSided();

            materials.add(new GltfMaterialData(
                mat.getName(),
                baseColorTex,
                new Vector4f(bcf[0], bcf[1], bcf[2], bcf[3]),
                metallicRoughnessTex,
                metallicFactor,
                roughnessFactor,
                normalTex,
                occlusionTex,
                emissiveTex,
                new Vector3f(ef[0], ef[1], ef[2]),
                alphaMode,
                alphaCutoff,
                doubleSided
            ));
        }
        return materials;
    }

    /**
     * Resolves a texture reference (file-based or embedded) from a glTF texture model.
     *
     * <p>In glTF, textures reference images, and images have a URI pointing to the actual
     * image file. For separate .gltf files, this URI is relative to the .gltf file.
     * For .glb files with embedded textures, the URI may be null (data is in the binary chunk).
     *
     * @param texture       the texture model (may be null if no texture assigned)
     * @param baseDir       the directory containing the glTF file (for resolving relative paths)
     * @param sourcePath    the original file path (used to build unique cache keys for embedded textures)
     * @param imageIndexMap tracks unique images across all materials for deduplication
     * @return texture reference, or null if no texture/image
     */
    private GltfTextureRef resolveTextureRef(TextureModel texture, String baseDir, String sourcePath,
                                             Map<ImageModel, Integer> imageIndexMap) {
        if (texture == null) return null;
        ImageModel image = texture.getImageModel();
        if (image == null) return null;
        String uri = image.getUri();
        if (uri != null) {
            return GltfTextureRef.fromPath(baseDir + uri);
        }
        // Embedded texture: extract raw image bytes from GLB binary chunk
        ByteBuffer imageData = image.getImageData();
        if (imageData == null) return null;
        int imageIndex = imageIndexMap.computeIfAbsent(image, k -> imageIndexMap.size());
        String cacheKey = "embedded:" + sourcePath + ":img" + imageIndex;
        return GltfTextureRef.fromMemory(imageData, cacheKey);
    }

    /**
     * Extracts all nodes from the glTF model into a flat list.
     *
     * <p>The node hierarchy is preserved via child indices: each node stores the indices
     * of its children in the flat list. This allows efficient random access while
     * maintaining the tree structure.
     *
     * <p><strong>Transform decomposition:</strong> glTF nodes can specify their transform as
     * either a 4x4 matrix OR as separate translation/rotation/scale (TRS) components.
     * We always use TRS here because it's more useful for animation and physics.
     * If only a matrix is set, jglTF decomposes it into TRS for us.
     */
    private List<GltfNodeData> extractNodes(GltfModel model) {
        List<GltfNodeData> nodes = new ArrayList<>();
        List<NodeModel> allNodes = model.getNodeModels();

        for (NodeModel node : allNodes) {
            float[] translation = node.getTranslation() != null
                ? node.getTranslation() : new float[]{0, 0, 0};
            float[] rotation = node.getRotation() != null
                ? node.getRotation() : new float[]{0, 0, 0, 1};
            float[] scale = node.getScale() != null
                ? node.getScale() : new float[]{1, 1, 1};

            // Find mesh index (-1 if this node has no mesh)
            int meshIndex = node.getMeshModels().isEmpty() ? -1
                : model.getMeshModels().indexOf(node.getMeshModels().get(0));

            // Find skin index (-1 if this node is not skinned)
            int skinIndex = node.getSkinModel() != null
                ? model.getSkinModels().indexOf(node.getSkinModel()) : -1;

            // Collect child indices (references into this same flat list)
            List<Integer> childIndices = new ArrayList<>();
            for (NodeModel child : node.getChildren()) {
                childIndices.add(allNodes.indexOf(child));
            }

            nodes.add(new GltfNodeData(
                node.getName(),
                new Vector3f(translation[0], translation[1], translation[2]),
                new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]),
                new Vector3f(scale[0], scale[1], scale[2]),
                meshIndex,
                skinIndex,
                childIndices
            ));
        }
        return nodes;
    }

    /**
     * Extracts the indices of root nodes from the default scene.
     *
     * <p>A glTF file can contain multiple scenes, but typically has one "default" scene.
     * Root nodes are the top-level nodes in that scene (nodes with no parent). All other
     * nodes are reachable by traversing children from these roots.
     *
     * @param model the parsed glTF model
     * @return indices into the flat node list for all root nodes
     */
    private List<Integer> extractRootNodeIndices(GltfModel model) {
        List<Integer> roots = new ArrayList<>();
        List<NodeModel> allNodes = model.getNodeModels();

        // Use the first (default) scene's root nodes
        if (!model.getSceneModels().isEmpty()) {
            SceneModel scene = model.getSceneModels().get(0);
            for (NodeModel root : scene.getNodeModels()) {
                roots.add(allNodes.indexOf(root));
            }
        }
        return roots;
    }

    /**
     * Extracts all skins (skeleton rig definitions) from the glTF model.
     *
     * <p>Each skin defines a set of joints (nodes acting as bones) and their inverse bind
     * matrices. The inverse bind matrix for a joint transforms vertices from mesh space into
     * that joint's bone-local space at the bind pose.
     *
     * <p><strong>Joint ordering:</strong> The order of joints in a skin is significant —
     * it defines the "joint index" that vertex JOINTS_0 attributes reference. Joint 0 in
     * JOINTS_0 maps to the first entry in the skin's joint list.
     *
     * @param model the parsed glTF model
     * @return list of extracted skin data (empty if no skins in the file)
     */
    private List<GltfSkinData> extractSkins(GltfModel model) {
        List<GltfSkinData> skins = new ArrayList<>();
        List<NodeModel> allNodes = model.getNodeModels();

        for (SkinModel skin : model.getSkinModels()) {
            List<NodeModel> joints = skin.getJoints();
            int jointCount = joints.size();

            // Map each joint to its index in the global node list
            int[] jointNodeIndices = new int[jointCount];
            for (int j = 0; j < jointCount; j++) {
                jointNodeIndices[j] = allNodes.indexOf(joints.get(j));
            }

            // Extract inverse bind matrices (one per joint, 4x4 column-major)
            Matrix4f[] inverseBindMatrices = new Matrix4f[jointCount];
            for (int j = 0; j < jointCount; j++) {
                float[] raw = skin.getInverseBindMatrix(j, null);
                Matrix4f ibm = new Matrix4f();
                ibm.set(raw);
                inverseBindMatrices[j] = ibm;
            }

            skins.add(new GltfSkinData(skin.getName(), jointNodeIndices, inverseBindMatrices));
        }
        return skins;
    }

    /**
     * Extracts all animation clips from the glTF model.
     *
     * <p>Each animation contains one or more channels. A channel binds a sampler (keyframe
     * data + interpolation method) to a target node property. The sampler's input accessor
     * provides keyframe times, and the output accessor provides the corresponding values.
     *
     * <p><strong>Duration computation:</strong> The animation's total duration is the maximum
     * time value found in any channel's keyframe times. This is needed for looping/blending.
     *
     * @param model the parsed glTF model
     * @return list of extracted animation data (empty if no animations in the file)
     */
    private List<GltfAnimationData> extractAnimations(GltfModel model) {
        List<GltfAnimationData> animations = new ArrayList<>();
        List<NodeModel> allNodes = model.getNodeModels();

        for (AnimationModel anim : model.getAnimationModels()) {
            List<GltfAnimationData.ChannelData> channels = new ArrayList<>();
            float duration = 0f;

            for (AnimationModel.Channel channel : anim.getChannels()) {
                int targetNodeIndex = allNodes.indexOf(channel.getNodeModel());
                String path = channel.getPath();

                AnimationModel.Sampler sampler = channel.getSampler();
                String interpolation = sampler.getInterpolation().name();
                float[] keyframeTimes = readFloatAccessor(sampler.getInput());
                float[] keyframeValues = readFloatAccessor(sampler.getOutput());

                // Track the maximum keyframe time for overall animation duration
                if (keyframeTimes.length > 0) {
                    float lastTime = keyframeTimes[keyframeTimes.length - 1];
                    if (lastTime > duration) {
                        duration = lastTime;
                    }
                }

                channels.add(new GltfAnimationData.ChannelData(
                    targetNodeIndex, path, interpolation, keyframeTimes, keyframeValues));
            }

            animations.add(new GltfAnimationData(anim.getName(), duration, channels));
        }
        return animations;
    }
}
