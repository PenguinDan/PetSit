package edu.csulb.petsitter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivityContainer extends AppCompatActivity {

    private TextView mTextMessage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_container);
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.main_activity_bottom_navigation_view);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.find_item);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.favorite_item:
                                return true;
                            case R.id.history_item:
                                return true;
                            case R.id.find_item:
                                return true;
                            case R.id.inbox_item:
                                return true;
                            case R.id.profile_item:
                                return true;
                        }
                        return false;
                    }
                });
    }



}
