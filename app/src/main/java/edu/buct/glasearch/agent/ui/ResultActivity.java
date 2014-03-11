package edu.buct.glasearch.agent.ui;

import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import edu.buct.glasearch.agent.R;

public class ResultActivity extends Activity {
	
	private ListView resultList = null;
	
	private Gson gson = new Gson();

    private class ResultListAdapter extends ArrayAdapter<Map> {

        private static final int LAYOUT_RESOURCE = R.layout.list_item_search_result;

        private class ViewHolder {
            ImageView resultImage;
            TextView resultName;
            TextView resultDetail;
        }

        public ResultListAdapter(Context context, List<Map> listMap) {
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


            final Map result = getItem(position);

            if (result != null) {
                holder.resultName.setText((String)result.get("title"));
                holder.resultDetail.setText((String)result.get("location"));
                holder.resultImage.setImageURI(null);
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

        Intent intent = getIntent();
        String result = intent.getStringExtra("result");
        List<Map> jsonResult = gson.fromJson(result, List.class);

        resultList.setAdapter(new ResultListAdapter(this, jsonResult));
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
