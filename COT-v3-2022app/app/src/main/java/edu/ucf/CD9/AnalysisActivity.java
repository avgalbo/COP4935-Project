package edu.ucf.CD9;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

public class AnalysisActivity extends AppCompatActivity implements
        AnalysisSummaryFragment.OnFragmentInteractionListener{

    FragmentManager fragmentManager;
//    String number, contact;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            fragmentManager = getSupportFragmentManager();

            switch (item.getItemId()) {
                case R.id.navigation_text_analysis:
                    if(fragmentManager.getBackStackEntryCount()>0)
                        fragmentManager.popBackStack();
                    AnalysisSummaryFragment textAnalysisFragment = new AnalysisSummaryFragment();
                    //setTitle("Summary");
                    fragmentManager.beginTransaction().replace(R.id.rlContentAnalysis, textAnalysisFragment, "TextAnalysisScreen").commit();
                    return true;
                case R.id.navigation_image_analysis:
                    if(fragmentManager.getBackStackEntryCount()>0)
                        fragmentManager.popBackStack();
                    //setTitle("Details");
                    TextsFragment textsFragment = TextsFragment.newInstance(getIntent().getStringExtra("number"),
                            getIntent().getStringExtra("contact"), getIntent().getByteArrayExtra("pic"), false, AnalysisSummaryFragment.trusted);
                    fragmentManager.beginTransaction().replace(R.id.rlContentAnalysis, textsFragment, "TextDetailsScreen").commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        FragmentManager fragmentManager = getSupportFragmentManager();
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
//        number = getIntent().getStringExtra("number");
//        contact = getIntent().getStringExtra("contact");

        fragmentManager = getSupportFragmentManager();
        if(fragmentManager.getBackStackEntryCount()>0)
            fragmentManager.popBackStack();

        if(getIntent().getBooleanExtra("showDetails", false)){
            //launched form notification, show texts
            TextsFragment textsFragment = TextsFragment.newInstance(getIntent().getStringExtra("number"),
                    getIntent().getStringExtra("contact"), null, true,
                    getIntent().getBooleanExtra("cot", false));
            fragmentManager.beginTransaction().replace(R.id.rlContentAnalysis, textsFragment, "TextDetailsScreen").commit();
            navigation.setSelectedItemId(R.id.navigation_image_analysis);
        }else{
            AnalysisSummaryFragment textAnalysisFragment = new AnalysisSummaryFragment();
            fragmentManager.beginTransaction().replace(R.id.rlContentAnalysis, textAnalysisFragment, "TextAnalysisScreen").commit();
        }
    }

    @Override
    public void onFragmentInteraction(String data) {

    }
}
