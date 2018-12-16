package com.example.yuval.takecare;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class GiverMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giver_menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.giver_menu_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                intent = new Intent(this, TakerMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCategorySelect(View view) {
        String category;
        switch(view.getId()) {
            case R.id.category_food:
                category = "Food";
                break;
            case R.id.category_study_material:
                category = "Study Material";
                break;
            case R.id.category_households:
                category = "Households";
                break;
            case R.id.category_lost_and_found:
                category = "Lost & Found";
                break;
            case R.id.category_hitchhike:
                category = "Hitchhike";
                break;
            case R.id.category_other:
                category = "Other";
                break;
            default:
                category = "ERROR";
        }
        Intent intent = new Intent(this, GiverFormActivity.class);
        intent.putExtra("CATEGORY", category);
        startActivity(intent);
    }
}
