package com.applikeysolutions.cosmocalendar.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.applikeysolutions.cosmocalendar.FetchMonthsAsyncTask;
import com.applikeysolutions.cosmocalendar.adapter.MonthAdapter;
import com.applikeysolutions.cosmocalendar.listeners.OnMonthChangeListener;
import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.model.Month;
import com.applikeysolutions.cosmocalendar.selection.BaseSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.MultipleSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.NoneSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.OnDaySelectedListener;
import com.applikeysolutions.cosmocalendar.selection.RangeSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.SingleSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.selectionbar.MultipleSelectionBarAdapter;
import com.applikeysolutions.cosmocalendar.selection.selectionbar.SelectionBarItem;
import com.applikeysolutions.cosmocalendar.settings.SettingsManager;
import com.applikeysolutions.cosmocalendar.settings.appearance.AppearanceInterface;
import com.applikeysolutions.cosmocalendar.settings.date.DateInterface;
import com.applikeysolutions.cosmocalendar.settings.lists.CalendarListsInterface;
import com.applikeysolutions.cosmocalendar.settings.lists.DisabledDaysCriteria;
import com.applikeysolutions.cosmocalendar.settings.lists.connected_days.ConnectedDays;
import com.applikeysolutions.cosmocalendar.settings.lists.connected_days.ConnectedDaysManager;
import com.applikeysolutions.cosmocalendar.settings.selection.SelectionInterface;
import com.applikeysolutions.cosmocalendar.utils.CalendarUtils;
import com.applikeysolutions.cosmocalendar.utils.SelectionType;
import com.applikeysolutions.cosmocalendar.utils.WeekDay;
import com.applikeysolutions.cosmocalendar.utils.snap.GravitySnapHelper;
import com.applikeysolutions.cosmocalendar.view.customviews.CircleAnimationTextView;
import com.applikeysolutions.cosmocalendar.view.customviews.SquareTextView;
import com.applikeysolutions.cosmocalendar.view.delegate.MonthDelegate;
import com.applikeysolutions.customizablecalendar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CalendarView extends RelativeLayout implements OnDaySelectedListener,
        AppearanceInterface, DateInterface, CalendarListsInterface, SelectionInterface, MultipleSelectionBarAdapter.ListItemClickListener, GravitySnapHelper.SnapListener {

    public String daysOfWeekFormat = "EEE";
    private List<Day> selectedDays;

    //Recycler
    private SlowdownRecyclerView rvMonths;
    private MonthAdapter monthAdapter;

    //Bottom selection bar
    private FrameLayout flBottomSelectionBar;
    //Multiple mode
    private RecyclerView rvMultipleSelectedList;
    private MultipleSelectionBarAdapter multipleSelectionBarAdapter;
    //Range mode
    private LinearLayout llRangeSelection;

    //Views
    private LinearLayout llDaysOfWeekTitles;
    private FrameLayout flNavigationButtons;
    public ImageView ivPrevious;
    public ImageView ivNext;

    //Helpers
    private SettingsManager settingsManager;
    private BaseSelectionManager selectionManager;
    private GravitySnapHelper snapHelper;

    //Listeners
    private OnMonthChangeListener onMonthChangeListener;
    private Month previousSelectedMonth;

    private int lastVisibleMonthPosition = SettingsManager.DEFAULT_MONTH_COUNT / 2;

    private FetchMonthsAsyncTask asyncTask;

    public CalendarView(Context context) {
        super(context);
        init();
    }

    public CalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        handleAttributes(attrs, 0, 0);
    }

    public CalendarView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        handleAttributes(attrs, defStyle, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CalendarView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        handleAttributes(attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (asyncTask != null && !asyncTask.isCancelled()) {
            asyncTask.cancel(false);
        }
    }

    private void handleAttributes(AttributeSet attrs, int defStyle, int defStyleRes) {
        settingsManager = new SettingsManager();
        final TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CalendarView, defStyle, defStyleRes);
        try {
            handleAttributes(typedArray);
            handleWeekendDaysAttributes(typedArray);
        } finally {
            typedArray.recycle();
        }
        init();
    }

    /**
     * Handles custom attributes and sets them to settings manager
     *
     * @param typedArray
     */
    private void handleAttributes(TypedArray typedArray) {
        int orientation = typedArray.getInteger(R.styleable.CalendarView_orientation, SettingsManager.DEFAULT_ORIENTATION);
        int firstDayOfWeek = typedArray.getInteger(R.styleable.CalendarView_firstDayOfTheWeek, SettingsManager.DEFAULT_FIRST_DAY_OF_WEEK);
        int selectionType = typedArray.getInteger(R.styleable.CalendarView_selectionType, SettingsManager.DEFAULT_SELECTION_TYPE);
        boolean showDaysOfWeekTitle = orientation != LinearLayoutManager.HORIZONTAL;
        boolean showDaysOfWeek = orientation == LinearLayoutManager.HORIZONTAL;
        int calendarBackgroundColor = typedArray.getColor(R.styleable.CalendarView_calendarBackgroundColor, ContextCompat.getColor(getContext(), R.color.default_calendar_background_color));
        int monthTextColor = typedArray.getColor(R.styleable.CalendarView_monthTextColor, ContextCompat.getColor(getContext(), R.color.default_month_text_color));
        int otherDayTextColor = typedArray.getColor(R.styleable.CalendarView_otherDayTextColor, ContextCompat.getColor(getContext(), R.color.default_other_day_text_color));
        int dayTextColor = typedArray.getColor(R.styleable.CalendarView_dayTextColor, ContextCompat.getColor(getContext(), R.color.default_day_text_color));
        int weekendDayTextColor = typedArray.getColor(R.styleable.CalendarView_weekendDayTextColor, ContextCompat.getColor(getContext(), R.color.default_weekend_day_text_color));
        int weekDayTitleTextColor = typedArray.getColor(R.styleable.CalendarView_weekDayTitleTextColor, ContextCompat.getColor(getContext(), R.color.default_week_day_title_text_color));
        int selectedDayTextColor = typedArray.getColor(R.styleable.CalendarView_selectedDayTextColor, ContextCompat.getColor(getContext(), R.color.default_selected_day_text_color));
        int selectedDayBackgroundColor = typedArray.getColor(R.styleable.CalendarView_selectedDayBackgroundColor, ContextCompat.getColor(getContext(), R.color.default_selected_day_background_color));
        int selectedDayBackgroundStartColor = typedArray.getColor(R.styleable.CalendarView_selectedDayBackgroundStartColor, ContextCompat.getColor(getContext(), R.color.default_selected_day_background_start_color));
        int selectedDayBackgroundEndColor = typedArray.getColor(R.styleable.CalendarView_selectedDayBackgroundEndColor, ContextCompat.getColor(getContext(), R.color.default_selected_day_background_end_color));
        int currentDayTextColor = typedArray.getColor(R.styleable.CalendarView_currentDayTextColor, ContextCompat.getColor(getContext(), R.color.default_day_text_color));
        //int currentDayIconRes = typedArray.getResourceId(R.styleable.CalendarView_currentDayIconRes, R.drawable.ic_triangle_green);
        int currentDaySelectedIconRes = typedArray.getResourceId(R.styleable.CalendarView_currentDaySelectedIconRes, R.drawable.ic_triangle_white);
        int connectedDayIconRes = typedArray.getResourceId(R.styleable.CalendarView_connectedDayIconRes, 0);
        int connectedDaySelectedIconRes = typedArray.getResourceId(R.styleable.CalendarView_connectedDaySelectedIconRes, 0);
        int connectedDayIconPosition = typedArray.getInteger(R.styleable.CalendarView_connectedDayIconPosition, SettingsManager.DEFAULT_CONNECTED_DAY_ICON_POSITION);
        int disabledDayTextColor = typedArray.getColor(R.styleable.CalendarView_disabledDayTextColor, ContextCompat.getColor(getContext(), R.color.default_disabled_day_text_color));
        int selectionBarMonthTextColor = typedArray.getColor(R.styleable.CalendarView_selectionBarMonthTextColor, ContextCompat.getColor(getContext(), R.color.default_selection_bar_month_title_text_color));
        int previousMonthIconRes = typedArray.getResourceId(R.styleable.CalendarView_previousMonthIconRes, R.drawable.ic_chevron_left_gray);
        int nextMonthIconRes = typedArray.getResourceId(R.styleable.CalendarView_nextMonthIconRes, R.drawable.ic_chevron_right_gray);

        setBackgroundColor(calendarBackgroundColor);
        settingsManager.appearanceModel.setCalendarBackgroundColor(calendarBackgroundColor);
        settingsManager.appearanceModel.setMonthTextColor(monthTextColor);
        settingsManager.appearanceModel.setOtherDayTextColor(otherDayTextColor);
        settingsManager.appearanceModel.setDayTextColor(dayTextColor);
        settingsManager.appearanceModel.setWeekendDayTextColor(weekendDayTextColor);
        settingsManager.appearanceModel.setWeekDayTitleTextColor(weekDayTitleTextColor);
        settingsManager.appearanceModel.setSelectedDayTextColor(selectedDayTextColor);
        settingsManager.appearanceModel.setSelectedDayBackgroundColor(selectedDayBackgroundColor);
        settingsManager.appearanceModel.setSelectedDayBackgroundStartColor(selectedDayBackgroundStartColor);
        settingsManager.appearanceModel.setSelectedDayBackgroundEndColor(selectedDayBackgroundEndColor);
        settingsManager.appearanceModel.setConnectedDayIconRes(connectedDayIconRes);
        settingsManager.appearanceModel.setConnectedDaySelectedIconRes(connectedDaySelectedIconRes);
        settingsManager.appearanceModel.setConnectedDayIconPosition(connectedDayIconPosition);
        settingsManager.appearanceModel.setDisabledDayTextColor(disabledDayTextColor);
        settingsManager.appearanceModel.setSelectionBarMonthTextColor(selectionBarMonthTextColor);
        settingsManager.appearanceModel.setCurrentDayTextColor(currentDayTextColor);
        //settingsManager.setCurrentDayIconRes(currentDayIconRes);
        settingsManager.appearanceModel.setCurrentDaySelectedIconRes(currentDaySelectedIconRes);
        settingsManager.appearanceModel.setCalendarOrientation(orientation);
        settingsManager.setFirstDayOfWeek(firstDayOfWeek);
        settingsManager.appearanceModel.setShowDaysOfWeek(showDaysOfWeek);
        settingsManager.appearanceModel.setShowDaysOfWeekTitle(showDaysOfWeekTitle);
        settingsManager.setSelectionType(selectionType);
        settingsManager.appearanceModel.setPreviousMonthIconRes(previousMonthIconRes);
        settingsManager.appearanceModel.setNextMonthIconRes(nextMonthIconRes);
    }

    private void handleWeekendDaysAttributes(TypedArray typedArray) {
        if (typedArray.hasValue(R.styleable.CalendarView_weekendDays)) {
            Set<Long> weekendDays = new TreeSet<>();

            int weekdaysAttr = typedArray.getInteger(R.styleable.CalendarView_weekendDays, WeekDay.SUNDAY);
            if (containsFlag(weekdaysAttr, WeekDay.MONDAY))
                weekendDays.add((long) Calendar.MONDAY);
            if (containsFlag(weekdaysAttr, WeekDay.TUESDAY))
                weekendDays.add((long) Calendar.TUESDAY);
            if (containsFlag(weekdaysAttr, WeekDay.WEDNESDAY))
                weekendDays.add((long) Calendar.WEDNESDAY);
            if (containsFlag(weekdaysAttr, WeekDay.THURSDAY))
                weekendDays.add((long) Calendar.THURSDAY);
            if (containsFlag(weekdaysAttr, WeekDay.FRIDAY))
                weekendDays.add((long) Calendar.FRIDAY);
            if (containsFlag(weekdaysAttr, WeekDay.SATURDAY))
                weekendDays.add((long) Calendar.SATURDAY);
            if (containsFlag(weekdaysAttr, WeekDay.SUNDAY))
                weekendDays.add((long) Calendar.SUNDAY);

            settingsManager.setWeekendDays(weekendDays);
        }
    }

    private boolean containsFlag(int attr, int flag) {
        return (attr | flag) == attr;
    }

    private void init() {
        setDaysOfWeekTitles();

        setSelectionManager();
        createRecyclerView();
        createBottomSelectionBar();

        if (settingsManager.appearanceModel.getCalendarOrientation() == LinearLayoutManager.HORIZONTAL) {
            createNavigationButtons();
        }
    }

    /**
     * Defines days of week displaying according to calendar orientation
     * HORIZONTAL - displaying below month name and above dates
     * VERTICAL - displaying above whole calendar
     */
    private void setDaysOfWeekTitles() {
        settingsManager.appearanceModel.setShowDaysOfWeekTitle(settingsManager.appearanceModel.getCalendarOrientation() != LinearLayoutManager.HORIZONTAL);
        settingsManager.appearanceModel.setShowDaysOfWeek(settingsManager.appearanceModel.getCalendarOrientation() == LinearLayoutManager.HORIZONTAL);

        if (llDaysOfWeekTitles == null) {
            createDaysOfWeekTitle();
        }
        if (settingsManager.appearanceModel.getShowDaysOfWeekTitle()) {
            showDaysOfWeekTitle();
        } else {
            hideDaysOfWeekTitle();
        }
    }

    /**
     * Creates days of week title above calendar
     */
    private void createDaysOfWeekTitle() {
        boolean isTitleAlreadyAdded = llDaysOfWeekTitles != null;
        if (!isTitleAlreadyAdded) {
            llDaysOfWeekTitles = new LinearLayout(getContext());
            llDaysOfWeekTitles.setId(View.generateViewId());
            llDaysOfWeekTitles.setOrientation(LinearLayout.HORIZONTAL);
            llDaysOfWeekTitles.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else {
            llDaysOfWeekTitles.removeAllViews();
        }

        //creating days of week views
        LinearLayout.LayoutParams textViewParam = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        textViewParam.weight = 1;
        for (String title : CalendarUtils.createWeekDayTitles(settingsManager.getFirstDayOfWeek())) {
            SquareTextView tvDayTitle = new SquareTextView(getContext());
            tvDayTitle.setText(title);
            tvDayTitle.setLayoutParams(textViewParam);
            tvDayTitle.setGravity(Gravity.CENTER);
            llDaysOfWeekTitles.addView(tvDayTitle);
        }

        //adding borders
        llDaysOfWeekTitles.setBackgroundResource(R.drawable.border_top_bottom);

        if (!isTitleAlreadyAdded) {
            addView(llDaysOfWeekTitles);
        }
    }

    /**
     * Creates bottom selection bar to show selected days
     */
    private void createBottomSelectionBar() {
        flBottomSelectionBar = new FrameLayout(getContext());
//        flBottomSelectionBar.setLayoutTransition(new LayoutTransition());
        flBottomSelectionBar.setId(View.generateViewId());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, rvMonths.getId());
        flBottomSelectionBar.setLayoutParams(params);
        flBottomSelectionBar.setBackgroundResource(R.drawable.border_top_bottom);
        flBottomSelectionBar.setVisibility(settingsManager.appearanceModel.getCalendarOrientation() == OrientationHelper.HORIZONTAL ? View.VISIBLE : View.GONE);
        addView(flBottomSelectionBar);

        createMultipleSelectionBarRecycler();
        createRangeSelectionLayout();
    }

    /**
     * Creates recycler view to display selected days in bottom selection bar
     * Visible only for Multiple selection mode
     */
    private void createMultipleSelectionBarRecycler() {
        rvMultipleSelectedList = new RecyclerView(getContext());
        rvMultipleSelectedList.setId(View.generateViewId());
        rvMultipleSelectedList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rvMultipleSelectedList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        multipleSelectionBarAdapter = new MultipleSelectionBarAdapter(this, this);
        rvMultipleSelectedList.setAdapter(multipleSelectionBarAdapter);
        flBottomSelectionBar.addView(rvMultipleSelectedList);
    }

    private void createRangeSelectionLayout() {
        llRangeSelection = (LinearLayout) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_selection_bar_range, null);
        llRangeSelection.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        llRangeSelection.setVisibility(GONE);
        flBottomSelectionBar.addView(llRangeSelection);
    }

    private void showDaysOfWeekTitle() {
        llDaysOfWeekTitles.setVisibility(View.VISIBLE);
    }

    private void hideDaysOfWeekTitle() {
        llDaysOfWeekTitles.setVisibility(View.GONE);
    }

    private void setSelectionManager() {
        switch (getSelectionType()) {
            case SelectionType.SINGLE:
                selectionManager = new SingleSelectionManager(this);
                break;

            case SelectionType.MULTIPLE:
                selectionManager = new MultipleSelectionManager(this);
                break;

            case SelectionType.RANGE:
                selectionManager = new RangeSelectionManager(this);
                break;

            case SelectionType.NONE:
                selectionManager = new NoneSelectionManager();
                break;
        }
    }

    public void setSelectionManager(BaseSelectionManager selectionManager) {
        this.selectionManager = selectionManager;
        monthAdapter.setSelectionManager(selectionManager);
        update();
    }

    public BaseSelectionManager getSelectionManager() {
        return selectionManager;
    }

    public void update() {
        if (monthAdapter != null) {
            monthAdapter.notifyDataSetChanged();
            rvMonths.scrollToPosition(lastVisibleMonthPosition);
            multipleSelectionBarAdapter.notifyDataSetChanged();
        }
    }

    private void createRecyclerView() {
        rvMonths = new SlowdownRecyclerView(getContext());
        rvMonths.setId(View.generateViewId());
        rvMonths.setHasFixedSize(true);
        rvMonths.setNestedScrollingEnabled(false);
        ((SimpleItemAnimator) rvMonths.getItemAnimator()).setSupportsChangeAnimations(false);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, llDaysOfWeekTitles.getId());
        rvMonths.setLayoutParams(params);

        rvMonths.setLayoutManager(new GridLayoutManager(getContext(), 1, settingsManager.appearanceModel.getCalendarOrientation(), false));
        monthAdapter = createAdapter();

        changeSnapHelper();

        rvMonths.setAdapter(monthAdapter);
        rvMonths.scrollToPosition(SettingsManager.DEFAULT_MONTH_COUNT / 2);
        rvMonths.addOnScrollListener(pagingScrollListener);
        rvMonths.getRecycledViewPool().setMaxRecycledViews(ItemViewType.MONTH, 10);
        addView(rvMonths);
    }

    /**
     * Creates Next/Previous buttons to navigate months.
     * Visible only in HORIZONTAL mode
     */
    private void createNavigationButtons() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        flNavigationButtons = (FrameLayout) inflater.inflate(R.layout.calendar_navigation_buttons, this, false);

        setPreviousNavigationButton();
        setNextNavigationButton();

        addView(flNavigationButtons);
    }

    private void setPreviousNavigationButton() {
        ivPrevious = (ImageView) flNavigationButtons.findViewById(R.id.iv_previous_month);
        ivPrevious.setImageResource(settingsManager.appearanceModel.getPreviousMonthIconRes());
        ivPrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPreviousMonth();
            }
        });
    }

    private void setNextNavigationButton() {
        ivNext = (ImageView) flNavigationButtons.findViewById(R.id.iv_next_month);
        ivNext.setImageResource(settingsManager.appearanceModel.getNextMonthIconRes());
        ivNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextMonth();
            }
        });
    }

    private MonthAdapter createAdapter() {
        return new MonthAdapter.MonthAdapterBuilder()
                .setMonths(CalendarUtils.createInitialMonths(settingsManager))
                .setMonthDelegate(new MonthDelegate(settingsManager))
                .setCalendarView(this)
                .setSelectionManager(selectionManager)
                .createMonthAdapter();
    }

    /**
     * Scroll listener for month pagination
     */
    private RecyclerView.OnScrollListener pagingScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            //Fix for bug with bottom selection bar and different month item height in horizontal mode (different count of weeks)
            View view = rvMonths.getLayoutManager().findViewByPosition(getFirstVisiblePosition(rvMonths.getLayoutManager()));
            if (view != null) {
                view.requestLayout();
            }

            if (getCalendarOrientation() == OrientationHelper.HORIZONTAL) {
                multipleSelectionBarAdapter.notifyDataSetChanged();

                //Hide navigation buttons
                boolean show = newState != RecyclerView.SCROLL_STATE_DRAGGING;
                ivPrevious.setVisibility(show ? View.VISIBLE : View.GONE);
                ivNext.setVisibility(show ? View.VISIBLE : View.GONE);
            }

            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            final RecyclerView.LayoutManager manager = rvMonths.getLayoutManager();

            int totalItemCount = manager.getItemCount();
            int firstVisibleItemPosition = getFirstVisiblePosition(manager);
            lastVisibleMonthPosition = firstVisibleItemPosition;

            if (firstVisibleItemPosition < 2) {
                loadAsyncMonths(false);
            } else if (firstVisibleItemPosition >= totalItemCount - 2) {
                loadAsyncMonths(true);
            }
        }
    };

    private int getFirstVisiblePosition(RecyclerView.LayoutManager manager) {
        if (manager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
        } else {
            throw new IllegalArgumentException("Unsupported Layout Manager");
        }
    }

    private void loadAsyncMonths(final boolean future) {
        if (asyncTask != null && (asyncTask.getStatus() == AsyncTask.Status.PENDING || asyncTask.getStatus() == AsyncTask.Status.RUNNING))
            return;

        asyncTask = new FetchMonthsAsyncTask();
        Month month;

        if (future) {
            month = monthAdapter.getData().get(monthAdapter.getData().size() - 1);
        } else {
            month = monthAdapter.getData().get(0);
        }

        asyncTask.execute(new FetchMonthsAsyncTask.FetchParams(future, month, settingsManager, monthAdapter, SettingsManager.DEFAULT_MONTH_COUNT));
    }

    @Override
    public Set<Long> getDisabledDays() {
        return settingsManager.getDisabledDays();
    }

    @Override
    public ConnectedDaysManager getConnectedDaysManager() {
        return settingsManager.getConnectedDaysManager();
    }

    @Override
    public Set<Long> getWeekendDays() {
        return settingsManager.getWeekendDays();
    }

    @Override
    public DisabledDaysCriteria getDisabledDaysCriteria() {
        return settingsManager.getDisabledDaysCriteria();
    }

    public void setDisabledDays(Set<Long> disabledDays) {
        settingsManager.setDisabledDays(disabledDays);
        monthAdapter.setDisabledDays(disabledDays);
    }

    public void setWeekendDays(Set<Long> weekendDays) {
        settingsManager.setWeekendDays(weekendDays);
        monthAdapter.setWeekendDays(weekendDays);
    }

    @Override
    public void setDisabledDaysCriteria(DisabledDaysCriteria criteria) {
        settingsManager.setDisabledDaysCriteria(criteria);
        monthAdapter.setDisabledDaysCriteria(criteria);
    }

    @Override
    public void addConnectedDays(ConnectedDays connectedDays) {
        settingsManager.getConnectedDaysManager().addConnectedDays(connectedDays);
        recreateInitialMonth();
    }

    /**
     * Removes all selections (manual and by criteria)
     */
    public void clearSelections() {
        selectionManager.clearSelections();
        if (selectionManager instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) selectionManager).clearCriteriaList();
        }
        multipleSelectionBarAdapter.setData(new ArrayList<SelectionBarItem>());
        setSelectionBarVisibility();
        update();
    }

    /**
     * Returns all selected days
     *
     * @return
     */
    public List<Day> getSelectedDays() {
        List<Day> selectedDays = new ArrayList<>();
        for (Iterator<Month> monthIterator = monthAdapter.getData().iterator(); monthIterator.hasNext(); ) {
            Month month = monthIterator.next();
            for (Iterator<Day> dayIterator = month.getDaysWithoutTitlesAndOnlyCurrent().iterator(); dayIterator.hasNext(); ) {
                Day day = dayIterator.next();
                if (selectionManager.isDaySelected(day)) {
                    selectedDays.add(day);
                }
            }
        }
        return selectedDays;
    }

    /**
     * Returns all selected dates
     *
     * @return
     */
    public List<Calendar> getSelectedDates() {
        List<Calendar> selectedDays = new ArrayList<>();
        for (Day day : getSelectedDays()) {
            selectedDays.add(day.getCalendar());
        }
        return selectedDays;
    }

    /**
     * Scroll calendar to previous month
     */
    public void goToPreviousMonth() {
        int currentVisibleItemPosition = ((GridLayoutManager) rvMonths.getLayoutManager()).findFirstVisibleItemPosition();
        if (currentVisibleItemPosition != 0) {
            rvMonths.smoothScrollToPosition(currentVisibleItemPosition - 1);
        }
    }

    /**
     * Scroll calendar to next month
     */
    public void goToNextMonth() {
        int currentVisibleItemPosition = ((GridLayoutManager) rvMonths.getLayoutManager()).findFirstVisibleItemPosition();
        if (currentVisibleItemPosition != monthAdapter.getData().size() - 1) {
            rvMonths.smoothScrollToPosition(currentVisibleItemPosition + 1);
        }
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    private void recreateInitialMonth() {
        monthAdapter.getData().clear();
        monthAdapter.getData().addAll(CalendarUtils.createInitialMonths(settingsManager));
        lastVisibleMonthPosition = SettingsManager.DEFAULT_MONTH_COUNT / 2;
    }

    @Override
    public void onDaySelected() {
        selectedDays = getSelectedDays();
        displaySelectedDays();
    }

    /**
     * Displays selected days
     */
    private void displaySelectedDays() {
        switch (settingsManager.getSelectionType()) {
            case SelectionType.MULTIPLE:
                displaySelectedDaysMultiple();
                break;

            case SelectionType.RANGE:
                displaySelectedDaysRange();
                break;

            default:
                llRangeSelection.setVisibility(GONE);
                break;
        }
    }

    /**
     * Display selected days for MULTIPLE mode in bottom bar
     */
    private void displaySelectedDaysMultiple() {
        multipleSelectionBarAdapter.setData(CalendarUtils.getSelectedDayListForMultipleMode(selectedDays));
    }

    /**
     * Display selected days for RANGE mode in bottom bar
     */
    private void displaySelectedDaysRange() {
        if (selectionManager instanceof RangeSelectionManager) {
            Pair<Day, Day> days = ((RangeSelectionManager) selectionManager).getDays();
            if (days != null) {
                llRangeSelection.setVisibility(VISIBLE);
                TextView tvStartRangeTitle = (TextView) llRangeSelection.findViewById(R.id.tv_range_start_date);
                tvStartRangeTitle.setText(CalendarUtils.getYearNameTitle(days.first));
                tvStartRangeTitle.setTextColor(getSelectionBarMonthTextColor());

                TextView tvEndRangeTitle = (TextView) llRangeSelection.findViewById(R.id.tv_range_end_date);
                tvEndRangeTitle.setText(CalendarUtils.getYearNameTitle(days.second));
                tvEndRangeTitle.setTextColor(getSelectionBarMonthTextColor());

                CircleAnimationTextView catvStart = (CircleAnimationTextView) llRangeSelection.findViewById(R.id.catv_start);
                catvStart.setText(String.valueOf(days.first.getDayNumber()));
                catvStart.setTextColor(getSelectedDayTextColor());
                catvStart.showAsStartCircle(this, true);

                CircleAnimationTextView catvEnd = (CircleAnimationTextView) llRangeSelection.findViewById(R.id.catv_end);
                catvEnd.setText(String.valueOf(days.second.getDayNumber()));
                catvEnd.setTextColor(getSelectedDayTextColor());
                catvEnd.showAsEndCircle(this, true);

                CircleAnimationTextView catvMiddle = (CircleAnimationTextView) llRangeSelection.findViewById(R.id.catv_middle);
                catvMiddle.showAsRange(this);
            } else {
                llRangeSelection.setVisibility(GONE);
            }
        }
    }

    /**
     * Defines do we need to show range of selected days in bottom selection bar
     *
     * @return
     */
    private boolean needToShowSelectedDaysRange() {
        if (getCalendarOrientation() == OrientationHelper.HORIZONTAL && getSelectionType() == SelectionType.RANGE) {
            if (selectionManager instanceof RangeSelectionManager) {
                if (((RangeSelectionManager) selectionManager).getDays() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets selection bar layout visibility
     */
    private void setSelectionBarVisibility() {
        flBottomSelectionBar.setVisibility(getCalendarOrientation() == OrientationHelper.HORIZONTAL ? View.VISIBLE : View.GONE);
        rvMultipleSelectedList.setVisibility(getCalendarOrientation() == OrientationHelper.HORIZONTAL && getSelectionType() == SelectionType.MULTIPLE ? View.VISIBLE : View.GONE);
        llRangeSelection.setVisibility(needToShowSelectedDaysRange() ? View.VISIBLE : View.GONE);
    }

    @Override
    @SelectionType
    public int getSelectionType() {
        return settingsManager.getSelectionType();
    }

    @Override
    public void setSelectionType(@SelectionType int selectionType) {
        settingsManager.setSelectionType(selectionType);
        setSelectionManager();
        monthAdapter.setSelectionManager(selectionManager);
        setSelectionBarVisibility();

        //Clear selections and selection bar
        multipleSelectionBarAdapter.setData(new ArrayList<SelectionBarItem>());
        selectionManager.clearSelections();
        if (selectionManager instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) selectionManager).clearCriteriaList();
        }

        update();
    }

    @Override
    public int getCalendarBackgroundColor() {
        return settingsManager.appearanceModel.getCalendarBackgroundColor();
    }

    @Override
    public int getMonthTextColor() {
        return settingsManager.appearanceModel.getMonthTextColor();
    }

    @Override
    public int getOtherDayTextColor() {
        return settingsManager.appearanceModel.getOtherDayTextColor();
    }

    @Override
    public int getDayTextColor() {
        return settingsManager.appearanceModel.getDayTextColor();
    }

    @Override
    public int getWeekendDayTextColor() {
        return settingsManager.appearanceModel.getWeekendDayTextColor();
    }

    @Override
    public int getWeekDayTitleTextColor() {
        return settingsManager.appearanceModel.getWeekDayTitleTextColor();
    }

    @Override
    public int getSelectedDayTextColor() {
        return settingsManager.appearanceModel.getSelectedDayTextColor();
    }

    @Override
    public int getSelectedDayBackgroundColor() {
        return settingsManager.appearanceModel.getSelectedDayBackgroundColor();
    }

    @Override
    public int getSelectedDayBackgroundStartColor() {
        return settingsManager.appearanceModel.getSelectedDayBackgroundStartColor();
    }

    @Override
    public int getSelectedDayBackgroundEndColor() {
        return settingsManager.appearanceModel.getSelectedDayBackgroundEndColor();
    }

    @Override
    public int getCurrentDayTextColor() {
        return settingsManager.appearanceModel.getCurrentDayTextColor();
    }

    @Override
    public int getCurrentDayIconRes() {
        return settingsManager.appearanceModel.getCurrentDayIconRes();
    }

    @Override
    public int getCurrentDaySelectedIconRes() {
        return settingsManager.appearanceModel.getCurrentDaySelectedIconRes();
    }

    @Override
    public int getCalendarOrientation() {
        return settingsManager.appearanceModel.getCalendarOrientation();
    }

    @Override
    public int getConnectedDayIconRes() {
        return settingsManager.appearanceModel.getConnectedDayIconRes();
    }

    @Override
    public int getConnectedDaySelectedIconRes() {
        return settingsManager.appearanceModel.getConnectedDaySelectedIconRes();
    }

    @Override
    public int getConnectedDayIconPosition() {
        return settingsManager.appearanceModel.getConnectedDayIconPosition();
    }

    @Override
    public int getDisabledDayTextColor() {
        return settingsManager.appearanceModel.getDisabledDayTextColor();
    }

    @Override
    public int getSelectionBarMonthTextColor() {
        return settingsManager.appearanceModel.getSelectionBarMonthTextColor();
    }

    @Override
    public int getPreviousMonthIconRes() {
        return settingsManager.appearanceModel.getPreviousMonthIconRes();
    }

    @Override
    public int getNextMonthIconRes() {
        return settingsManager.appearanceModel.getNextMonthIconRes();
    }

    @Override
    public boolean isShowDaysOfWeek() {
        return settingsManager.appearanceModel.getShowDaysOfWeek();
    }

    @Override
    public boolean isShowDaysOfWeekTitle() {
        return settingsManager.appearanceModel.getShowDaysOfWeekTitle();
    }

    @Override
    public void setCalendarBackgroundColor(int calendarBackgroundColor) {
        settingsManager.appearanceModel.setCalendarBackgroundColor(calendarBackgroundColor);
        setBackgroundColor(calendarBackgroundColor);
    }

    @Override
    public void setMonthTextColor(int monthTextColor) {
        settingsManager.appearanceModel.setMonthTextColor(monthTextColor);
        update();
    }

    @Override
    public void setOtherDayTextColor(int otherDayTextColor) {
        settingsManager.appearanceModel.setOtherDayTextColor(otherDayTextColor);
        update();
    }

    @Override
    public void setDayTextColor(int dayTextColor) {
        settingsManager.appearanceModel.setDayTextColor(dayTextColor);
        update();
    }

    @Override
    public void setWeekendDayTextColor(int weekendDayTextColor) {
        settingsManager.appearanceModel.setWeekendDayTextColor(weekendDayTextColor);
        update();
    }

    @Override
    public void setWeekDayTitleTextColor(int weekDayTitleTextColor) {
        settingsManager.appearanceModel.setWeekDayTitleTextColor(weekDayTitleTextColor);
        for (int i = 0; i < llDaysOfWeekTitles.getChildCount(); i++) {
            ((SquareTextView) llDaysOfWeekTitles.getChildAt(i)).setTextColor(weekDayTitleTextColor);
        }
        update();
    }

    @Override
    public void setSelectedDayTextColor(int selectedDayTextColor) {
        settingsManager.appearanceModel.setSelectedDayTextColor(selectedDayTextColor);
        update();
    }

    @Override
    public void setSelectedDayBackgroundColor(int selectedDayBackgroundColor) {
        settingsManager.appearanceModel.setSelectedDayBackgroundColor(selectedDayBackgroundColor);
        update();
    }

    @Override
    public void setSelectedDayBackgroundStartColor(int selectedDayBackgroundStartColor) {
        settingsManager.appearanceModel.setSelectedDayBackgroundStartColor(selectedDayBackgroundStartColor);
        update();
    }

    @Override
    public void setSelectedDayBackgroundEndColor(int selectedDayBackgroundEndColor) {
        settingsManager.appearanceModel.setSelectedDayBackgroundEndColor(selectedDayBackgroundEndColor);
        update();
    }

    @Override
    public void setCurrentDayTextColor(int currentDayTextColor) {
        settingsManager.appearanceModel.setCurrentDayTextColor(currentDayTextColor);
        update();
    }

    @Override
    public void setCurrentDayIconRes(int currentDayIconRes) {
        settingsManager.appearanceModel.setCurrentDayIconRes(currentDayIconRes);
        update();
    }

    @Override
    public void setCurrentDaySelectedIconRes(int currentDaySelectedIconRes) {
        settingsManager.appearanceModel.setCurrentDaySelectedIconRes(currentDaySelectedIconRes);
        update();
    }

    @Override
    public void setCalendarOrientation(int calendarOrientation) {
        clearSelections();
        settingsManager.appearanceModel.setCalendarOrientation(calendarOrientation);
        setDaysOfWeekTitles();
        recreateInitialMonth();

        rvMonths.setLayoutManager(new GridLayoutManager(getContext(), 1, getCalendarOrientation(), false));

        changeSnapHelper();

        if (getCalendarOrientation() == LinearLayout.HORIZONTAL) {
            if (flNavigationButtons != null) {
                flNavigationButtons.setVisibility(VISIBLE);
            } else {
                createNavigationButtons();
            }
        } else {
            if (flNavigationButtons != null) {
                flNavigationButtons.setVisibility(GONE);
            }
        }

        setSelectionBarVisibility();
        update();
    }

    @Override
    public void setConnectedDayIconRes(int connectedDayIconRes) {
        settingsManager.appearanceModel.setConnectedDayIconRes(connectedDayIconRes);
        update();
    }

    @Override
    public void setConnectedDaySelectedIconRes(int connectedDaySelectedIconRes) {
        settingsManager.appearanceModel.setConnectedDaySelectedIconRes(connectedDaySelectedIconRes);
        update();
    }

    @Override
    public void setConnectedDayIconPosition(int connectedDayIconPosition) {
        settingsManager.appearanceModel.setConnectedDayIconPosition(connectedDayIconPosition);
        update();
    }

    @Override
    public void setDisabledDayTextColor(int disabledDayTextColor) {
        settingsManager.appearanceModel.setDisabledDayTextColor(disabledDayTextColor);
        update();
    }

    @Override
    public void setSelectionBarMonthTextColor(int selectionBarMonthTextColor) {
        settingsManager.appearanceModel.setSelectionBarMonthTextColor(selectionBarMonthTextColor);
        update();
    }

    @Override
    public void setPreviousMonthIconRes(int previousMonthIconRes) {
        settingsManager.appearanceModel.setPreviousMonthIconRes(previousMonthIconRes);
        setPreviousNavigationButton();
    }

    @Override
    public void setNextMonthIconRes(int nextMonthIconRes) {
        settingsManager.appearanceModel.setNextMonthIconRes(nextMonthIconRes);
        setNextNavigationButton();
    }

    @Override
    public void setShowDaysOfWeek(boolean showDaysOfWeek) {
        settingsManager.appearanceModel.setShowDaysOfWeek(showDaysOfWeek);
        recreateInitialMonth();
    }

    @Override
    public void setShowDaysOfWeekTitle(boolean showDaysOfWeekTitle) {
        settingsManager.appearanceModel.setShowDaysOfWeekTitle(showDaysOfWeekTitle);
        if (showDaysOfWeekTitle) {
            showDaysOfWeekTitle();
        } else {
            hideDaysOfWeekTitle();
        }
    }

    @Override
    public int getFirstDayOfWeek() {
        return settingsManager.getFirstDayOfWeek();
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek > 0 && firstDayOfWeek < 8) {
            settingsManager.setFirstDayOfWeek(firstDayOfWeek);
            recreateInitialMonth();
            createDaysOfWeekTitle();
        } else {
            throw new IllegalArgumentException("First day of week must be 1 - 7");
        }
    }

    private void changeSnapHelper() {
        rvMonths.setOnFlingListener(null);
        if (snapHelper == null) {
            snapHelper = new GravitySnapHelper(settingsManager.appearanceModel.getCalendarOrientation() == LinearLayoutManager.VERTICAL ? Gravity.TOP : Gravity.START, true, this);
            snapHelper.attachToRecyclerView(rvMonths);
        } else {
            snapHelper.setGravity(settingsManager.appearanceModel.getCalendarOrientation() == LinearLayoutManager.VERTICAL ? Gravity.TOP : Gravity.START);
        }
    }

    /*
     * Removes selected day by click in bottom selection bar
     */
    @Override
    public void onMultipleSelectionListItemClick(final Day day) {
        if (getSelectionManager() instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) getSelectionManager()).removeDay(day);
            monthAdapter.notifyDataSetChanged();
        }
    }

    public void setOnMonthChangeListener(OnMonthChangeListener onMonthChangeListener) {
        this.onMonthChangeListener = onMonthChangeListener;
    }

    @Override
    public void onSnap(int position) {
        Month month = monthAdapter.getData().get(position);
        if (onMonthChangeListener != null
                && (previousSelectedMonth == null || !previousSelectedMonth.getMonthName().equals(month.getMonthName()))) {
            onMonthChangeListener.onMonthChanged(month);
            previousSelectedMonth = month;
        }
    }
}