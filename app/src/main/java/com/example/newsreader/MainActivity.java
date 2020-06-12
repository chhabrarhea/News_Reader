package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase articlesDB;
    ArrayList<String> titles;
    ArrayList<String> content;
    ArrayAdapter<String> arrayAdapter;


    public class Download extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... urls) {
            URL url;
            HttpURLConnection http=null;
            String result="";
            try{
                url=new URL(urls[0]);
                http=(HttpURLConnection)url.openConnection();
                InputStream inputStream=http.getInputStream();
                InputStreamReader inputStreamReader=new InputStreamReader(inputStream);
                int data=inputStreamReader.read();
                while(data!=-1)
                {
                    char c=(char) data;
                    result+=c;
                    data=inputStreamReader.read();
                }
                JSONArray JSarr=new JSONArray(result);
                int num=20;
                if(JSarr.length()<num)
                    num=JSarr.length();
                articlesDB.execSQL("DELETE FROM articles");

                for(int i=0;i<num;i++)
                {
                    String articleId=JSarr.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    http=(HttpURLConnection) url.openConnection();
                    inputStream=http.getInputStream();
                    inputStreamReader=new InputStreamReader(inputStream);
                    data=inputStreamReader.read();
                    String res="";
                    while(data!=-1)
                    {
                        char c=(char) data;
                        res+=c;
                        data=inputStreamReader.read();
                    }

                    JSONObject obj=new JSONObject(res);
                    if(!obj.isNull("title") && (!obj.isNull("url")))
                    {
                        String articleTitle=obj.getString("title");
                        String articleUrl=obj.getString("url");
                        url=new URL(articleUrl);
                        http=(HttpURLConnection) url.openConnection();
                        inputStream=http.getInputStream();
                        inputStreamReader=new InputStreamReader(inputStream);
                        data=inputStreamReader.read();
                        String articleContent="";
                        while(data!=-1)
                        {
                            char c=(char) data;
                            articleContent+=c;
                            data=inputStreamReader.read();
                        }
                        String sql="INSERT INTO articles(articleId,articleTitle,articleContent) VALUES(?,?,?)";
                        SQLiteStatement statement=articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);
                        statement.execute();

                    }


                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }


    public void updateListView()
    {
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {

                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articlesDB=this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,articleTitle VARCHAR,articleContent VARCHAR)");
        arrayAdapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,titles);
        Download task=new Download();
        try {

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {

        }

        ListView lv=(ListView)  findViewById(R.id.kk);
        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent in=new Intent(MainActivity.this,contentView.class);
                in.putExtra("content",position);
                startActivity(in);

            }
        });
        updateListView();
    }
}
