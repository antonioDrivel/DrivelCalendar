package com.applikeysolutions.cosmocalendar.settings;

import android.support.v7.widget.LinearLayoutManager;
import android.util.TypedValue;
import android.widget.TextView;

import com.applikeysolutions.cosmocalendar.settings.appearance.AppearanceModel;
import com.applikeysolutions.cosmocalendar.settings.appearance.ConnectedDayIconPosition;
import com.applikeysolutions.cosmocalendar.settings.date.DateInterface;
import com.applikeysolutions.cosmocalendar.settings.date.DateModel;
import com.applikeysolutions.cosmocalendar.settings.lists.CalendarListsInterface;
import com.applikeysolutions.cosmocalendar.settings.lists.CalendarListsModel;
import com.applikeysolutions.cosmocalendar.settings.lists.DisabledDaysCriteria;
import com.applikeysolutions.cosmocalendar.settings.lists.connected_days.ConnectedDays;
import com.applikeysolutions.cosmocalendar.settings.lists.connected_days.ConnectedDaysManager;
import com.applikeysolutions.cosmocalendar.settings.selection.SelectionInterface;
import com.applikeysolutions.cosmocalendar.settings.selection.SelectionModel;
import com.applikeysolutions.cosmocalendar.utils.SelectionType;

import java.util.Calendar;
import java.util.Set;

public class SettingsManager implements DateInterface, CalendarListsInterface, SelectionInterface {

    //Default values
    public static final int DEFAULT_MONTH_COUNT = 20;
    public static final int DEFAULT_SELECTION_TYPE = SelectionType.SINGLE;
    public static final int DEFAULT_FIRST_DAY_OF_WEEK = Calendar.MONDAY;
    public static final int DEFAULT_ORIENTATION = LinearLayoutManager.VERTICAL;
    public static final int DEFAULT_CONNECTED_DAY_ICON_POSITION = ConnectedDayIconPosition.BOTTOM;

    //Models
    public AppearanceModel appearanceModel;
    private DateModel dateModel;
    private CalendarListsModel calendarListsModel;
    private SelectionModel selectionModel;

    public SettingsManager() {
        appearanceModel = new AppearanceModel();
        dateModel = new DateModel();
        calendarListsModel = new CalendarListsModel();
        selectionModel = new SelectionModel();
    }

    @Override
    @SelectionType
    public int getSelectionType() {
        return selectionModel.getSelectionType();
    }

    @Override
    public void setSelectionType(@SelectionType int selectionType) {
        selectionModel.setSelectionType(selectionType);
    }

    @Override
    public Set<Long> getDisabledDays() {
        return calendarListsModel.getDisabledDays();
    }

    @Override
    public ConnectedDaysManager getConnectedDaysManager() {
        return calendarListsModel.getConnectedDaysManager();
    }

    @Override
    public Set<Long> getWeekendDays() {
        return calendarListsModel.getWeekendDays();
    }

    @Override
    public DisabledDaysCriteria getDisabledDaysCriteria() {
        return calendarListsModel.getDisabledDaysCriteria();
    }

    @Override
    public void setDisabledDays(Set<Long> disabledDays) {
        calendarListsModel.setDisabledDays(disabledDays);
    }

    @Override
    public void setWeekendDays(Set<Long> weekendDays) {
        calendarListsModel.setWeekendDays(weekendDays);
    }

    @Override
    public void setDisabledDaysCriteria(DisabledDaysCriteria criteria) {
        calendarListsModel.setDisabledDaysCriteria(criteria);
    }

    @Override
    public void addConnectedDays(ConnectedDays connectedDays) {
        calendarListsModel.addConnectedDays(connectedDays);
    }

    @Override
    public int getFirstDayOfWeek() {
        return dateModel.getFirstDayOfWeek();
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        dateModel.setFirstDayOfWeek(firstDayOfWeek);
    }

    public void applyAppearance(TextView textView, AppearanceModel.TextAttributes textAttributes) {
        if (textAttributes.getColor() != null)
            textView.setTextColor(textView.getContext().getResources().getColor(textAttributes.getColor()));
        if (textAttributes.getTypeface() != null)
            textView.setTypeface(textAttributes.getTypeface());
        if (textAttributes.getSize() != null)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getContext().getResources().getDimension(textAttributes.getSize()));
    }
}
