package edu.csulb.petsitter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class MainActivityContainer extends AppCompatActivity {

    //BottomNavigationBar item select listener
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()){
                        case R.id.favorite_item:
                            //Set the fragment to favorite fragment
                            selectedFragment = FilterCalendarFragment.newInstance();
                            break;
                        case R.id.history_item:
                            //Set the fragment to history fragment
                            selectedFragment = HistoryFragment.newInstance();
                            break;
                        case R.id.find_item:
                            //Set fragment to find fragment
                            selectedFragment = FindFragment.newInstance();
                            break;
                        case R.id.inbox_item:
                            //Set fragment to inbox fragment
                            selectedFragment = InboxFragment.newInstance();
                            break;
                        case R.id.profile_item:
                            //Set fragment to profile fragment
                            selectedFragment = ProfileFragment.newInstance();
                            break;
                    }
                    //To the program to change to the current selected fragment
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_content, selectedFragment);
                    transaction.commit();
                    return true;
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_container);
        setUpBottomNavigationBar();

    }

    //Create bottomNavigationBar listener and set default selected item to findItem
    private void setUpBottomNavigationBar(){
        BottomNavigationView bottomNavigationBar = (BottomNavigationView) findViewById(R.id.main_activity_bottom_navigation_view);
        //Disable the shift animation when the item is selected
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationBar);
        //Set the default selected item to find item
        bottomNavigationBar.setSelectedItemId(R.id.find_item);
        bottomNavigationBar.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, FindFragment.newInstance());
        transaction.commit();
    }
}
