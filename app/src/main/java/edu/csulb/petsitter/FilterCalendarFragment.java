package edu.csulb.petsitter;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andexert.calendarlistview.library.DatePickerController;
import com.andexert.calendarlistview.library.DayPickerView;
import com.andexert.calendarlistview.library.SimpleMonthAdapter;

import java.util.Calendar;
import java.util.Date;

import static com.andexert.calendarlistview.library.SimpleMonthAdapter.CalendarDay;


/**
 * A simple {@link Fragment} subclass.
 */
public class FilterCalendarFragment extends Fragment implements DatePickerController {

    private Calendar nextSixMonth;
    private TextView startDateTextView, endDateTextView, clearTextView;
    private DayPickerView dayPickerView;
    private ConstraintLayout startEndDateLayout;
    private boolean isStartDateSelected;
    private Date firstSelectedDate, secondSelectedDate;
    private View dateSeparator;



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
        startDateTextView = (TextView) view.findViewById(R.id.start_date_text_view);
        endDateTextView = (TextView) view.findViewById(R.id.end_date_text_view);
        startEndDateLayout = (ConstraintLayout) view.findViewById(R.id.display_dates_constraint_layout);
        clearTextView = (TextView) view.findViewById(R.id.clear_text_view);
        dateSeparator = (View) view.findViewById(R.id.date_separator);

        Calendar calendar = Calendar.getInstance();
        dayPickerView.setController(this);

        //Clear all of the highlight dates in the calendar view
        clearTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dayPickerView.getSelectedDays().setFirst(new CalendarDay(-1, -1,-1));
                dayPickerView.getSelectedDays().setLast(new CalendarDay(-1, -1,-1));
                dayPickerView.getAdapter().notifyDataSetChanged();
                isStartDateSelected = false;
                startDateTextView.setText("Start Date");
                startDateTextView.setTextColor(getResources().getColor(R.color.gray));
                endDateTextView.setText("End Date");
                endDateTextView.setTextColor(getResources().getColor(R.color.gray));
                dateSeparator.setBackgroundColor(getResources().getColor(R.color.gray));
                clearTextView.setTextColor(getResources().getColor(R.color.gray));
                clearTextView.setEnabled(false);
            }
        });

        return view;
    }

    //Calculate when for the next siz month
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
        if(isStartDateSelected){
            secondSelectedDate = new Date(year, month, day);
            if(secondSelectedDate.after(firstSelectedDate)){
                displayEndDate(month, day);
                isStartDateSelected = false;
            }
            else{
                //Set the second date picked to be the starting date
                dayPickerView.getSelectedDays().setFirst(new CalendarDay(year,month,day));
                //Set the end date to equal to null
                dayPickerView.getSelectedDays().setLast(new CalendarDay(-1,-1,-1));
                firstSelectedDate = new Date(year, month, day);
                isStartDateSelected = true;
            }

        }
        else{
            displayStartDate(month, day);
            firstSelectedDate = new Date(year, month, day);
        }

    }

    @Override
    public void onDateRangeSelected(SimpleMonthAdapter.SelectedDays<SimpleMonthAdapter.CalendarDay> selectedDays) {

    }

    /*
       Display the start date that the user selected

       @Param:
        month: the start month
        day: the start day
       @Return:
        None

     */
    private void displayStartDate(int month, int day){
        startEndDateLayout.setVisibility(View.VISIBLE);
        startDateTextView.setText(String.format("%s %d", getMonthAbbreviation(month), day));
        startDateTextView.setTextColor(getResources().getColor(R.color.black));
        endDateTextView.setText("End Date");
        endDateTextView.setTextColor(getResources().getColor(R.color.gray));
        dateSeparator.setBackgroundColor(getResources().getColor(R.color.gray));
        clearTextView.setEnabled(true);
        clearTextView.setTextColor(getResources().getColor(R.color.blue));
        isStartDateSelected = true;
    }

    /*
       Display the end date that the user selected

       @Param:
        month: end month
        day: end day
       @Return:
        None

     */
    private void displayEndDate(int month, int day){
        endDateTextView.setText(String.format("%s %d", getMonthAbbreviation(month), day));
        endDateTextView.setTextColor(getResources().getColor(R.color.black));
        dateSeparator.setBackgroundColor(getResources().getColor(R.color.black));
        isStartDateSelected = false;
    }

    /*
       A list of all the month abbreviated

       @Param:
        month: the month number that the methods want to get abbreviation for
       @Return:
        A string of the abbreavated month

     */
    private String getMonthAbbreviation(int month){
        switch(month){
            case 1:
                return "Jan";
            case 2:
                return "Feb";
            case 3:
                return "Mar";
            case 4:
                return "Apr";
            case 5:
                return "May";
            case 6:
                return "Jun";
            case 7:
                return "Jul";
            case 8:
                return "Aug";
            case 9:
                return "Sep";
            case 10:
                return "Oct";
            case 11:
                return "Nov";
            case 12:
                return "Dec";
            default:
                return "Error";

        }
    }
}
