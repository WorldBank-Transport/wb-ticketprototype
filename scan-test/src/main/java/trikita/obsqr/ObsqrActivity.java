package trikita.obsqr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import hugo.weaving.DebugLog;

public class ObsqrActivity extends Activity implements CameraPreview.OnQrDecodedListener {

	public final static int PERMISSIONS_REQUEST = 100;

	private CameraPreview mCameraPreview;
	private QrContentDialog mDialog;

	private QrContent mQrContent = null;

	private String mLastKnownContent = "";

	private FusedLocationProviderClient mFusedLocationClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

		mCameraPreview = (CameraPreview) findViewById(R.id.surface);
		mDialog = (QrContentDialog) findViewById(R.id.container);

		mCameraPreview.setOnQrDecodedListener(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST);
			}
		}
	}

	public JSONObject getJSONPayload(Location location) {
		JSONObject payload = new JSONObject();
		try {
			payload.put("value1", Double.toString(location.getLatitude()));
			payload.put("value2", Double.toString(location.getLongitude()));
			if (location.hasSpeed()) {
				payload.put("value3", Float.toString(location.getSpeed()));
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return payload;
	}

	class SendPostRequestTask extends AsyncTask<String, Void, Void> {
		Location location;

		public SendPostRequestTask(Location location) {
			this.location = location;
		}

		@Override
		protected Void doInBackground (String... params) {
			try {
				String url = "https://maker.ifttt.com/trigger/qr_scanned/with/key/dezRlQRUUUWhyCBsyVrjNJ";
				URL obj = new URL(url);
				HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

				//add reuqest header
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

				String urlParameters = "value1=" + Double.toString(location.getLatitude()) + "&value2=" + Double.toString(location.getLongitude());

				// Send post request
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.writeBytes(urlParameters);
				wr.flush();
				wr.close();

				int responseCode = con.getResponseCode();
				System.out.println("\nSending 'POST' request to URL : " + url);
				System.out.println("Post parameters : " + urlParameters);
				System.out.println("Response Code : " + responseCode);

				BufferedReader in = new BufferedReader(
						new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();


				//print result
				System.out.println(response.toString());
				audioPlayer("elevator_ding.mp3");
			}
			catch (Exception e) {
				System.out.println("weirdness occurred");
				e.printStackTrace();
			}
			return null;
		}
	}

	public void audioPlayer(String fileName){
		//set up MediaPlayer
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		MediaPlayer mp = new MediaPlayer();

		try {
			mp.setDataSource(this.getApplicationContext(), soundUri);
			mp.prepare();
			mp.start();
			System.out.println("played a sound");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onQrDecoded(String s) {
		if (mLastKnownContent.equals(s)) { // Same content was cancelled
			return;
		}
		final ObsqrActivity context = this;
		if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			System.err.println("no permission for location");
		}
		mFusedLocationClient.getLastLocation()
				.addOnSuccessListener(this, new OnSuccessListener<Location>() {
					@Override
					public void onSuccess(Location location) {

						// Got last known location. In some rare situations this can be null.
						if (location != null) {

							new SendPostRequestTask(location).execute();
							// Logic to handle location object
						}
					}
				});
		mLastKnownContent = s;
		mQrContent = QrContent.from(this, s);
		mDialog.open(mQrContent);
	}

	@Override
	public void onQrNotFound() {
		// mLastKnownContent = "";
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mQrContent == null) {
			return super.dispatchKeyEvent(event);
		}
		// Pressing DPAD, Volume keys or Camera key would call the QR action
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_CAMERA:
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mQrContent.performAction();
				return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
		}
		boolean success = mCameraPreview.acquireCamera(getWindowManager()
			.getDefaultDisplay().getRotation());
		if (!success) {
			new AlertDialog.Builder(this)
					.setMessage(getString(R.string.dlg_alert_msg))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.dlg_alert_ok_btn_caption),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									ObsqrActivity.this.finish();
									dialog.dismiss();
								}
							})
					.create().show();
		}
	}

	@DebugLog
	@Override
	protected void onPause() {
		mDialog.close();
		mCameraPreview.releaseCamera();
		super.onPause();
	}

	@DebugLog
	@Override
	public void onBackPressed() {
		if (!mDialog.close()) {
			super.onBackPressed();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST &&
				grantResults.length == 1 &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			recreate();
			return;
		}
		finish();
	}
}
