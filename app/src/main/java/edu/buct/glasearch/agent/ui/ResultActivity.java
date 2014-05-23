package edu.buct.glasearch.agent.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.List;

import edu.buct.glasearch.agent.R;
import edu.buct.glasearch.agent.entity.ImageInfo;
import edu.buct.glasearch.agent.entity.SearchResult;

public class ResultActivity extends Activity {

    private static final String IMAGE_URL = "http://192.168.1.109:9096/imagesearch/search/image?id=";
	
	private ListView resultList = null;
    private TextView searchParam;
	
	private Gson gson = new Gson();

    private class ResultListAdapter extends ArrayAdapter<ImageInfo> {

        private static final int LAYOUT_RESOURCE = R.layout.list_item_search_result;

        private class ViewHolder {
            ImageView resultImage;
            TextView resultName;
            TextView resultDetail;
        }

        public ResultListAdapter(Context context, List<ImageInfo> listMap) {
            super(context, R.layout.list_item_search_result, listMap);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(LAYOUT_RESOURCE, null);

                holder = new ViewHolder();
                holder.resultImage = (ImageView) convertView.findViewById(R.id.resultImage);
                holder.resultName = (TextView) convertView.findViewById(R.id.resultName);
                holder.resultDetail = (TextView) convertView.findViewById(R.id.resultDetail);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }


            final ImageInfo result = getItem(position);

            if (result != null) {
                holder.resultName.setText(result.getTitle());
                holder.resultDetail.setText(result.getLocation());

                holder.resultImage.setImageURI(Uri.parse(IMAGE_URL + result.getFileName()));
            } else {
                holder.resultName.setText("");
                holder.resultDetail.setText("");
                holder.resultImage.setImageURI(null);
            }

            return convertView;
        }
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);
		
		resultList = (ListView)findViewById(R.id.resultList);
        searchParam = (TextView)findViewById(R.id.searchParam);

        Intent intent = getIntent();
        String result = intent.getStringExtra("result");
        SearchResult jsonResult = gson.fromJson(result, SearchResult.class);

        resultList.setAdapter(new ResultListAdapter(this, jsonResult.getImageList()));

        searchParam.setText("Voice text: " + jsonResult.getWords() +
                            ". lat:" + jsonResult.getLat() + ", lng: " + jsonResult.getLng());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.result, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
