package com.PouyaApp.kookyargitar;

import com.kookyar.common.PersianReshape;
import com.kookyar.common.PitchView;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.PouyaApp.kookyargitar.R;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

public class GitarTuner extends AppCompatActivity implements OnClickListener, OnItemSelectedListener
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
	SharedPreferences getprefs;
	private int laFrequens = 440;
	private int sensitiveLevel = midSensitive;
	private int pressure;
	private MaterialSwitch tuning;
	private Spinner kookChangeSpinner;
	private double nimpardehSub = 0;
	private int selectedStringIndex = -1;
	private int lastTunedIndex = -1;
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
				
				tar1.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
				tar2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
				tar3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
				tar4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
				tar5.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
				tar6.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));

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

		Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
		setSupportActionBar(toolbar);

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


		getprefs = PreferenceManager
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

		kookChangeSpinner = (Spinner) findViewById(R.id.spinnerKookChange);
		ArrayAdapter<CharSequence> kookChangeAdapter = ArrayAdapter.createFromResource(this, R.array.kookChange, R.layout.spinner_item);
		kookChangeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		kookChangeSpinner.setAdapter(kookChangeAdapter);
		kookChangeSpinner.setOnItemSelectedListener(this);
		kookChangeSpinner.setSelection(getprefs.getInt("change", 0));

		tuning = (MaterialSwitch) findViewById(R.id.checkBox_tuning);
		findViewById(R.id.tuning).setOnClickListener(this);
		tuning.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				lastTunedIndex = -1;
				if (isChecked) {
					Toast.makeText(getApplicationContext(),
							"توجه : در صورتی که اولین بار است ساز خود را با کوک یار کوک می کنید، بهتر است سیم ها را به صورت دستی انتخاب کنید",
							Toast.LENGTH_LONG).show();
				}
			}
		});

		pitchLabel.setText(PersianReshape
				.reshape("سیم مورد نظر جهت کوک کردن را انتخاب کنید"));


		tar1.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
		tar2.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
		tar3.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
		tar4.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
		tar5.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
		tar6.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF303F9F")));
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

					if (tuning.isChecked()) {
						for (int i = 0; i < Miditone.length; i++) {
							double target = Miditone[i] - nimpardehSub;
							if (pitch >= target - 0.4 && pitch <= target + 0.4) {
								autoTune(i);
								break;
							}
						}
					}

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
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.temp),
				dir, true);
		File patchFile = new File(dir, "path");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}

	// Phone state listener removed - not needed for core functionality
	// and requires READ_PHONE_STATE permission which is restricted on modern Android

	private void triggerNote(float triggeredNote) {
		float realMidi = midiToRealMidi(triggeredNote);
		pitchView.setCenterPitch(realMidi);
		pitchView.setMidiRef(triggeredNote);
		PdBase.sendFloat("midinote", realMidi);
		PdBase.sendBang("trigger");
	}

	private void triggerNote2(float triggeredNote) {
		float realMidi = midiToRealMidi(triggeredNote);
		pitchView.setCenterPitch(realMidi);
		pitchView.setMidiRef(triggeredNote);
	}
	
	private float midiToRealMidi(float midi) {
		double frequens = laFrequens * Math.pow(2, ((midi - 69) / 12));
		float RealMidi = 69 + 12 * (float) Math.log(frequens / 440.0f)
				/ (float) Math.log(2.0);
		RealMidi = (float) Math.round(RealMidi * 1000) / 1000;
		return RealMidi;
	}

	private void resetStringTints() {
		int defaultTint = Color.parseColor("#FF303F9F");
		tar1.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
		tar2.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
		tar3.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
		tar4.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
		tar5.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
		tar6.setBackgroundTintList(ColorStateList.valueOf(defaultTint));
	}

	private void highlightString(int i) {
		resetStringTints();
		int selectedTint = Color.parseColor("#FFFFBB33");
		Button[] btns = {tar1, tar2, tar3, tar4, tar5, tar6};
		btns[i].setBackgroundTintList(ColorStateList.valueOf(selectedTint));
	}

	private void selectString(int i, int imageRes, String label) {
		selectedStringIndex = i;
		lastTunedIndex = -1;
		triggerNote((float) (Miditone[i] - nimpardehSub));
		pitchLabel.setText(PersianReshape.reshape(label));
		ivTar.setBackgroundResource(imageRes);
		YoYo.with(Techniques.Landing).duration(700).playOn(findViewById(R.id.layout_sim));
		highlightString(i);
	}

	private void autoTune(int i) {
		if (i == lastTunedIndex) return;
		lastTunedIndex = i;
		selectedStringIndex = i;
		int[] imgs = {R.drawable.gitar1, R.drawable.gitar2, R.drawable.gitar3, R.drawable.gitar4, R.drawable.gitar5, R.drawable.gitar6};
		String[] labels = {"سیم اول", "سیم دوم", "سیم سوم", "سیم چهارم", "سیم پنجم", "سیم ششم"};
		triggerNote2((float) (Miditone[i] - nimpardehSub));
		pitchLabel.setText(PersianReshape.reshape(labels[i]));
		ivTar.setBackgroundResource(imgs[i]);
		highlightString(i);
	}

	@Override
	public void onClick(View v) {
		int viewId = v.getId();

		if (viewId == R.id.tuning) {
			tuning.setChecked(!tuning.isChecked());
		} else if (viewId == R.id.tar_1) {
			selectString(0, R.drawable.gitar1, "سیم اول");
		} else if (viewId == R.id.tar_2) {
			selectString(1, R.drawable.gitar2, "سیم دوم");
		} else if (viewId == R.id.tar_3) {
			selectString(2, R.drawable.gitar3, "سیم سوم");
		} else if (viewId == R.id.tar_4) {
			selectString(3, R.drawable.gitar4, "سیم چهارم");
		} else if (viewId == R.id.tar_5) {
			selectString(4, R.drawable.gitar5, "سیم پنجم");
		} else if (viewId == R.id.tar_6) {
			selectString(5, R.drawable.gitar6, "سیم ششم");
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent.getId() == R.id.spinnerKookChange) {
			switch (position) {
				case 0: nimpardehSub = 0; break;
				case 1: nimpardehSub = 0.5; break;
				case 2: nimpardehSub = 1; break;
				case 3: nimpardehSub = 2; break;
			}
			if (selectedStringIndex >= 0) {
				triggerNote2((float) (Miditone[selectedStringIndex] - nimpardehSub));
			}
			if (getprefs != null) {
				SharedPreferences.Editor editor = getprefs.edit();
				editor.putInt("change", position);
				editor.apply();
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

}