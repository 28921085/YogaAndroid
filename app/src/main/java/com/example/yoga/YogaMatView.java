package com.example.yoga;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class YogaMatView extends View {

    private Paint paint;
    private Bitmap leftBitmap, rightBitmap;
    private float leftImageX, leftImageY, rightImageX, rightImageY;

    public YogaMatView(Context context) {
        super(context);
        init();
    }

    public YogaMatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public YogaMatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);

        // 加載兩張 PNG 圖片
        leftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.left_feet);
        rightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.right_feet);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 繪製綠色背景
        canvas.drawColor(Color.GREEN);

        // 旋轉 bitmap1 180 度
        Matrix matrix = new Matrix();
        matrix.postRotate(0);

        // 繪製 PNG 圖片，並調整大小和位置
        int width = 64; // 設定寬度
        int height = 100; // 設定高度

        Bitmap rotatedBitmap1 = Bitmap.createBitmap(leftBitmap, 0, 0, leftBitmap.getWidth(), leftBitmap.getHeight(), matrix, true);
        Bitmap scaledBitmap1 = Bitmap.createScaledBitmap(rotatedBitmap1, width, height, true);
        canvas.drawBitmap(scaledBitmap1, leftImageX, leftImageY, paint);

        Bitmap rotatedBitmap2 = Bitmap.createBitmap(rightBitmap, 0, 0, rightBitmap.getWidth(), rightBitmap.getHeight(), matrix, true);
        Bitmap scaledBitmap2 = Bitmap.createScaledBitmap(rotatedBitmap2, width, height, true);
        canvas.drawBitmap(scaledBitmap2, rightImageX, rightImageY, paint);

        // 繪製文字
        paint.setColor(Color.BLACK);
        paint.setTextSize(100);

//        String text1 = "F X: " + leftImageX / getWidth() + ", R: " + rightImageX / getWidth();
//        canvas.drawText(text1, 10, getHeight() - 30, paint);

//        String text2 = "SX: " + secondImageX + ", Y: " + secondImageY;
//        canvas.drawText(text2, 10, getHeight() - 10, paint);
    }

    // 新增方法：設定第一張圖片位置
    public void setLeftFeetPosition(float x, float y) {
        leftImageX = x * getWidth();
        leftImageY = (1-y) * getHeight();

        invalidate(); // 通知 View 重新繪製
    }

    // 新增方法：設定第二張圖片位置
    public void setRightFeetPosition(float x, float y) {
        rightImageX = x * getWidth();
        rightImageY = (1-y) * getHeight();

        invalidate(); // 通知 View 重新繪製
    }
}
