package dev.nyx.utils.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.Arrays;

/**
 * Collects many solid rectangles and submits them as one gui element. Every {@link GuiGraphics#fill}
 * is its own element, which gets expensive with hundreds of particles; a batch is a single one.
 */
public final class QuadBatch {
    private float[] rects = new float[256 * 4];
    private int[] colors = new int[256];
    private int count;
    private float minX, minY, maxX, maxY;

    public void begin() {
        count = 0;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    public void add(float x0, float y0, float x1, float y1, int color) {
        if ((color >>> 24) == 0 || x1 <= x0 || y1 <= y0) return;
        if (count == colors.length) {
            rects = Arrays.copyOf(rects, rects.length * 2);
            colors = Arrays.copyOf(colors, colors.length * 2);
        }
        int i = count * 4;
        rects[i] = x0;
        rects[i + 1] = y0;
        rects[i + 2] = x1;
        rects[i + 3] = y1;
        colors[count++] = color;
        minX = Math.min(minX, x0);
        minY = Math.min(minY, y0);
        maxX = Math.max(maxX, x1);
        maxY = Math.max(maxY, y1);
    }

    /** Hands everything collected since {@link #begin()} to the gui renderer in one go. */
    public void submit(GuiGraphics g) {
        if (count == 0) return;
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle scissor = g.scissorStack.peek();
        ScreenRectangle area = new ScreenRectangle((int) Math.floor(minX), (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX) + 1, (int) Math.ceil(maxY - minY) + 1).transformMaxBounds(pose);
        ScreenRectangle bounds = scissor != null ? scissor.intersection(area) : area;
        // the gui renders later in the frame, so the state gets its own copy of the data
        g.guiRenderState.submitGuiElement(new State(Arrays.copyOf(rects, count * 4), Arrays.copyOf(colors, count),
                count, pose, scissor, bounds));
    }

    private record State(float[] rects, int[] colors, int count, Matrix3x2f pose,
                         @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int q = 0; q < count; q++) {
                int i = q * 4, color = colors[q];
                consumer.addVertexWith2DPose(pose, rects[i], rects[i + 1]).setColor(color);
                consumer.addVertexWith2DPose(pose, rects[i], rects[i + 3]).setColor(color);
                consumer.addVertexWith2DPose(pose, rects[i + 2], rects[i + 3]).setColor(color);
                consumer.addVertexWith2DPose(pose, rects[i + 2], rects[i + 1]).setColor(color);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
