package com.example.money_graph;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DonutChartView extends View {

    private List<SegmentData> segmentDataList = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float donutWidth;
    private float donutPieSkip;

    public static class SegmentData {
        public double value;    // Значение сегмента
        public int color;       // Цвет сегмента

        public SegmentData(double value, int color, float width) {
            this.value = value;
            this.color = color;
        }

        public SegmentData(double value, int color) {
            this(value, color, 40f);
        }

        public SegmentData(double value) {
            this(value, Color.GRAY, 40f);
        }
    }

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Double> values) {
        segmentDataList.clear();
        for (int i = 0; i < values.size(); i++) {
            float[] hsv = {360f * (float)i / values.size(), 1f, 1f};
            int color = Color.HSVToColor(hsv);
            segmentDataList.add(new SegmentData(values.get(i), color));
        }
        invalidate();
    }

    // Метод с кастомными цветами
    public void setData(List<Double> values, List<Integer> colors) {
        segmentDataList.clear();
        if (values.size() == colors.size()) {
            for (int i = 0; i < values.size(); i++) {
                segmentDataList.add(new SegmentData(values.get(i), colors.get(i)));
            }
        } else {
            setData(values);
        }
        invalidate();
    }

    public void setDonutWidth(float width) {
        this.donutWidth = width;
        invalidate();
    }
    public void setDonutPieSkip(float pieSkip) {
        this.donutPieSkip = pieSkip;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (segmentDataList.isEmpty()) return;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float minDimension = Math.min(getWidth(), getHeight());
        float min_percentage = (float)((donutWidth / (minDimension - donutWidth)) / (Math.PI * 2)) + 10f / 360f;

        Log.d("DRAW_SEGMENT", "Min percenrage: " + min_percentage);

        // Вычисляем общую сумму значений
        double total = 0;
        for (SegmentData data : segmentDataList) {
            total += data.value;
        }

        // Добавляем долю от "мёртвой зоны"
//        total *= 360f / (360f - 10f);

        // Расчитываем доли
        double [] segments_percentage = new double[segmentDataList.size()];
        int count_final_segments = 0;
        double other_segments_percentage = 0;
        int index_of_min_segments_with_valid_percentage = 0;
        for (int i = 0; i < segmentDataList.size(); i++) {
            segments_percentage[i] = segmentDataList.get(i).value / total;
            Log.d("DRAW_SEGMENT", "Pergentage (" + i + "): " + segments_percentage[i]);
            if (segments_percentage[i] > min_percentage) {
                count_final_segments++;
                index_of_min_segments_with_valid_percentage = (segments_percentage[i] < segments_percentage[index_of_min_segments_with_valid_percentage]) ? i : index_of_min_segments_with_valid_percentage;
            } else {
                other_segments_percentage += segments_percentage[i];
                segments_percentage[i] = -1;
            }
        }

        if (other_segments_percentage < min_percentage) {
            double delta_percentage = (min_percentage - other_segments_percentage) / count_final_segments;
            if (segments_percentage[index_of_min_segments_with_valid_percentage] - delta_percentage < min_percentage) {
                other_segments_percentage += segments_percentage[index_of_min_segments_with_valid_percentage];
                segments_percentage[index_of_min_segments_with_valid_percentage] = -1;
            } else {
                for (double seg : segments_percentage) {
                    seg -= delta_percentage;
                }
            }
        }

        // Рисуем сегменты
        float startAngle = -90f + donutPieSkip / 2f;
        for (int i = 0; i < segmentDataList.size(); i++) {
            if (segments_percentage[i] > 0) {
                float sweepAngle = (float) ((360f - donutPieSkip) * segments_percentage[i]) - donutPieSkip;

                // Рисуем сегмент
                drawSegment(canvas, centerX, centerY, minDimension,
                        startAngle, sweepAngle, segmentDataList.get(i).color);

                startAngle += sweepAngle + donutPieSkip;
            }
        }
        drawSegment(canvas, centerX, centerY, minDimension,
                startAngle, (float) ((360f - donutPieSkip) * other_segments_percentage), Color.GRAY);
    }

    private void drawSegment(Canvas canvas, float centerX, float centerY, float size,
                             float startAngle, float sweepAngle, int color) {

        Log.d("DRAW_SEGMENT", "Start angle: " + startAngle + ", sweep angle: " + sweepAngle);

        // ============================================================ //

        // Вычисляем радиусы
        float outerRadius = size / 2f;
        float innerRadius = outerRadius - donutWidth;

        // Преобразуем углы старта и размаха в радианы
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(startAngle + sweepAngle);

        // Определяем отклонения для дуг
        double outerDeltaRad = donutWidth / outerRadius / 2f;
        double innerDeltaRad = donutWidth / innerRadius / 2f;

        // Преобразуем радианы дельт в углы
        double outerDeltaAngle = Math.toDegrees(outerDeltaRad);
        double innerDeltaAngle = Math.toDegrees(innerDeltaRad);

        // Вычисляем точки
        float dotRefRightUpX = centerX + outerRadius * (float)Math.cos(endRad);
        float dotRefRightUpY = centerY + outerRadius * (float)Math.sin(endRad);

        float dotRefRightDownX = centerX + innerRadius * (float)Math.cos(endRad);
        float dotRefRightDownY = centerY + innerRadius * (float)Math.sin(endRad);

        float dotRefLeftUpX = centerX + outerRadius * (float)Math.cos(startRad);
        float dotRefLeftUpY = centerY + outerRadius * (float)Math.sin(startRad);

        float dotRefLeftDownX = centerX + innerRadius * (float)Math.cos(startRad);
        float dotRefLeftDownY = centerY + innerRadius * (float)Math.sin(startRad);

        float stickRightUpX = centerX + (outerRadius - donutWidth / 2f) * (float)Math.cos(endRad);
        float stickRightUpY = centerY + (outerRadius - donutWidth / 2f) * (float)Math.sin(endRad);
        float stickRightDownX = centerX + (innerRadius + donutWidth / 2f) * (float)Math.cos(endRad);
        float stickRightDownY = centerY + (innerRadius + donutWidth / 2f) * (float)Math.sin(endRad);

        float stickLeftUpX = centerX + (outerRadius - donutWidth / 2f) * (float)Math.cos(startRad);
        float stickLeftUpY = centerY + (outerRadius - donutWidth / 2f) * (float)Math.sin(startRad);
        float stickLeftDownX = centerX + (innerRadius + donutWidth / 2f) * (float)Math.cos(startRad);
        float stickLeftDownY = centerY + (innerRadius + donutWidth / 2f) * (float)Math.sin(startRad);

        // ============================================================ //

        Path path = new Path();

        // Старт
        // Вычисляем стартовую точку
        float startPathX = (float)Math.cos(startRad + outerDeltaRad) * outerRadius + centerX;
        float startPathY = (float)Math.sin(startRad + outerDeltaRad) * outerRadius + centerY;

        path.moveTo(startPathX, startPathY);

        // Внешняя дуга
        // Большой прямоугольник
        RectF outerRect = new RectF(
                centerX - outerRadius,
                centerY - outerRadius,
                centerX + outerRadius,
                centerY + outerRadius
        );

        path.arcTo(outerRect, (float)(startAngle + outerDeltaAngle), (float)(sweepAngle - outerDeltaAngle * 2));

        // Внешнее правое скругление
        path.quadTo(dotRefRightUpX, dotRefRightUpY, stickRightUpX, stickRightUpY);

        // Правая радиальная прямая
        path.lineTo(stickRightDownX, stickRightDownY);

        // Внутреннее правое скругление
        // вычисляем конечную точку
        float endQuadX = (float)Math.cos(endRad - innerDeltaRad) * innerRadius + centerX;
        float endQuadY = (float)Math.sin(endRad - innerDeltaRad) * innerRadius + centerY;

        path.quadTo(dotRefRightDownX, dotRefRightDownY, endQuadX, endQuadY);

        // Внутренняя дуга
        // Маленький прямоугольник
        RectF innerRect = new RectF(
                centerX - innerRadius,
                centerY - innerRadius,
                centerX + innerRadius,
                centerY + innerRadius
        );

        path.arcTo(innerRect, (float)(startAngle + sweepAngle - outerDeltaAngle), (float)(outerDeltaAngle * 2 - sweepAngle));

        // Внутреннее левое скругление
        path.quadTo(dotRefLeftDownX, dotRefLeftDownY, stickLeftDownX, stickLeftDownY);

        // Левая радиальная прямая
        path.lineTo(stickLeftUpX, stickLeftUpY);

        // Внешнее левое скругление
        path.quadTo(dotRefLeftUpX, dotRefLeftUpY, startPathX, startPathY);

        path.close();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }
}