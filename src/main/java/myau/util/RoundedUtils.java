package myau.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class RoundedUtils {

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {

        radius = Math.min(radius, Math.min(width / 2f, height / 2f)); // prevent broken radius

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8  & 255) / 255f;
        float b = (color       & 255) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );

        GL11.glColor4f(r, g, b, a);

        // Center rectangles
        drawQuad(x + radius, y, x + width - radius, y + height);
        drawQuad(x, y + radius, x + radius, y + height - radius);
        drawQuad(x + width - radius, y + radius, x + width, y + height - radius);

        // Corners
        drawCorner(x + radius, y + radius, radius, 180, 270);
        drawCorner(x + width - radius, y + radius, radius, 270, 360);
        drawCorner(x + radius, y + height - radius, radius, 90, 180);
        drawCorner(x + width - radius, y + height - radius, radius, 0, 90);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GlStateManager.popMatrix();
    }

    private static void drawQuad(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private static void drawCorner(float cx, float cy, float radius, int start, int end) {

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);

        for (int i = start; i <= end; i += 3) { // smoother corners
            double rad = Math.toRadians(i);
            GL11.glVertex2f(
                    cx + (float) Math.cos(rad) * radius,
                    cy + (float) Math.sin(rad) * radius
            );
        }

        GL11.glEnd();
    }

    public static void drawOutlineRect(float x, float y, float width, float height, float lineWidth, int color) {

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8  & 255) / 255f;
        float b = (color       & 255) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(lineWidth);

        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}