package com.PouyaApp.kookyargitar;

import com.kookyar.common.KookyarFonts;
import com.kookyar.common.PersianReshape;








import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;
import com.PouyaApp.kookyargitar.R;

public class About extends Activity {
	public String fonts="BZar.ttf";
	private TextView tv1,tv2,tv3,tv4 ;
 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		Typeface face = KookyarFonts.get(this, fonts);
		tv1 = (TextView) findViewById(R.id.aboutText) ;
		tv1.setTypeface(face);
		String str_tv = (String) tv1.getText().toString();
		tv1.setText(PersianReshape.forDisplay(this, str_tv));

		tv2 = (TextView) findViewById(R.id.aboutText1) ;
		tv2.setTypeface(face);
		String str_tv2 = (String) tv2.getText().toString();
		tv2.setText(PersianReshape.forDisplay(this, str_tv2));
		
		tv3 = (TextView) findViewById(R.id.aboutText2) ;
		tv3.setTypeface(face);
		String str_tv3 = (String) tv3.getText().toString();
		tv3.setText(PersianReshape.forDisplay(this, str_tv3));
		
		tv4 = (TextView) findViewById(R.id.aboutText3) ;
		tv4.setTypeface(face);
		String str_tv4 = (String) tv4.getText().toString();
		tv4.setText(PersianReshape.forDisplay(this, str_tv4));
	}
	

}
