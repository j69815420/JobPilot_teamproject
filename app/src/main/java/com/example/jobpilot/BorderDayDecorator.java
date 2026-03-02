package com.example.jobpilot;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.CalendarDay;

public class BorderDayDecorator implements DayViewDecorator {

    private final int strokeWidth;
    private final int strokeColor;
    private CalendarDay selectedDay;

    public BorderDayDecorator(int strokeWidth, int strokeColor) {
        this.strokeWidth = strokeWidth;
        this.strokeColor = strokeColor;
    }

    public void setSelectedDay(CalendarDay day) {
        selectedDay = day;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return selectedDay != null && day.equals(selectedDay);
    }

    @Override
    public void decorate(DayViewFacade view) {
        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.OVAL);
        border.setStroke(strokeWidth, strokeColor);
        border.setColor(Color.TRANSPARENT);  // 내부는 투명
        view.setSelectionDrawable(border);
    }
}
