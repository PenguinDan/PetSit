package edu.csulb.petsitter;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ProfileFragment extends Fragment {

    private ConstraintLayout profileConstraintLayout;
    private ConstraintLayout notificationsConstraintLayout;
    private ConstraintLayout settingsConstraintLayout;
    private ConstraintLayout becomeHostConstraintLayout;
    private ConstraintLayout giveFeedbackConstraintLayout;

    public static ProfileFragment newInstance(){
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    public
}
