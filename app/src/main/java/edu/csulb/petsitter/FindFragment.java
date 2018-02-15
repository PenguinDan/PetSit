package edu.csulb.petsitter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FindFragment extends Fragment {

    private Button filterCalendarButton;
    FindSectionListener activityCommander;

    public interface FindSectionListener{
        public void displayFilterCalendar();
    }

    public static FindFragment newInstance(){
        FindFragment fragment = new FindFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {

        }catch(ClassCastException e){
            throw new ClassCastException();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find, container, false);
        filterCalendarButton = view.findViewById(R.id.dates_button);
        filterCalendarButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }

        });
        return view;

    }
}
