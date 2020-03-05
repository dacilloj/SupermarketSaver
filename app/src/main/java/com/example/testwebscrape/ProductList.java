package com.example.testwebscrape;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import java.util.ArrayList;

public class ProductList extends AppCompatActivity  implements  LoaderManager.LoaderCallbacks<ArrayList<Products>> {
    private ViewStub list_stub;
    private ViewStub grid_stub;
    GridView rootGrid;
    ListView rootList;
    GridProductAdapter gridAdapter;
    ListProductAdapter listAdapter;
    private int currentViewMode=0;

    TextView emptyState;
    static final int VIEW_MODE_LISTVIEW=0;
    static final int VIEW_MODE_GRIDVIEW=1;
    ArrayList<Products> product;

    ProgressBar progressBar;
    int alreadySearched=0;

    static String TescoUrl="";
    static String SupervaluUrl ="";

    private RecyclerView recyclerView;
    private RecycleListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);
        list_stub=findViewById(R.id.stub_list);
        grid_stub=findViewById(R.id.stub_grid);

        progressBar=findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.VISIBLE);

        Bundle bundle=getIntent().getExtras();
        TescoUrl=bundle.getString("TescoUrl");
        SupervaluUrl=bundle.getString("SupervaluUrl");

        list_stub.inflate();
        grid_stub.inflate();

        rootList=findViewById(R.id.listView);
        rootGrid=findViewById(R.id.gridView);

        recyclerView=findViewById(R.id.recycle_list);
        layoutManager=new LinearLayoutManager(this);


        //Checking the default view saved in the shared preference
        SharedPreferences sharedPreferences=getSharedPreferences("ViewMode",MODE_PRIVATE);
        currentViewMode=sharedPreferences.getInt("currentViewMode",VIEW_MODE_LISTVIEW);

        //Displays back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Checking the network connectivity
        ConnectivityManager conManager= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=conManager.getActiveNetworkInfo();
        //Checking network connectivity
        if (networkInfo !=null && networkInfo.isConnected()){

            if(alreadySearched==0) {
                getSupportLoaderManager().initLoader(100, null, this).forceLoad();
                alreadySearched=1;
            }
        }else{
            progressBar.setVisibility(View.GONE);
//            emptyState.setText("No network Connection");
        }
    }

    //saving instance for before rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("alreadySearch",alreadySearched);
        super.onSaveInstanceState(outState);
    }

    //restoring instaces after rotation
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        alreadySearched=savedInstanceState.getInt("alreadySearch");
        super.onRestoreInstanceState(savedInstanceState);
    }

    //used to switch the view
    private void switchView() {
        if (VIEW_MODE_LISTVIEW==currentViewMode){
            list_stub.setVisibility(View.VISIBLE);
            grid_stub.setVisibility(View.GONE);
        }else {
            list_stub.setVisibility(View.GONE);
            grid_stub.setVisibility(View.VISIBLE);
        }

        setAdapter();
    }

    //used to set the Adapter
    private void setAdapter() {
        if (VIEW_MODE_LISTVIEW==currentViewMode){
            adapter=new RecycleListAdapter(product);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener(new RecycleListAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    Products pro=product.get(position);
                    String url=pro.getUrlLink();

                    Intent i=new Intent(ProductList.this,webView.class);
                    i.putExtra("UrlWebLink",url);
                    startActivity(i);
                }

                @Override
                public void onShareClick(int position) {
                    Products pro=product.get(position);
                    String url=pro.getUrlLink();

                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "I found this item in price Compare App \n"+url);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                    Toast.makeText(ProductList.this,"the share postion "+position,Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSaveClick(int position) {
                    product.get(position);
                    Toast.makeText(ProductList.this,"the postion "+position,Toast.LENGTH_SHORT).show();
                }
            });

            //listAdapter=new ListProductAdapter(this,product);
            //rootList.setAdapter(listAdapter);
        }else{
            gridAdapter=new GridProductAdapter(this,product);
            rootGrid.setAdapter(gridAdapter);
        }
    }

    //setting the menu with the switch mode option
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    //listener when an item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item1:
                if (VIEW_MODE_LISTVIEW==currentViewMode){
                    currentViewMode=VIEW_MODE_GRIDVIEW;
                }else{
                    currentViewMode=VIEW_MODE_LISTVIEW;
                }

                //switch the view
                switchView();

                //save
                SharedPreferences sharedPreferences=getSharedPreferences("ViewMode",MODE_PRIVATE);
                SharedPreferences.Editor editor=sharedPreferences.edit();
                editor.putInt("currentViewMode",currentViewMode);
                editor.commit();
                break;

            //used to close the activity
            case R.id.search:
                finish();
                break;

            //back button used to close the activity
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<ArrayList<Products>> onCreateLoader(int id, @Nullable Bundle args) {
        return new ProductAsyncLoader(ProductList.this);
    }

    //Call when background thread has finished loading
    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Products>> loader, ArrayList<Products> data) {
        progressBar.setVisibility(View.GONE);
        if (data!=null){
            UpdateUi(data);
        }else {

        }


    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Products>> loader) {

    }

    private static class ProductAsyncLoader extends AsyncTaskLoader<ArrayList<Products>> {

        ProductAsyncLoader(@NonNull Context context) {
            super(context);
        }

        @Nullable
        @Override
        public ArrayList<Products> loadInBackground() {
            ArrayList<Products>  prod= (ArrayList<Products>) QueryUtil.fetchWebsiteData(TescoUrl, SupervaluUrl);

            return prod;
        }
    }

    //Used to update xml layouts
    private void UpdateUi(ArrayList<Products> data) {
        product=data;
        switchView();

        rootList.setOnItemClickListener(onItemClickListener);
        rootGrid.setOnItemClickListener(onItemClickListener);
    }

    //used to set item listener
    AdapterView.OnItemClickListener onItemClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Products pro=product.get(position);
            String url=pro.getUrlLink();

            Intent i=new Intent(ProductList.this,webView.class);
            i.putExtra("UrlWebLink",url);
            startActivity(i);
        }
    };

    public void queryProducts(){
    }
}