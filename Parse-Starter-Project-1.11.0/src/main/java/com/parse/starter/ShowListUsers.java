package com.parse.starter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ShowListUsers extends AppCompatActivity {

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        ArrayList<String> listToShow = new ArrayList<String>();

        //Collect the arraylist in the intent
        listToShow = getIntent().getStringArrayListExtra("users_list");


        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listToShow);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);


        }

    public void closeAct(View v){
        finish();
    }

}
