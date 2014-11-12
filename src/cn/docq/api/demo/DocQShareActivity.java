package cn.docq.api.demo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;

public class DocQShareActivity extends Activity {
	private static final int REQ_FILE_CHOOSE = 1;
	private static final String DOCQ_SHARE_URI = "http://api.docq.cn/convert";
	@SuppressWarnings("unused")
	@Deprecated
	private static final String DOCQ_SHARE_URI_CANDICATE = "http://api.top1m.net/convert";

	private static class AsyncShareTask extends AsyncTask<Uri, Long, JSONObject> {
		private Context context;
		public AsyncShareTask(Context context){
			this.context = context;
		}
		
		
		@Override
		protected JSONObject doInBackground(Uri... params) {
			Assert.assertTrue(params.length == 1);
			Uri uri = params[0];

			String fileName = null;
			ParcelFileDescriptor pfd = null;

			String scheme = uri.getScheme();
			if (scheme.equals("file")) {

				File file = new File(uri.getPath());
				fileName = file.getName();
				try {
					pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (scheme.equals("content")) {
				ContentResolver resolver = this.context.getContentResolver();
				Cursor cursor = resolver.query(uri, null, null, null, null);
				if (cursor.moveToFirst()) {
					int nameIndex = cursor.getColumnIndex("_display_name");
					if (nameIndex != -1) {
						fileName = cursor.getString(nameIndex);
						if (fileName != null) {
							try {
								pfd = resolver.openFileDescriptor(uri, "r");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			if (fileName != null && pfd != null) {
				List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
				reqParams.add(new BasicNameValuePair("app_key", "(Your app key)")); // TODO Fill with your app key
				reqParams.add(new BasicNameValuePair("target_format", "html"));

				HttpClient client = new DefaultHttpClient();
				HttpPost request = new HttpPost(DocQShareActivity.DOCQ_SHARE_URI);
				// We recommend using http-client-mime
				// http://hc.apache.org/
				DocQFileEntity entity = new DocQFileEntity(fileName, pfd, reqParams);
				request.setEntity(entity);
				try {
					HttpResponse response = client.execute(request);
					String jsonStr = EntityUtils.toString(response.getEntity());
					return new JSONObject(jsonStr);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				try {
					pfd.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	
	private Button addFileBtn;
	private Button docStatusBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_doc_share);
		// 初始化组件
		this.addFileBtn = (Button) this.findViewById(R.id.docq_share_add_file);
		this.addFileBtn.setText("Choose file...");
		this.addFileBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*");
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				DocQShareActivity.this.startActivityForResult(intent, DocQShareActivity.REQ_FILE_CHOOSE);
			}
		});
		

		this.docStatusBtn = (Button) this.findViewById(R.id.docq_share_doc_status);
		this.docStatusBtn.setVisibility(View.GONE);
		this.docStatusBtn.setTag(null);
		this.docStatusBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Object tag = v.getTag();
				if(tag != null) {
					 Intent intent = new Intent();
					 intent.setAction(Intent.ACTION_VIEW);
					 Uri contenturl = Uri.parse(tag.toString());   
					 intent.setData(contenturl);  
					 DocQShareActivity.this.startActivity(intent);
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_FILE_CHOOSE:
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				this.addTask(uri);
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void addTask(Uri uri) {
		new AsyncShareTask(this) {
			@Override
			protected void onPreExecute() {
				DocQShareActivity.this.addFileBtn.setText("Uploading...");
				DocQShareActivity.this.addFileBtn.setEnabled(false);
				DocQShareActivity.this.docStatusBtn.setVisibility(View.GONE);
				DocQShareActivity.this.docStatusBtn.setTag(null);
				super.onPreExecute();
			}

			@Override
			protected void onPostExecute(JSONObject result) {
				DocQShareActivity.this.addFileBtn.setText("Choose file...");
				DocQShareActivity.this.addFileBtn.setEnabled(true);
				DocQShareActivity.this.docStatusBtn.setVisibility(View.VISIBLE);
				if(result != null && result.has("url")) {
					String url = result.optString("url");
					DocQShareActivity.this.docStatusBtn.setText("View file...");
					DocQShareActivity.this.docStatusBtn.setTag(url);
					DocQShareActivity.this.docStatusBtn.setEnabled(true);	
				} else {
					String errorMsg = result == null? "Fail" : result.optString("error", "Fail");
					DocQShareActivity.this.docStatusBtn.setText(errorMsg);
					DocQShareActivity.this.docStatusBtn.setEnabled(false);	
				}
				super.onPostExecute(result);
			}
		}.execute(uri);
	}
}
