package org.foundation101.karatel.utils;

import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

/**
 * Created by Dima on 03.12.2017.
 */

public class DrawingUtils {

    public PaintDrawable fourSidesGradient() {
        ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
            final int TRANSPARENT_WHITE = 0x00FFFFFF;
            final int TRANSPARENT90_WHITE = 0xE6FFFFFF;
            @Override
            public Shader resize(int width, int height) {
                LinearGradient lgHorizontal = new LinearGradient(0, 0, width, 0,
                        new int[]{TRANSPARENT_WHITE, TRANSPARENT90_WHITE, TRANSPARENT90_WHITE, TRANSPARENT_WHITE},
                        new float[]{0, 0.2f, 0.8f, 1}, Shader.TileMode.CLAMP);
                LinearGradient lgVertical = new LinearGradient(0, 0, 0, height,
                        new int[]{TRANSPARENT_WHITE, TRANSPARENT90_WHITE, TRANSPARENT90_WHITE, TRANSPARENT_WHITE},
                        new float[]{0, 0.2f, 0.8f, 1}, Shader.TileMode.CLAMP);
                ComposeShader cs = new ComposeShader(lgHorizontal, lgVertical, PorterDuff.Mode.MULTIPLY);
                return cs;
            }
        };

        PaintDrawable p = new PaintDrawable();
        p.setShape(new RectShape());
        p.setShaderFactory(sf);

        return p;
    }
}
