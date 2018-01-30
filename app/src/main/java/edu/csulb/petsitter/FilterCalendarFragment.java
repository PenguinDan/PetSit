package edu.csulb.petsitter;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andexert.calendarlistview.library.DatePickerController;
import com.andexert.calendarlistview.library.SimpleMonthAdapter;
import com.andexert.calendarlistview.library.DayPickerView;

import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 */
public class FilterCalendarFragment extends Fragment implements DatePickerController {

    private Calendar nextSixMonth;
    private DayPickerView dayPickerView;
    private boolean isSelectedFirstDate, isSelectedLastDate;
    private int selectedFirstDay, selectedFirstMonth, selectedFirstYear;



    public static FilterCalendarFragment newInstance(){
        FilterCalendarFragment fragment = new FilterCalendarFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter_calendar, container, false);
        setNextSixMonth();
        dayPickerView = (DayPickerView) view.findViewById(R.id.filter_calendar_view);
        Calendar calendar = Calendar.getInstance();
        dayPickerView.setController(this);
        return view;
    }

    private void setNextSixMonth(){
        nextSixMonth = Calendar.getInstance();
        nextSixMonth.set(Calendar.getInstance().get(Calendar.YEAR),Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        nextSixMonth.add(Calendar.MONTH, 6);
    }

    @Override
    public int getMaxYear() {
        return nextSixMonth.get(Calendar.YEAR);
    }

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {

    }

    @Override
    public void onDateRangeSelected(SimpleMonthAdapter.SelectedDays<SimpleMonthAdapter.CalendarDay> selectedDays) {

    }
}
