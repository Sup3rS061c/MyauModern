package myau.module.modules.chatting.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * GUI 绘制辅助类 - 支持圆角矩形等高级效果
 */
public class GuiHelper {

    /**
     * 绘制圆角矩形
     * @param x 左上角X
     * @param y 左上角Y
     * @param x2 右下角X
     * @param y2 右下角Y
     * @param radius 圆角半径
     * @param color 颜色
     */
    public static void drawRoundedRect(int x, int y, int x2, int y2, int radius, int color) {
        if (radius < 1) {
            Gui.drawRect(x, y, x2, y2, color);
            return;
        }

        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // 限制半径不超过矩形尺寸的一半
        radius = Math.min(radius, Math.min((x2 - x) / 2, (y2 - y) / 2));

        // 绘制主体矩形（不含圆角部分）
        worldrenderer.pos(x + radius, y + radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + radius, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2 - radius, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2 - radius, y + radius, 0).color(red, green, blue, alpha).endVertex();

        // 左侧矩形
        worldrenderer.pos(x, y + radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + radius, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + radius, y + radius, 0).color(red, green, blue, alpha).endVertex();

        // 右侧矩形
        worldrenderer.pos(x2 - radius, y + radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2 - radius, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2, y2 - radius, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2, y + radius, 0).color(red, green, blue, alpha).endVertex();

        // 中心矩形
        worldrenderer.pos(x + radius, y, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x + radius, y2, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2 - radius, y2, 0).color(red, green, blue, alpha).endVertex();
        worldrenderer.pos(x2 - radius, y, 0).color(red, green, blue, alpha).endVertex();

        tessellator.draw();

        // 绘制四个圆角
        drawCircleCorner(worldrenderer, x + radius, y + radius, radius, 180, 90, red, green, blue, alpha);
        drawCircleCorner(worldrenderer, x2 - radius, y + radius, radius, 270, 90, red, green, blue, alpha);
        drawCircleCorner(worldrenderer, x + radius, y2 - radius, radius, 90, 90, red, green, blue, alpha);
        drawCircleCorner(worldrenderer, x2 - radius, y2 - radius, radius, 0, 90, red, green, blue, alpha);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * 绘制圆角（扇形）
     */
    private static void drawCircleCorner(WorldRenderer renderer, int cx, int cy, int r, int startAngle, int sweep, int red, int green, int blue, int alpha) {
        int segments = Math.max(4, (int) (sweep / 5));
        double angleStep = Math.toRadians(sweep) / segments;
        double startRad = Math.toRadians(startAngle);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(6, DefaultVertexFormats.POSITION_COLOR);

        // 中心点
        worldrenderer.pos(cx, cy, 0).color(red, green, blue, alpha).endVertex();

        for (int i = 0; i <= segments; i++) {
            double angle = startRad + angleStep * i;
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            worldrenderer.pos(x, y, 0).color(red, green, blue, alpha).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 绘制带边框的圆角矩形
     */
    public static void drawRoundedRectWithBorder(int x, int y, int x2, int y2, int radius, int fillColor, int borderColor, int borderWidth) {
        // 先绘制外框（稍大的矩形）
        drawRoundedRect(x - borderWidth, y - borderWidth, x2 + borderWidth, y2 + borderWidth, radius + borderWidth, borderColor);
        // 再绘制内部填充
        drawRoundedRect(x, y, x2, y2, radius, fillColor);
    }
}
