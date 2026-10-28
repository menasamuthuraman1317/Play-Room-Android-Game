package com.example.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public final class GuessPortraitDrawable extends Drawable {

    private final GuessCharacter character;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int opacity = 255;

    public GuessPortraitDrawable(GuessCharacter character) {
        if (character == null) {
            throw new IllegalArgumentException("Character is required.");
        }

        this.character = character;
    }

    @Override
    public void draw(Canvas canvas) {
        int saved = canvas.save();

        // Keep the portrait proportions on different card sizes.
        float scale = Math.min(
                getBounds().width() / 160f,
                getBounds().height() / 180f
        );

        canvas.translate(
                getBounds().left
                        + (getBounds().width() - 160f * scale) / 2f,
                getBounds().top
                        + (getBounds().height() - 180f * scale) / 2f
        );

        canvas.scale(scale, scale);

        int layer = canvas.saveLayerAlpha(
                0f, 0f, 160f, 180f, opacity
        );

        int skin = skinColor();
        int hair = hairColor();

        // Soft background.
        oval(canvas, 5, 5, 155, 175, Color.rgb(232, 244, 255));

        // Long hair sits behind the face and shoulders.
        if (character.hairStyle == GuessCharacter.HairStyle.LONG) {
            oval(canvas, 28, 20, 132, 169, hair);
        }

        // Shirt and neck.
        int[] shirtColors = {
                Color.rgb(37, 99, 235),
                Color.rgb(234, 88, 12),
                Color.rgb(147, 51, 234),
                Color.rgb(13, 148, 136),
                Color.rgb(219, 39, 119)
        };

        int shirtIndex = (character.id.hashCode() & 0x7fffffff)
                % shirtColors.length;

        oval(canvas, 22, 137, 138, 205, shirtColors[shirtIndex]);
        rectangle(canvas, 67, 119, 93, 151, skin);
        oval(canvas, 65, 137, 95, 157, skin);

        // Ears and face.
        oval(canvas, 29, 73, 49, 105, skin);
        oval(canvas, 111, 73, 131, 105, skin);

        oval(canvas, 37, 30, 123, 137, skin);
        outlineOval(canvas, 37, 30, 123, 137,
                Color.rgb(106, 69, 52), 1.5f);

        drawHair(canvas, hair);
        drawEyes(canvas);

        // Eyebrows.
        line(canvas, 49, 67, 67, 65, hair, 3f);
        line(canvas, 93, 65, 111, 67, hair, 3f);

        // Nose.
        line(canvas, 80, 86, 77, 102,
                Color.rgb(153, 101, 77), 2f);
        line(canvas, 77, 102, 84, 103,
                Color.rgb(153, 101, 77), 2f);

        if (character.hasBeard) {
            Path beard = new Path();
            beard.moveTo(42, 101);
            beard.quadTo(48, 143, 80, 145);
            beard.quadTo(112, 143, 118, 101);
            beard.lineTo(102, 111);
            beard.quadTo(80, 130, 58, 111);
            beard.close();

            fill(hair);
            canvas.drawPath(beard, paint);
        }

        // Smile remains visible above the beard.
        stroke(Color.rgb(116, 48, 48), 2.5f);
        canvas.drawArc(64, 104, 96, 120,
                15, 150, false, paint);

        if (character.hasMoustache) {
            oval(canvas, 59, 105, 82, 114, hair);
            oval(canvas, 78, 105, 101, 114, hair);
        }

        if (character.hasGlasses) {
            drawGlasses(canvas);
        }

        if (character.hasEarrings) {
            outlineOval(canvas, 29, 99, 41, 118,
                    Color.rgb(202, 138, 4), 3f);
            outlineOval(canvas, 119, 99, 131, 118,
                    Color.rgb(202, 138, 4), 3f);
        }

        if (character.hasHat) {
            // High crown leaves the hairline visible.
            fill(Color.rgb(245, 158, 11));
            canvas.drawRoundRect(
                    43, 4, 117, 34, 10, 10, paint
            );

            rectangle(canvas, 43, 24, 117, 31,
                    Color.rgb(180, 83, 9));

            fill(Color.rgb(245, 158, 11));
            canvas.drawRoundRect(
                    24, 30, 136, 39, 5, 5, paint
            );
        }

        canvas.restoreToCount(layer);
        canvas.restoreToCount(saved);
    }

    private void drawHair(Canvas canvas, int color) {
        switch (character.hairStyle) {
            case SHORT:
                Path shortHair = new Path();
                shortHair.moveTo(36, 69);
                shortHair.lineTo(38, 43);
                shortHair.quadTo(48, 17, 82, 22);
                shortHair.quadTo(119, 18, 125, 54);
                shortHair.lineTo(121, 68);
                shortHair.lineTo(105, 48);
                shortHair.lineTo(85, 55);
                shortHair.lineTo(72, 45);
                shortHair.lineTo(49, 59);
                shortHair.close();

                fill(color);
                canvas.drawPath(shortHair, paint);
                break;

            case LONG:
                oval(canvas, 33, 28, 54, 122, color);
                oval(canvas, 106, 28, 127, 122, color);

                Path fringe = new Path();
                fringe.moveTo(39, 62);
                fringe.quadTo(43, 20, 81, 23);
                fringe.quadTo(117, 21, 122, 61);
                fringe.quadTo(96, 55, 81, 39);
                fringe.quadTo(65, 57, 39, 62);
                fringe.close();

                fill(color);
                canvas.drawPath(fringe, paint);
                break;

            case CURLY:
                for (int i = 0; i < 7; i++) {
                    float x = 43 + i * 12;
                    float y = 38 + Math.abs(3 - i) * 4;
                    circle(canvas, x, y, 14, color);
                }

                circle(canvas, 37, 65, 11, color);
                circle(canvas, 123, 65, 11, color);
                break;

            case BALD:
                // No head hair.
                break;
        }
    }

    private void drawEyes(Canvas canvas) {
        int iris;

        switch (character.eyeColor) {
            case BLUE:
                iris = Color.rgb(37, 99, 235);
                break;
            case GREEN:
                iris = Color.rgb(22, 163, 74);
                break;
            default:
                iris = Color.rgb(146, 87, 40);
                break;
        }

        oval(canvas, 47, 73, 70, 92, Color.WHITE);
        oval(canvas, 90, 73, 113, 92, Color.WHITE);

        circle(canvas, 59, 82, 7, iris);
        circle(canvas, 101, 82, 7, iris);

        circle(canvas, 59, 82, 3, Color.rgb(23, 30, 42));
        circle(canvas, 101, 82, 3, Color.rgb(23, 30, 42));

        circle(canvas, 61, 79, 1.5f, Color.WHITE);
        circle(canvas, 103, 79, 1.5f, Color.WHITE);
    }

    private void drawGlasses(Canvas canvas) {
        int frame = Color.rgb(30, 41, 59);

        stroke(frame, 3f);
        canvas.drawRoundRect(43, 70, 74, 97, 7, 7, paint);
        canvas.drawRoundRect(86, 70, 117, 97, 7, 7, paint);

        line(canvas, 74, 80, 86, 80, frame, 3f);
        line(canvas, 34, 77, 43, 80, frame, 3f);
        line(canvas, 117, 80, 126, 77, frame, 3f);
    }

    private int skinColor() {
        switch (character.skinTone) {
            case LIGHT:
                return Color.rgb(255, 219, 190);
            case MEDIUM:
                return Color.rgb(204, 145, 103);
            case DARK:
                return Color.rgb(132, 83, 59);
            default:
                throw new IllegalStateException("Unknown skin tone.");
        }
    }

    private int hairColor() {
        switch (character.hairColor) {
            case BLACK:
                return Color.rgb(35, 35, 43);
            case BROWN:
                return Color.rgb(101, 59, 36);
            case BLONDE:
                return Color.rgb(229, 181, 65);
            case RED:
                return Color.rgb(184, 65, 35);
            case GREY:
                return Color.rgb(139, 148, 160);
            case NONE:
                // Used for eyebrows or facial hair on bald characters.
                return Color.rgb(75, 53, 42);
            default:
                throw new IllegalStateException("Unknown hair color.");
        }
    }

    private void fill(int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private void oval(
            Canvas canvas, float left, float top,
            float right, float bottom, int color
    ) {
        fill(color);
        canvas.drawOval(left, top, right, bottom, paint);
    }

    private void outlineOval(
            Canvas canvas, float left, float top,
            float right, float bottom, int color, float width
    ) {
        stroke(color, width);
        canvas.drawOval(left, top, right, bottom, paint);
    }

    private void rectangle(
            Canvas canvas, float left, float top,
            float right, float bottom, int color
    ) {
        fill(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private void circle(
            Canvas canvas, float x, float y,
            float radius, int color
    ) {
        fill(color);
        canvas.drawCircle(x, y, radius, paint);
    }

    private void line(
            Canvas canvas, float startX, float startY,
            float endX, float endY, int color, float width
    ) {
        stroke(color, width);
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        opacity = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}