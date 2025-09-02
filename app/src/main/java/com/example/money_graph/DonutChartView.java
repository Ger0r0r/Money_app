package com.example.money_graph;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DonutChartView extends View {

    private List<SegmentData> segmentDataList = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Random random = new Random();

    // Настройки по умолчанию
    private float donutWidth = 40f;
    private int[] defaultColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.GRAY, Color.rgb(255, 165, 0)
    };

    public static class SegmentData {
        public double value;    // Значение сегмента
        public int color;       // Цвет сегмента
        public float width;     // Ширина сегмента

        public SegmentData(double value, int color, float width) {
            this.value = value;
            this.color = color;
            this.width = width;
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

    // Основной метод для установки данных
    public void setData(List<Double> values) {
        segmentDataList.clear();
        for (int i = 0; i < values.size(); i++) {
            int color = defaultColors[i % defaultColors.length];
            segmentDataList.add(new SegmentData(values.get(i), color, donutWidth));
        }
        invalidate();
    }

    // Метод с кастомными цветами
    public void setData(List<Double> values, List<Integer> colors) {
        segmentDataList.clear();
        for (int i = 0; i < values.size(); i++) {
            int color = i < colors.size() ? colors.get(i) : defaultColors[i % defaultColors.length];
            segmentDataList.add(new SegmentData(values.get(i), color, donutWidth));
        }
        invalidate();
    }

    // Метод с кастомными цветами и ширинами
    public void setData(List<Double> values, List<Integer> colors, List<Float> widths) {
        segmentDataList.clear();
        for (int i = 0; i < values.size(); i++) {
            int color = i < colors.size() ? colors.get(i) : defaultColors[i % defaultColors.length];
            float width = i < widths.size() ? widths.get(i) : donutWidth;
            segmentDataList.add(new SegmentData(values.get(i), color, width));
        }
        invalidate();
    }

    // Метод для ручной установки SegmentData
    public void setSegmentData(List<SegmentData> data) {
        segmentDataList = new ArrayList<>(data);
        invalidate();
    }

    // Установка ширины donut
    public void setDonutWidth(float width) {
        this.donutWidth = width;
        invalidate();
    }

    // Генерация случайного цвета
    private int generateRandomColor() {
        return Color.rgb(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (segmentDataList.isEmpty()) return;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float minDimension = Math.min(getWidth(), getHeight());
        float min_percentage = (float)((donutWidth / (minDimension - donutWidth)) / (Math.PI * 2));

        // Вычисляем общую сумму значений
        double total = 0;
        for (SegmentData data : segmentDataList) {
            total += data.value;
        }

        double [] segments_percentage = new double[segmentDataList.size()];


        // Рисуем сегменты
        float startAngle = 0f;
        for (int i = 0; i < segmentDataList.size(); i++) {
            SegmentData data = segmentDataList.get(i);

            // Рассчитываем угол для этого сегмента
            float sweepAngle = (float) (360f * (data.value / total)) - 5;

            // Рисуем сегмент
            drawSegment(canvas, centerX, centerY, minDimension,
                    startAngle, sweepAngle, data.color, data.width, data.width / 2f);

            startAngle += sweepAngle + 5;
        }
    }

    private void drawSegment(Canvas canvas, float centerX, float centerY, float size,
                             float startAngle, float sweepAngle, int color, float width, float curvness) {

        // ============================================================ //

        // Вычисляем радиусы
        float outerRadius = size / 2f;
        float innerRadius = outerRadius - width;

        // Преобразуем углы старта и размаха в радианы
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(startAngle + sweepAngle);

        // Определяем отклонения для дуг
        double outerDeltaRad = curvness / outerRadius;
        double innerDeltaRad = curvness / innerRadius;

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

        float stickRightUpX = centerX + (outerRadius - curvness) * (float)Math.cos(endRad);
        float stickRightUpY = centerY + (outerRadius - curvness) * (float)Math.sin(endRad);
        float stickRightDownX = centerX + (innerRadius + curvness) * (float)Math.cos(endRad);
        float stickRightDownY = centerY + (innerRadius + curvness) * (float)Math.sin(endRad);

        float stickLeftUpX = centerX + (outerRadius - curvness) * (float)Math.cos(startRad);
        float stickLeftUpY = centerY + (outerRadius - curvness) * (float)Math.sin(startRad);
        float stickLeftDownX = centerX + (innerRadius + curvness) * (float)Math.cos(startRad);
        float stickLeftDownY = centerY + (innerRadius + curvness) * (float)Math.sin(startRad);

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