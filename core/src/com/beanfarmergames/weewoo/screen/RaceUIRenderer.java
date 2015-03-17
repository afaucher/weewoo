package com.beanfarmergames.weewoo.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;

public class RaceUIRenderer {

    public static void drawGuage(ShapeRenderer renderer, Vector2 pos, float target, float actual, float multiplier) {

        int guageWidth = 10;
        int guageHeight = 80;

        renderer.begin(ShapeType.Filled);

        Color actualColor = Color.RED.cpy().lerp(Color.BLUE, actual);
        Color targetColor = Color.RED.cpy().lerp(Color.BLUE, target);

        // The 'offset' from the target
        if (actual < target) {
            /**
             * [actual][error] [target ]
             */
            renderer.setColor(Color.YELLOW);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * target);

            renderer.setColor(actualColor);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * actual);
        } else {

            /**
             * [actual ][error] [target ]
             */
            renderer.setColor(Color.YELLOW);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * actual);

            renderer.setColor(actualColor);
            renderer.rect(pos.x, pos.y, guageWidth, guageHeight * target);
        }

        renderer.setColor(targetColor);
        renderer.rect(pos.x + guageWidth, pos.x, guageWidth, guageHeight * target);

        //Multiplier
        renderer.setColor(Color.ORANGE);
        renderer.rect(pos.x + guageWidth * 3, pos.x, guageWidth, guageHeight * multiplier);

        renderer.setColor(Color.BLACK);

        // Guage Lines
        final int guageLineWidth = 5;
        final int guageHorizLineCount = 4;
        final int guageHorizLineOvershoot = 2;

        for (int i = 0; i < guageHorizLineCount; i++) {
            float ratio = (float) i / (guageHorizLineCount - 1);
            renderer.rectLine(pos.x - guageHorizLineOvershoot, pos.y + guageHeight * ratio, pos.x + guageWidth * 2
                    + guageHorizLineOvershoot, pos.y + guageHeight * ratio, guageLineWidth);
        }
        // Vert
        renderer.rectLine(pos.x + guageWidth, pos.y, pos.x + guageWidth, pos.y + guageHeight, guageLineWidth);

        renderer.end();

    }

}
