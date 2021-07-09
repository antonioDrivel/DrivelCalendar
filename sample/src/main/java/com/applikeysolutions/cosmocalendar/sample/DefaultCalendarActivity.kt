package com.applikeysolutions.cosmocalendar.sample

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.OrientationHelper
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioGroup
import android.widget.Toast
import com.applikeysolutions.cosmocalendar.settings.appearance.AppearanceModel
import com.applikeysolutions.cosmocalendar.settings.lists.DisabledDaysCriteria
import com.applikeysolutions.cosmocalendar.settings.lists.DisabledDaysCriteriaType
import com.applikeysolutions.cosmocalendar.utils.SelectionType
import kotlinx.android.synthetic.main.activity_default_calendar.*
import java.util.*

class DefaultCalendarActivity : AppCompatActivity(), RadioGroup.OnCheckedChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_calendar)

        initViews()
    }

    private fun initViews() {
        rg_orientation.setOnCheckedChangeListener(this)
        rg_selection_type.setOnCheckedChangeListener(this)

        setupCalendarAppearance()
        disableBusyDays()
    }

    private fun disableBusyDays() {
        val disabledDaysSet = HashSet<Long>()
        disabledDaysSet.add(System.currentTimeMillis())
        calendar_view.disabledDays = disabledDaysSet
        calendar_view.currentDayIconRes = 0

        calendar_view.disabledDaysCriteria = DisabledDaysCriteria(Calendar.SATURDAY, Calendar.SATURDAY, DisabledDaysCriteriaType.DAYS_OF_WEEK)
        calendar_view.disabledDaysCriteria = DisabledDaysCriteria(Calendar.SUNDAY, Calendar.SUNDAY, DisabledDaysCriteriaType.DAYS_OF_WEEK)
    }

    private fun setupCalendarAppearance() {
        val appearanceModel = calendar_view.settingsManager.appearanceModel

        calendar_view.daysOfWeekFormat = "EEE"
        calendar_view.calendarOrientation = OrientationHelper.HORIZONTAL

        calendar_view.selectedDayBackgroundColor = getColor(R.color.colorPrimary)
        appearanceModel.selectedDayText = AppearanceModel.TextAttributes(Typeface.DEFAULT_BOLD, android.R.color.white)
        appearanceModel.dayText = AppearanceModel.TextAttributes(Typeface.DEFAULT, R.color.colorAccent)
        appearanceModel.disabledDayText = AppearanceModel.TextAttributes(Typeface.DEFAULT_BOLD, android.R.color.holo_red_dark)
        appearanceModel.monthNameText = AppearanceModel.TextAttributes(Typeface.DEFAULT_BOLD, R.color.material_blue_grey_900)
        appearanceModel.weekDayText = AppearanceModel.TextAttributes(Typeface.DEFAULT, R.color.colorAccent)

        calendar_view.ivNext.setColorFilter(ContextCompat.getColor(this, R.color.error_color_material_light), PorterDuff.Mode.MULTIPLY)
        calendar_view.ivPrevious.setColorFilter(ContextCompat.getColor(this, R.color.error_color_material_light), PorterDuff.Mode.MULTIPLY)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_default_calendar_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_selections -> {
                clearSelectionsMenuClick()
                return true
            }

            R.id.log_selected_days -> {
                logSelectedDaysMenuClick()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun clearSelectionsMenuClick() {
        calendar_view.clearSelections()
    }

    private fun logSelectedDaysMenuClick() {
        Toast.makeText(this, "Selected " + calendar_view.selectedDays.size, Toast.LENGTH_SHORT).show()
    }

    override fun onCheckedChanged(group: RadioGroup, @IdRes checkedId: Int) {
        clearSelectionsMenuClick()
        initViews()
        when (checkedId) {
            R.id.rb_horizontal -> calendar_view.calendarOrientation = OrientationHelper.HORIZONTAL

            R.id.rb_vertical -> calendar_view.calendarOrientation = OrientationHelper.VERTICAL

            R.id.rb_single -> calendar_view.selectionType = SelectionType.SINGLE

            R.id.rb_multiple -> calendar_view.selectionType = SelectionType.MULTIPLE

            R.id.rb_range -> calendar_view.selectionType = SelectionType.RANGE

            R.id.rb_none -> calendar_view.selectionType = SelectionType.NONE
        }
    }
}

