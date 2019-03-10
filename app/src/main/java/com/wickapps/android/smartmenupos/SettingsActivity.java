/*
 * Copyright (C) 2019 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wickapps.android.smartmenupos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends Activity {
	private File textDir;
	private File logsDir;
	private ConnectionLog mLog;
	private ArrayList<String> catList = new ArrayList<String>();
	protected ArrayList<CharSequence> selectedP2Cat = new ArrayList<CharSequence>();
	protected ArrayList<CharSequence> selectedP3Cat = new ArrayList<CharSequence>();
	private SharedPreferences prefs;

	//	Need handler for callbacks to the UI thread
	final Handler mHandler = new Handler();

	final Runnable noConnection = new Runnable() {
		public void run() {
			failedAuth0();
		}
	};
	final Runnable exceptionConnection = new Runnable() {
		public void run() {
			failedAuth2();
		}
	};
	final Runnable uploadSuccess = new Runnable() {
		public void run() {
			TextView tvU = (TextView) findViewById(R.id.textUpload);
			tvU.setText("Success");
		}
	};
	final Runnable uploadFail = new Runnable() {
		public void run() {
			TextView tvU = (TextView) findViewById(R.id.textUpload);
			tvU.setText("Failed");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		textDir = new File(getFilesDir(), "SmartMenuFiles");
		if (!textDir.exists()) textDir.mkdirs();

		logsDir = getExternalFilesDir("SmartMenuLogs");
		if (!logsDir.exists()) logsDir.mkdirs();

		try {
			mLog = new ConnectionLog(this);
		} catch (Exception e) {
		}

		setContentView(R.layout.settings_layout);

		// Setup the ActionBar
		getActionBar().setDisplayShowTitleEnabled(true);
		getActionBar().setTitle(Global.AppNameA);
		getActionBar().setSubtitle(Global.AppNameB);
		getActionBar().setDisplayUseLogoEnabled(false);
		getActionBar().setDisplayShowHomeEnabled(false);

		// Load the priority setting from the Preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Global.AutoMenuReload = (new Boolean(prefs.getBoolean("automenureload", true)));
		CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);
		if (Global.AutoMenuReload) {
			cb9.setChecked(true);
		} else {
			cb9.setChecked(false);
		}

		Global.POS1Enable = (new Boolean(prefs.getBoolean("pos1enable", false)));
		CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
		if (Global.POS1Enable) {
			cb20.setChecked(true);
		} else {
			cb20.setChecked(false);
		}

		Global.POS2Enable = (new Boolean(prefs.getBoolean("pos2enable", false)));
		CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
		if (Global.POS2Enable) {
			cb21.setChecked(true);
		} else {
			cb21.setChecked(false);
		}

		Global.POS3Enable = (new Boolean(prefs.getBoolean("pos3enable", false)));
		CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
		if (Global.POS3Enable) {
			cb22.setChecked(true);
		} else {
			cb22.setChecked(false);
		}

		Global.PrintRoundTrip = (new Boolean(prefs.getBoolean("printroundtrip", true)));
		CheckBox cbprt = (CheckBox) findViewById(R.id.printRoundTrip);
		if (Global.PrintRoundTrip) {
			cbprt.setChecked(true);
		} else {
			cbprt.setChecked(false);
		}

		// Load the remainder from Settings string
		Global.Printer1Type = (Boolean) jsonGetter(Global.Settings, "printer1type");
		RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
		RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
		if (Global.Printer1Type) {
			p1rbpt1.setChecked(true);
			p1rbpt2.setChecked(false);
		} else {
			p1rbpt1.setChecked(false);
			p1rbpt2.setChecked(true);
		}

		Global.Printer2Type = (Boolean) jsonGetter(Global.Settings, "printer2type");
		RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
		RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
		if (Global.Printer2Type) {
			p2rbpt1.setChecked(true);
			p2rbpt2.setChecked(false);
		} else {
			p2rbpt1.setChecked(false);
			p2rbpt2.setChecked(true);
		}

		Global.Printer3Type = (Boolean) jsonGetter(Global.Settings, "printer3type");
		RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
		RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
		if (Global.Printer3Type) {
			p3rbpt1.setChecked(true);
			p3rbpt2.setChecked(false);
		} else {
			p3rbpt1.setChecked(false);
			p3rbpt2.setChecked(true);
		}

		Global.POS1Logo = (Boolean) jsonGetter(Global.Settings, "pos1logo");
		CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
		if (Global.POS1Logo) {
			cb19.setChecked(true);
		} else {
			cb19.setChecked(false);
		}

		Global.P2KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p2kitchencodes");
		CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
		if (Global.P2KitchenCodes) {
			cb23.setChecked(true);
		} else {
			cb23.setChecked(false);
		}

		Global.P3KitchenCodes = (Boolean) jsonGetter(Global.Settings, "p3kitchencodes");
		CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
		if (Global.P3KitchenCodes) {
			cb24.setChecked(true);
		} else {
			cb24.setChecked(false);
		}

		Global.P1PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p1printsenttime");
		CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
		if (Global.P1PrintSentTime) {
			cbp1pst.setChecked(true);
		} else {
			cbp1pst.setChecked(false);
		}

		Global.P2PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p2printsenttime");
		CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
		if (Global.P2PrintSentTime) {
			cbp2pst.setChecked(true);
		} else {
			cbp2pst.setChecked(false);
		}

		Global.P3PrintSentTime = (Boolean) jsonGetter(Global.Settings, "p3printsenttime");
		CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
		if (Global.P3PrintSentTime) {
			cbp3pst.setChecked(true);
		} else {
			cbp3pst.setChecked(false);
		}

		Global.AutoOpenDrawer = (Boolean) jsonGetter(Global.Settings, "autoopendrawer");
		CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
		if (Global.AutoOpenDrawer) {
			cbaod.setChecked(true);
		} else {
			cbaod.setChecked(false);
		}

		Global.PrintDishID = (Boolean) jsonGetter(Global.Settings, "printdishid");
		CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);
		if (Global.PrintDishID) {
			cbprdid.setChecked(true);
		} else {
			cbprdid.setChecked(false);
		}

		Global.P2FilterCats = jsonGetter(Global.Settings, "p2filtercats").toString();
		Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
		fc2.setText(Global.P2FilterCats);

		Global.P3FilterCats = jsonGetter(Global.Settings, "p3filtercats").toString();
		Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
		fc3.setText(Global.P3FilterCats);

		// set the selected filter for P2
		String[] eCat = Global.P2FilterCats.split(",");
		selectedP2Cat.clear();
		for (int i = 0; i < eCat.length; i++) {
			selectedP2Cat.add(eCat[i]);
		}

		eCat = Global.P3FilterCats.split(",");
		selectedP3Cat.clear();
		for (int i = 0; i < eCat.length; i++) {
			selectedP3Cat.add(eCat[i]);
		}

		Global.POS1Ip = jsonGetter(Global.Settings, "pos1ip").toString();
		EditText ip1 = (EditText) findViewById(R.id.ip1);
		ip1.setText(Global.POS1Ip);

		Global.POS2Ip = jsonGetter(Global.Settings, "pos2ip").toString();
		EditText ip2 = (EditText) findViewById(R.id.ip2);
		ip2.setText(Global.POS2Ip);

		Global.POS3Ip = jsonGetter(Global.Settings, "pos3ip").toString();
		EditText ip3 = (EditText) findViewById(R.id.ip3);
		ip3.setText(Global.POS3Ip);

		Global.Printer1Copy = (Integer) jsonGetter(Global.Settings, "printer1copy");
		EditText p1c = (EditText) findViewById(R.id.etP1C);
		p1c.setText(String.valueOf(Global.Printer1Copy));

		newCatList();

		final Button p2c = (Button) findViewById(R.id.butP2FilterCats);
		p2c.setText(Global.P2FilterCats);
		p2c.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showP2CatDialog(p2c);
			}
		});

		final Button p3c = (Button) findViewById(R.id.butP3FilterCats);
		p3c.setText(Global.P3FilterCats);
		p3c.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showP3CatDialog(p3c);
			}
		});

		Button bBackBack = (Button) findViewById(R.id.butExit);
		bBackBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		Button bSave = (Button) findViewById(R.id.butSaveSettings);
		bSave.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String sendserver = "7," + Utils.GetDateTime() + "," + Global.ServerName;
				activityLogger(sendserver);
				// Update the Settings with the selections

				RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
				RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
				if (p1rbpt1.isChecked()) {
					jsonSetter(Global.Settings, "printer1type", true);
				}
				if (p1rbpt2.isChecked()) {
					jsonSetter(Global.Settings, "printer1type", false);
				}

				RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
				RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
				if (p2rbpt1.isChecked()) {
					jsonSetter(Global.Settings, "printer2type", true);
				}
				if (p2rbpt2.isChecked()) {
					jsonSetter(Global.Settings, "printer2type", false);
				}

				RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
				RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
				if (p3rbpt1.isChecked()) {
					jsonSetter(Global.Settings, "printer3type", true);
				}
				if (p3rbpt2.isChecked()) {
					jsonSetter(Global.Settings, "printer3type", false);
				}

				CheckBox cb4 = (CheckBox) findViewById(R.id.checkEng);
				CheckBox cb7 = (CheckBox) findViewById(R.id.showPics);
				CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);
				CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
				CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
				CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
				CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
				CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
				CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
				CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
				CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
				CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
				CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
				CheckBox cbprt = (CheckBox) findViewById(R.id.printRoundTrip);
				CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);

				SharedPreferences.Editor prefEdit = prefs.edit();

				if (cb4.isChecked()) {
					Global.StartEnglish = true;
					prefEdit.putBoolean("startenglish", true);
				} else {
					Global.StartEnglish = false;
					prefEdit.putBoolean("startenglish", false);
				}
				if (cb7.isChecked()) {
					jsonSetter(Global.Settings, "showpics", true);
				} else {
					jsonSetter(Global.Settings, "showpics", false);
				}
				if (cb9.isChecked()) {
					Global.AutoMenuReload = true;
					prefEdit.putBoolean("automenureload", true);
				} else {
					Global.AutoMenuReload = false;
					prefEdit.putBoolean("automenureload", false);
				}
				if (cb19.isChecked()) {
					Global.POS1Logo = true;
					jsonSetter(Global.Settings, "pos1logo", true);
				} else {
					Global.POS1Logo = false;
					jsonSetter(Global.Settings, "pos1logo", false);
				}
				if (cb20.isChecked()) {
					Global.POS1Enable = true;
					prefEdit.putBoolean("pos1enable", true);
				} else {
					Global.POS1Enable = false;
					prefEdit.putBoolean("pos1enable", false);
				}
				if (cb21.isChecked()) {
					Global.POS2Enable = true;
					prefEdit.putBoolean("pos2enable", true);
				} else {
					Global.POS2Enable = false;
					prefEdit.putBoolean("pos2enable", false);
				}
				if (cb22.isChecked()) {
					Global.POS3Enable = true;
					prefEdit.putBoolean("pos3enable", true);
				} else {
					Global.POS3Enable = false;
					prefEdit.putBoolean("pos3enable", false);
				}
				if (cb23.isChecked()) {
					Global.P2KitchenCodes = true;
					jsonSetter(Global.Settings, "p2kitchencodes", true);
				} else {
					Global.P2KitchenCodes = false;
					jsonSetter(Global.Settings, "p2kitchencodes", false);
				}
				if (cb24.isChecked()) {
					Global.P3KitchenCodes = true;
					jsonSetter(Global.Settings, "p3kitchencodes", true);
				} else {
					Global.P3KitchenCodes = false;
					jsonSetter(Global.Settings, "p3kitchencodes", false);
				}
				if (cbp1pst.isChecked()) {
					Global.P1PrintSentTime = true;
					jsonSetter(Global.Settings, "p1printsenttime", true);
				} else {
					Global.P1PrintSentTime = false;
					jsonSetter(Global.Settings, "p1printsenttime", false);
				}
				if (cbp2pst.isChecked()) {
					Global.P2PrintSentTime = true;
					jsonSetter(Global.Settings, "p2printsenttime", true);
				} else {
					Global.P2PrintSentTime = false;
					jsonSetter(Global.Settings, "p2printsenttime", false);
				}
				if (cbp3pst.isChecked()) {
					Global.P3PrintSentTime = true;
					jsonSetter(Global.Settings, "p3printsenttime", true);
				} else {
					Global.P3PrintSentTime = false;
					jsonSetter(Global.Settings, "p3printsenttime", false);
				}
				if (cbaod.isChecked()) {
					Global.AutoOpenDrawer = true;
					jsonSetter(Global.Settings, "autoopendrawer", true);
				} else {
					Global.AutoOpenDrawer = false;
					jsonSetter(Global.Settings, "autoopendrawer", false);
				}
				if (cbprt.isChecked()) {
					Global.PrintRoundTrip = true;
					prefEdit.putBoolean("printroundtrip", true);
				} else {
					Global.PrintRoundTrip = false;
					prefEdit.putBoolean("printroundtrip", false);
				}
				if (cbprdid.isChecked()) {
					Global.PrintDishID = true;
					jsonSetter(Global.Settings, "printdishid", true);
				} else {
					Global.PrintDishID = false;
					jsonSetter(Global.Settings, "printdishid", false);
				}

				prefEdit.commit();

				Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
				Global.P2FilterCats = fc2.getText().toString();
				jsonSetter(Global.Settings, "p2filtercats", Global.P2FilterCats);

				Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
				Global.P3FilterCats = fc3.getText().toString();
				jsonSetter(Global.Settings, "p3filtercats", Global.P3FilterCats);

				EditText et = (EditText) findViewById(R.id.ip1);
				Global.POS1Ip = et.getText().toString();
				jsonSetter(Global.Settings, "pos1ip", Global.POS1Ip);

				et = (EditText) findViewById(R.id.ip2);
				Global.POS2Ip = et.getText().toString();
				jsonSetter(Global.Settings, "pos2ip", Global.POS2Ip);

				et = (EditText) findViewById(R.id.ip3);
				Global.POS3Ip = et.getText().toString();
				jsonSetter(Global.Settings, "pos3ip", Global.POS3Ip);

				et = (EditText) findViewById(R.id.etP1C);
				int tmp = Integer.parseInt(et.getText().toString());
				if (tmp < 1) tmp = 1;
				if (tmp > 3) tmp = 3;
				Global.Printer1Copy = tmp;
				jsonSetter(Global.Settings, "printer1copy", Global.Printer1Copy);

				// Save the Settings.txt file locally on device
				try {
					writeOutFile(Global.Settings.toString(4), "settings.txt");
				} catch (Exception e) {
					log("Sync: Save settings local file FAILED, e=" + e);
				}
			}
		});

		Button bUpload = (Button) findViewById(R.id.butUpload);
		bUpload.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Upload the Settings
				final ProgressDialog pd = ProgressDialog.show(SettingsActivity.this, "Uploading", "Uploading settings to the server...", true, false);
				new Thread(new Runnable() {
					public void run() {
						// see if we can ping the server first
						try {
							if ((!Global.CheckAvailability) || pingIP()) {
								// increment the menu version
								Integer mv = Integer.parseInt(Global.MenuVersion);
								Integer newmv = mv + 1;
								String formatmv = String.format("%04d", newmv);
								Global.MenuVersion = formatmv;
								// update the json
								jsonSetter(Global.Settings, "menuversion", Global.MenuVersion);
								// save the updated settings.txt file
								String fname = "settings.txt";
								writeOutFile(Global.Settings.toString(4), fname);
								// upload the updated settings.txt file
								String fpath = Global.SMID;
								File fbody = new File(textDir, fname);
								int sc = Utils.Uploader(fbody, fname, fpath);
								log("Sync: Upload Status: fname=" + fname + " fpath=" + fpath + " new menu ver=" + Global.MenuVersion + " statusCode=" + sc);
								TextView tvU = (TextView) findViewById(R.id.textUpload);
								if ((sc == 200) || (sc == 204)) {
									mHandler.post(uploadSuccess);
								} else {
									mHandler.post(uploadFail);
								}
							} else {
								// failed to upload
								mHandler.post(exceptionConnection);
								log("Sync: Upload Fail");
							}
						} catch (Exception e) {
							// failed to upload
							mHandler.post(exceptionConnection);
							log("Sync: Upload Fail: e=" + e);
						}
						pd.dismiss();
					}
				}).start();

			}
		});
	}

	private void writeOutFile(String fcontent, String fname) {
		File writeFile = new File(textDir, fname);
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
			writer.write(fcontent);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			log("SyncActivity: writeOutFile ex=" + e);
		}
	}

	protected void showP2CatDialog(final Button bc2) {
		boolean[] checkedP2Cat = new boolean[catList.size()];
		int count = catList.size();
		for (int i = 0; i < count; i++) {
			checkedP2Cat[i] = selectedP2Cat.contains(catList.get(i));
		}
		DialogInterface.OnMultiChoiceClickListener p2DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) selectedP2Cat.add(catList.get(which));
				else selectedP2Cat.remove(catList.get(which));
				StringBuilder stringBuilder = new StringBuilder();
				for (CharSequence ext : selectedP2Cat) {
					// There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
					if (ext.length() > 0) {
						stringBuilder.append(ext.toString() + ",");
					}
				}
				// Kill the last comma...
				String ss = stringBuilder.toString();
				if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
				bc2.setText(ss);
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Print Categories");
		// need the opts in a string [] to pass in multi
		String[] tmpArr = new String[catList.size()];
		for (int i = 0; i < catList.size(); i++) {
			tmpArr[i] = catList.get(i);
		}
		builder.setMultiChoiceItems(tmpArr, checkedP2Cat, p2DialogListener);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void showP3CatDialog(final Button bc3) {
		boolean[] checkedP3Cat = new boolean[catList.size()];
		int count = catList.size();
		for (int i = 0; i < count; i++) {
			checkedP3Cat[i] = selectedP3Cat.contains(catList.get(i));
		}
		DialogInterface.OnMultiChoiceClickListener p3DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked) selectedP3Cat.add(catList.get(which));
				else selectedP3Cat.remove(catList.get(which));
				StringBuilder stringBuilder = new StringBuilder();
				for (CharSequence ext : selectedP3Cat) {
					// There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
					if (ext.length() > 0) {
						stringBuilder.append(ext.toString() + ",");
					}
				}
				// remove last ","
				String ss = stringBuilder.toString();
				if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
				bc3.setText(ss);
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Print Categories");
		// need the opts in a string [] to pass in multi
		String[] tmpArr = new String[catList.size()];
		for (int i = 0; i < catList.size(); i++) {
			tmpArr[i] = catList.get(i);
		}
		builder.setMultiChoiceItems(tmpArr, checkedP3Cat, p3DialogListener);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void newCatList() {
		// get the extra list for the button
		catList.clear();
		String[] lines = Global.CATEGORYTXT.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			int start = 0;
			int end = lines[i].indexOf("|");
			catList.add(lines[i].substring(start, end));
		}
	}

	private boolean checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	// check for ping connectivity
	private boolean pingIP() {
		String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
		int status = -1;
		Boolean downloadSuccess = false;
		try {
			URL url = new URL(ip1);
			OkHttpClient.Builder b = new OkHttpClient.Builder();
			b.readTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
			b.writeTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
			b.connectTimeout(Global.ConnectTimeout, TimeUnit.MILLISECONDS);
			final OkHttpClient client = b.build();
			Request request = new Request.Builder()
					.url(url)
					.build();
			Response response = client.newCall(request).execute();
			status = response.code();
			if (status == 204) {
				// reachable server
				downloadSuccess = true;
			} else {
				downloadSuccess = false;
				log("SyncActivity: PingIP Exception No 204");
			}
		} catch (Exception e) {
			downloadSuccess = false;
			log("SyncActivity: PingIP Exception SC=" + status + " e=" + e);
		}
		return downloadSuccess;
	}

	private void failedAuth0() {
		AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
		alertDialog.setTitle("Connection");
		alertDialog.setIcon(android.R.drawable.stat_sys_warning);
		alertDialog.setMessage("Data connection not available. Files cannot be reloaded.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertDialog.show();
	}

	private void failedAuth2() {
		AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
		alertDialog.setTitle("Uploading");
		alertDialog.setIcon(android.R.drawable.stat_sys_warning);
		alertDialog.setMessage("Uploading not successful. Please try again.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertDialog.show();
	}

	private void failedAuth3() {
		AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
		alertDialog.setTitle("Reloading");
		alertDialog.setIcon(android.R.drawable.stat_sys_warning);
		alertDialog.setMessage("Reload not successful. Please try again.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertDialog.show();
	}

	private void activityLogger(final String sendServer) {
		// Save to a file
		String fname = "activity.txt";
		File writeFile = new File(logsDir, fname);
		FileWriter writer;

		try {
			writer = new FileWriter(writeFile, true);
			writer.write(sendServer + "\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
		}

		// send to server
		new Thread(new Runnable() {
			public void run() {
				if ((checkInternetConnection()) & (Global.TicketToCloud)) {
					String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveActivityURL;
					Utils.SendMultipartAdhoc(postURL,
							sendServer,
							Global.SMID);
				} else {
					// add it to the retry directory
					String fname = "retry-activity-" + Utils.GetDateTime() + ".txt";
					File writeFile = new File(logsDir, fname);
					FileWriter writer;
					try {
						writer = new FileWriter(writeFile, true);
						writer.write(sendServer);
						writer.flush();
						writer.close();
					} catch (IOException e) {
					}
				}
			}
		}).start();
	}

	public void onBackPressed() {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, menu.NONE, "Exit");
		MenuItem item0 = menu.getItem(0);
		item0.setIcon(null);
		item0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		item0.setTitle("Exit    ");
		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			finish();
			return (true);
		}
		return (super.onOptionsItemSelected(item));
	}

	private Object jsonGetter(JSONArray json, String key) {
		Object value = null;
		for (int i = 0; i < json.length(); i++) {
			try {
				JSONObject obj = json.getJSONObject(i);
				String name = obj.getString("name");
				if (name.equalsIgnoreCase(key)) {
					value = obj.get("value");
				}
			} catch (JSONException e) {
			}
		}
		return value;
	}

	private void jsonSetter(JSONArray array, String key, Object replace) {
		for (int i = 0; i < array.length(); i++) {
			try {
				JSONObject obj = array.getJSONObject(i);
				String value = obj.getString("name");
				if (value.equalsIgnoreCase(key)) {
					obj.putOpt("value", replace);
				}
			} catch (JSONException e) {
			}
		}
	}

	// Log helper function
	private void log(String message) {
		log(message, null);
	}

	private void log(String message, Throwable e) {
		if (mLog != null) {
			try {
				mLog.println(message);
			} catch (IOException ex) {
			}
		}
	}

}