package cn.docq.api.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, DocQShareActivity.class);
		this.startActivity(intent);
		this.finish();
	}
}
