package com.applikeysolutions.cosmocalendar.adapter.viewholder;

import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.applikeysolutions.cosmocalendar.adapter.DaysAdapter;
import com.applikeysolutions.cosmocalendar.model.Month;
import com.applikeysolutions.cosmocalendar.settings.SettingsManager;
import com.applikeysolutions.cosmocalendar.view.MonthView;
import com.applikeysolutions.customizablecalendar.R;

public class MonthHolder extends RecyclerView.ViewHolder {

    private LinearLayout llMonthHeader;
    private TextView tvMonthName;
    private View viewLeftLine;
    private View viewRightLine;
    private MonthView monthView;
    private SettingsManager settingsManager;

    public MonthHolder(View itemView, SettingsManager settingsManager) {
        super(itemView);
        llMonthHeader = (LinearLayout) itemView.findViewById(R.id.ll_month_header);
        monthView = (MonthView) itemView.findViewById(R.id.month_view);
        tvMonthName = (TextView) itemView.findViewById(R.id.tv_month_name);
        viewLeftLine = itemView.findViewById(R.id.view_left_line);
        viewRightLine = itemView.findViewById(R.id.view_right_line);
        this.settingsManager = settingsManager;
    }

    public void setDayAdapter(DaysAdapter adapter) {
        getMonthView().setAdapter(adapter);
    }

    public void bind(Month month) {
        tvMonthName.setText(month.getMonthName());

        settingsManager.applyAppearance(tvMonthName,settingsManager.appearanceModel.getMonthNameText());

        viewLeftLine.setVisibility(settingsManager.appearanceModel.getCalendarOrientation() == OrientationHelper.HORIZONTAL ? View.INVISIBLE : View.VISIBLE);
        viewRightLine.setVisibility(settingsManager.appearanceModel.getCalendarOrientation() == OrientationHelper.HORIZONTAL ? View.INVISIBLE : View.VISIBLE);
        llMonthHeader.setBackgroundResource(settingsManager.appearanceModel.getCalendarOrientation() == OrientationHelper.HORIZONTAL ? R.drawable.border_top_bottom : 0);

        monthView.initAdapter(month);
    }

    public MonthView getMonthView() {
        return monthView;
    }
}
