package com.appspot.wecookbob.activity;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.appspot.wecookbob.R;
import com.appspot.wecookbob.R.id;
import com.appspot.wecookbob.R.layout;
import com.appspot.wecookbob.R.menu;
import com.appspot.wecookbob.data.BobLogSQLiteOpenHelper;
import com.appspot.wecookbob.data.PreferenceUtil;
import com.appspot.wecookbob.data.PreferenceUtil.PROPERTY;
import com.appspot.wecookbob.lib.PostRequestForm;
import com.appspot.wecookbob.lib.PostRequestForm.OnResponse;
import com.appspot.wecookbob.view.BobLog;
import com.appspot.wecookbob.view.BobLogListviewAdapter;
import com.appspot.wecookbob.view.SignUpDialog;
import com.appspot.wecookbob.view.BobLog.NotificationType;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends ActionBarActivity implements OnResponse {
	private SQLiteDatabase bobLogDb;
	private BobLogSQLiteOpenHelper bobLogHelper;

	//declare main listview components
	private ListView BobLogListView;
	public static BobLogListviewAdapter BobLogAdapter;
	private ArrayList<BobLog> bobLogArray;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String SENDER_ID = "71421696637";

	private GoogleCloudMessaging _gcm;
	private String _regId;
	private PreferenceUtil pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.bob_main);
		// Listener 
		this.findViewById(R.id.addFriendBtn).setOnClickListener(this.showAddFriendTab);
		
		String deviceId = ((TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		
		this.pref = PreferenceUtil.getInstance(getApplicationContext());
		this.pref.putString("", PROPERTY.SIGNUP_ID);
		this.pref.putString("", PROPERTY.SIGNUP_PW);
		this.pref.putString("", PROPERTY.SIGNUP_MOBILE);
		this.pref.putString(deviceId, PROPERTY.DEVICE_ID);
		if(pref.getBoolean(PROPERTY.REGISTERED, false)!=null && pref.getBoolean(PROPERTY.REGISTERED, false).booleanValue())
			new PostRequestForm(MainActivity.this, "http://wecookbob.appspot.com/register")
			.put("user-id", pref.getString(PROPERTY.USER_ID, ""))
			.put("reg-id", pref.getString(PROPERTY.REG_ID, ""))
			.put("device-id", pref.getString(PROPERTY.DEVICE_ID, ""))
			.submit();

		// Switches
		((Switch) findViewById(R.id.alarm_switch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean ischecked) {
				if (ischecked) {
					Toast.makeText(getApplicationContext(), "알림", Toast.LENGTH_LONG).show();
					PreferenceUtil.getInstance(getApplicationContext()).putBoolean(true, PROPERTY.ALARM);
				} else {
					Toast.makeText(getApplicationContext(), "알림 ㄴㄴ", Toast.LENGTH_LONG).show();
					PreferenceUtil.getInstance(getApplicationContext()).putBoolean(false, PROPERTY.ALARM);
				}
			}
		});
		((Switch) findViewById(R.id.status_switch)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean ischecked) {
				if (ischecked) {
					Toast.makeText(getApplicationContext(), "배곺",
							Toast.LENGTH_LONG).show();
					PreferenceUtil pref = PreferenceUtil.getInstance(getApplicationContext());
					PostRequestForm form = new PostRequestForm(MainActivity.this, "http://wecookbob.appspot.com/set_hungry");
					form.put("user-id", pref.getString(PROPERTY.USER_ID, ""));
					form.submit();

				} else {
					Toast.makeText(getApplicationContext(), "배불",
							Toast.LENGTH_LONG).show();
					PreferenceUtil pref = PreferenceUtil.getInstance(getApplicationContext());
					PostRequestForm form = new PostRequestForm(MainActivity.this, "http://wecookbob.appspot.com/set_full");
					form.put("user-id", pref.getString(PROPERTY.USER_ID, ""));
					form.submit();
				}
			}
		});

		// Signup Dialog
		new SignUpDialog().show(getFragmentManager(), "Mytag");

		// Check if GooglePlayService is availiable
		if (this.checkPlayServices()) {
			_gcm = GoogleCloudMessaging.getInstance(this);
			_regId = getRegistrationId();
			System.out.println(_regId);
			if (TextUtils.isEmpty(_regId)) registerInBackground();
		} else {
			Log.i("MainActivity.java | onCreate", "|No valid Google Play Services APK found.|");
		}
		// display received msg
		String msg = getIntent().getStringExtra("msg");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.main_activity_action, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	// Check if GooglePlayService is availiable
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i("MainActivity.java | checkPlayService", "|This device is not supported.|");
				finish();
			} return false;
		} return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO : Implementation of function with item button should be implemented.
//		if(item.getItemId() == R.id.action_settings){
//			// ( 1 ) add a new item 
//			// ( 2 ) change add to remove
//			Toast.makeText(MainActivity.this, "식사를 시작해볼텐가?", Toast.LENGTH_SHORT).show();
//			System.out.println("unregister");
//			System.out.println(pref.getString(PROPERTY.USER_ID, ""));
//			System.out.println(pref.getString(PROPERTY.REG_ID, ""));
//			System.out.println(pref.getString(PROPERTY.DEVICE_ID, ""));
//			
//			new PostRequestForm(MainActivity.this, "http://wecookbob.appspot.com/unregister")
//				.put("user-id"  , this.pref.getString(PROPERTY.USER_ID  , ""))
//				.put("reg-id"   , this.pref.getString(PROPERTY.REG_ID   , ""))
//				.put("device-id", this.pref.getString(PROPERTY.DEVICE_ID, ""))
//				.submit();
//			this.pref.putString("", PROPERTY.USER_ID   );
//			this.pref.putString("", PROPERTY.USER_NAME );
//			this.pref.putBoolean(false, PROPERTY.REGISTERED);
//	        this.startActivity(new Intent(this, SignUpActivity.class));
//		} else {
//			// TODO : if a the new item is clicked show "Toast" message.
//		}
		return super.onOptionsItemSelected(item);
	}

	public void showList() {
		System.out.println("showlist!");
		bobLogHelper = new BobLogSQLiteOpenHelper(MainActivity.this,
				"boblog.db",
				null,
				1);
		bobLogDb = bobLogHelper.getReadableDatabase();
		Cursor Cursor = bobLogDb.rawQuery("SELECT * FROM boblog", null);

		// build listview for boblog
		BobLogListView = (ListView) findViewById(R.id.lv_bob_log);
		bobLogArray = new ArrayList<BobLog>();

		while (Cursor.moveToNext()) {
			System.out.println("asdfsa");
			Long bobRequestTime = Cursor.getLong(Cursor.getColumnIndex("bobRequestTime"));
			String bobtnerId = Cursor.getString(Cursor.getColumnIndex("bobtnerId"));
			String bobtnerName = Cursor.getString(Cursor.getColumnIndex("bobtnerName"));
			BobLog.NotificationType notificationType = Cursor.getString(Cursor.getColumnIndex("notificationType")).equals("sent") ? NotificationType.SENT : NotificationType.RECEIVED;
			bobLogArray.add(new BobLog (bobtnerId, bobtnerName, notificationType, bobRequestTime));
		}

		BobLogAdapter = new BobLogListviewAdapter(this, bobLogArray, R.layout.bob_log_list_item, R.id.btn_bob);
		BobLogListView.setAdapter(BobLogAdapter);
	}
	
	View.OnClickListener showAddFriendTab = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			Toast.makeText(MainActivity.this, "애드 프렌드", Toast.LENGTH_SHORT).show();
			startActivity(new Intent(MainActivity.this, ContactsActivity.class));		}
	};
	
	private boolean checkDataBase() {
		SQLiteDatabase checkBobLogDb = null;
		try {
			checkBobLogDb = SQLiteDatabase.openDatabase("//data/data/com.appspot.wecookbob/databases/boblog.db", null, SQLiteDatabase.OPEN_READONLY);
			checkBobLogDb.close();
		} catch (SQLiteException e) {
			// database doesn't exist yet.
		}
		return checkBobLogDb != null ? true : false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// display received msg
		String msg = intent.getStringExtra("msg");
		Log.i("MainActivity.java | onNewIntent", "|" + msg + "|");
	}


	// registration  id를 가져온다.
	private String getRegistrationId() {
		String registrationId = PreferenceUtil.getInstance(getApplicationContext()).getString(PROPERTY.REG_ID, "");
		if (TextUtils.isEmpty(registrationId)) {
			Log.i("MainActivity.java | getRegistrationId", "|Registration not found.|");
			return "";
		}
		int registeredVersion = PreferenceUtil.getInstance(getApplicationContext()).getInteger(PROPERTY.APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion();
		if (registeredVersion != currentVersion) {
			Log.i("MainActivity.java | getRegistrationId", "|App version changed.|");
			return "";
		}
		return registrationId;
	}

	// app version을 가져온다. 뭐에 쓰는건지는 모르겠다.
	private int getAppVersion() {
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	// gcm 서버에 접속해서 registration id를 발급받는다.
	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (_gcm == null) _gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
					_regId = _gcm.register(SENDER_ID);
					msg = "Device registered, registration ID=" + _regId;

					// For this demo: we don't need to send it because the device
					// will send upstream messages to a server that echo back the
					// message using the 'from' address in the message.

					// Persist the regID - no need to register again.
					storeRegistrationId(_regId);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
				} 
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Log.i("MainActivity.java | onPostExecute", "|" + msg + "|");
			}
		}.execute(null, null, null);
	}

	// registraion id를 preference에 저장한다.
	private void storeRegistrationId(String regId) {
		int appVersion = getAppVersion();
		Log.i("MainActivity.java | storeRegistrationId", "|" + "Saving regId on app version " + appVersion + "|");
		this.pref.putString(regId, PROPERTY.REG_ID);
		this.pref.putInteger(appVersion,PROPERTY.APP_VERSION);
	}

	@Override
	public void onResponse(String responseBody) {
		JSONObject jsonResponse;
		try {
			jsonResponse = new JSONObject(responseBody);
			String responseType = jsonResponse.getString("type");
			if(responseType.equals("register-check")) {
				if (jsonResponse.getBoolean("registered")) pref.putBoolean(true, PROPERTY.REGISTERED);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}
}