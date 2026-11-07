package dev.nyx.utils.features.modules.render.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nyx.utils.util.traits.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A static glTF binary (.glb) model: triangles, normals, UVs and its base colour texture.
 * The mesh is normalised to stand on y = 0, centred on x/z and exactly 1 unit tall,
 * so the renderer only has to scale it to the entity's height.
 */
public final class GlbModel implements Util {
    public final String name;
    private final float[] positions; // xyz per vertex
    private final float[] normals;   // xyz per vertex
    private final float[] uvs;       // uv per vertex
    private final int[] indices;     // 3 per triangle
    private final int color;
    private final NativeImage image;
    private Identifier texture;

    private GlbModel(String name, float[] positions, float[] normals, float[] uvs, int[] indices, int color, NativeImage image) {
        this.name = name;
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.indices = indices;
        this.color = color;
        this.image = image;
    }

    public int triangles() {
        return indices.length / 3;
    }

    /** The model's texture, uploaded to the GPU on first use (must be called on the render thread). */
    public Identifier texture() {
        if (texture == null) {
            String id = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            texture = Identifier.fromNamespaceAndPath("nyxutils", "custom_model/" + id);
            NativeImage pixels = image;
            if (pixels == null) {
                pixels = new NativeImage(1, 1, false);
                pixels.setPixel(0, 0, 0xFFFFFFFF);
            }
            mc.getTextureManager().register(texture, new DynamicTexture(() -> "nyx custom model " + name, pixels));
        }
        return texture;
    }

    /** Emits the whole mesh. Triangles go out as quads with a repeated last corner. */
    public void emit(PoseStack.Pose pose, VertexConsumer consumer, int light) {
        for (int t = 0; t < indices.length; t += 3) {
            vertex(pose, consumer, indices[t], light);
            vertex(pose, consumer, indices[t + 1], light);
            vertex(pose, consumer, indices[t + 2], light);
            vertex(pose, consumer, indices[t + 2], light);
        }
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer consumer, int i, int light) {
        consumer.addVertex(pose, positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2])
                .setColor(color)
                .setUv(uvs[i * 2], uvs[i * 2 + 1])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
    }

    // ------------------------------------------------------------- loading

    public static GlbModel load(String name, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (data.length < 20 || buffer.getInt(0) != 0x46546C67) throw new IOException("not a .glb file");
        int length = Math.min(buffer.getInt(8), data.length);

        JsonObject json = null;
        ByteBuffer bin = null;
        int offset = 12;
        while (offset + 8 <= length) {
            int chunkLength = buffer.getInt(offset);
            int chunkType = buffer.getInt(offset + 4);
            if (chunkType == 0x4E4F534A) {
                json = JsonParser.parseString(new String(data, offset + 8, chunkLength, StandardCharsets.UTF_8)).getAsJsonObject();
            } else if (chunkType == 0x004E4942) {
                bin = ByteBuffer.wrap(data, offset + 8, chunkLength).slice().order(ByteOrder.LITTLE_ENDIAN);
            }
            offset += 8 + chunkLength;
        }
        if (json == null || bin == null) throw new IOException("missing JSON or BIN chunk");
        return new Parser(json, bin).build(name);
    }

    private static final class Parser {
        private final JsonObject json;
        private final ByteBuffer bin;
        private final List<Float> positions = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();
        private final List<Float> uvs = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
        private int materialIndex = -1;

        Parser(JsonObject json, ByteBuffer bin) {
            this.json = json;
            this.bin = bin;
        }

        GlbModel build(String name) throws IOException {
            JsonArray nodes = json.getAsJsonArray("nodes");
            int sceneIndex = json.has("scene") ? json.get("scene").getAsInt() : 0;
            JsonArray roots = json.getAsJsonArray("scenes").get(sceneIndex).getAsJsonObject().getAsJsonArray("nodes");
            for (JsonElement root : roots) visit(nodes, root.getAsInt(), new Matrix4f());
            if (indices.isEmpty()) throw new IOException("no triangles");

            float[] p = toArray(positions);
            normalise(p);
            int[] idx = indices.stream().mapToInt(Integer::intValue).toArray();
            return new GlbModel(name, p, toArray(normals), toArray(uvs), idx, baseColor(), texture());
        }

        private void visit(JsonArray nodes, int index, Matrix4f parent) {
            JsonObject node = nodes.get(index).getAsJsonObject();
            Matrix4f world = new Matrix4f(parent).mul(local(node));
            if (node.has("mesh")) addMesh(json.getAsJsonArray("meshes").get(node.get("mesh").getAsInt()).getAsJsonObject(), world);
            if (node.has("children")) {
                for (JsonElement child : node.getAsJsonArray("children")) visit(nodes, child.getAsInt(), world);
            }
        }

        private static Matrix4f local(JsonObject node) {
            Matrix4f m = new Matrix4f();
            if (node.has("matrix")) {
                float[] v = floats(node.getAsJsonArray("matrix"));
                m.set(v); // glTF matrices are column-major, like JOML's set(float[])
                return m;
            }
            if (node.has("translation")) {
                float[] t = floats(node.getAsJsonArray("translation"));
                m.translate(t[0], t[1], t[2]);
            }
            if (node.has("rotation")) {
                float[] r = floats(node.getAsJsonArray("rotation"));
                m.rotate(new Quaternionf(r[0], r[1], r[2], r[3]));
            }
            if (node.has("scale")) {
                float[] s = floats(node.getAsJsonArray("scale"));
                m.scale(s[0], s[1], s[2]);
            }
            return m;
        }

        private void addMesh(JsonObject mesh, Matrix4f world) {
            Matrix3f normalMatrix = new Matrix3f(world).invert().transpose();
            for (JsonElement element : mesh.getAsJsonArray("primitives")) {
                JsonObject primitive = element.getAsJsonObject();
                if (primitive.has("mode") && primitive.get("mode").getAsInt() != 4) continue; // triangles only
                JsonObject attributes = primitive.getAsJsonObject("attributes");
                if (!attributes.has("POSITION")) continue;
                if (materialIndex < 0 && primitive.has("material")) materialIndex = primitive.get("material").getAsInt();

                float[] pos = readFloats(attributes.get("POSITION").getAsInt());
                int count = pos.length / 3;
                float[] nor = attributes.has("NORMAL") ? readFloats(attributes.get("NORMAL").getAsInt()) : null;
                float[] uv = attributes.has("TEXCOORD_0") ? readFloats(attributes.get("TEXCOORD_0").getAsInt()) : null;
                int base = positions.size() / 3;

                Vector3f v = new Vector3f();
                for (int i = 0; i < count; i++) {
                    world.transformPosition(v.set(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]));
                    positions.add(v.x);
                    positions.add(v.y);
                    positions.add(v.z);
                    if (nor != null) {
                        normalMatrix.transform(v.set(nor[i * 3], nor[i * 3 + 1], nor[i * 3 + 2])).normalize();
                    } else {
                        v.set(0, 1, 0);
                    }
                    normals.add(v.x);
                    normals.add(v.y);
                    normals.add(v.z);
                    uvs.add(uv != null ? uv[i * 2] : 0);
                    uvs.add(uv != null ? uv[i * 2 + 1] : 0);
                }
                if (primitive.has("indices")) {
                    for (int i : readInts(primitive.get("indices").getAsInt())) indices.add(base + i);
                } else {
                    for (int i = 0; i < count; i++) indices.add(base + i);
                }
            }
        }

        private JsonObject accessor(int index) {
            return json.getAsJsonArray("accessors").get(index).getAsJsonObject();
        }

        private ByteBuffer view(JsonObject accessor) {
            JsonObject view = json.getAsJsonArray("bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            int start = (view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0)
                    + (accessor.has("byteOffset") ? accessor.get("byteOffset").getAsInt() : 0);
            ByteBuffer slice = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            slice.position(start);
            return slice.slice().order(ByteOrder.LITTLE_ENDIAN);
        }

        private int stride(JsonObject accessor, int naturalSize) {
            JsonObject view = json.getAsJsonArray("bufferViews").get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
            return view.has("byteStride") ? view.get("byteStride").getAsInt() : naturalSize;
        }

        private static int components(String type) {
            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> 16;
            };
        }

        private float[] readFloats(int index) {
            JsonObject accessor = accessor(index);
            int count = accessor.get("count").getAsInt();
            int n = components(accessor.get("type").getAsString());
            int type = accessor.get("componentType").getAsInt();
            boolean normalized = accessor.has("normalized") && accessor.get("normalized").getAsBoolean();
            int size = type == 5126 ? 4 : type == 5123 || type == 5122 ? 2 : 1;
            ByteBuffer data = view(accessor);
            int stride = stride(accessor, n * size);
            float[] out = new float[count * n];
            for (int i = 0; i < count; i++) {
                for (int c = 0; c < n; c++) {
                    int at = i * stride + c * size;
                    out[i * n + c] = switch (type) {
                        case 5126 -> data.getFloat(at);
                        case 5123 -> (data.getShort(at) & 0xFFFF) / (normalized ? 65535f : 1f);
                        case 5122 -> data.getShort(at) / (normalized ? 32767f : 1f);
                        case 5121 -> (data.get(at) & 0xFF) / (normalized ? 255f : 1f);
                        default -> data.get(at) / (normalized ? 127f : 1f);
                    };
                }
            }
            return out;
        }

        private int[] readInts(int index) {
            JsonObject accessor = accessor(index);
            int count = accessor.get("count").getAsInt();
            int type = accessor.get("componentType").getAsInt();
            int size = type == 5125 ? 4 : type == 5123 ? 2 : 1;
            ByteBuffer data = view(accessor);
            int stride = stride(accessor, size);
            int[] out = new int[count];
            for (int i = 0; i < count; i++) {
                int at = i * stride;
                out[i] = type == 5125 ? data.getInt(at) : type == 5123 ? data.getShort(at) & 0xFFFF : data.get(at) & 0xFF;
            }
            return out;
        }

        private JsonObject material() {
            if (materialIndex < 0 || !json.has("materials")) return null;
            return json.getAsJsonArray("materials").get(materialIndex).getAsJsonObject();
        }

        private int baseColor() {
            JsonObject material = material();
            if (material == null || !material.has("pbrMetallicRoughness")) return 0xFFFFFFFF;
            JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
            if (!pbr.has("baseColorFactor")) return 0xFFFFFFFF;
            float[] f = floats(pbr.getAsJsonArray("baseColorFactor"));
            return (Math.round(f[3] * 255) << 24) | (Math.round(f[0] * 255) << 16) | (Math.round(f[1] * 255) << 8) | Math.round(f[2] * 255);
        }

        private NativeImage texture() {
            try {
                JsonObject material = material();
                if (material == null || !material.has("pbrMetallicRoughness")) return null;
                JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
                if (!pbr.has("baseColorTexture")) return null;
                int textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
                JsonObject texture = json.getAsJsonArray("textures").get(textureIndex).getAsJsonObject();
                JsonObject image = json.getAsJsonArray("images").get(texture.get("source").getAsInt()).getAsJsonObject();
                if (!image.has("bufferView")) return null;
                JsonObject view = json.getAsJsonArray("bufferViews").get(image.get("bufferView").getAsInt()).getAsJsonObject();
                int start = view.has("byteOffset") ? view.get("byteOffset").getAsInt() : 0;
                byte[] bytes = new byte[view.get("byteLength").getAsInt()];
                bin.duplicate().position(start).get(bytes);
                // NativeImage only reads PNG; JPEG textures fall back to the base colour
                return NativeImage.read(bytes);
            } catch (Exception e) {
                return null;
            }
        }

        private void normalise(float[] p) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (int i = 0; i < p.length; i += 3) {
                minX = Math.min(minX, p[i]);
                maxX = Math.max(maxX, p[i]);
                minY = Math.min(minY, p[i + 1]);
                maxY = Math.max(maxY, p[i + 1]);
                minZ = Math.min(minZ, p[i + 2]);
                maxZ = Math.max(maxZ, p[i + 2]);
            }
            float height = Math.max(1e-4f, maxY - minY);
            float cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
            for (int i = 0; i < p.length; i += 3) {
                p[i] = (p[i] - cx) / height;
                p[i + 1] = (p[i + 1] - minY) / height;
                p[i + 2] = (p[i + 2] - cz) / height;
            }
        }

        private static float[] floats(JsonArray array) {
            float[] out = new float[array.size()];
            for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
            return out;
        }

        private static float[] toArray(List<Float> list) {
            float[] out = new float[list.size()];
            for (int i = 0; i < out.length; i++) out[i] = list.get(i);
            return out;
        }
    }
}
