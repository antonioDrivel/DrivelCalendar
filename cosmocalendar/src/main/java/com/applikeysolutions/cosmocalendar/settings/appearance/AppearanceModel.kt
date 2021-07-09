package com.applikeysolutions.cosmocalendar.settings.appearance

import android.graphics.Typeface
import android.support.annotation.ColorRes
import android.support.annotation.DimenRes

class AppearanceModel {

    class TextAttributes {
        var typeface: Typeface? = null
        @DimenRes
        var size: Int? = null
        @ColorRes
        var color: Int? = null

        constructor(typeface: Typeface?, size: Int?, color: Int?) {
            this.typeface = typeface
            this.size = size
            this.color = color
        }

        constructor(typeface: Typeface?, color: Int?) {
            this.typeface = typeface
            this.color = color
        }

        constructor() {
        }
    }

    //Background color of whole calendar
    var calendarBackgroundColor: Int = 0

    //Text color of month title
    var monthTextColor: Int = 0

    //Text color of day that month doesn't include
    var otherDayTextColor: Int = 0

    //Text color of day
    var dayTextColor: Int = 0

    //Text color of weekend day (ex. Saturday, Sunday)
    var weekendDayTextColor: Int = 0

    //Text color of week day titles
    var weekDayTitleTextColor: Int = 0

    //Text color of selected days
    var selectedDayTextColor: Int = 0

    //Background color of selected days
    var selectedDayBackgroundColor: Int = 0

    //Background color of START day from selected range
    var selectedDayBackgroundStartColor: Int = 0

    //Background color of END day from selected range
    var selectedDayBackgroundEndColor: Int = 0

    //Text color of current day
    var currentDayTextColor: Int = 0

    //Icon resource of current day
    var currentDayIconRes = 0

    //Icon resource of current day selected
    var currentDaySelectedIconRes: Int = 0

    //Icon resource of connected day
    var connectedDayIconRes: Int = 0

    //Icon resource of connected day selected
    var connectedDaySelectedIconRes: Int = 0

    //Position of connected day icon (TOP/BOTTOM)
    var connectedDayIconPosition: Int = 0

    //Text color of disabled day
    var disabledDayTextColor: Int = 0

    //Text color of month titles in selection bar
    var selectionBarMonthTextColor: Int = 0

    //Icon resource of previous month navigation button
    var previousMonthIconRes: Int = 0

    //Icon resource of next month navigation button
    var nextMonthIconRes: Int = 0

    /**
     * Orientation of calendar
     * possible values:
     * HORIZONTAL - left/right swipable months, navigation buttons
     * VERTICAL - top/bottom swipable months
     */
    var calendarOrientation: Int = 0

    //Defines if we need to display week day titles for every month
    var showDaysOfWeek: Boolean = false

    //Defines if we need to display week day title for whole calendar
    var showDaysOfWeekTitle: Boolean = false

    var monthNameText = TextAttributes()
    var weekDayText = TextAttributes()
    var disabledDayText = TextAttributes()
    var dayText = TextAttributes()
    var selectedDayText = TextAttributes()
}
