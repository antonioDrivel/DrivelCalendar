package com.applikeysolutions.cosmocalendar.adapter.viewholder;

import android.util.Log;
import android.view.View;

import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.settings.appearance.AppearanceModel;
import com.applikeysolutions.cosmocalendar.view.CalendarView;
import com.applikeysolutions.customizablecalendar.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DayOfWeekHolder extends BaseDayHolder {

    private SimpleDateFormat mDayOfWeekFormatter;

    public DayOfWeekHolder(View itemView, CalendarView calendarView) {
        super(itemView, calendarView);
        tvDay = itemView.findViewById(R.id.tv_day_name);
        AppearanceModel.TextAttributes weekText = calendarView.getSettingsManager().appearanceModel.getWeekDayText();
        calendarView.getSettingsManager().applyAppearance(tvDay, weekText);

        String format = calendarView.daysOfWeekFormat;
        Log.e("DayOfWeekHolder", format);
        mDayOfWeekFormatter = new SimpleDateFormat(format, Locale.getDefault());
    }

    public void bind(Day day) {
        tvDay.setText(mDayOfWeekFormatter.format(day.getCalendar().getTime()));
        //tvDay.setTextColor(calendarView.getWeekDayTitleTextColor());
    }
}