package com.PouyaApp.kookyargitar;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.PouyaApp.kookyargitar.R;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

public class GitarTuner extends Activity implements OnClickListener
		 {

	private static final String TAG = "KookYaR";
	private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1001;
	
	final private int highSensitive = 55;
	final private int midSensitive = 50;
	final private int lowSensitive = 45;
	
	private PdUiDispatcher dispatcher;
	public String fonts = "BZar.ttf";
	private LinearLayout ivTar;
	private Button tar1, tar2, tar3, tar4, tar5, tar6;
	private TextView pitchLabel;
	private PitchView pitchView;
	private boolean kukSelected = false;
	private double trigeeredNote;
	OnSharedPreferenceChangeListener listener;
	private int laFrequens = 440;
	private int sensitiveLevel = midSensitive;
	private int pressure;
	Thread t;
	private boolean threadRuned = false;
	private boolean threadRuned2 = false;

	private double[] Miditone = {64, 59, 55, 50, 45, 40};

	private PdService pdService = null;
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		MenuInflater blowUp = getMenuInflater();
		blowUp.inflate(R.menu.gitar_menu, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int id = item.getItemId();
		if (id == R.id.help_tuner) {
			Intent i = new Intent(GitarTuner.this, TunerHelp.class);
			startActivity(i);
		} else if (id == R.id.bazgasht) {
			finish();
		} else if (id == R.id.settings_menu) {
			Intent j = new Intent(GitarTuner.this, Prefs.class);
			startActivity(j);
		}
		return true;
	}

	private void prefsListnner() {
		// TODO Auto-generated method stub
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs,
					String key) {
				// Implementation

				laFrequens = Integer.parseInt(prefs.getString("ref", "440"));
				pitchView.setAFrequnse(laFrequens);

				if (prefs.getString("sensitive", "2").compareTo("3") == 0) {
					sensitiveLevel = highSensitive;

				} else if (prefs.getString("sensitive", "2").compareTo("1") == 0) {
					sensitiveLevel = midSensitive;
				} else {
					sensitiveLevel = lowSensitive;
				}
				
				tar1.setBackgroundResource(R.drawable.button_tuner_style);
				tar2.setBackgroundResource(R.drawable.button_tuner_style);
				tar3.setBackgroundResource(R.drawable.button_tuner_style);
				tar4.setBackgroundResource(R.drawable.button_tuner_style);
				tar5.setBackgroundResource(R.drawable.button_tuner_style);
				tar6.setBackgroundResource(R.drawable.button_tuner_style);

				pitchLabel.setText(PersianReshape
						.reshape("سیم مورد نظر جهت کوک کردن را انتخاب کنید"));
				pitchView.setCenterPitch(0);

			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}

	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder) service).getService();
			try {
				initPd();
				loadPatch();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// this method will never be called
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gitar_tuner);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		initGui();
		prefsListnner();

		// Check and request microphone permission
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.RECORD_AUDIO},
					PERMISSION_REQUEST_RECORD_AUDIO);
		} else {
			initAudio();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				initAudio();
			} else {
				Toast.makeText(this, "دسترسی به میکروفون برای کار کردن کوک یار ضروری است", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	private void initAudio() {
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		pitchView.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		pitchView.resume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(pdConnection);
	}

	private void initGui() {


		Typeface face = Typeface.createFromAsset(getAssets(), "font/" + fonts
				+ "");
		
		ivTar = (LinearLayout) findViewById(R.id.layout_sim);

		tar1 = (Button) findViewById(R.id.tar_1);
		tar1.setOnClickListener(this);
		tar1.setTypeface(face);
		String str_b1 = (String) tar1.getText().toString();
		tar1.setText(PersianReshape.reshape(str_b1));
		

		// -----------------------------------------------
		tar2 = (Button) findViewById(R.id.tar_2);
		tar2.setOnClickListener(this);
		tar2.setTypeface(face);
		String str_b2 = (String) tar2.getText().toString();
		tar2.setText(PersianReshape.reshape(str_b2));

		// ------------------------------------------------
		tar3 = (Button) findViewById(R.id.tar_3);
		tar3.setOnClickListener(this);
		tar3.setTypeface(face);
		String str_b3 = (String) tar3.getText().toString();
		tar3.setText(PersianReshape.reshape(str_b3));

		// ------------------------------------------------
		tar4 = (Button) findViewById(R.id.tar_4);
		tar4.setOnClickListener(this);
		tar4.setTypeface(face);
		String str_b4 = (String) tar4.getText().toString();
		tar4.setText(PersianReshape.reshape(str_b4));


		// ------------------------------------------------
		tar5 = (Button) findViewById(R.id.tar_5);
		tar5.setOnClickListener(this);
		tar5.setTypeface(face);
		String str_b5 = (String) tar5.getText().toString();
		tar5.setText(PersianReshape.reshape(str_b5));


		// ------------------------------------------------
		tar6 = (Button) findViewById(R.id.tar_6);
		tar6.setOnClickListener(this);
		tar6.setTypeface(face);
		String str_b6 = (String) tar6.getText().toString();
		tar6.setText(PersianReshape.reshape(str_b6));


		// ------------------------------------------------
		pitchLabel = (TextView) findViewById(R.id.pitch_label);
		pitchView = (PitchView) findViewById(R.id.pitch_view);


		SharedPreferences getprefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		laFrequens = Integer.parseInt(getprefs.getString("ref", "440"));
		pitchView.setAFrequnse(laFrequens);

		if (getprefs.getString("sensitive", "2").compareTo("3") == 0) {
			sensitiveLevel = highSensitive;

		} else if (getprefs.getString("sensitive", "2").compareTo("1") == 0) {
			sensitiveLevel = midSensitive;
		} else {
			sensitiveLevel = lowSensitive;
		}

		
		pitchLabel.setText(PersianReshape
				.reshape("سیم مورد نظر جهت کوک کردن را انتخاب کنید"));


		tar1.setBackgroundResource(R.drawable.button_tuner_style);
		tar2.setBackgroundResource(R.drawable.button_tuner_style);
		tar3.setBackgroundResource(R.drawable.button_tuner_style);
		tar4.setBackgroundResource(R.drawable.button_tuner_style);
		tar5.setBackgroundResource(R.drawable.button_tuner_style);
		tar6.setBackgroundResource(R.drawable.button_tuner_style);
	}

	private void initPd() throws IOException {
		// Configure the audio glue
		AudioParameters.init(this);
		int sampleRate = AudioParameters.suggestSampleRate();
		pdService.initAudio(sampleRate, 1, 2, 20.0f);
		start();

		// Create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);
		
		dispatcher.addListener("pitch", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, final float x) {

				if (pressure >= sensitiveLevel) {
					if(threadRuned){
						threadRuned2=false;
					}
					float pitch = (float) Math.round(x * 1000) / 1000;
					pitchView.setCurrentPitch(pitch);

					if (t == null || (pitchView.getCenterPitch() != 12 && !threadRuned)) {
						t = new Thread(new Thread() {

							@Override
							public void run() {
								threadRuned = true;
								threadRuned2 = true;
								try {
									Log.d(TAG, "run: " + Thread.currentThread());
									sleep(2500);
									Log.d(TAG, "stop: " + Thread.currentThread());
									if (threadRuned2) {
										pitchView.setCurrentPitch(12);
									}
									threadRuned = false;
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						});
						t.start();
					}
				}
			}
		});
		
		dispatcher.addListener("pressure", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, final float z) {
				pressure = (int) z;
			}
		});
	}

	private void start() {
		if (!pdService.isRunning()) {
			// Use startAudio without notification to avoid PendingIntent issues on Android S+
			pdService.startAudio();
		}
	}

	private void loadPatch() throws IOException {
		File dir = getFilesDir();
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.tuner),
				dir, true);
		File patchFile = new File(dir, "tuner.pd");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}

	// Phone state listener removed - not needed for core functionality
	// and requires READ_PHONE_STATE permission which is restricted on modern Android

	private void triggerNote(float triggeredNote) {
		float realMidi = midiToRealMidi(triggeredNote);
		pitchView.setCenterPitch(realMidi);
		pitchView.setCurrentPitch(12);
		pitchView.setMidiRef(triggeredNote);
		PdBase.sendFloat("midinote", realMidi);
		PdBase.sendBang("trigger");
	}
	
	private float midiToRealMidi(float midi) {
		// Convert MIDI note to real frequency representation
		return midi;
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		
		// Reset all button backgrounds
		tar1.setBackgroundResource(R.drawable.button_tuner_style);
		tar2.setBackgroundResource(R.drawable.button_tuner_style);
		tar3.setBackgroundResource(R.drawable.button_tuner_style);
		tar4.setBackgroundResource(R.drawable.button_tuner_style);
		tar5.setBackgroundResource(R.drawable.button_tuner_style);
		tar6.setBackgroundResource(R.drawable.button_tuner_style);
		
		if (viewId == R.id.tar_1) {
			triggerNote((float) (Miditone[0]));
			pitchLabel.setText(PersianReshape.reshape("سیم اول"));
			ivTar.setBackgroundResource(R.drawable.gitar1);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar1.setBackgroundResource(R.drawable.button_tuner_selected);
		} else if (viewId == R.id.tar_2) {
			triggerNote((float) (Miditone[1]));
			pitchLabel.setText(PersianReshape.reshape("سیم دوم"));
			ivTar.setBackgroundResource(R.drawable.gitar2);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar2.setBackgroundResource(R.drawable.button_tuner_selected);
		} else if (viewId == R.id.tar_3) {
			triggerNote((float) (Miditone[2]));
			pitchLabel.setText(PersianReshape.reshape("سیم سوم"));
			ivTar.setBackgroundResource(R.drawable.gitar3);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar3.setBackgroundResource(R.drawable.button_tuner_selected);
		} else if (viewId == R.id.tar_4) {
			triggerNote((float) (Miditone[3]));
			pitchLabel.setText(PersianReshape.reshape("سیم چهارم"));
			ivTar.setBackgroundResource(R.drawable.gitar4);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar4.setBackgroundResource(R.drawable.button_tuner_selected);
		} else if (viewId == R.id.tar_5) {
			triggerNote((float) (Miditone[4]));
			pitchLabel.setText(PersianReshape.reshape("سیم پنجم"));
			ivTar.setBackgroundResource(R.drawable.gitar5);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar5.setBackgroundResource(R.drawable.button_tuner_selected);
		} else if (viewId == R.id.tar_6) {
			triggerNote((float) (Miditone[5]));
			pitchLabel.setText(PersianReshape.reshape("سیم ششم"));
			ivTar.setBackgroundResource(R.drawable.gitar6);
			YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
			tar6.setBackgroundResource(R.drawable.button_tuner_selected);
		}
	}

	private float midiToFrequnes(float i) {
		double frequens = laFrequens*Math.pow(2, ((i-69)/12)) ;
		return (float) frequens;
	}

}