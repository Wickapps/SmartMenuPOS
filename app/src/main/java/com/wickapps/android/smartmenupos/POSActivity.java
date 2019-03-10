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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.epson.EpsonCom.EpsonCom;
import com.epson.EpsonCom.EpsonCom.ALIGNMENT;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonCom.FONT;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("NewApi")
public class POSActivity extends Activity {
	private Button saveButton, closeButton, clearButton, p1Button, p2Button, p3Button, ODButton;
	private TextView tvcc, tvautoprint;

	private ImageView ivpf1, ivpf2, ivpf3;

	private Integer txtSize0, txtSize1, txtSize2, txtSize3;

	EditText specET;
	TextView txt1, txt2, txt3;
	Dialog dialog;

	Locale lc, locale;
	Boolean autoPrint = true;

	private EditText etPassword1, etPassword2, etPassword3, etPassword4;
	private GenericTextWatcher watcher1, watcher2, watcher3, watcher4;

	SharedPreferences prefs;
	Editor prefEdit;

	private String FromTableName = "";
	private int FromTableID = 0;
	private String ToTableName = "";
	private int ToTableID = 0;

	//Status Thread
	Thread m_statusThread;
	boolean m_bStatusThreadStop;

	private CustomDialog customDialogDS;

	private static ConnectionLog mLog;

	public static ViewFlipper vfMenuTable; // (menu/tables)

	private File ordersDir;
	private File retryDir;
	private File logsDir;

	private String infoWifi;
	private String formatTicket;

	private String incomingTableName;
	private String lastIncomingMsgData;

	ListView listOrder, listUnsent, listCalls;
	OrderAdapter orderAdapter;

	GridView gridview, gridReload;
	GridAdapter gridAdapter, reloadAdapter;

	TicketAdapter ticketAdapter;

	ArrayAdapter<String> unsentAdapter, last10Adapter;

	// The Table buttons are hard coded in placeorder.xml with name format "butt0 - butt44"
	// Later these button array needs to be dynamicall created.
	private static int MaxTABLES = 45; // total number of tables
	private static int TakeOutTableID = 44; // zero relative

	// Only one table's dishes can be displayed at any time. This ArrayList used to display the DISHES of currentTableID in ListAdapter
	// It can be set with the dish list of any table by calling setJSONOrderList
	private static ArrayList<JSONArray> JSONOrderList = new ArrayList<JSONArray>();

	// The following string array will hold each of the tables' orders including all dishes (represented with a JSON structure)
	private static String[] JSONOrderStr = new String[MaxTABLES];

	// Array of flags that we will use to solve the multiple tablets per table sending at the same time problem
	// True indicates a table is currently sending an order (printing if Auto mode is enabled)
	public static Boolean[] TableSending = new Boolean[MaxTABLES];

	private static int currentTableID;

	private static Button[] tableButtons = new Button[]{};

	// The print status for each table/order on each of 3 printers is stored here, not in JSON for performance reasons
	// 0=Not available (gray)
	// 1=Failed (red)
	// 2=Success (green)
	private static int[][] printStatus = new int[MaxTABLES][3];

	private StateListDrawable states;

	private static String[] rmbItem = new String[]{};
	private static String[] rmbItemAlt = new String[]{};
	private static String[] optionsItem = new String[]{};
	private static String[] extrasItem = new String[]{};
	private static String[] menuItem = new String[]{};
	private static String[] optionsAll = new String[]{};
	private static String[] extrasAll = new String[]{};
	public static String[] categoryAll = new String[]{};

	private static Button[] rbM = new Button[25];
	private static Button[][] rbE = new Button[5][25];
	private static String[][] rbEEng = new String[5][25];
	private static String[][] rbEAlt = new String[5][25];
	private static Button[][] rbO = new Button[5][25];
	private static Button[] butOIP = new Button[10];
	private static Button[] butPT = new Button[10];

	private static ArrayList<String> CategoryEng = new ArrayList<String>();
	private static ArrayList<String> CategoryAlt = new ArrayList<String>();

	public static ArrayList<Boolean> P2Filter = new ArrayList<Boolean>();
	public static ArrayList<Boolean> P3Filter = new ArrayList<Boolean>();

	private static String[] colors = new String[]{
			"#db35d5", "#81baff", "#cd5067", "#00cf70", "#fe7f3d", "#ff9a9a", "#247fca", "#00b49c", "#888888", "#83c748",
			"#9553c5", "#d8994d", "#fad666", "#f6adcd", "#3e8872", "#6f2c91", "#d24a85", "#fad666", "#fe7f3d", "#e3495a",
			"#d24a85", "#ff0000", "#aaaaaa", "#000000", "#5d9356", "#3a6e52"};

	private static String[] menubutcolors = new String[]{
			"#ffffff", "#111111", "#ffffff", "#111111", "#ffffff", "#111111", "#ffffff", "#ffffff", "#ffffff", "#111111",
			"#ffffff", "#ffffff", "#111111", "#111111", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff",
			"#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff"};

	private static String[] textColors = new String[]{
			"#ffffff", "#3a6e52", "#10000000", "#fad666", "#fe7f3d", "#3a6e52", "#247fca", "#000000", "#bbbbbb", "#111111"};
	// white    green     clear       yellow    orange    dkgreen   blue      black     gray      gray

	ArrayList<String> dishArrayList = new ArrayList<String>();
	ArrayList<String> unsentItemList = new ArrayList<String>();
	ArrayList<String> reloadItemList = new ArrayList<String>();
	ArrayList<String> last10Calls = new ArrayList<String>();

	String OrderItem, OrderItemAlt, OrderDesc, ItemCat, ItemCatAlt;
	int Position, ItemCatId;
	boolean ItemCounterOnly;

	//EpsonCom Objects
	private static EpsonComDevice POS1Dev;
	private static EpsonComDeviceParameters POS1Params;
	private static ERROR_CODE err;

	private boolean isChinese() {
		boolean usingAltLang = false;
		// Determine if we doing Chinese
		lc = Locale.getDefault();
		String ll = lc.getLanguage().substring(0, 2).toLowerCase();
		;
		if ("zh".equalsIgnoreCase(ll)) {
			usingAltLang = true;
		}
		return usingAltLang;
	}

	private int numOptions;
	private int numExtras;

	//	Need handler for callbacks to the UI thread
	final Handler mHandler = new Handler();

	//	Create runnable for posting
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			// Grid
			menuItem = Global.MENUTXT.split("\\n");
			optionsAll = Global.OPTIONSTXT.split("\\n");
			extrasAll = Global.EXTRASTXT.split("\\n");
			categoryAll = Global.CATEGORYTXT.split("\\n");
			Global.MenuMaxItems = menuItem.length;
			Global.NumCategory = categoryAll.length;

			dishArrayList.clear();
			for (int i = 0; i < menuItem.length; i++) {
				String line = menuItem[i];
				String[] menuColumns = line.split("\\|");
				String[] menuLang = menuColumns[2].split("\\\\");
				// just keep the first 30 characters for the array list dish name
				int lngth = menuLang[0].length();
				if (lngth > 30) menuLang[0] = menuLang[0].substring(0, 30);
				dishArrayList.add(menuLang[0]);
			}

			// update the gridView
			gridview = (GridView) findViewById(R.id.gridView1);
            gridAdapter = new GridAdapter(POSActivity.this, R.layout.array_list_item, dishArrayList);
			gridview.setAdapter(gridAdapter);

			// Update the Order Items
			setJSONOrderList(currentTableID);

			listOrder = (ListView) findViewById(R.id.listOrder);
            orderAdapter = new OrderAdapter(POSActivity.this, R.layout.list_item, JSONOrderList);
			listOrder.setAdapter(orderAdapter);

			// set the headers TextView for the Order from the JSON
			try {
				if (currentTableID != -1) {
					JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);

					String sendtype = jsonGetter2(JSONtmp, "sendtype").toString();

					TextView tName = (TextView) findViewById(R.id.textheaderTable);
					tName.setText("Table: " + jsonGetter2(JSONtmp, "tablename").toString());
					tName.setTextSize(txtSize0);
					if (sendtype.equalsIgnoreCase("1")) {
						tName.setBackgroundColor(Color.parseColor(textColors[3]));
					} else {
						tName.setBackgroundColor(Color.parseColor(textColors[4]));
					}
					tName.setTextColor(Color.parseColor(textColors[7]));

					TextView oName = (TextView) findViewById(R.id.textheader1);
					oName.setText(jsonGetter2(JSONtmp, "orderid").toString());
					oName.setBackgroundColor(Color.parseColor(textColors[3]));
					oName.setTextColor(Color.parseColor(textColors[7]));

					// update the text for the Order Total RMB
					TextView text = (TextView) findViewById(R.id.textTotal);
					text.setTextSize(txtSize1);
					text.setText(getString(R.string.tab3_rmb) + " " + Integer.toString(updateOrderTotalRMB(currentTableID)));
				} else {
					// No ticket is selected
					TextView tName = (TextView) findViewById(R.id.textheaderTable);
					tName.setText("No Table Selected");
					tName.setTextSize(txtSize0);
					tName.setBackgroundColor(Color.parseColor(textColors[9]));
					tName.setTextColor(Color.parseColor(textColors[0]));

					TextView oName = (TextView) findViewById(R.id.textheader1);
					oName.setText("");
					oName.setBackgroundColor(Color.parseColor(textColors[9]));
					oName.setTextColor(Color.parseColor(textColors[0]));

					// update the text for the Order Total RMB
					TextView text = (TextView) findViewById(R.id.textTotal);
					text.setTextSize(txtSize1);
					text.setText("");
				}
			} catch (JSONException e) {
				log("JSON Exception setting header, table=" + currentTableID + ", e=" + e);
			}
			updateTableButtons();
			invalidateOptionsMenu();
		}
	};

	final Runnable mOrderArrived = new Runnable() {
		public void run() {
			if (!autoPrint) {
                //Toast.makeText(POSActivity.this, "\n\n\nNew Order Arrived\n\n\n", Toast.LENGTH_LONG).show();
				LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final View textEntryView = factory.inflate(R.layout.new_order_dialog, null);
                final CustomDialog customDialog = new CustomDialog(POSActivity.this);
				customDialog.setContentView(textEntryView);
				customDialog.show();
				customDialog.setCancelable(true);
				customDialog.setCanceledOnTouchOutside(true);
				//log("No autoSave- IncomingTableID=" + incomingTableID);
				TextView tv = (TextView) customDialog.findViewById(R.id.newOrderTableTxt);
				tv.setText("Table: " + incomingTableName);
			} else {
				//log("autoSave- IncomingTableID=" + incomingTableID);
			}
			updateTableButtons();
			invalidateOptionsMenu();
		}
	};

	final Runnable mMsgArrived = new Runnable() {
		public void run() {
			LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View textEntryView = factory.inflate(R.layout.new_msg_dialog, null);
            final CustomDialog customDialog = new CustomDialog(POSActivity.this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);
			TextView tv1 = (TextView) customDialog.findViewById(R.id.newMsgTxt);
			tv1.setText(lastIncomingMsgData);
		}
	};

	final Runnable mUpdateNetworkNotSent = new Runnable() {
		public void run() {
			LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View textEntryView = factory.inflate(R.layout.network_send_fail_dialog, null);
            final CustomDialog customDialog = new CustomDialog(POSActivity.this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);
		}
	};

	final Runnable mUpdateCantClose = new Runnable() {
		public void run() {
			LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View textEntryView = factory.inflate(R.layout.cant_close_dialog, null);
            final CustomDialog customDialog = new CustomDialog(POSActivity.this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);
		}
	};

	final Runnable mUpdatePrinters = new Runnable() {
		public void run() {
			updatePrinters(currentTableID);
		}
	};

	final Runnable mClearTableSelection = new Runnable() {
		public void run() {
			try {
				JSONArray tmp = new JSONArray(Global.saletypes.get(0));
				String nam = "";
				if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
				else nam = jsonGetter2(tmp, "display").toString();
				String color = jsonGetter2(tmp, "color").toString();

				tvcc.setText(nam);
				tvcc.setTextColor(Color.parseColor(textColors[0]));
				tvcc.setBackgroundColor(Color.parseColor(color));

				Global.Guests = "";
				currentTableID = -1;
				Global.TableID = -1;
				Global.TableName = "";
				// set up the View (menu/tables)
				vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
				vfMenuTable.setDisplayedChild(1);
				// update UI
				invalidateOptionsMenu();
			} catch (Exception e) {
				log("mClearTableSel Exception=" + e);
			}
		}
	};

	final Runnable mUpdateDailySummary = new Runnable() {
		public void run() {
			TextView tv = (TextView) customDialogDS.findViewById(R.id.dailySummaryTxt);
			tv.setText(formatTicket);
			tv.setTextColor(Color.parseColor(textColors[0]));
			// modify for POS printer
			formatTicket = formatTicket.replaceAll("\\r", "");
			String[] tmpLine = formatTicket.split("\\n");
			formatTicket = "";
			for (int i = 0; i < tmpLine.length; i++) {
				formatTicket = formatTicket + addPad(tmpLine[i]);
			}
		}
	};

	@Override
	public void onPause() {
		Global.PausedOrder = true;
		this.unregisterReceiver(wifiStatusReceiver);
		this.unregisterReceiver(messageReceiver);

		// write out the currentTableID
		prefEdit.putInt("currenttableid", currentTableID);
		// write out state info for all the orders
		for (int i = 0; i < MaxTABLES; i++) {
			if (tabIsOpen(i)) {
				prefEdit.putString("jsonorderstr" + i, JSONOrderStr[i]);
				//log("onPause: hasOrderID=" + i);
			} else {
				prefEdit.remove("jsonorderstr" + i);
			}
		}
		// Write out the Print filters
		/*
		for (int i = 0; i < categoryAll.length; i++) {
			if (P2Filter.get(i)) {
				prefEdit.remove("p2filter" + i);
				prefEdit.putBoolean("p2filter" + i, true);
				//log("onPause: REPLACE TRUE p2filter" + i);
			} else {
				prefEdit.remove("p2filter" + i);
				//log("onPause: REMOVE p2filter" + i);
			}
			if (P3Filter.get(i)) {
				prefEdit.remove("p3filter" + i);
				prefEdit.putBoolean("p3filter" + i, true);
				//log("onPause: REPLACE TRUE p3filter" + i);
			} else {
				prefEdit.remove("p3filter" + i);
				//log("onPause: REMOVE p3filter" + i);
			}
		}
		log("OPOPOP: P2size=" + P2Filter.size());
        log("OPOPOP: P2Filter=" + P2Filter);
		log("OPOPOP: P3size=" + P3Filter.size());
        log("OPOPOP: P3Filter=" + P3Filter);
        */

		prefEdit.commit();
		log("onPause");
		super.onPause();
	}

	protected void onResume() {
		log("onResume");
		super.onResume();
		IntentFilter filter1 = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		this.registerReceiver(wifiStatusReceiver, filter1);

		this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.TICKET_BUMPED));
		this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.NEW_ORDER));
		this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.NEW_MSG));
		this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.PRINT_STATUS));

		// get the currentTableID
		currentTableID = prefs.getInt("currenttableid", -1);

		// read in the tables' orders
		for (int i = 0; i < MaxTABLES; i++) {
			try {
				// load the JSON strings
				String tmp = prefs.getString("jsonorderstr" + i, "");
				if (tmp.length() > 0) {
					JSONOrderStr[i] = tmp;
					//log("onResume: setJSON table=" + i);
				} else {
					JSONArray JSONtmp = getInitialJSONOrder(i);
					JSONOrderStr[i] = JSONtmp.toString();
				}
			} catch (Exception e) {
				log("onResume Exception= " + e);
			}
			// Clear any in progress indications
			TableSending[i] = false;
		}

		Global.PausedOrder = false;

		if (currentTableID == -1) clearTableSelection();
		else if (numberOfDishes(currentTableID) == 0) clearTableSelection();

		setJSONOrderList(currentTableID);

		mHandler.post(mUpdateResults);
		invalidateOptionsMenu();
	}

	@Override
	protected void onDestroy() {
		log("onDestroy");
		super.onDestroy();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefEdit = prefs.edit();

		Global.LoggedIn = true;

		// Start the Service
		SmartMenuService.actionStart(getApplicationContext());

		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		// For Opening of register Drawer and printing of summarys, we need to set up printer params
		// This should be moved into the service so that it handles ALL printer activity
		POS1Dev = new EpsonComDevice();
		POS1Params = new EpsonComDeviceParameters();
		POS1Params.PortType = EpsonCom.PORT_TYPE.ETHERNET;
		POS1Params.IPAddress = Global.POS1Ip;
		POS1Params.PortNumber = 9100;
		POS1Dev.setDeviceParameters(POS1Params);

		setContentView(R.layout.placeorder);

        txtSize0 = (Utils.getFontSize(POSActivity.this));
        txtSize1 = (int) (Utils.getFontSize(POSActivity.this) / 1.2);
        txtSize2 = (int) (Utils.getFontSize(POSActivity.this) / 1.4);
        txtSize3 = (int) (Utils.getFontSize(POSActivity.this) / 1.1);

		// setup the button references
		setupButtons();

		// grab the directory where orders and retrys will be stored
		ordersDir = new File(getFilesDir(), "SmartMenuOrders");
		if (!ordersDir.exists()) ordersDir.mkdirs();
		retryDir = new File(getFilesDir(), "SmartMenuRetry");
		if (!retryDir.exists()) retryDir.mkdirs();

		File logsDir = getExternalFilesDir("SmartMenuLogs");
		if (!logsDir.exists()) logsDir.mkdirs();

		try {
			mLog = new ConnectionLog(this);
		} catch (Exception e) {
		}

		// Setup the ActionBar
		getActionBar().setDisplayShowTitleEnabled(true);
		getActionBar().setTitle(Global.AppNameA);
		getActionBar().setSubtitle(Global.AppNameB);
		getActionBar().setDisplayUseLogoEnabled(false);
		getActionBar().setDisplayShowHomeEnabled(false);

		// setup the arrayList of menu items from the OrderItem dish name
		menuItem = Global.MENUTXT.split("\\n");
		optionsAll = Global.OPTIONSTXT.split("\\n");
		extrasAll = Global.EXTRASTXT.split("\\n");
		categoryAll = Global.CATEGORYTXT.split("\\n");

		// For each table, setup a new blank order structure if it is empty
		for (int i = 0; i < MaxTABLES; i++) {
			try {
				// set up the initial order
				JSONArray JSONtmp = getInitialJSONOrder(i);
				JSONOrderStr[i] = JSONtmp.toString();

				// see if the preferences has any info to update
				String tmp = prefs.getString("jsonorderstr" + i, "");
				if (tmp.length() > 0) {
					JSONOrderStr[i] = tmp;
				}
			} catch (JSONException e) {
				log("JSONtmp Initialize exception=" + e);
			}
			clearPrinterStatus(i);
			TableSending[i] = false;
		}

		// Start with no table selected
		clearTableSelection();

		// set up the Tables View
		setupTablesView();

		// set up the Categories List
		setupCatList();

		// setup the Menu Grid
		dishArrayList.clear();
		for (int i = 0; i < menuItem.length; i++) {
			String line = menuItem[i];
			String[] menuColumns = line.split("\\|");
			String[] menuLang = menuColumns[2].split("\\\\");
			// just keep the first 30 characters for the array list dish name
			int lngth = menuLang[0].length();
			if (lngth > 30) menuLang[0] = menuLang[0].substring(0, 30);
			dishArrayList.add(menuLang[0]);
		}

		GridView gridview = (GridView) findViewById(R.id.gridView1);
        gridAdapter = new GridAdapter(POSActivity.this, R.layout.array_list_item, dishArrayList);
		gridview.setAdapter(gridAdapter);

		// set up for the Order List
		// set the Order List to be 18% of screen width
		LinearLayout llOrderList = (LinearLayout) findViewById(R.id.col2);
		final float WIDE = this.getResources().getDisplayMetrics().widthPixels;
		int valueWide = (int) (WIDE * 0.22f);
		llOrderList.setLayoutParams(new LinearLayout.LayoutParams(valueWide, LayoutParams.FILL_PARENT));

		listOrder = (ListView) findViewById(R.id.listOrder);
		listOrder.setItemsCanFocus(true);
        orderAdapter = new OrderAdapter(POSActivity.this, R.layout.list_item, JSONOrderList);
		listOrder.setAdapter(orderAdapter);

		listOrder.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listOrder.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				// Total checked items
				final int checkedCount = listOrder.getCheckedItemCount();
				// Set the CAB title according to total checked items
				mode.setTitle(checkedCount + " Selected");
				// Calls toggleSelection method from ListViewAdapter Class
				orderAdapter.toggleSelection(position);
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				SparseBooleanArray selected = orderAdapter.getSelectedIds();
				if (item.getItemId() == R.id.delete) {
					if (Global.UserLevel > 0) {
						// Delete items operation
						for (int i = (selected.size() - 1); i >= 0; i--) {
							if (selected.valueAt(i)) {
								int position = selected.keyAt(i);
								deleteDishAtPosition(position);
							}
						}
					} else {
                        Toast.makeText(POSActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
					}
					// Close CAB
					mode.finish();
					return true;
				} else if ((item.getItemId() >= 100) && (item.getItemId() <= MaxTABLES + 100)) {
					if (Global.UserLevel > 0) {
						// Move To operation
						int newTable = item.getItemId() - 101;
						if (newTable != currentTableID) {
							for (int i = (selected.size() - 1); i >= 0; i--) {
								if (selected.valueAt(i)) {
									int position = selected.keyAt(i);
									moveDishAtPosition(position, newTable);
									deleteDishAtPosition(position);
								}
							}
						}
					} else {
                        Toast.makeText(POSActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
					}
					// Close CAB
					mode.finish();
					return true;
				}
				return false;
			}

			private void deleteDishAtPosition(int position) {
				//if (!dishHasBeenPrinted(currentTableID,position) || Global.PrintedAllowDelete) {
				// delete the item
				try {
					JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
					JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
					JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
					// Remove the selected item
					// Check for SDK version to see if we can use the JSON function directly
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
						JSONdishesAry.remove(position);
					} else {
						// Do it the old-school way
						JSONdishesAry = RemoveJSONArray(JSONdishesAry, position);
					}
					// replace it
					JSONObject ary = new JSONObject();    // new object to store the new dishes
					ary.put("dishes", JSONdishesAry);
					JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
					saveJsonTable(currentTableID, JSONOrderAry);
					// update the total price of the order
					ary = new JSONObject();
					ary.put("ordertotal", updateOrderTotalRMB(currentTableID));
					JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
					saveJsonTable(currentTableID, JSONOrderAry);
					// See if the ticket is now empty and clear it
					if (numberOfDishes(currentTableID) == 0) {
						JSONArray JSONtmp = getInitialJSONOrder(currentTableID);
						// some required updates
						Global.TableTime = Utils.GetTime();
						Global.OrderId = Utils.GetDateTime() + "-" + Global.TableName;
						jsonSetter(JSONtmp, "tablename", Global.TableName);
						jsonSetter(JSONtmp, "orderid", Global.OrderId);
						jsonSetter(JSONtmp, "waiter", Global.IncomingServerName);
						jsonSetter(JSONtmp, "tabletime", Global.TableTime);
						saveJsonTable(currentTableID, JSONtmp);
					}
				} catch (JSONException e) {
					log("JSON Delete Dish Exception=" + e);
				}
				mHandler.post(mUpdateResults);
				//}
			}

			private void moveDishAtPosition(int position, int table) {
				try {
					// Move the single dish at currentTableID:position to newTable
					// get the existing dish
					JSONArray JSONcurrent = new JSONArray(JSONOrderStr[currentTableID]);
					JSONObject JSONdishObj = JSONcurrent.getJSONObject(jsonGetter3(JSONcurrent, "dishes"));
					JSONArray jdcurtmp = JSONdishObj.getJSONArray("dishes");
					//log("Number of existing dishes=" + numdishex);
					JSONArray jd = jdcurtmp.getJSONArray(position);
					// See if its the first dish and create a new order
					if (numberOfDishes(table) == 0) {
						JSONArray JSONtmp = getInitialJSONOrder(table);
						// some required updates
						String tmpTableTime = Utils.GetTime();
						String tmpTableName = Global.tablenames.get(table);
						String tmpOrderID = Utils.GetDateTime() + "-" + tmpTableName;
						jsonSetter(JSONtmp, "tablename", tmpTableName);
						jsonSetter(JSONtmp, "currenttableid", table);
						jsonSetter(JSONtmp, "orderid", tmpOrderID);
						jsonSetter(JSONtmp, "guests", "");
						jsonSetter(JSONtmp, "waiter", Global.IncomingServerName);
						jsonSetter(JSONtmp, "sendtime", tmpTableTime);
						jsonSetter(JSONtmp, "tabletime", tmpTableTime);
						saveJsonTable(table, JSONtmp);
					}
					// Grab the target table where we will add the selected dish
					JSONArray JSONtarget = new JSONArray(JSONOrderStr[table]);
					JSONObject JSONtargetObj = JSONtarget.getJSONObject(jsonGetter3(JSONtarget, "dishes"));
					JSONArray jdtargettmp = JSONtargetObj.getJSONArray("dishes");
					// Append the dish to the target table
					jdtargettmp.put(jd);
					// JSONtmp has all the dishes, update table
					saveJsonTable(table, JSONtarget);
					// update the price
					JSONObject ary = new JSONObject();
					ary.put("ordertotal", updateOrderTotalRMB(table));
					JSONtarget.put(jsonGetter3(JSONtarget, "ordertotal"), ary);
					// save the JSON for the new table
					saveJsonTable(table, JSONtarget);
					setJSONOrderList(table);
				} catch (JSONException e) {
					log("JSON MoveOrder Exception=" + e);
				}
				mHandler.post(mUpdateResults);
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.multiselect_menu, menu);
				//SubMenu subMenuTables = menu.addSubMenu(0,8,menu.NONE,"Tables");
				MenuItem tablesItem = menu.findItem(R.id.move);
				SubMenu subMenuTables = tablesItem.getSubMenu();
				//SubMenu subMenuTables = menu.addSubMenu(0, Menu.FIRST, menu.NONE, "MOVE TO TABLE#");

				int j = 0;
				for (int i = 0; i < MaxTABLES; i++) {
					j++;
					String tmp = Global.tablenames.get(i);
					if ((!tmp.equalsIgnoreCase("blank")) && (!tmp.equalsIgnoreCase("take out")))
						subMenuTables.add(0, j + 100, menu.NONE, tmp);
				}
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// TODO Auto-generated method stub
				orderAdapter.removeSelection();
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// TODO Auto-generated method stub
				return false;
			}
		});

		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position, long id) {
				Position = position;
				setUpScreen();

				// if first dish, then set a new tabletime
				if (JSONOrderList.isEmpty()) {
					Global.TableTime = Utils.GetTime();
					Global.OrderId = Utils.GetDateTime() + "-" + Global.TableName;
					Global.IncomingServerName = Global.ServerName;
				}
				// check if popup or quick access
				if (dishHasChoices(position)) {
					// Show the popup and allow for all the selections to be made
					showThePopup();
				} else {
					// This dish has no choices to make, so just directly add the main dish to the orderlist
					// Create the initial JSON order and Options and Extras holders
					JSONArray JSONOptionsAry = new JSONArray();
					JSONArray JSONExtrasAry = new JSONArray();

					// Reset the dish price
					int priceUnitTotal = 0;
					int priceUnitTotalFull = 0;
					int priceUnitBase = 0;
					int priceDiscount = 100;
					int priceQtyTotal = 0;
					int dishQty = 1;

					// until we have the menu in a JSON structure, we need to use the following functions to get our names and pricing numbers
					String priceOptionName = removeRMBnumber(rmbItem[0]);
					String priceOptionNameAlt = removeRMBnumber(rmbItemAlt[0]);
					priceUnitBase = getRMBnumber(rmbItem[0]);
					priceUnitTotal = priceUnitTotal + priceUnitBase;
					priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations
					priceQtyTotal = priceUnitTotal * dishQty;

					// add it to the order list, new items placed at the top of the list
					try {
						// update the changed values
						JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);
						jsonSetter(JSONtmp, "orderid", Global.OrderId);
						jsonSetter(JSONtmp, "tabletime", Global.TableTime);
						jsonSetter(JSONtmp, "sendtime", Global.SendTime);
						jsonSetter(JSONtmp, "waiter", Global.IncomingServerName);
						jsonSetter(JSONtmp, "tabstate", 0);
						saveJsonTable(currentTableID, JSONtmp);

						// update the JSON with this item
						JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
						JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
						JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

						JSONArray JSONDishAry = new JSONArray();
						JSONDishAry.put(createInt("dishId", Position));
						JSONDishAry.put(createStr("dishName", OrderItem));
						JSONDishAry.put(createStr("dishNameAlt", OrderItemAlt));
						JSONDishAry.put(createStr("categoryName", ItemCat));
						JSONDishAry.put(createStr("categoryNameAlt", ItemCatAlt));
						JSONDishAry.put(createInt("categoryId", ItemCatId));
						JSONDishAry.put(createInt("priceOptionId", 0));
						JSONDishAry.put(createStr("priceOptionName", priceOptionName));
						JSONDishAry.put(createStr("priceOptionNameAlt", priceOptionNameAlt));
						JSONDishAry.put(createInt("qty", dishQty));
						JSONDishAry.put(createInt("priceUnitBase", priceUnitBase));
						JSONDishAry.put(createInt("priceUnitTotal", priceUnitTotal));
						JSONDishAry.put(createInt("priceUnitTotalFull", priceUnitTotalFull));
						JSONDishAry.put(createInt("priceDiscount", priceDiscount));
						JSONDishAry.put(createInt("priceQtyTotal", priceQtyTotal));
						JSONDishAry.put(createStr("specIns", ""));
						JSONDishAry.put(createBoolean("dishPrinted", false));
						JSONDishAry.put(createBoolean("counterOnly", ItemCounterOnly));

						// Add the dish Options which were built when they were selected ...
						JSONObject aryO = new JSONObject();
						aryO.put("options", JSONOptionsAry);
						JSONDishAry.put(aryO);

						// Add the dish Extras which were built when they were selected ...
						JSONObject aryE = new JSONObject();
						aryE.put("extras", JSONExtrasAry);
						JSONDishAry.put(aryE);

						JSONdishesAry.put(JSONDishAry);    // append this dish to the JSON dishes
						JSONObject ary = new JSONObject(); // new object to store the new dishes
						ary.put("dishes", JSONdishesAry);
						JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
						saveJsonTable(currentTableID, JSONOrderAry);

						// update the total price of the order
						ary = new JSONObject();
						ary.put("ordertotal", updateOrderTotalRMB(currentTableID));
						JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
						saveJsonTable(currentTableID, JSONOrderAry);

					} catch (JSONException e) {
						log("JSON Add Dish Exception=" + e);
					}
					listOrder.scrollTo(0, 0);
					mHandler.post(mUpdateResults);
				}
			}
		});

        /*
         * Disable due to payment popup screen
        if (Global.PayTypeEnabled) {
        	tvcc.setVisibility(View.VISIBLE);
        } else {
        	tvcc.setVisibility(View.GONE);
        }
        */
		tvcc.setText(getString(R.string.tab3_cash));
		tvcc.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					// Clicked on the payment type button, so bump it to the next type
					JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);
					String tmpsaletype = jsonGetter2(JSONtmp, "saletype").toString();

					int SaleType = Integer.valueOf(tmpsaletype);
					int maxvalue = Global.saletypes.size();
					int nextid = SaleType + 1;
					if (nextid == maxvalue) nextid = 0;

					// Set the new button color and SaleType
					JSONArray tmp = new JSONArray(Global.saletypes.get(nextid));
					String nam = "";
					if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
					else nam = jsonGetter2(tmp, "display").toString();
					String color = jsonGetter2(tmp, "color").toString();
					tvcc.setText(nam);
					tvcc.setTextColor(Color.parseColor(textColors[0]));
					tvcc.setBackgroundColor(Color.parseColor(color));
					SaleType = nextid;
					// update the json for the new SaleType

					jsonSetter(JSONtmp, "saletype", Integer.toString(SaleType));
					// re-save it
					saveJsonTable(currentTableID, JSONtmp);
				} catch (JSONException e) {
					log("JSONOtmp Exception saletype=" + e);
				}
				// update the UI
				mHandler.post(mUpdateResults);
			}
		});

		tvautoprint.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (autoPrint) {
					tvautoprint.setTextColor(Color.parseColor(textColors[0]));
					tvautoprint.setBackgroundColor(Color.parseColor(textColors[9]));
					tvautoprint.setText(getString(R.string.tab3_auto));
					autoPrint = false;
				} else {
					tvautoprint.setTextColor(Color.parseColor(textColors[0]));
					tvautoprint.setBackgroundColor(Color.parseColor(textColors[6]));
					tvautoprint.setText(getString(R.string.tab3_auto));
					autoPrint = true;
				}
			}
		});

		clearButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// only clear tabs with printed dishes if allowed
				try {
					// clear the selected table
					JSONArray JSONtmp = getInitialJSONOrder(currentTableID);
					setJSONOrderList(currentTableID);

					removeJsonTable(currentTableID, JSONtmp);
					mHandler.post(mUpdateResults);
					clearPrinterStatus(currentTableID);

					// take them to takeout table to clear the headers
					//getTakeOutTableID();
					clearTableSelection();
				} catch (JSONException e) {
					log("JSONOtmp ClearButton Exception=" + e);
				}
			}
		});

		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (JSONOrderList.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(POSActivity.this).create();
					alertDialog.setTitle(getString(R.string.tab3_empty_title));
					alertDialog.setMessage(getString(R.string.tab3_empty_text));
					alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
					alertDialog.show();
				}
				//else if (currentTableID != TakeOutTableID) {
				else {
					// set the TABSTATE to OPEN=1
					try {
						JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);
						jsonSetter(JSONtmp, "tabstate", 1);
						// re-save it
						saveJsonTable(currentTableID, JSONtmp);
						// Do printing
						saveTheTab(JSONtmp.toString());
					} catch (JSONException e) {
						log("JSONOtmp SendButton Exception curTable=" + currentTableID + " tabstate1=" + e);
					}
				}
			}
		});

		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (JSONOrderList.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(POSActivity.this).create();
					alertDialog.setTitle(getString(R.string.tab3_empty_title));
					alertDialog.setMessage(getString(R.string.tab3_empty_text));
					alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
					alertDialog.show();
				} else {
					// Popup a payment type dialog
                    dialog = new Dialog(POSActivity.this);
					dialog.setContentView(R.layout.payment_type_popup);
					// Title for the popup modify item box
					String tit = getString(R.string.msg_saletype_choose);
					dialog.setTitle(tit);
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(true);

					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					layoutParams.setMargins(5, 0, 5, 5);
					layoutParams.gravity = Gravity.LEFT;

					LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(120, 80);
					layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
					layoutParams3.gravity = Gravity.CENTER;

					// set up the Headers and Buttons under this main LinearLayout
					LinearLayout PTPMain = (LinearLayout) dialog.findViewById(R.id.PaymentTypePopupMain);
					PTPMain.removeAllViews();

					// New VERTICAL Linear Layout for each TextView header and BUTTON row
					LinearLayout newLLV;
                    newLLV = new LinearLayout(POSActivity.this);
					newLLV.setLayoutParams(layoutParams);
					newLLV.setOrientation(LinearLayout.VERTICAL);
					newLLV.setHorizontalGravity(Gravity.LEFT);
					// New HORIZONTAL Linear Layout for the buttons in the group
					LinearLayout newLLH;
                    newLLH = new LinearLayout(POSActivity.this);
					newLLH.setLayoutParams(layoutParams);
					newLLH.setOrientation(LinearLayout.HORIZONTAL);
					newLLH.setHorizontalGravity(Gravity.CENTER);

					// Loop through the Sales Types add a button for each one
					for (int i = 0; i < Global.saletypes.size(); i++) {
						try {
							JSONArray tmp = new JSONArray(Global.saletypes.get(i));
							String nam = "";
							if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
							else nam = jsonGetter2(tmp, "display").toString();

							String st = String.valueOf(i);

                            butPT[i] = new Button(POSActivity.this);
							butPT[i].setTag(st); // put the Saletype=i in the button TAG

							butPT[i].setText(nam);
							butPT[i].setTextColor(Color.parseColor(textColors[0]));
							butPT[i].setTextSize(txtSize1);
							butPT[i].setBackgroundResource(R.drawable.border_yellow_tight);
							butPT[i].setPadding(5, 5, 5, 5);
							butPT[i].setGravity(Gravity.CENTER);
							// set up the clickers which do the ADD TO ORDER function
							butPT[i].setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									String tmpSaleType = v.getTag().toString();
									try {
										// set the TABSTATE to PAY=2 and update the sale type
										JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);
										jsonSetter(JSONtmp, "tabstate", 2);
										// set time and ticketnum
										Global.SendTime = Utils.GetTime();
										jsonSetter(JSONtmp, "sendtime", Global.SendTime);
										jsonSetter(JSONtmp, "ticketnum", Global.TicketNum);
										jsonSetter(JSONtmp, "saletype", tmpSaleType);
										// re-save it
										saveJsonTable(currentTableID, JSONtmp);

										// Do printing
										sendTheTab(JSONtmp.toString());

										// when order is completed, update
										mHandler.post(mUpdatePrinters);
										mHandler.post(mUpdateResults);

										dialog.dismiss();
									} catch (JSONException e) {
										log("JSON Choose Payment Type Exception=" + e);
									}
								}
							});
							newLLH.addView(butPT[i], i, layoutParams3);

						} catch (Exception e) {
							log("PaymentTypePopup Exception=" + e);
						}
					}
					// Add the header - NOT NEEDED, THE POPUP ALREADY HAS A TITLE
                    //TextView tvtitle = new TextView(POSActivity.this);
					//tvtitle.setText("Payment Type");
					//tvtitle.setLayoutParams(layoutParams);
					//newLLV.addView(tvtitle);
					// Add the button row
					newLLV.addView(newLLH);
					// Update the main view
					PTPMain.addView(newLLV, 0);
					dialog.show();
				}
			}
		});

		p1Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				reprint1();
			}
		});

		p2Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				reprint2();
			}
		});

		p3Button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				reprint3();
			}
		});

		ODButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Open the register Drawer
				openDrawer();
			}
		});
	}

	private void saveTheTab(final String json) {
		// build the tickets
		// Thread safe - we need to do everything from the passed in JSONtmp
        final ProgressDialog pd = ProgressDialog.show(POSActivity.this, getString(R.string.tab3_sending_title), getString(R.string.tab3_sending), true, false);
		new Thread(new Runnable() {
			public void run() {
				try {
					// Do the printing.
					// Grab the table from the JSON
					JSONArray JA = new JSONArray(json);
					int table = (Integer) jsonGetter2(JA, "currenttableid");
					int count2 = unprintedDishCount(table, true, 2);
					int count3 = unprintedDishCount(table, true, 3);
					//log("count2=" + count2);
					//log("count3=" + count3);
					// Set the flag so other tablets cant send orders for this table while its printing
					// Not working, so just false it out...
					TableSending[table] = false;
					// Print via the background service
					SmartMenuService.actionPrintTicket1(getApplicationContext(), json, false);
					if (count2 > 0) {
						SmartMenuService.actionPrintTicket2(getApplicationContext(), json, false);
						if (!Global.PrintRoundTrip) {
							if (Global.POS2Enable)
								printStatus[table][1] = 1;    // mark as successfully printed
						}
					}
					if (count3 > 0) {
						SmartMenuService.actionPrintTicket3(getApplicationContext(), json, false);
						if (!Global.PrintRoundTrip) {
							if (Global.POS3Enable)
								printStatus[table][2] = 1;    // mark as successfully printed
						}
					}
					if (!Global.PrintRoundTrip) {
						if (Global.POS1Enable)
							printStatus[table][0] = 1;    // mark as successfully printed
						int size = numberOfDishes(table);
						for (int i = 0; i < size; i++) {
							// mark as printed
							setDishPrinted(table, i);
						}
						// Clear the flag so other tablets can send orders for this table
						TableSending[table] = false;
					}
					pd.dismiss();
				} catch (JSONException e) {
					log("saveTheTab Exception=" + e);
				}
			} // thread close
		}).start();
	}

	private void sendTheTab(final String json) {
		// build the tickets
		// Thread safe - we need to do everything from the passed in JSONtmp
        final ProgressDialog pd = ProgressDialog.show(POSActivity.this, getString(R.string.tab3_sending_title), getString(R.string.tab3_sending), true, false);
		new Thread(new Runnable() {
			public void run() {
				try {
					// Do the printing.
					// Grab the table from the JSON
					JSONArray JA = new JSONArray(json);
					String sendtype = jsonGetter2(JA, "sendtype").toString();
					int table = (Integer) jsonGetter2(JA, "currenttableid");

					String tmpsaletype = jsonGetter2(JA, "saletype").toString();
					int SaleType = Integer.valueOf(tmpsaletype);

					int count2 = unprintedDishCount(table, true, 2);
					int count3 = unprintedDishCount(table, true, 3);
					//log("count2=" + count2);
					//log("count3=" + count3);
					// Set the flag so other tablets cant send orders for this table while its printing
					// Not working so false it out ...
					TableSending[table] = false;
					// Print via the background service
					SmartMenuService.actionPrintTicket1(getApplicationContext(), json, false);
					if (count2 > 0) {
						SmartMenuService.actionPrintTicket2(getApplicationContext(), json, false);
						if (!Global.PrintRoundTrip) {
							if (Global.POS2Enable)
								printStatus[table][1] = 1;    // mark as successfully printed
						}
					}
					if (count3 > 0) {
						SmartMenuService.actionPrintTicket3(getApplicationContext(), json, false);
						if (!Global.PrintRoundTrip) {
							if (Global.POS3Enable)
								printStatus[table][2] = 1;    // mark as successfully printed
						}
					}
					if (!Global.PrintRoundTrip) {
						if (Global.POS1Enable)
							printStatus[table][0] = 1;    // mark as successfully printed
						int size = numberOfDishes(table);
						for (int i = 0; i < size; i++) {
							// mark as printed
							setDishPrinted(table, i);
						}
						// Clear the flag so other tablets can send orders for this table
						TableSending[table] = false;
					}
					if (!sendtype.equalsIgnoreCase("2")) {        // 2=order app
						// Close out the order and bump the counters
						int tot = getOrderTotalJSON(JA);
						if (SaleType == 0) {
							Global.RegisterCash = Global.RegisterCash + tot;
						} else if (SaleType == 1) {
							Global.RegisterCredit = Global.RegisterCredit + tot;
						} else {
							Global.RegisterOther = Global.RegisterOther + tot;
						}
						Global.RegisterCashTotal = Global.RegisterCash + Global.RegisterFloat - Global.RegisterPayout;
						Global.RegisterSalesTotal = Global.RegisterCash + Global.RegisterCredit + Global.RegisterOther;
						Global.OrdersSent = Global.OrdersSent + 1;

						Global.TicketNum = Global.TicketNum + 1;
						prefEdit.putInt("ticketnum", Global.TicketNum);
						// send up the order. The order will be sent up to the server by the Service
						SmartMenuService.actionSave(getApplicationContext(), json, "0");
					}
					// Update
					mHandler.post(mUpdatePrinters);
					mHandler.post(mUpdateResults);
					// reset the table
					JSONArray jtmp = getInitialJSONOrder(currentTableID);
					setJSONOrderList(currentTableID);
					removeJsonTable(currentTableID, jtmp);
					clearPrinterStatus(currentTableID);
					// Clear the headers
					mHandler.post(mClearTableSelection);
					pd.dismiss();
				} catch (JSONException e) {
					log("sendTheTab Exception=" + e);
				}
			} // thread close
		}).start();
	}

	private void reprint1() {
		SmartMenuService.actionPrintTicket1(getApplicationContext(), JSONOrderStr[currentTableID], true);
		if (!Global.PrintRoundTrip) {
			if (Global.POS1Enable)
				printStatus[currentTableID][0] = 1;    // mark as successfully printed
		}
	}

	private void reprint2() {
		SmartMenuService.actionPrintTicket2(getApplicationContext(), JSONOrderStr[currentTableID], true);
		if (!Global.PrintRoundTrip) {
			if (Global.POS2Enable)
				printStatus[currentTableID][1] = 1;    // mark as successfully printed
		}
	}

	private void reprint3() {
		SmartMenuService.actionPrintTicket3(getApplicationContext(), JSONOrderStr[currentTableID], true);
		if (!Global.PrintRoundTrip) {
			if (Global.POS3Enable)
				printStatus[currentTableID][2] = 1;    // mark as successfully printed
		}
	}

	private void setUpScreen() {
		// setup the Global Strings so the popup options have what they need
		String[] menuItem = Global.MENUTXT.split("\\n");
		String line = menuItem[Position].trim();
		String[] menuColumns = line.split("\\|");

		// If they want to over ride the category filters to select printer, store the flag
		String typeFlags = menuColumns[0];
		if (typeFlags.substring(5, 6).equals("1")) ItemCounterOnly = true;
		else ItemCounterOnly = false;

		// we have our array of columns for the selected line, set up the language specific fields using the divider "\"
		String[] itemColumns = menuColumns[2].split("\\\\");
		String[] descColumns = menuColumns[4].split("\\\\");
		String[] rmbColumns = menuColumns[5].split("\\\\");

		// grab the category information for this dish
		String catColumns = menuColumns[1];
		ItemCatId = categoryGetIndex(catColumns);
		ItemCat = CategoryEng.get(ItemCatId).trim();
		ItemCatAlt = CategoryAlt.get(ItemCatId).trim();

		if (isChinese()) {
			OrderDesc = descColumns[1];
		} else {
			OrderDesc = descColumns[0];
		}
		OrderItem = itemColumns[0];
		OrderItemAlt = itemColumns[1];
		rmbItem = rmbColumns[0].split("%");
		rmbItemAlt = rmbColumns[1].split("%");

		String optionColumns = menuColumns[7];
		String extraColumns = menuColumns[8];

		optionsItem = optionColumns.split("%");
		extrasItem = extraColumns.split("%");
	}

	public void showThePopup() {

		dialog = new Dialog(this);

		// Now ready to display the popup chooser

		dialog.setContentView(R.layout.bigpic_popup);

		// lets scale the title on the popup box
		String tit = OrderItem;
		if (isChinese()) tit = OrderItemAlt;

		dialog.setTitle(tit);

		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		txt2 = (TextView) dialog.findViewById(R.id.Text1b);
		txt2.setText(OrderDesc);
		txt2.setTextSize(txtSize0);
		txt2.setVisibility(View.GONE);

		// set up all the elements for the dish popup dialog: 1)main 2)options 3)extras 4)special instructions
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(5, 0, 5, 5);
		layoutParams.gravity = Gravity.LEFT;

		LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(150, 100);
		layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
		layoutParams3.gravity = Gravity.CENTER;

		// set up the main dish Buttons
		LinearLayout llMain = (LinearLayout) dialog.findViewById(R.id.llMain);
		// new HOR lin Lay
		LinearLayout newLL;
        newLL = new LinearLayout(POSActivity.this);
		newLL.setLayoutParams(layoutParams);
		newLL.setOrientation(LinearLayout.HORIZONTAL);
		newLL.setHorizontalGravity(Gravity.RIGHT);
		int lineLL = 0;
		int itemLL = 0;
		llMain.addView(newLL, lineLL);
		// work through the items
		for (int i = 0; i < rmbItem.length; i++) {
            rbM[i] = new Button(POSActivity.this);
			//rbM[i].setId(i);
			rbM[i].setTag(i);
			if (isChinese()) {
				String s = rmbItemAlt[i];
				s = removeRMBnumber(s);
				if (s.length() == 0) s = tit;
				rbM[i].setText(s);
			} else {
				String s = rmbItem[i];
				s = removeRMBnumber(s);
				if (s.length() == 0) s = tit;
				rbM[i].setText(s);
			}
			rbM[i].setTextColor(Color.parseColor(textColors[0]));
			rbM[i].setTextSize(txtSize0);
			rbM[i].setBackgroundResource(R.drawable.border_yellow_tight);
			rbM[i].setPadding(5, 5, 5, 5);
			rbM[i].setGravity(Gravity.CENTER);
			// set up the clickers which do the ADD TO ORDER function
			rbM[i].setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					butMainClick(v);
				}
			});
			// new row every 5 items
			newLL.addView(rbM[i], itemLL % 5, layoutParams3);
			itemLL = itemLL + 1;
			int remainder = itemLL % 5;
			if (remainder == 0) {
				// start a new LL Horizontal
				lineLL = lineLL + 1;
                newLL = new LinearLayout(POSActivity.this);
				newLL.setLayoutParams(layoutParams);
				newLL.setOrientation(LinearLayout.HORIZONTAL);
				llMain.addView(newLL, lineLL);
			}
		}

		// set up the Options
		numOptions = 0;        // support multiple option groups per dish
		if (!optionsItem[0].equalsIgnoreCase("none")) {
			numOptions = optionsItem.length;
			LinearLayout llOption = (LinearLayout) dialog.findViewById(R.id.llOptions);
			// set up array of BUTTONS for each group
			for (int j = 0; j < numOptions; j++) {
				// new HOR lin Lay
                newLL = new LinearLayout(POSActivity.this);
				newLL.setLayoutParams(layoutParams);
				newLL.setOrientation(LinearLayout.HORIZONTAL);
				lineLL = 0;
				itemLL = 0;
				llOption.addView(newLL, lineLL);
				// get the index of the option
				int oo = optionsGetIndex(optionsItem[j]);
				// get the options into an array parsing by the %
				String line = optionsAll[oo];
				String[] optColumns = line.split("\\|");
				String[] Opt = optColumns[1].split("\\\\");
				String[] OptDetail = Opt[0].split("%");        // english
				String[] OptDetailAlt = Opt[1].split("%");    // alt language
				// set up the buttons
				for (int i = 0; i < OptDetail.length; i++) {
                    rbO[j][i] = new Button(POSActivity.this);
					rbO[j][i].setTag(j);
					if (isChinese()) {
						String s = OptDetailAlt[i];
						s = removeRMBnumber(s);
						rbO[j][i].setText(s);
					} else {
						String s = OptDetail[i];
						s = removeRMBnumber(s);
						rbO[j][i].setText(s);
					}
					rbO[j][i].setTextColor(Color.parseColor(textColors[3]));
					rbO[j][i].setTextSize(txtSize0);
					rbO[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
					rbO[j][i].setPadding(5, 5, 5, 5);
					rbO[j][i].setGravity(Gravity.CENTER);
					// set up the clickers which do the ADD TO ORDER function
					rbO[j][i].setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							butOptionClick(v);
						}
					});
					// new row every 5 items
					newLL.addView(rbO[j][i], itemLL % 5, layoutParams3);
					itemLL = itemLL + 1;
					int remainder = itemLL % 5;
					if (remainder == 0) {
						// start a new LL Horizontal
						lineLL = lineLL + 1;
                        newLL = new LinearLayout(POSActivity.this);
						newLL.setLayoutParams(layoutParams);
						newLL.setOrientation(LinearLayout.HORIZONTAL);
						llOption.addView(newLL, lineLL);
					}
				}
				rbO[j][0].setBackgroundResource(R.drawable.border_grey_tight);
				rbO[j][0].setTag(1000 + (Integer) rbO[j][0].getTag());
			}
			addDividerGap(llOption);
		}

		// set up the EXTRAS check boxes
		numExtras = 0;

        //Toast.makeText(POSActivity.this, "B4Inside=" + extrasItem[0] + " Len=" + extrasItem[0].length(), Toast.LENGTH_SHORT).show();
		if (!extrasItem[0].equalsIgnoreCase("none")) {
            //Toast.makeText(POSActivity.this, "Inside=" + extrasItem[0] + " Len=" + extrasItem[0].length(), Toast.LENGTH_SHORT).show();
			numExtras = extrasItem.length;
			LinearLayout llExtra = (LinearLayout) dialog.findViewById(R.id.llExtras);
			// set up array of BUTTONS for each group
			for (int j = 0; j < numExtras; j++) {
				// new HOR lin Lay
                newLL = new LinearLayout(POSActivity.this);
				newLL.setLayoutParams(layoutParams);
				newLL.setOrientation(LinearLayout.HORIZONTAL);
				lineLL = 0;
				itemLL = 0;
				llExtra.addView(newLL, lineLL);
				// get the index of the extra
				int ee = extrasGetIndex(extrasItem[j]);
				// get the options into an array parsing by the %
				String line = extrasAll[ee];
				String[] extColumns = line.split("\\|");
				String[] Ext = extColumns[1].split("\\\\");
				String[] ExtDetail = Ext[0].split("%");        // english
				String[] ExtDetailAlt = Ext[1].split("%");    // alt language

				// set up the buttons
				for (int i = 0; i < ExtDetail.length; i++) {
                    rbE[j][i] = new Button(POSActivity.this);
					final Integer btnID = 100 * j + i;
					rbE[j][i].setId(btnID);
					rbE[j][i].setTag(j);

					String s = ExtDetail[i];
					s = removeRMBnumber(s);
					rbEEng[j][i] = ExtDetail[i];
					rbEAlt[j][i] = ExtDetailAlt[i];
					if (isChinese()) {
						s = ExtDetailAlt[i];
						s = removeRMBnumber(s);
						rbE[j][i].setText(s);
					} else {
						rbE[j][i].setText(s);

					}
					rbE[j][i].setTextColor(Color.parseColor(textColors[4]));
					rbE[j][i].setTextSize(txtSize0);
					rbE[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
					rbE[j][i].setPadding(5, 5, 5, 5);
					rbE[j][i].setGravity(Gravity.CENTER);
					// set up the clickers which do the ADD TO ORDER function
					rbE[j][i].setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							butExtraClick(v);
						}
					});

					rbE[j][i].setOnLongClickListener(new View.OnLongClickListener() {
						public boolean onLongClick(View v) {
							butExtraLongClick(v);
							return true;
						}
					});
					// new row every 5 items
					newLL.addView(rbE[j][i], itemLL % 5, layoutParams3);
					itemLL = itemLL + 1;
					int remainder = itemLL % 5;
					if (remainder == 0) {
						// start a new LL Horizontal
						lineLL = lineLL + 1;
                        newLL = new LinearLayout(POSActivity.this);
						newLL.setLayoutParams(layoutParams);
						newLL.setOrientation(LinearLayout.HORIZONTAL);
						llExtra.addView(newLL, lineLL);
					}
				}
			}
		}
		//now that the dialog is set up, it's time to show it
		dialog.show();
	}

	public void butOptionClick(View v) {
		// clear all buttons in the group
		int j = (Integer) v.getTag();
		j = j % 1000;
		int oo = optionsGetIndex(optionsItem[j]);
		String line = optionsAll[oo];
		String[] optColumns = line.split("\\|");
		String[] Opt = optColumns[1].split("\\\\");
		String[] OptDetail = Opt[0].split("%");        // english only for the ticket
		for (int i = 0; i < OptDetail.length; i++) {
			rbO[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
			rbO[j][i].setTag(j);
		}
		// set the selected
		v.setBackgroundResource(R.drawable.border_grey_tight);
		v.setTag(1000 + (Integer) v.getTag());
	}

	private void butExtraClick(View v) {
		// set the selected
		v.setBackgroundResource(R.drawable.border_grey_tight);
		v.setTag(1000 + (Integer) v.getTag());
	}

	public void butExtraLongClick(View v) {
		int j = (Integer) v.getTag() % 1000;
		// set the selected
		v.setBackgroundResource(R.drawable.border_grey2_tight);
		v.setTag(j);
	}

	// This routine is called when a Price selector is pressed, this will add the dish and its
	// selections (options/extras/qty/specins) to the order ticket
	public void butMainClick(View v) {
		int value = (Integer) v.getTag();
		// load up the dish main option (price selector)

		// Create the initial JSON order and Options and Extras holders
		JSONArray JSONOptionsAry = new JSONArray();
		JSONArray JSONExtrasAry = new JSONArray();

		// Reset the dish price
		int priceUnitTotal = 0;
		int priceUnitTotalFull = 0;
		int priceUnitBase = 0;
		int priceDiscount = 100;
		int priceQtyTotal = 0;
		int dishQty = 1;

		// until we have a JSON menu, we need to use the following functions to get our pricing numbers
		String priceOptionName = removeRMBnumber(rmbItem[value]);
		String priceOptionNameAlt = removeRMBnumber(rmbItemAlt[value]);

		// load up the dish options (non-price) if available
		if (numOptions > 0) {
			for (int j = 0; j < numOptions; j++) {
				int oo = optionsGetIndex(optionsItem[j]);
				String line = optionsAll[oo];
				String[] optColumns = line.split("\\|");
				String[] Opt = optColumns[1].split("\\\\");
				String[] OptDetail = Opt[0].split("%");        // english only for the ticket
				String[] OptDetailAlt = Opt[1].split("%");    // alt language

				for (int i = 0; i < OptDetail.length; i++) {
					if ((Integer) (rbO[j][i].getTag()) >= 1000) {
						String OptDet = OptDetail[i];        // english only for the ticket
						try {
							// Append each Dish Option to the global array
							JSONArray aryO = new JSONArray();
							aryO.put(createInt("optionId", j));

							String orderSecondaryTxt = OptDetail[i].trim();
							String orderSecondaryTxtAlt = OptDetailAlt[i].trim();

							aryO.put(createInt("optionPrice", getRMBnumber(orderSecondaryTxt)));
							priceUnitTotal = priceUnitTotal + getRMBnumber(orderSecondaryTxt);

							aryO.put(createStr("optionName", removeRMBnumber(orderSecondaryTxt)));
							aryO.put(createStr("optionNameAlt", removeRMBnumber(orderSecondaryTxtAlt)));
							JSONOptionsAry.put(aryO);    // append the dish options
						} catch (JSONException e) {
							log("JSON Add Dish Options Exception=" + e);
						}
					}
				}
			}
		}

		// load up the dish extras
		if (numExtras > 0) {
			for (int j = 0; j < numExtras; j++) {
				int ee = extrasGetIndex(extrasItem[j]);
				String line = extrasAll[ee];
				String[] extColumns = line.split("\\|");
				String[] Ext = extColumns[1].split("\\\\");
				String[] ExtDetail = Ext[0].split("%");        // english
				String[] ExtDetailAlt = Ext[1].split("%");    // alt language

				for (int i = 0; i < ExtDetail.length; i++) {
					if ((Integer) (rbE[j][i].getTag()) >= 1000) {
						//get the modified text from the button
						////int k = (Integer)rbE[j][i].getId();
						////Button but = (Button) dialog.findViewById(k);
						////String ExtDet = but.getText().toString();
						String ExtDet = rbEEng[j][i];
						String ExtDetAlt = rbEAlt[j][i];

						try {
							// Append selected Dish Extras to the global array
							JSONArray aryE = new JSONArray();
							aryE.put(createInt("extraId", j));
							aryE.put(createStr("extraItem", extrasItem[j]));
							aryE.put(createInt("extraIndex", ee));
							aryE.put(createInt("extraPrice", getRMBnumber(ExtDet)));
							priceUnitTotal = priceUnitTotal + getRMBnumber(ExtDet);
							aryE.put(createStr("extraName", removeRMBnumber(ExtDet)));
							aryE.put(createStr("extraNameAlt", removeRMBnumber(ExtDetAlt)));
							JSONExtrasAry.put(aryE);    // append the dish extras
						} catch (JSONException e) {
							log("JSON Add Dish Extras Exception=" + e);
						}
					}
				}
			}
		}

		// note that priceUnitTotal may already contain value from the Options and Extras processing
		priceUnitBase = getRMBnumber(rmbItem[value]);
		priceUnitTotal = priceUnitTotal + priceUnitBase;
		priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations
		priceQtyTotal = priceUnitTotal * dishQty;

		// add it to the order list, new items placed at the top of the list
		try {
			// update the JSON with this item
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

			JSONArray JSONDishAry = new JSONArray();
			JSONDishAry.put(createInt("dishId", Position));
			JSONDishAry.put(createStr("dishName", OrderItem));
			JSONDishAry.put(createStr("dishNameAlt", OrderItemAlt));
			JSONDishAry.put(createStr("categoryName", ItemCat));
			JSONDishAry.put(createStr("categoryNameAlt", ItemCatAlt));
			JSONDishAry.put(createInt("categoryId", ItemCatId));
			JSONDishAry.put(createInt("priceOptionId", value));
			JSONDishAry.put(createStr("priceOptionName", priceOptionName));
			JSONDishAry.put(createStr("priceOptionNameAlt", priceOptionNameAlt));
			JSONDishAry.put(createInt("qty", dishQty));
			JSONDishAry.put(createInt("priceUnitBase", priceUnitBase));
			JSONDishAry.put(createInt("priceUnitTotal", priceUnitTotal));
			JSONDishAry.put(createInt("priceUnitTotalFull", priceUnitTotalFull));
			JSONDishAry.put(createInt("priceDiscount", priceDiscount));
			JSONDishAry.put(createInt("priceQtyTotal", priceQtyTotal));
			JSONDishAry.put(createStr("specIns", ""));
			JSONDishAry.put(createBoolean("dishPrinted", false));
			JSONDishAry.put(createBoolean("counterOnly", ItemCounterOnly));

			// Add the dish Options which were built when they were selected ...
			JSONObject aryO = new JSONObject();
			aryO.put("options", JSONOptionsAry);
			JSONDishAry.put(aryO);

			// Add the dish Extras which were built when they were selected ...
			JSONObject aryE = new JSONObject();
			aryE.put("extras", JSONExtrasAry);
			JSONDishAry.put(aryE);

			// Update the order dishes
			JSONdishesAry.put(JSONDishAry);     // append this dish to the JSON dishes
			JSONObject ary = new JSONObject(); // new object to store the new dishes
			ary.put("dishes", JSONdishesAry);
			JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
			saveJsonTable(currentTableID, JSONOrderAry);

			// update the tabstate
			ary = new JSONObject();
			ary.put("tabstate", 0);
			JSONOrderAry.put(jsonGetter3(JSONOrderAry, "tabstate"), ary);
			saveJsonTable(currentTableID, JSONOrderAry);

			// update the total price of the order
			ary = new JSONObject();
			ary.put("ordertotal", updateOrderTotalRMB(currentTableID));
			JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
			saveJsonTable(currentTableID, JSONOrderAry);

		} catch (JSONException e) {
			log("JSON Add Dish Exception=" + e);
		}
		listOrder.scrollTo(0, 0);
		dialog.dismiss();
		mHandler.post(mUpdateResults);
	}

	public void showOrderItemPopup(final int position) {
		dialog = new Dialog(this);
		dialog.setContentView(R.layout.order_popup);
		// Title for the popup modify item box
		String tit = getString(R.string.msg_modify_item);
		dialog.setTitle(tit);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(5, 0, 5, 5);
		layoutParams.gravity = Gravity.LEFT;

		LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(120, 80);
		layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
		layoutParams3.gravity = Gravity.CENTER;

		// set up the Headers and Buttons under this main LinearLayout
		LinearLayout OIPMain = (LinearLayout) dialog.findViewById(R.id.OrderItemPopupMain);
		OIPMain.removeAllViews();

		// Walk through the modifier ArrayList, which contains JSON entries for 3 types of modifiers:
		// Type 0 - apply discounts to the dish
		// Type 1 - change the quantity of a dish
		// Type 2 - add special instructions to the dish
		// Lastly, Special Instructions will also have a CUSTOM option --->> Need to port to the POS app.

		// Work from last to first so they appear in the order as they are listed in the JSON modifiers file
		//for (int i=0; i<Global.modifiers.size(); i++) {
		for (int i = Global.modifiers.size() - 1; i >= 0; i--) {
			try {
				JSONArray tmp = new JSONArray(Global.modifiers.get(i));
				String nam = "";
				if (isChinese()) nam = jsonGetter2(tmp, "namealt").toString();
				else nam = jsonGetter2(tmp, "name").toString();
				JSONObject JSONdishObj = tmp.getJSONObject(jsonGetter3(tmp, "items"));
				JSONArray JSONitems = JSONdishObj.getJSONArray("items");

				// New VERTICAL Linear Layout for each TextView header and BUTTON row
				LinearLayout newLLV;
                newLLV = new LinearLayout(POSActivity.this);
				newLLV.setLayoutParams(layoutParams);
				newLLV.setOrientation(LinearLayout.VERTICAL);
				newLLV.setHorizontalGravity(Gravity.LEFT);
				// New HORIZONTAL Linear Layout for the buttons in the group
				LinearLayout newLLH;
                newLLH = new LinearLayout(POSActivity.this);
				newLLH.setLayoutParams(layoutParams);
				newLLH.setOrientation(LinearLayout.HORIZONTAL);
				newLLH.setHorizontalGravity(Gravity.CENTER);

				for (int j = 0; j < JSONitems.length(); j++) {
					JSONArray ji = JSONitems.getJSONArray(j);
                    butOIP[j] = new Button(POSActivity.this);
					butOIP[j].setTag(ji.toString()); // put the whole JSON item into the tag so we know what to do when it gets pressed
					if (isChinese()) butOIP[j].setText(jsonGetter2(ji, "titlealt").toString());
					else butOIP[j].setText(jsonGetter2(ji, "title").toString());
					butOIP[j].setTextColor(Color.parseColor(textColors[0]));
					butOIP[j].setTextSize(txtSize1);
					butOIP[j].setBackgroundResource(R.drawable.border_yellow_tight);
					butOIP[j].setPadding(5, 5, 5, 5);
					butOIP[j].setGravity(Gravity.CENTER);
					// set up the clickers which do the ADD TO ORDER function
					butOIP[j].setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							butUpdateClick(v, position);
						}
					});
					newLLH.addView(butOIP[j], j, layoutParams3);
				}
				// Add the header
                TextView tvtitle = new TextView(POSActivity.this);
				tvtitle.setText(nam);
				tvtitle.setLayoutParams(layoutParams);
				newLLV.addView(tvtitle);
				// Add the button row
				newLLV.addView(newLLH);
				// Update the main view
				OIPMain.addView(newLLV, 0);
			} catch (Exception e) {
				log("orderItemPopup Exception=" + e);
			}
		}

		// Add the final custom spec ins button
		// New VERTICAL Linear Layout for each TextView header and BUTTON row
		LinearLayout newLLV;
        newLLV = new LinearLayout(POSActivity.this);
		newLLV.setLayoutParams(layoutParams);
		newLLV.setOrientation(LinearLayout.VERTICAL);
		newLLV.setHorizontalGravity(Gravity.LEFT);
		// New HORIZONTAL Linear Layout for the buttons in the group
		LinearLayout newLLH;
        newLLH = new LinearLayout(POSActivity.this);
		newLLH.setLayoutParams(layoutParams);
		newLLH.setOrientation(LinearLayout.HORIZONTAL);
		newLLH.setHorizontalGravity(Gravity.CENTER);
		// Add the button
        Button butCustSI = new Button(POSActivity.this);
		butCustSI.setText(getString(R.string.special_ins_custom));
		butCustSI.setTextColor(Color.parseColor(textColors[0]));
		butCustSI.setTextSize(txtSize1);
		butCustSI.setBackgroundResource(R.drawable.border_yellow_tight);
		butCustSI.setPadding(5, 5, 5, 5);
		butCustSI.setGravity(Gravity.CENTER);
		// set up the clickers which do the ADD TO ORDER function
		butCustSI.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					// Custom dialog needed to get the instructions. position=the dish index
                    final Dialog dialogAS = new Dialog(POSActivity.this);

					dialogAS.setContentView(R.layout.special_instruction);
					dialogAS.setCancelable(true);
					dialogAS.setCanceledOnTouchOutside(true);

					final JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
					JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
					JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
					final JSONArray jd = JSONdishesAry.getJSONArray(position);

					// lets scale the title on the popup box
					String tit = getString(R.string.special_ins_title);
					dialogAS.setTitle(tit);

					TextView AStext = (TextView) dialogAS.findViewById(R.id.SItext);
					AStext.setText(getString(R.string.special_ins_text1));
					AStext.setTextSize(txtSize0);
					AStext.setTextColor(Color.parseColor(textColors[0]));
					// edit text box is next
					Button AScancel = (Button) dialogAS.findViewById(R.id.SIcancel);
					AScancel.setTextSize(txtSize1);
					AScancel.setText(getString(R.string.tab2_si_cancel));
					AScancel.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							dialogAS.dismiss();
						}
					});
					Button ASsave = (Button) dialogAS.findViewById(R.id.SIadd);
					ASsave.setTextSize(txtSize1);
					ASsave.setText(getString(R.string.tab2_si_save));
					ASsave.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							specET = (EditText) dialogAS.findViewById(R.id.SIedit);
							String specins = specET.getText().toString();
							specins = specins.replaceAll("[^\\p{L}\\p{N}\\s]", "");
							if (specins.length() > 0) specins = specins + " ";
							// Save it
							jsonSetter(jd, "specIns", specins);
							// update everything
							saveJsonTable(currentTableID, JSONOrderAry);
							mHandler.post(mUpdateResults);
							// Clear soft keyboard
							specET.clearFocus();
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							if (imm != null)
								imm.hideSoftInputFromWindow(specET.getWindowToken(), 0);
							dialogAS.dismiss();
						}
					});
					// Set the initial value of the ET box
					specET = (EditText) dialogAS.findViewById(R.id.SIedit);
					String specins = jsonGetter2(jd, "specIns").toString();
					specET.setText(specins);

					dialogAS.show();
				} catch (Exception e) {
					log("orderItemPopup2 Exception=" + e);
				}
			}
		});
		newLLH.addView(butCustSI, 0, layoutParams3);
		// Add the header
        TextView tvtitle = new TextView(POSActivity.this);
		tvtitle.setText(getString(R.string.special_ins_title));
		tvtitle.setLayoutParams(layoutParams);
		newLLV.addView(tvtitle);
		// Add the button row
		newLLV.addView(newLLH);
		// Update the main view
		OIPMain.addView(newLLV, 0);

		dialog.show();
	}

	public void butUpdateClick(View v, int position) {
		String tmp = v.getTag().toString();
		try {
			// The button tag has all the needed information encoded in JSON, so grab the type to get started
			JSONArray ji = new JSONArray(tmp);
			int type = (Integer) jsonGetter2(ji, "type");

			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
			int dishnum = position;
			JSONArray jd = JSONdishesAry.getJSONArray(dishnum);
			String specins = jsonGetter2(jd, "specIns").toString();

			// Handle discounts
			if (type == 0) {
				if (Global.UserLevel > 0) {
					int discount = (Integer) jsonGetter2(ji, "discount");
					int putf = (Integer) jsonGetter2(jd, "priceUnitTotalFull");
					int qty = (Integer) jsonGetter2(jd, "qty");
					float ratio = ((float) discount / (float) 100.0);
					int newput = (int) ((float) putf * ratio);
					jsonSetter(jd, "priceUnitTotal", newput);
					jsonSetter(jd, "priceDiscount", discount);
					jsonSetter(jd, "priceQtyTotal", newput * qty);
					// update everything
					saveJsonTable(currentTableID, JSONOrderAry);
					// and then update the total price of the order
					JSONObject ary = new JSONObject();
					ary.put("ordertotal", updateOrderTotalRMB(currentTableID));
					JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
					saveJsonTable(currentTableID, JSONOrderAry);
					mHandler.post(mUpdateResults);
					// save record on server
					String sendserver = "10," + Utils.GetDateTime() + "," + Global.ServerName + "," + discount;
					activityLogger(sendserver);
				} else {
                    Toast.makeText(POSActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
				}
			}
			// Handle Quantities
			if (type == 1) {
				int newqty = (Integer) jsonGetter2(ji, "qty");
				//int qty = (Integer) jsonGetter2(jd,"qty");
				int put = (Integer) jsonGetter2(jd, "priceUnitTotal");
				jsonSetter(jd, "qty", newqty);
				jsonSetter(jd, "priceQtyTotal", put * newqty);
			}
			// handle the MODIFIERS for the Special Instructions area
			if (type == 2) {
				String spec = jsonGetter2(ji, "title").toString();
				if (specins.indexOf(spec) < 0) specins = specins + spec + " ";
				jsonSetter(jd, "specIns", specins);
			}

			dialog.dismiss();
			// update everything
			saveJsonTable(currentTableID, JSONOrderAry);
			// and then update the total price of the order
			JSONObject ary = new JSONObject();
			ary.put("ordertotal", updateOrderTotalRMB(currentTableID));
			JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
			saveJsonTable(currentTableID, JSONOrderAry);
		} catch (JSONException e) {
			log("JSON Add Modifier Exception=" + e);
		}
		mHandler.post(mUpdateResults);
	}

	private class GridAdapter extends ArrayAdapter {
		Context ctxt;
		private ArrayList<String> data;
		String[] menuItem;
		String[] categoryAll;
		int textSize;

		GridAdapter(Context ctxt, int resource, ArrayList<String> items) {
			super(ctxt, resource, items);
			this.ctxt = ctxt;
			data = items;
			menuItem = Global.MENUTXT.split("\\n");
			categoryAll = Global.CATEGORYTXT.split("\\n");
			textSize = (txtSize3);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) convertView;
			if (convertView == null) {
				convertView = new TextView(ctxt);
				label = (TextView) convertView;
			}
			String tempString = data.get(position);
			SpannableString spanString = new SpannableString(tempString);
			spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
			label.setText(spanString);

			String line = menuItem[position];
			String[] menuColumns = line.split("\\|");
			if (isChinese()) {
				String[] menuLang = menuColumns[2].split("\\\\");
				tempString = menuLang[1];
				spanString = new SpannableString(tempString);
				spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
				label.setText(spanString);
			}
			label.setTextSize(textSize);
            label.setHeight((int) (Utils.getWindowButtonHeight(POSActivity.this)));

			String catColumns = menuColumns[1];
			// look up the category and set the language
			int qq = categoryGetIndex(catColumns);

			states = new StateListDrawable();
			states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor(textColors[2])));
			states.addState(new int[]{}, new ColorDrawable(Color.parseColor(colors[qq])));
			label.setBackgroundDrawable(states);
			//label.setTextColor(Color.parseColor(textColors[0]));		// just white text
			label.setTextColor(Color.parseColor(menubutcolors[qq]));    // black or white text
			if (menubutcolors[qq].equalsIgnoreCase("#ffffff")) {
				label.setShadowLayer((float) 0.01, 1, 2, Color.parseColor(textColors[7]));    // dark shadow
			}
			label.setPadding(4, 2, 4, 2);    // l,t,r,b
			label.setGravity(Gravity.CENTER);
			return (convertView);
		}
	}

	private class OrderAdapter extends ArrayAdapter<JSONArray> {
		private ArrayList<JSONArray> items;
		private int orderFontSize;
		private int orderFontSizeSmall;
		private SparseBooleanArray mSelectedItemsIds;

        public OrderAdapter(POSActivity posActivity, int textViewResourceId, ArrayList<JSONArray> items) {
			super(getBaseContext(), textViewResourceId, items);
			this.items = items;
            orderFontSize = (Utils.getFontSize(POSActivity.this));
            orderFontSizeSmall = (int) (Utils.getFontSize(POSActivity.this) / 1.2);
			mSelectedItemsIds = new SparseBooleanArray();
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_item, null);
			}

			JSONArray o = items.get(position);
			if (o != null) {
				TextView qt = (TextView) v.findViewById(R.id.list_item_qty);
				String quantity = jsonGetter2(o, "qty").toString();
				String discount = jsonGetter2(o, "priceDiscount").toString();

				String discountstr = "";
				if (discount.equalsIgnoreCase("100")) {
					discountstr = "";
				} else if (discount.equalsIgnoreCase("0")) {
					discountstr = "(free)";
				} else {
					discountstr = "(" + discount + "%)";
				}
				qt.setText("x " + quantity);
				qt.setTextSize(orderFontSizeSmall);

				TextView up = (TextView) v.findViewById(R.id.list_item_unitprice);
				up.setText(jsonGetter2(o, "priceUnitTotal").toString() + discountstr);
				up.setTextSize(orderFontSizeSmall);

				TextView qp = (TextView) v.findViewById(R.id.list_item_qtyprice);
				qp.setText(jsonGetter2(o, "priceQtyTotal").toString() + ".00");
				qp.setTextSize(orderFontSizeSmall);

				// Set the main dish title with price option + options + extras + special instructions
				TextView tt = (TextView) v.findViewById(R.id.list_item_title);
				TextView st = (TextView) v.findViewById(R.id.list_item_subtitle);
				CheckBox cb = (CheckBox) v.findViewById(R.id.sentPrintersCB);

				try {
					tt.setTextSize(orderFontSizeSmall);
					st.setTextSize(orderFontSizeSmall);

					// Start with dish name
					String dishtext;
					if (Global.EnglishLang) dishtext = jsonGetter2(o, "dishName").toString();
					else dishtext = jsonGetter2(o, "dishNameAlt").toString();
					tt.setText(dishtext.trim());

					// Handle price option
					String dishsubtext = "";
					String priceopt;
					if (Global.EnglishLang) priceopt = jsonGetter2(o, "priceOptionName").toString();
					else priceopt = jsonGetter2(o, "priceOptionNameAlt").toString();
					if (priceopt.length() > 0) {
						dishsubtext = dishsubtext + priceopt;
					}

					// Add all the Option choices
					JSONObject dishopt = new JSONObject();
					dishopt = o.getJSONObject(jsonGetter3(o, "options"));
					JSONArray dishoptAry = dishopt.getJSONArray("options");
					//log("opt=" + dishoptAry.toString(1));
					if (dishoptAry.length() > 0) {
						dishsubtext = dishsubtext + "\n";
						// Loop print
						for (int i = 0; i < dishoptAry.length(); i++) {
							//dishtext = dishtext + dishoptAry.getString(i);
							// Grab just the optionName
							if (Global.EnglishLang)
								dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionName").toString();
							else
								dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionNameAlt").toString();
							if (i != dishoptAry.length() - 1) dishsubtext = dishsubtext + ", ";
						}
					}
					// Add selected Extra choices
					JSONObject dishext = new JSONObject();
					dishext = o.getJSONObject(jsonGetter3(o, "extras"));
					JSONArray dishextAry = dishext.getJSONArray("extras");
					if (dishextAry.length() > 0) {
						dishsubtext = dishsubtext + "\n";
						// Loop print
						for (int i = 0; i < dishextAry.length(); i++) {
							// Grab just the extraName
							if (Global.EnglishLang)
								dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraName").toString();
							else
								dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraNameAlt").toString();
							if (i != dishextAry.length() - 1) dishsubtext = dishsubtext + ", ";
						}
					}

					// Handle special Instructions
					String specins = jsonGetter2(o, "specIns").toString();
					if (specins.length() > 0) {
						dishsubtext = dishsubtext + "\n";
						dishsubtext = dishsubtext + getString(R.string.special_string) + specins;
					}
					if (dishsubtext.length() == 0) {
						st.setVisibility(View.GONE);
					} else {
						st.setText(dishsubtext.trim());
					}

					// Set up the checkboxes for printed status
					Boolean printed = (Boolean) jsonGetter2(o, "dishPrinted");
					if (printed) {
						cb.setVisibility(View.VISIBLE);
						cb.setChecked(true);
					} else {
						cb.setVisibility(View.VISIBLE);
						cb.setChecked(false);
					}

					listOrder.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView parent, View v, final int position, long id) {
							showOrderItemPopup(position);
							mHandler.post(mUpdateResults);
						}
					});

					// This will be handled by the MultiChoiceModeListener
                	/*
                    listOrder.setOnItemLongClickListener(new OnItemLongClickListener() {
                        public boolean onItemLongClick(AdapterView parent, View v, final int position, long id) {
                        	// Possibly remove the references to currentTableID below
                        	// Only delete orders that have not been printed
                           	if (!dishHasBeenPrinted(currentTableID,position) || Global.PrintedAllowDelete) {
                				// delete the item
                				try {
                					JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTableID]);
                		    		JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry,"dishes"));
                		    		JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                		    		// Remove the selected item
                		    		// Check for SDK version to see if we can use the JSON function directly
                		            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                		            	JSONdishesAry.remove(position);
                		            } else {
                		            	// Do it the old-school way
                		            	JSONdishesAry = RemoveJSONArray(JSONdishesAry,position);
                		            }
                					// replace it
                					JSONObject ary=new JSONObject();	// new object to store the new dishes
                					ary.put("dishes",JSONdishesAry);
                		            JSONOrderAry.put(jsonGetter3(JSONOrderAry,"dishes"), ary);
                		            saveJsonTable(currentTableID,JSONOrderAry);

                		      	  	// update the total price of the order
                		            ary=new JSONObject();
                		            ary.put("ordertotal",updateOrderTotalRMB(currentTableID));
                		      	  	JSONOrderAry.put(jsonGetter3(JSONOrderAry,"ordertotal"), ary);
                		      	  	saveJsonTable(currentTableID,JSONOrderAry);
                				} catch (JSONException e) {
                	  				log("JSON Delete Dish Exception=" + e);
                	            }
                           		mHandler.post(mUpdateResults);
                           	}
                            return true;
                        }
                    });
                    */
				} catch (JSONException e) {
					log("JSON Opt+Ext Exception=" + e);
				}
			}
			return v;
		}

		public void removeSelection() {
			mSelectedItemsIds = new SparseBooleanArray();
			notifyDataSetChanged();
		}

		public void selectView(int position, boolean value) {
			if (value)
				mSelectedItemsIds.put(position, value);
			else
				mSelectedItemsIds.delete(position);
			notifyDataSetChanged();
		}

		public int getSelectedCount() {
			return mSelectedItemsIds.size();
		}

		public SparseBooleanArray getSelectedIds() {
			return mSelectedItemsIds;
		}

		public void toggleSelection(int position) {
			selectView(position, !mSelectedItemsIds.get(position));
		}
	}

	private class TicketAdapter extends GridAdapter {
		Context ctxt;
		private ArrayList<String> data;
		int textSize;
		String telnum;

		TicketAdapter(Context ctxt, int resource, ArrayList<String> items) {
			super(ctxt, resource, items);
			this.ctxt = ctxt;
			data = items;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.ticket_item, null);
			}
			try {
				// read the file to get some key info, need to check in ORDERS and UNSENT
				String fname = data.get(position);
				File readFileO = new File(ordersDir, fname);
				File readFileR = new File(retryDir, fname);
				String order = Utils.ReadLocalFile(readFileO);
				if (order.length() == 0)
					// Must be UNSENT order
					order = Utils.ReadLocalFile(readFileR);
				JSONArray o = new JSONArray(order);
				//JSONArray o = new JSONArray(data.get(position));

				if (o != null) {
					TextView t1 = (TextView) v.findViewById(R.id.ticket_item_orderid);
					//String oid = jsonGetter2(o,"orderid").toString();
					String tt = jsonGetter2(o, "tabletime").toString();
					String tid = jsonGetter2(o, "tablename").toString();
					t1.setText(tt + "-" + tid);
					t1.setTextSize(txtSize2);

					t1 = (TextView) v.findViewById(R.id.ticket_item_ticketnum);
					String tnum = jsonGetter2(o, "ticketnum").toString();
					t1.setText(tnum);
					t1.setTextSize(txtSize2);

					t1 = (TextView) v.findViewById(R.id.ticket_item_time);
					//String tt = jsonGetter2(o,"tabletime").toString();
					t1.setText("Time: " + tt.substring(0, 2) + ":" + tt.substring(2, 4) + ":" + tt.substring(4, 6));
					t1.setTextSize(txtSize2);

					t1 = (TextView) v.findViewById(R.id.ticket_item_ticketid);
					t1.setText("Table: " + tid);
					t1.setTextSize(txtSize2);

					t1 = (TextView) v.findViewById(R.id.ticket_item_dishes);

					JSONObject JSONdishObj = o.getJSONObject(jsonGetter3(o, "dishes"));
					JSONArray jda = JSONdishObj.getJSONArray("dishes");
					int dish = 0;
					if (jda != null) dish = jda.length();
					t1.setText("Dishes: " + dish);
					t1.setTextSize(txtSize2);

					t1 = (TextView) v.findViewById(R.id.ticket_item_total);
					String tot = jsonGetter2(o, "ordertotal").toString();
					t1.setText("Total: " + tot);
					t1.setTextSize(txtSize2);
				}
			} catch (Exception e) {
				log("JSON TicketGetView Exception=" + e);
			}

			return (v);
		}
	}

	public boolean dishHasChoices(int itemPosition) {
		boolean choices1 = true;
		boolean choices2 = true;
		boolean choices3 = true;

		// see if the dish has empty popup, then we can save some clicks
		if (rmbItem.length == 1) choices1 = false;
		if (extrasItem[0].equalsIgnoreCase("none")) choices2 = false;
		if (optionsItem[0].equalsIgnoreCase("none")) choices3 = false;

		return choices1 || choices2 || choices3;
	}

	public boolean anyTabsOpen() {
		boolean opentab = false;
		// see if any of the tables have a tab open
		for (int i = 0; i < MaxTABLES; i++) {
			if (numberOfDishes(i) > 0) {
				opentab = true;
				break;
			}
		}
		return opentab;
	}

	public boolean anyTabsAvailable() {
		boolean availtab = false;
		// see if any of the tables have a tab empty
		for (int i = 0; i < MaxTABLES; i++) {
			if (numberOfDishes(i) == 0) {
				availtab = true;
				break;
			}
		}
		return availtab;
	}

	public boolean checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			// Log.v("KIOSK", "Internet Connection Not Present");
			return false;
		}
	}

	public void lostConnection() {
        AlertDialog alertDialog = new AlertDialog.Builder(POSActivity.this).create();
		alertDialog.setTitle("Connection");
		alertDialog.setIcon(android.R.drawable.stat_sys_warning);
		alertDialog.setMessage("Data connection not available. Please restart.");
		alertDialog.setCancelable(false);
		alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alertDialog.show();
	}

	private void addDividerLine(LinearLayout ll) {
		LinearLayout mll;
		mll = ll;
		// add divider line
        ImageView imageLine = new ImageView(POSActivity.this);
		imageLine.setBackgroundResource(R.drawable.bar_white);
		mll.addView(imageLine);
	}

	private void addDividerGap(LinearLayout ll) {
		LinearLayout mll;
		mll = ll;
		// add divider line
        ImageView imageLine = new ImageView(POSActivity.this);
		imageLine.setBackgroundResource(R.drawable.gap);
		mll.addView(imageLine);
	}

	// Scan though the optionsItem array, find the str, return the location index
	private int optionsGetIndex(String str) {
		int found = 0;
		for (int i = 0; i < optionsAll.length; i++) {
			if (str.equalsIgnoreCase(optionsAll[i].substring(0, str.length()))) {
				found = i;
				break;
			}
		}
		return found;
	}

	// Scan though the extrasItem array, find the str, return the location index
	private int extrasGetIndex(String str) {
		int found = 0;
		for (int i = 0; i < extrasAll.length; i++) {
			if (str.equalsIgnoreCase(extrasAll[i].substring(0, str.length()))) {
				found = i;
				break;
			}
		}
		return found;
	}

	// Scan though the Category array, find the str, return the location index
	private int categoryGetIndex(String str) {
		int found = 0;
		for (int i = 0; i < categoryAll.length; i++) {
			if (str.equalsIgnoreCase(categoryAll[i].substring(0, str.length()))) {
				found = i;
				break;
			}
		}
		return found;
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return (ni != null && ni.isAvailable() && ni.isConnected());
	}

	private boolean haveNetworkConnection() {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}

	public void messageBox(final Context context, final String message, final String title) {
		this.runOnUiThread(
				new Runnable() {
					public void run() {
						final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
						alertDialog.setTitle(title);
						alertDialog.setIcon(android.R.drawable.stat_sys_warning);
						alertDialog.setMessage(message);
						alertDialog.setCancelable(false);
						alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								alertDialog.cancel();
								//finish();
							}
						});
						alertDialog.show();
					}
				}
		);
	}

	BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			infoWifi = "Checking";
			SupplicantState supState;
			WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			supState = wifiInfo.getSupplicantState();
			infoWifi = "" + supState;
			if (supState.equals(SupplicantState.COMPLETED)) {
				// wifi is up so set the title bar
				infoWifi = "OK";
			} else {
				// no wifi so give an update
				if (supState.equals(SupplicantState.SCANNING)) {
					infoWifi = "Scanning";
				} else if (supState.equals(SupplicantState.DISCONNECTED)) {
					infoWifi = "Not Available";
				} else {
					infoWifi = "Connecting";
				}
			}
		}
	};

	private void clearTableSelection() {
		try {
			JSONArray tmp = new JSONArray(Global.saletypes.get(0));
			String nam = "";
			if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
			else nam = jsonGetter2(tmp, "display").toString();
			String color = jsonGetter2(tmp, "color").toString();

			tvcc.setText(nam);
			tvcc.setTextColor(Color.parseColor(textColors[0]));
			tvcc.setBackgroundColor(Color.parseColor(color));

			Global.Guests = "";
			currentTableID = -1;
			Global.TableID = -1;
			Global.TableName = "";

			// set up the View (menu/tables)
			vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
			vfMenuTable.setDisplayedChild(1);

			// update UI
			invalidateOptionsMenu();
			mHandler.post(mUpdatePrinters);
			mHandler.post(mUpdateResults);
		} catch (JSONException e) {
			log("ClearTableSelection Exception=" + e);
		}
	}

	public void tableButClick(View v) {
		try {
			String value = v.getTag().toString();
			currentTableID = Integer.valueOf(value);
			Global.TableID = currentTableID;
			Global.TableName = Global.tablenames.get(currentTableID);

			// reset Saletype
			JSONArray tmp = new JSONArray(Global.saletypes.get(0));
			String nam = "";
			if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
			else nam = jsonGetter2(tmp, "display").toString();
			String color = jsonGetter2(tmp, "color").toString();
			tvcc.setText(nam);
			tvcc.setTextColor(Color.parseColor(textColors[0]));
			tvcc.setBackgroundColor(Color.parseColor(color));

			// reload the order ID
			//if (JSONORDERLIST[currentTableID].isEmpty()) {
			if (numberOfDishes(currentTableID) == 0) {
				Global.TableTime = Utils.GetTime();
				Global.OrderId = Utils.GetDateTime() + "-" + Global.TableName;
				JSONArray JSONtmp = getNewJSONOrder(currentTableID);
				saveJsonTable(currentTableID, JSONtmp);
			} else {
				JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTableID]);
				JSONOrderStr[currentTableID] = JSONtmp.toString(1);
				Global.OrderId = jsonGetter2(JSONtmp, "orderid").toString();
				Global.TableTime = jsonGetter2(JSONtmp, "tabletime").toString();
			}
			invalidateOptionsMenu();
			mHandler.post(mUpdatePrinters);
			mHandler.post(mUpdateResults);
		} catch (JSONException e) {
			log("JSONtmp Exception-TableButClk=" + e);
		}
		// update ui
		invalidateOptionsMenu();
		mHandler.post(mUpdatePrinters);
		mHandler.post(mUpdateResults);
	}

	private void openDrawer() {
		if (isOnline()) {
			try {
				err = POS1Dev.openDevice();
				// ready to print
				err = POS1Dev.sendCommand("ESC p 0 2 2");    // open the money kick pin2 4ms on 2ms off
				err = POS1Dev.sendCommand("ESC p 1 2 2");    // open the money kick pin5 4ms on 2ms off
				err = POS1Dev.closeDevice();
			} catch (Exception ex) {
				String errorString = "";
				if (err != null) errorString = EpsonCom.getErrorText(err);
                messageBox(POSActivity.this,
						"Sorry, Cash Drawer cannot be opened. " + errorString,
						"Connection problem 1");
			}
		} else {
			String errorString = "Sorry, Cash Drawer cannot be opened. ";
            messageBox(POSActivity.this, errorString, "Connection problem 1b");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add(0, 0, menu.NONE, "POS App");
		MenuItem item0 = menu.getItem(0);
		item0.setIcon(null);
		item0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		String vname = getVersionName();
		item0.setTitle(" Ver " + "\n " + vname + " ");

		menu.add(0, 1, menu.NONE, "Printers");
		MenuItem item1 = menu.getItem(1);
		item1.setIcon(null);
		item1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		String char1 = "\u2460";
		String char2 = "\u2461";
		String char3 = "\u2462";
		if (Global.POS1Enable) char1 = "\u2776";
		if (Global.POS2Enable) char2 = "\u2777";
		if (Global.POS3Enable) char3 = "\u2778";
		item1.setTitle(" PRINT " + "\n" + " " + char1 + char2 + char3 + " ");

		menu.add(0, 2, menu.NONE, "User");
		MenuItem item2 = menu.getItem(2);
		item2.setIcon(null);
		item2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		item2.setTitle(" USER " + "\n" + " " + Global.ServerName + " ");

		SubMenu subMenu3 = menu.addSubMenu(0, 3, menu.NONE, "LANG");
		subMenu3.add(0, 10, menu.NONE, "English");
		subMenu3.add(0, 11, menu.NONE, "Chinese");
		MenuItem subMenu3Item = subMenu3.getItem();
		subMenu3Item.setIcon(null);
		subMenu3Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (Global.EnglishLang) {
			subMenu3Item.setTitle("LANG\n" +
					" ENG ");
		} else {
			subMenu3Item.setTitle("LANG\n" +
					"  CH  ");
		}

		menu.add(0, 4, menu.NONE, "Register");
		MenuItem item4 = menu.getItem(4);
		item4.setIcon(null);
		item4.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		item4.setTitle(" REG ");

		menu.add(0, 5, menu.NONE, "Menu");
		MenuItem item5 = menu.getItem(5);
		item5.setIcon(null);
		item5.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (vfMenuTable.getDisplayedChild() == 1) {
			item5.setTitle("    MENU\n" +
					"\u25FC" + " TABLE");
		} else {
			item5.setTitle(" \u25FC" + " MENU\n" +
					"    TABLE");
		}

		menu.add(0, 6, menu.NONE, "Ticket");
		MenuItem item6 = menu.getItem(6);
		item6.setIcon(null);
		item6.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		item6.setTitle(" TICKET " + "\n" + "  " + Global.TicketNum + "  ");

		menu.add(0, 7, menu.NONE, "ID");
		MenuItem item7 = menu.getItem(7);
		item7.setIcon(null);
		item7.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		item7.setTitle("ID" + "\n" + Global.SMID);

		SubMenu subMenu8 = menu.addSubMenu(0, 8, menu.NONE, "Tools");
		subMenu8.setIcon(R.drawable.ic_menu_preferences);
		subMenu8.add(0, 12, menu.NONE, "Status");
		subMenu8.add(0, 13, menu.NONE, "Open Cash Drawer");
		subMenu8.add(0, 15, menu.NONE, "Unsent Orders");
		subMenu8.add(0, 16, menu.NONE, "Daily Summary");
		subMenu8.add(0, 17, menu.NONE, "Move Table");
		subMenu8.add(0, 21, menu.NONE, "Merge Table");
		subMenu8.add(0, 18, menu.NONE, "Clear Table");
		subMenu8.add(0, 19, menu.NONE, "Reload Order");
		subMenu8.add(0, 20, menu.NONE, "Register Logout");
		MenuItem subMenu8Item = subMenu8.getItem();
		subMenu8Item.setIcon(R.drawable.ic_menu_preferences);
		subMenu8Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 12) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.info_dialog, null);

			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			TextView tv0 = (TextView) customDialog.findViewById(R.id.AboutAppName);
			Map<String, String> map0 = new LinkedHashMap<String, String>();
			map0.put(getString(R.string.msg_about_app_name), Global.AppName);
			populateField(map0, tv0);

			TextView tv1 = (TextView) customDialog.findViewById(R.id.AboutVersion);
			Map<String, String> map1 = new LinkedHashMap<String, String>();
			map1.put(getString(R.string.msg_about_version_name), getVersionName());
			populateField(map1, tv1);

			TextView tv2 = (TextView) customDialog.findViewById(R.id.AboutFileSource);
			Map<String, String> map2 = new LinkedHashMap<String, String>();
			map2.put(getString(R.string.msg_about_filesource), Global.FileSource);
			populateField(map2, tv2);

			TextView tv5 = (TextView) customDialog.findViewById(R.id.AboutSmartMenuID);
			Map<String, String> map5 = new LinkedHashMap<String, String>();
			map5.put(getString(R.string.msg_about_smartmenuid), Global.SMID);
			populateField(map5, tv5);

			TextView tv6 = (TextView) customDialog.findViewById(R.id.AboutMenuVersion);
			Map<String, String> map6 = new LinkedHashMap<String, String>();
			map6.put(getString(R.string.msg_about_menuver), Global.MenuVersion);
			populateField(map6, tv6);

			TextView tv7 = (TextView) customDialog.findViewById(R.id.AboutDeviceId);
			Map<String, String> map7 = new LinkedHashMap<String, String>();
			map7.put(getString(R.string.msg_about_deviceid), Global.MasterDeviceId);
			populateField(map7, tv7);

			TextView tv7a = (TextView) customDialog.findViewById(R.id.AboutDeviceIP);
			Map<String, String> map7a = new LinkedHashMap<String, String>();
			map7a.put(getString(R.string.msg_about_deviceip), Utils.getIpAddress(true));
			populateField(map7a, tv7a);

			TextView tv8 = (TextView) customDialog.findViewById(R.id.AboutTicketnum);
			Map<String, String> map8 = new LinkedHashMap<String, String>();
			map8.put(getString(R.string.msg_about_ticketnum), Global.TicketNum.toString());
			populateField(map8, tv8);

			TextView tv9 = (TextView) customDialog.findViewById(R.id.AboutWifi);
			Map<String, String> map9 = new LinkedHashMap<String, String>();
			map9.put(getString(R.string.msg_about_wifistatus), infoWifi);
			populateField(map9, tv9);

			// Include the actual IP addresses do they can see them with the status indicators
			TextView tvServerIP = (TextView) customDialog.findViewById(R.id.label1a2);
			tvServerIP.setText(Global.ServerIP);
			TextView tvPOS1IP = (TextView) customDialog.findViewById(R.id.label2a);
			tvPOS1IP.setText(Global.POS1Ip);
			TextView tvPOS2IP = (TextView) customDialog.findViewById(R.id.label13a);
			tvPOS2IP.setText(Global.POS2Ip);
			TextView tvPOS3IP = (TextView) customDialog.findViewById(R.id.label14a);
			tvPOS3IP.setText(Global.POS3Ip);

			//Create and run status thread for continuous updates
			//createAndRunStatusThread(this,customDialog);
			// Just update once, no need to keep refreshing it.
			updateConnectionStatus(customDialog);
			return (true);
		}
		if (item.getItemId() == 13) {
			// save record on server
			String sendserver = "0," + Utils.GetDateTime() + "," + Global.ServerName;
			activityLogger(sendserver);
			openDrawer();
			return (true);
		}
		if (item.getItemId() == 15) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.queue_dialog, null);

			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			File[] files = retryDir.listFiles();
			unsentItemList.clear();

			for (File f : files)
				unsentItemList.add(f.getName());

			listUnsent = (ListView) customDialog.findViewById(R.id.unsentItemList);
            unsentAdapter = new ArrayAdapter<String>(POSActivity.this, android.R.layout.simple_list_item_1, unsentItemList);
			listUnsent.setAdapter(unsentAdapter);

			// set up a button, when they click, resend all the items
			Button butSnd = (Button) customDialog.findViewById(R.id.butSndAll);
			butSnd.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    final ProgressDialog pd = ProgressDialog.show(POSActivity.this, "Sending", "Sending order(s) to the server...", true, false);
					new Thread(new Runnable() {
						public void run() {
							if ((pingIP()) & (Global.TicketToCloud)) {
								for (String fname : unsentItemList) {

									String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveOrderJsonURL;
									try {
										File readFile = new File(retryDir, fname);
										JSONArray JSONOrder = new JSONArray(Utils.ReadLocalFile(readFile));
										String orderid = jsonGetter2(JSONOrder, "orderid").toString();

										// update the sendtype so resend=2
										JSONObject obj = new JSONObject();
										obj.put("sendtype", "2");
										JSONOrder.put(jsonGetter3(JSONOrder, "sendtype"), obj);

										int sc = Utils.SendMultipartJsonOrder(postURL, JSONOrder.toString(1), Global.SMID);
										log("Resent=" + orderid + " status code=" + sc);
										if (sc == 200) {
											if (readFile.delete()) {
												writeOutFile(ordersDir, fname, JSONOrder.toString());
												log("file deleted:" + fname + " orderid=" + orderid + " sc=" + sc);
											} else {
												log("file not deleted:" + fname + " orderid=" + orderid + " sc=" + sc);
											}
										}
									} catch (Exception e) {
										log("Resending from JSON failed");
									}
								}
							}
							pd.dismiss();
							customDialog.dismiss();
						}
					}).start();
				}
			});
			// set up a button, when they click, send all the items
			Button butDel = (Button) customDialog.findViewById(R.id.butDelAll);
			butDel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					File[] files = retryDir.listFiles();
					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							files[i].delete();
						}
					}
					customDialog.dismiss();
				}
			});
			return (true);
		}
		if (item.getItemId() == 16) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.daily_summary, null);
			final CustomDialog customDialog = new CustomDialog(this);

			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			final Button reloadDate = (Button) customDialog.findViewById(R.id.reloadDate);
			reloadDate.setText(getString(R.string.reload_date));

			// Let them choose a date
			final Calendar myCalendar = Calendar.getInstance();
			final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					myCalendar.set(Calendar.YEAR, year);
					myCalendar.set(Calendar.MONTH, monthOfYear);
					myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

					String myFormat = "yyyy/MM/dd";
					String myFormatYYYY = "yyyy";
					String myFormatMM = "MM";
					String myFormatDD = "dd";
					SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
					SimpleDateFormat sdfYYYY = new SimpleDateFormat(myFormatYYYY, Locale.getDefault());
					SimpleDateFormat sdfMM = new SimpleDateFormat(myFormatMM, Locale.getDefault());
					SimpleDateFormat sdfDD = new SimpleDateFormat(myFormatDD, Locale.getDefault());
					final String myDate = sdf.format(myCalendar.getTime());
					reloadDate.setText(myDate);
					final String myDateYYYY = sdfYYYY.format(myCalendar.getTime());
					final String myDateMM = sdfMM.format(myCalendar.getTime());
					final String myDateDD = sdfDD.format(myCalendar.getTime());

                    final ProgressDialog pd = ProgressDialog.show(POSActivity.this, "Daily Summary", "Retrieving from the server...", true, false);
					new Thread(new Runnable() {
						public void run() {
							if (pingIP()) {
								String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosShowDateURL;
								formatTicket = Utils.SendMultipartDatePHP(postURL,
										Global.SMID,
										myDateYYYY,
										myDateMM,
										myDateDD);
							} else {
								formatTicket = "Data not available";
							}
							pd.dismiss();
							customDialogDS = customDialog;
							mHandler.post(mUpdateDailySummary);
						}
					}).start();

					// check for print
					Button butPrnt = (Button) customDialog.findViewById(R.id.dailySummaryPrint);
					butPrnt.setText(getString(R.string.register_print_sum));
					butPrnt.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							if (Global.POS1Enable) {
								try {
									err = POS1Dev.openDevice();
									// ready to print
									err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);
									err = POS1Dev.sendCommand("ESC d 4");
									err = POS1Dev.printString("DAILY SUMMARY", FONT.FONT_A, true, false, true, true);
									err = POS1Dev.sendCommand("ESC d 2");
									err = POS1Dev.printString(formatTicket, FONT.FONT_A, true, false, false, false);
									err = POS1Dev.sendCommand("ESC d 4");
									err = POS1Dev.cutPaper();
									// Close the connection so others can use it
									err = POS1Dev.closeDevice();
								} catch (Exception ex) {
									String errorString = "";
									if (err != null) errorString = EpsonCom.getErrorText(err);
                                    messageBox(POSActivity.this,
											getString(R.string.tab3_pos_err_1) + errorString +
													getString(R.string.tab3_pos_err_2), "Connection problem 1");
								}
							}
						}
					});
				}
			};
			reloadDate.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
                    new DatePickerDialog(POSActivity.this, date, myCalendar
							.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
							myCalendar.get(Calendar.DAY_OF_MONTH)).show();
				}
			});
			return (true);
		}
		if (item.getItemId() == 17) {
			// Move table
			if (anyTabsOpen() && anyTabsAvailable()) {
				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.move_table, null);
				final CustomDialog customDialog = new CustomDialog(this);
				customDialog.setContentView(textEntryView);
				customDialog.show();
				customDialog.setCancelable(true);
				customDialog.setCanceledOnTouchOutside(true);

				final Button fromButton = (Button) customDialog.findViewById(R.id.spinnerNameFrom);
				fromButton.setText(getString(R.string.table_select_from_name));
				fromButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						showTableFromDialog(fromButton);
					}
				});
				final Button toButton = (Button) customDialog.findViewById(R.id.spinnerNameTo);
				toButton.setText(getString(R.string.table_select_to_name));
				toButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						showTableToDialog(toButton);
					}
				});
				Button moveButton = (Button) customDialog.findViewById(R.id.butMove);
				moveButton.setText(getString(R.string.table_move_but));
				moveButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// do the move here
						if ((FromTableID >= 0) && (ToTableID >= 0) && (FromTableID < MaxTABLES) && (ToTableID < MaxTABLES)) {
							try {
								printStatus[ToTableID][0] = printStatus[FromTableID][0];
								printStatus[ToTableID][1] = printStatus[FromTableID][1];
								printStatus[ToTableID][2] = printStatus[FromTableID][2];

								// do the move
								JSONOrderStr[ToTableID] = JSONOrderStr[FromTableID];

								// update the tablename in the new to table JSON, no need to update the OrderID with the original tableID
								JSONArray JSONtmp = new JSONArray(JSONOrderStr[ToTableID]);
								jsonSetter(JSONtmp, "tablename", Global.tablenames.get(ToTableID));
								jsonSetter(JSONtmp, "currenttableid", ToTableID);

								// make it the new active table
								currentTableID = ToTableID;
								// save the JSON for the new table
								saveJsonTable(ToTableID, JSONtmp);

								CheckBox cb = (CheckBox) customDialog.findViewById(R.id.printMoveCB);
								if ((Global.POS1Enable) && (cb.isChecked())) {
									// build the string
									String formatTime = addPad("TIME: " + Utils.GetTimeHM());
									String formatTicket = addPad("MOVE TABLE: " + Global.tablenames.get(FromTableID) + "->" + Global.tablenames.get(ToTableID));
									if (Global.POS1Enable) {
										try {
											err = POS1Dev.openDevice();
											// ready to print
											err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);
											err = POS1Dev.sendCommand("ESC d 5");
											err = POS1Dev.printString(formatTime, FONT.FONT_A, true, false, true, true);
											err = POS1Dev.printString(formatTicket, FONT.FONT_A, true, false, true, true);
											err = POS1Dev.sendCommand("ESC d 5");
											err = POS1Dev.cutPaper();
											// Close the connection so others can use it
											err = POS1Dev.closeDevice();
										} catch (Exception ex) {
											String errorString = "";
											if (err != null)
												errorString = EpsonCom.getErrorText(err);
                                            messageBox(POSActivity.this,
													getString(R.string.tab3_pos_err_1) + errorString +
															getString(R.string.tab3_pos_err_2), "Connection problem 1");
										}
									}
								}

								setJSONOrderList(ToTableID);
								// Save it
								JSONtmp = getNewJSONOrder(FromTableID);
								saveJsonTable(FromTableID, JSONtmp);
							} catch (JSONException e) {
								log("JSON MoveOrder Exception=" + e);
							}
							mHandler.post(mUpdateResults);
							mHandler.post(mUpdatePrinters);
							customDialog.dismiss();
						}
					}
				});
			}
			return (true);
		}
		if (item.getItemId() == 21) {
			// Merge table
			if (anyTabsOpen()) {
				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.merge_table, null);
				final CustomDialog customDialog = new CustomDialog(this);
				customDialog.setContentView(textEntryView);
				customDialog.show();
				customDialog.setCancelable(true);
				customDialog.setCanceledOnTouchOutside(true);

				final Button fromButton = (Button) customDialog.findViewById(R.id.spinnerNameFrom);
				fromButton.setText(getString(R.string.table_select_from_name));
				fromButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						showTableFromDialog(fromButton);
					}
				});
				final Button toButton = (Button) customDialog.findViewById(R.id.spinnerNameTo);
				toButton.setText(getString(R.string.table_select_to_name));
				toButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						showTableMergeDialog(toButton);
					}
				});
				Button mergeButton = (Button) customDialog.findViewById(R.id.butMerge);
				mergeButton.setText(getString(R.string.table_merge_but));
				mergeButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// do the merge here
						if ((FromTableID >= 0) && (ToTableID >= 0) && (FromTableID < MaxTABLES) && (ToTableID < MaxTABLES) && !(FromTableID == ToTableID)) {
							try {
								printStatus[ToTableID][0] = printStatus[FromTableID][0];
								printStatus[ToTableID][1] = printStatus[FromTableID][1];
								printStatus[ToTableID][2] = printStatus[FromTableID][2];

								// The FROM table already has dishes we need to add to the TO table (incremental order)
								// get number of existing dishes
								JSONArray JSONexisting = new JSONArray(JSONOrderStr[ToTableID]);
								JSONObject JSONdishObj = JSONexisting.getJSONObject(jsonGetter3(JSONexisting, "dishes"));
								JSONArray jdatmp = JSONdishObj.getJSONArray("dishes");
								int numdishex = jdatmp.length();
								//log("Number of existing dishes=" + numdishex);

								// get number of new dishes
								JSONArray JSONnew = new JSONArray(JSONOrderStr[FromTableID]);
								JSONdishObj = JSONnew.getJSONObject(jsonGetter3(JSONnew, "dishes"));
								JSONArray jdanew = JSONdishObj.getJSONArray("dishes");
								int numdishnew = jdanew.length();
								//log("Number of new dishes=" + numdishnew);

								// update the existing JSON with the new items, get each dish and copy it over
								for (int i = 0; i < numdishnew; i++) {
									JSONArray jd = jdanew.getJSONArray(i);
									//log("existing b4=" + JSONexisting.toString());
									jdatmp.put(jd);    // append this dish to the JSON dishes
								}

								// JSONtmp has all the dishes, so update the table
								saveJsonTable(ToTableID, JSONexisting);

								// there are some things we need to update now because of the appended dishes
								// update the tablename in the new to table JSON, no need to update the OrderID with the original tableID
								JSONArray JSONtmp = new JSONArray(JSONOrderStr[ToTableID]);
								jsonSetter(JSONtmp, "tablename", Global.tablenames.get(ToTableID));
								jsonSetter(JSONtmp, "currenttableid", ToTableID);

								// update the price
								jsonSetter(JSONtmp, "ordertotal", updateOrderTotalRMB(ToTableID));

								// update the new guests count
								String g1 = jsonGetter2(JSONnew, "guests").toString();
								String g2 = jsonGetter2(JSONexisting, "guests").toString();
								if ((g1.equalsIgnoreCase("")) || (g2.equalsIgnoreCase(""))) {
									jsonSetter(JSONtmp, "guests", "");
								} else {
									int fromGuests = Integer.parseInt(g1);
									int toGuests = Integer.parseInt(g2);
									int newGuests = fromGuests + toGuests;
									jsonSetter(JSONtmp, "guests", Integer.toString(newGuests));
								}

								// make it the new active table
								currentTableID = ToTableID;

								// save the JSON for the new table
								saveJsonTable(ToTableID, JSONtmp);
								setJSONOrderList(ToTableID);

								// Save the old empty table
								JSONtmp = getNewJSONOrder(FromTableID);
								saveJsonTable(FromTableID, JSONtmp);
							} catch (JSONException e) {
								log("JSON MoveOrder Exception=" + e);
							}
							mHandler.post(mUpdateResults);
							mHandler.post(mUpdatePrinters);
							customDialog.dismiss();
						}
					}
				});
			}
			return (true);
		}
		if (item.getItemId() == 18) {
			// Clear table
			if (anyTabsOpen()) {
				if (Global.UserLevel > 0) {
					LayoutInflater factory = LayoutInflater.from(this);
					final View textEntryView = factory.inflate(R.layout.clear_table, null);
					final CustomDialog customDialog = new CustomDialog(this);
					customDialog.setContentView(textEntryView);
					customDialog.show();
					customDialog.setCancelable(true);
					customDialog.setCanceledOnTouchOutside(true);

					final Button tablButton = (Button) customDialog.findViewById(R.id.spinnerNameClear);
					tablButton.setText(getString(R.string.table_select_clear_name));
					tablButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							showTableClearDialog(tablButton);
						}
					});
					Button clearButton = (Button) customDialog.findViewById(R.id.butClear);
					clearButton.setText(getString(R.string.table_clear_but));
					clearButton.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
								// do the move here
								if ((FromTableID >= 0) && (FromTableID < MaxTABLES)) {
									// clear the selected table
									JSONArray JSONtmp = getNewJSONOrder(FromTableID);
									JSONOrderStr[FromTableID] = JSONtmp.toString(1);
									setJSONOrderList(FromTableID);

									// re-save it
									saveJsonTable(FromTableID, JSONtmp);
									mHandler.post(mUpdateResults);
									clearPrinterStatus(FromTableID);

									// Log it
									String sendserver = "9," + Utils.GetDateTime() + "," + Global.ServerName + " Table=" + FromTableName;
									activityLogger(sendserver);
									// Done
									customDialog.dismiss();
								}
							} catch (Exception e) {
								log("Clear table JSON failed ex=" + e);
							}
						}
					});
				} else {
                    Toast.makeText(POSActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
				}
			}
			return (true);
		}
		if (item.getItemId() == 19) {
			// Reload a table from Local Storage
			if (anyTabsAvailable()) {
				// Give them a layout so they can choose the file to reload
				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.reload_dialog, null);

				final CustomDialog customDialog = new CustomDialog(this);
				customDialog.setContentView(textEntryView);
				customDialog.show();
				customDialog.setCancelable(true);
				customDialog.setCanceledOnTouchOutside(true);

				ToTableID = -1;

				final Button toButton = (Button) customDialog.findViewById(R.id.spinnerNew);
				toButton.setText(getString(R.string.table_select_to_name));
				toButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						showTableToDialog(toButton);
					}
				});

				final Button reloadDate = (Button) customDialog.findViewById(R.id.reloadDate);
				reloadDate.setText(getString(R.string.reload_date));

				// Let them choose a date
				final Calendar myCalendar = Calendar.getInstance();
				final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						myCalendar.set(Calendar.YEAR, year);
						myCalendar.set(Calendar.MONTH, monthOfYear);
						myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

						String myFormat = "yyMMdd"; //In which you need put here
						SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
						reloadDate.setText(sdf.format(myCalendar.getTime()));

						// update the tickets shown
						FileFilter filter = new FileFilter() {
							@Override
							public boolean accept(File arg0) {
        						/* get current date
        						Date dt = new Date();
        						Integer month = dt.getMonth() + 1;
        						String formatmon = String.format("%02d", month);
        						Integer day = dt.getDate();
        						String formatdy = String.format("%02d", day);
        						Integer yr = dt.getYear() - 100;
        						String formatyr = String.format("%02d", yr);
        						*/
								final Button reloadDate = (Button) customDialog.findViewById(R.id.reloadDate);
								String dat = reloadDate.getText().toString();
								//return arg0.getName().startsWith(formatyr+formatmon+formatdy);
								return arg0.getName().startsWith(dat);
								//return arg0.getName().endsWith(".jpg") || arg0.getName().endsWith(".bmp")|| arg0.getName().endsWith(".png") || arg0.isDirectory();
							}
						};
						// Allow them to reload either Saved or Unsent orders for Today
						File[] filesO = ordersDir.listFiles(filter);
						File[] filesR = retryDir.listFiles(filter);
						reloadItemList.clear();
						for (File f : filesO) reloadItemList.add(f.getName());
						for (File f : filesR) reloadItemList.add(f.getName());

						gridReload = (GridView) customDialog.findViewById(R.id.reloadItemGrid);
                        reloadAdapter = new TicketAdapter(POSActivity.this, R.layout.ticket_item, reloadItemList);
						gridReload.setAdapter(reloadAdapter);

						gridReload.setOnItemClickListener(new OnItemClickListener() {
							public void onItemClick(AdapterView parent, View v, final int position, long id) {
								final int pos = position;
								String fname = reloadItemList.get(position);
								if ((ToTableID >= 0) && (ToTableID < MaxTABLES)) {
									try {
										// Find out if this is a Saved or Unsent order
										File readFileO = new File(ordersDir, fname);
										File readFileR = new File(retryDir, fname);

										String order = Utils.ReadLocalFile(readFileO);
										if (order.length() == 0)
											// Must be UNSENT
											order = Utils.ReadLocalFile(readFileR);
										JSONArray JSONtmp = new JSONArray(order);

										//Global.OrderId = jsonGetter2(JSONtmp,"orderid").toString();
										//currentTableID = (Integer) jsonGetter2(JSONtmp,"currenttableid");
										currentTableID = ToTableID;
										Global.TableTime = jsonGetter2(JSONtmp, "tabletime").toString();

										// grab additional order details which will get saved below in getNewJSONOrder
										Global.Guests = jsonGetter2(JSONtmp, "guests").toString();
										Global.SendTime = jsonGetter2(JSONtmp, "sendtime").toString();
										Global.IncomingServerName = jsonGetter2(JSONtmp, "waiter").toString();
										incomingTableName = Global.tablenames.get(currentTableID);
										Global.TableName = incomingTableName;
										Global.OrderId = Utils.GetDateTime() + "-" + Global.TableName;
										Global.TableID = Integer.valueOf(currentTableID);

										// update the table in the new to table JSON,
										// update the orderid so it will get saved on the server based on the new filename
										jsonSetter(JSONtmp, "tablename", Global.tablenames.get(ToTableID));
										jsonSetter(JSONtmp, "currenttableid", ToTableID);
										jsonSetter(JSONtmp, "orderid", Global.OrderId);

										// Reloaded order so update the sendtype so resend=2
										//JSONObject obj=new JSONObject();
										//obj.put("sendtype","2");
										//JSONtmp.put(jsonGetter3(JSONtmp,"sendtype"), obj);
										jsonSetter(JSONtmp, "sendtype", "2");

										// re-save it
										saveJsonTable(currentTableID, JSONtmp);

										customDialog.dismiss();

										mHandler.post(mUpdateResults);
									} catch (Exception e) {
										log("Exception listReload=" + e);
									}
								}
							}
						});
					}
				};
				reloadDate.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
                        new DatePickerDialog(POSActivity.this, date, myCalendar
								.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
								myCalendar.get(Calendar.DAY_OF_MONTH)).show();
					}
				});
			}
			return (true);
		}

		if (item.getItemId() == 20) {
			if (anyTabsOpen()) {
                AlertDialog alertDialog = new AlertDialog.Builder(POSActivity.this).create();
				alertDialog.setTitle(getString(R.string.tab3_opentabs_title));
				alertDialog.setMessage(getString(R.string.tab3_opentabs_text));
				alertDialog.setButton(getString(R.string.tab3_continue), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						registerLogout();
					}
				});
				alertDialog.setButton2(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				alertDialog.show();
			} else {
				registerLogout();
			}
			return (true);
		}
		if (item.getItemId() == 0) {
			// Nothing to do here at the moment ...
			invalidateOptionsMenu();
		}
		if (item.getItemId() == 1) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.printers_dialog, null);

			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			final CheckBox cb1 = (CheckBox) customDialog.findViewById(R.id.cb1);
			final CheckBox cb2 = (CheckBox) customDialog.findViewById(R.id.cb2);
			final CheckBox cb3 = (CheckBox) customDialog.findViewById(R.id.cb3);
			if (Global.POS1Enable) cb1.setChecked(true);
			else cb1.setChecked(false);
			if (Global.POS2Enable) cb2.setChecked(true);
			else cb2.setChecked(false);
			if (Global.POS3Enable) cb3.setChecked(true);
			else cb3.setChecked(false);

			cb1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (cb1.isChecked()) {
						Global.POS1Enable = true;
						prefEdit.putBoolean("pos1enable", true);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 1";
						activityLogger(sendserver);
					} else {
						Global.POS1Enable = false;
						prefEdit.putBoolean("pos1enable", false);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 1";
						activityLogger(sendserver);
					}
					if (currentTableID != -1) printStatus[currentTableID][0] = 0;
					mHandler.post(mUpdatePrinters);
					invalidateOptionsMenu();
				}
			});
			cb2.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (cb2.isChecked()) {
						Global.POS2Enable = true;
						prefEdit.putBoolean("pos2enable", true);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 2";
						activityLogger(sendserver);
					} else {
						Global.POS2Enable = false;
						prefEdit.putBoolean("pos2enable", false);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 2";
						activityLogger(sendserver);
					}
					if (currentTableID != -1) printStatus[currentTableID][1] = 0;
					mHandler.post(mUpdatePrinters);
					invalidateOptionsMenu();
				}
			});
			cb3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (cb3.isChecked()) {
						Global.POS3Enable = true;
						prefEdit.putBoolean("pos3enable", true);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 3";
						activityLogger(sendserver);
					} else {
						Global.POS3Enable = false;
						prefEdit.putBoolean("pos3enable", false);
						prefEdit.commit();
						String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 3";
						activityLogger(sendserver);
					}
					if (currentTableID != -1) printStatus[currentTableID][2] = 0;
					mHandler.post(mUpdatePrinters);
					invalidateOptionsMenu();
				}
			});
			return (true);
		}
		if (item.getItemId() == 2) {
			// Handle a click on the Server, allow change of User in case a manager or admin password is required for an operation
            AlertDialog.Builder builder = new AlertDialog.Builder(POSActivity.this);
			builder.setTitle(getString(R.string.login_person_name));
			builder.setCancelable(true);
			String[] tmpArr = new String[Global.userList.size()];
			try {
				for (int i = 0; i < Global.userList.size(); i++) {
					JSONArray tmp = new JSONArray(Global.userList.get(i));
					tmpArr[i] = jsonGetter2(tmp, "name").toString();
				}
				DialogInterface.OnClickListener picDialogListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogint, int which) {
						try {
							// turn off keyboard
							//InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
							//if (imm != null) imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);

							Global.CheckedPicID = which;
							JSONArray tmp = new JSONArray(Global.userList.get(which));
							String nam = jsonGetter2(tmp, "name").toString();
							String pin = jsonGetter2(tmp, "pin").toString();
							//int level = (Integer) jsonGetter2(tmp,"userlevel");
							// Ask for password
							getPassword(tmp.toString(), pin, 2, 0, dialog);
						} catch (JSONException e) {
							log("Change User Json1 e=" + e);
						}
					}
				};
				builder.setSingleChoiceItems(tmpArr, Global.CheckedPicID, picDialogListener);
				dialog = builder.create();
				dialog.show();
			} catch (JSONException e) {
				log("Change User Json2 e=" + e);
			}
			return (true);
		}
		if (item.getItemId() == 10) {
			// Switch lang to English
			Configuration config = getBaseContext().getResources().getConfiguration();
			locale = new Locale("en");
			Locale.setDefault(locale);
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
			Global.EnglishLang = true;
			GridView gridview = (GridView) findViewById(R.id.gridView1);
			gridview.setAdapter(new GridAdapter(this, R.layout.array_list_item, dishArrayList));
			listOrder = (ListView) findViewById(R.id.listOrder);
            listOrder.setAdapter(new OrderAdapter(POSActivity.this, R.layout.list_item, JSONOrderList));
			invalidateOptionsMenu();
			setupButtons();
			return (true);
		}
		if (item.getItemId() == 11) {
			Configuration config = getBaseContext().getResources().getConfiguration();
			locale = new Locale("zh");
			Locale.setDefault(locale);
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
			Global.EnglishLang = false;
			GridView gridview = (GridView) findViewById(R.id.gridView1);
			gridview.setAdapter(new GridAdapter(this, R.layout.array_list_item, dishArrayList));
			listOrder = (ListView) findViewById(R.id.listOrder);
            listOrder.setAdapter(new OrderAdapter(POSActivity.this, R.layout.list_item, JSONOrderList));
			invalidateOptionsMenu();
			setupButtons();
			return (true);
		}
		if (item.getItemId() == 5) {
			// start with the TABLES View (menu/sent/tables/specials)
			vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
			if (vfMenuTable.getDisplayedChild() == 0) {
				vfMenuTable.setDisplayedChild(1);
			} else {
				if (currentTableID != -1) {
					vfMenuTable.setDisplayedChild(0);
				} else {
                    Toast.makeText(POSActivity.this, getString(R.string.msg_select_table), Toast.LENGTH_LONG).show();
				}
			}
			invalidateOptionsMenu();
			updateTableButtons();
			return (true);
		}
		if (item.getItemId() == 4) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.register, null);

			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			Global.LogoutTime = Utils.GetDateTime();

			TextView tv = (TextView) customDialog.findViewById(R.id.regSummary);
			tv.setText(getString(R.string.register_reg_sum));
			//tv.setTextColor(Color.parseColor(colors[1]));

			tv = (TextView) customDialog.findViewById(R.id.loginTimeHead);
			tv.setText(getString(R.string.register_login_time));

			tv = (TextView) customDialog.findViewById(R.id.loginTime);
			tv.setText(Global.LoginTime);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.logoutTimeHead);
			tv.setText(getString(R.string.register_cur_time));

			tv = (TextView) customDialog.findViewById(R.id.logoutTime);
			tv.setText(Global.LogoutTime);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textServer);
			tv.setText(getString(R.string.register_server_name));

			tv = (TextView) customDialog.findViewById(R.id.rserver);
			tv.setText(Global.ServerName);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textFloat);
			tv.setText(getString(R.string.register_reg_float));

			tv = (TextView) customDialog.findViewById(R.id.rfloat);
			tv.setText("RMB " + Global.RegisterFloat);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textCash);
			tv.setText(getString(R.string.register_reg_cash));

			tv = (TextView) customDialog.findViewById(R.id.rcash);
			tv.setText("RMB " + Global.RegisterCash);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textCreditRegister);
			tv.setText(getString(R.string.register_reg_credit));

			tv = (TextView) customDialog.findViewById(R.id.rcredit);
			tv.setText("RMB " + Global.RegisterCredit);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textOther);
			tv.setText(getString(R.string.register_reg_other));

			tv = (TextView) customDialog.findViewById(R.id.rother);
			tv.setText("RMB " + Global.RegisterOther);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textTotal);
			tv.setText(getString(R.string.register_reg_cashtotal));

			tv = (TextView) customDialog.findViewById(R.id.rtotal);
			tv.setText("RMB " + Global.RegisterCashTotal);
			tv.setTextColor(Color.parseColor(textColors[3]));

			tv = (TextView) customDialog.findViewById(R.id.textSalesTotal);
			tv.setText(getString(R.string.register_reg_salestotal));

			tv = (TextView) customDialog.findViewById(R.id.rsalestotal);
			tv.setText("RMB " + Global.RegisterSalesTotal);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textPayoutTotal);
			tv.setText(getString(R.string.register_reg_payouttotal));

			tv = (TextView) customDialog.findViewById(R.id.rpayouttotal);
			tv.setText("RMB " + Global.RegisterPayout);
			tv.setTextColor(Color.parseColor(textColors[0]));

			tv = (TextView) customDialog.findViewById(R.id.textSent);
			tv.setText(getString(R.string.register_orders_sent));

			tv = (TextView) customDialog.findViewById(R.id.totalorders);
			tv.setText(Global.OrdersSent + " ");
			tv.setTextColor(Color.parseColor(textColors[0]));

			// check for open
			Button butOpn = (Button) customDialog.findViewById(R.id.butOpen);
			butOpn.setText(getString(R.string.register_open_drawer));
			butOpn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// save record on server
					String sendserver = "0," + Utils.GetDateTime() + "," + Global.ServerName;
					activityLogger(sendserver);
					openDrawer();
				}
			});
			// check for print
			Button butPrnt = (Button) customDialog.findViewById(R.id.butPrint);
			butPrnt.setText(getString(R.string.register_print_sum));
			butPrnt.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// build the string
					String formatTicket = addPad("Login: " + Global.LoginTime) +
							addPad("Current: " + Utils.GetDateTime()) +
							addPad("Server: " + Global.ServerName) +
							addPad("Float         RMB " + Global.RegisterFloat) +
							addPad("Cash Sales    RMB " + Global.RegisterCash) +
							addPad("Credit Sales  RMB " + Global.RegisterCredit) +
							addPad("Other Sales   RMB " + Global.RegisterOther) +
							addPad("Sales Total   RMB " + Global.RegisterSalesTotal) +
							addPad("Payout Total  RMB " + Global.RegisterPayout) +
							addPad("Physical Cash RMB " + Global.RegisterCashTotal) +
							addPad("Orders Sent: " + Global.OrdersSent) +
							addPad("Last Ticket Number: " + (Global.TicketNum - 1));
					if (Global.POS1Enable) {
						try {
							err = POS1Dev.openDevice();
							// ready to print
							err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);
							err = POS1Dev.sendCommand("ESC d 4");
							err = POS1Dev.printString("REGISTER SUMMARY", FONT.FONT_A, true, false, true, true);
							err = POS1Dev.sendCommand("ESC d 2");
							err = POS1Dev.printString(formatTicket, FONT.FONT_A, true, false, false, false);
							err = POS1Dev.sendCommand("ESC d 4");
							err = POS1Dev.cutPaper();
							// Close the connection so others can use it
							err = POS1Dev.closeDevice();
						} catch (Exception ex) {
							String errorString = "";
							if (err != null) errorString = EpsonCom.getErrorText(err);
                            messageBox(POSActivity.this,
									getString(R.string.tab3_pos_err_1) + errorString +
											getString(R.string.tab3_pos_err_2), "Connection problem 1");
						}
					}
				}
			});

			Button butPay = (Button) customDialog.findViewById(R.id.butPayout);
			butPay.setText(getString(R.string.register_payout));
			butPay.setTextColor(Color.parseColor("#eeeeee"));
			butPay.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
                    final Dialog dialogPO = new Dialog(POSActivity.this);
					dialogPO.setContentView(R.layout.payout);
					dialogPO.setCancelable(true);
					dialogPO.setCanceledOnTouchOutside(true);
					String tit = getString(R.string.register_payout_desc);
					dialogPO.setTitle(tit);

					TextView POtext1 = (TextView) dialogPO.findViewById(R.id.POtext1);
					POtext1.setText(getString(R.string.register_pay_amount));
					POtext1.setTextColor(Color.parseColor("#EEEEEE"));

					TextView POtext2 = (TextView) dialogPO.findViewById(R.id.POtext2);
					POtext2.setText(getString(R.string.register_pay_desc));
					POtext2.setTextColor(Color.parseColor("#EEEEEE"));
					// edit text box is next
					Button POsave = (Button) dialogPO.findViewById(R.id.POadd);
					POsave.setText(getString(R.string.login_save));
					POsave.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							EditText et1 = (EditText) dialogPO.findViewById(R.id.POedit1);
							String po1 = et1.getText().toString();
							po1 = po1.replaceAll("[^\\p{N}]", "");
							EditText et2 = (EditText) dialogPO.findViewById(R.id.POedit2);
							String po2 = et2.getText().toString();
							po2 = po2.replaceAll("[^\\p{L}\\p{N}-\\s]", "");
							if ((po1.length() > 0) && (po2.length() > 0)) {
								dialogPO.dismiss();
								customDialog.dismiss();
								Global.RegisterPayout = Global.RegisterPayout + Integer.valueOf(po1);
								Global.RegisterCashTotal = Global.RegisterCashTotal - Integer.valueOf(po1);
                                //Toast.makeText(POSActivity.this, "DO PAYOUT", Toast.LENGTH_SHORT).show();
								// Save to a file
								String fname = "payouts.txt";
								final String sendServer = Utils.GetDateTime() + "," +
										Global.ServerName + "," +
										po1 + "," +
										po2;
								writeOutFile(logsDir, fname, sendServer);

								// send to server
								new Thread(new Runnable() {
									public void run() {
										if ((haveNetworkConnection()) & (Global.TicketToCloud)) {
											String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveAdHocURL;
											Utils.SendMultipartAdhoc(postURL,
													sendServer,
													Global.SMID);
										} else {
											// add it to the retry directory
											log("retry-payout-" + Utils.GetDateTime());
										}
									}
								}).start();

								// print a ticket
								if (Global.POS1Enable) {
									try {
										err = POS1Dev.openDevice();
										// ready to print
										err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);
										err = POS1Dev.sendCommand("ESC d 4");
										err = POS1Dev.printString("REGISTER PAYOUT", FONT.FONT_A, true, false, true, true);
										err = POS1Dev.sendCommand("ESC d 2");
										err = POS1Dev.printString("Time=" + Utils.GetDateTime(), FONT.FONT_A, true, false, false, false);
										err = POS1Dev.sendCommand("ESC d 1");
										err = POS1Dev.printString("Waiter=" + Global.ServerName, FONT.FONT_A, true, false, false, false);
										err = POS1Dev.sendCommand("ESC d 1");
										err = POS1Dev.printString("Amount=" + po1, FONT.FONT_A, true, false, false, false);
										err = POS1Dev.sendCommand("ESC d 1");
										err = POS1Dev.printString("Description=" + po2, FONT.FONT_A, true, false, false, false);
										err = POS1Dev.sendCommand("ESC d 5");
										err = POS1Dev.cutPaper();
										// Close the connection so others can use it
										err = POS1Dev.closeDevice();
									} catch (Exception ex) {
										String errorString = "";
										if (err != null) errorString = EpsonCom.getErrorText(err);
                                        messageBox(POSActivity.this,
												getString(R.string.tab3_pos_err_1) + errorString +
														getString(R.string.tab3_pos_err_2), "Connection problem 1");
									}
									// open the box
									// save record on server
									String sendserver = "4," + Utils.GetDateTime() + "," + Global.ServerName;
									activityLogger(sendserver);
									openDrawer();
								}
							}
						}
					});
					dialogPO.show();
				}
			});
			return (true);
		}
		if (item.getItemId() == 6) {
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.ticket_dialog, null);

			final CustomDialog customDialog = new CustomDialog(this);
			customDialog.setContentView(textEntryView);
			customDialog.show();
			customDialog.setCancelable(true);
			customDialog.setCanceledOnTouchOutside(true);

			TextView tv = (TextView) customDialog.findViewById(R.id.TicketNumber);
			tv.setText(" " + Global.TicketNum + " ");

			Button butAddNum = (Button) customDialog.findViewById(R.id.butAddTicket);
			butAddNum.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Global.TicketNum = Global.TicketNum + 1;
					TextView tv = (TextView) customDialog.findViewById(R.id.TicketNumber);
					tv.setText(" " + Global.TicketNum + " ");
					invalidateOptionsMenu();
				}
			});

			Button butResetNum = (Button) customDialog.findViewById(R.id.butResetTicket);
			butResetNum.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Global.TicketNum = 1;
					TextView tv = (TextView) customDialog.findViewById(R.id.TicketNumber);
					tv.setText(" " + Global.TicketNum + " ");
					invalidateOptionsMenu();
				}
			});
			return (true);
		}
		// end of all the menu items
		return (super.onOptionsItemSelected(item));
	}

	private void getPassword(String js, String expectedpw, int returnID, int dishPosition, Dialog dialogNames) {
		final Dialog dialogPW;
        dialogPW = new Dialog(POSActivity.this);
		dialogPW.setContentView(R.layout.password);
		dialogPW.setCancelable(true);
		dialogPW.setCanceledOnTouchOutside(true);

		etPassword1 = (EditText) dialogPW.findViewById(R.id.etPassword1);
		etPassword1.setRawInputType(Configuration.KEYBOARD_12KEY);
		etPassword2 = (EditText) dialogPW.findViewById(R.id.etPassword2);
		etPassword2.setRawInputType(Configuration.KEYBOARD_12KEY);
		etPassword3 = (EditText) dialogPW.findViewById(R.id.etPassword3);
		etPassword3.setRawInputType(Configuration.KEYBOARD_12KEY);
		etPassword4 = (EditText) dialogPW.findViewById(R.id.etPassword4);
		etPassword4.setRawInputType(Configuration.KEYBOARD_12KEY);

		// set the starting selected to et1
		etPassword1.requestFocus();

		// turn on keyboard
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		//InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		//if (imm != null) imm.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);

		// setup the text watchers
		watcher1 = new GenericTextWatcher(etPassword1, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
		etPassword1.addTextChangedListener(watcher1);
		watcher2 = new GenericTextWatcher(etPassword2, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
		etPassword2.addTextChangedListener(watcher2);
		watcher3 = new GenericTextWatcher(etPassword3, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
		etPassword3.addTextChangedListener(watcher3);
		watcher4 = new GenericTextWatcher(etPassword4, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
		etPassword4.addTextChangedListener(watcher4);

		// setup the title
		String tit = getString(R.string.msg_password);
		SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
		StyleSpan span = new StyleSpan(Typeface.NORMAL);
		ScaleXSpan span1 = new ScaleXSpan(2);
		ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		dialogPW.setTitle(ssBuilser);
		dialogPW.show();
	}

	private class GenericTextWatcher implements TextWatcher {
		private View view;
		private String js;
		private String expectedpw;
		private int returnID;
		private Dialog dialogPW;
		private Dialog dialogNames;
		private int dishPosition;

		private GenericTextWatcher(View view, String js, String expectedpw, int returnID, Dialog dialogPW, Dialog dialogNames, int position) {
			this.view = view;
			this.js = js;
			this.expectedpw = expectedpw;
			this.returnID = returnID;
			this.dialogPW = dialogPW;
			this.dialogNames = dialogNames;
			this.dishPosition = dishPosition;
		}

		public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
		}

		public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
		}

		public void afterTextChanged(Editable editable) {
			switch (view.getId()) {
				case R.id.etPassword1:
					etPassword2.requestFocus();
					break;
				case R.id.etPassword2:
					etPassword3.requestFocus();
					break;
				case R.id.etPassword3:
					etPassword4.requestFocus();
					break;
				case R.id.etPassword4:
					dialogPW.dismiss();
					dialogNames.dismiss();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						// In the order app, only the the first line works. Here, both seem to work
						// stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
						imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
						imm.hideSoftInputFromWindow(etPassword4.getWindowToken(), 0);
					}
					if (pwMatch(expectedpw)) {
						// turn off keyboard
						//InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
						//if (imm != null) imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
						switch (returnID) {
							case 2:
								try {
									// Insert handler for password success
									// Case 2 for Change User
									JSONArray ja = new JSONArray(js);
									int level = (Integer) jsonGetter2(ja, "userlevel");
									String name = jsonGetter2(ja, "name").toString();

									Global.CheckedPicName = name;
									Global.LoginTime = Utils.GetDateTime();
									Global.ServerName = Global.CheckedPicName;
									Global.UserLevel = level;
									// save new user logged in
									String sendserver = "3," + Utils.GetDateTime() + "," + Global.ServerName;
									activityLogger(sendserver);
									mHandler.post(mUpdateResults);
									break;
								} catch (Exception e) {
									log("Discount getPassword Exception e=" + e);
								}
						}
					}
					break;
			}
		}
	}

	private boolean pwMatch(String pw2) {
		Boolean result = false;
		String pw = etPassword1.getText().toString();
		pw = pw + etPassword2.getText().toString();
		pw = pw + etPassword3.getText().toString();
		pw = pw + etPassword4.getText().toString();
		if (pw.equals(pw2)) result = true;
		return (result);
	}

	private void registerLogout() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.logout, null);
		final CustomDialog customDialog = new CustomDialog(this);
		customDialog.setContentView(textEntryView);
		customDialog.setCancelable(true);
		customDialog.setCanceledOnTouchOutside(true);
		customDialog.show();
		// check for confirm
		Button butLogout = (Button) customDialog.findViewById(R.id.butLogout);
		butLogout.setText(getString(R.string.register_logout));
		butLogout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Global.LogoutTime = Utils.GetDateTime();
				String logOut = "Login:" + Global.LoginTime + " " +
						"Logout: " + Global.LogoutTime + " " +
						"Server: " + Global.ServerName + " " +
						"Float RMB:" + Global.RegisterFloat + " " +
						"Cash Sales RMB:" + Global.RegisterCash + " " +
						"Credit Sales RMB:" + Global.RegisterCredit + " " +
						"Other Sales RMB:" + Global.RegisterOther + " " +
						"Sales Total RMB:" + Global.RegisterSalesTotal + " " +
						"Payout Total RMB:" + Global.RegisterPayout + " " +
						"Physical Cash RMB:" + Global.RegisterCashTotal + " " +
						"Orders Sent:" + Global.OrdersSent + " " +
						"Last Ticket Number:" + (Global.TicketNum - 1);
				String sendserver = "2," + Utils.GetDateTime() + "," + logOut;
				activityLogger(sendserver);

				// build the string print the ticket
				String formatTicket = addPad("Login: " + Global.LoginTime) +
						addPad("Current: " + Global.LogoutTime) +
						addPad("Server: " + Global.ServerName) +
						addPad("Float         RMB " + Global.RegisterFloat) +
						addPad("Cash Sales    RMB " + Global.RegisterCash) +
						addPad("Credit Sales  RMB " + Global.RegisterCredit) +
						addPad("Other Sales   RMB " + Global.RegisterOther) +
						addPad("Sales Total   RMB " + Global.RegisterSalesTotal) +
						addPad("Payout Total  RMB " + Global.RegisterPayout) +
						addPad("Physical Cash RMB " + Global.RegisterCashTotal) +
						addPad("Orders Sent: " + Global.OrdersSent);
				CheckBox cb1 = (CheckBox) customDialog.findViewById(R.id.printSummaryCB);
				if ((Global.POS1Enable) && (cb1.isChecked())) {
					try {
						err = POS1Dev.openDevice();
						// ready to print
						err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);
						err = POS1Dev.sendCommand("ESC d 4");
						err = POS1Dev.printString("REGISTER SUMMARY", FONT.FONT_A, true, false, true, true);
						err = POS1Dev.sendCommand("ESC d 2");
						err = POS1Dev.printString(formatTicket, FONT.FONT_A, true, false, false, false);
						err = POS1Dev.sendCommand("ESC d 4");
						err = POS1Dev.cutPaper();
						// Close the connection so others can use it
						err = POS1Dev.closeDevice();
					} catch (Exception ex) {
						String errorString = "";
						if (err != null) errorString = EpsonCom.getErrorText(err);
                        messageBox(POSActivity.this,
								getString(R.string.tab3_pos_err_1) + errorString +
										getString(R.string.tab3_pos_err_2), "Connection problem 1");
					}
				}
				// Stop the service
				log("Register Logging Out...");
				//if (!Global.SlaveMode) {
				//	SmartMenuService.actionStop(getApplicationContext());
				//}
				finish();
				Global.LoggedIn = false;
				Intent kintent = new Intent(getApplicationContext(), LoginActivity.class);
				kintent.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
				startActivity(kintent);
			}
		});
	}

	private void activityLogger(final String sendServer) {
		// Log activities:
		//  0 - Open register drawer
		//  1 - New Waiter
		//  2 - Register logged out
		//  3 - User logged in
		//  4 - Payout entered
		//  5 - System Exit (not used?)
		//  6 - Enable/Disable Printers
		//  7 - Settings Saves
		//  8 - Settings Reloaded
		//  9 - Clear Table
		// 10 - Discount entered

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
				if ((pingIP()) & (Global.TicketToCloud)) {
					String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveActivityURL;
					Utils.SendMultipartAdhoc(postURL,
							sendServer,
							Global.SMID);
				} else {
                    log("POSActivity: activityLogger failed");
				}
			}
		}).start();
	}

	private String getVersionName() {
		String version = "";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			version = "Package name not found";
		}
		return version;
	}

	private int getVersionCode() {
		int version = -1;
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pi.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
		}
		return version;
	}

	// below is the over ride that will disable the back button
	public void onBackPressed() {
	}

	private int getOrderTotalRMB(int table) {
		// Grab the ordertotal from the JSON
		int TOTALRMB = 0;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[table]);
			if (JSONOrderAry != null) {
				String ordertotal = "0";
				ordertotal = jsonGetter2(JSONOrderAry, "ordertotal").toString();
				TOTALRMB = Integer.parseInt(ordertotal);
			}
		} catch (JSONException e) {
			log("JSON getOrderTotalRMB Exception=" + e);
		}
		return TOTALRMB;
	}

	private int getOrderTotalJSON(JSONArray json) {
		// Grab the ordertotal from the JSON
		int TOTALRMB = 0;
		try {
			if (json != null) {
				String ordertotal = "0";
				ordertotal = jsonGetter2(json, "ordertotal").toString();
				TOTALRMB = Integer.parseInt(ordertotal);
			}
		} catch (Exception e) {
			log("JSON getOrderTotal2 Exception=" + e);
		}
		return TOTALRMB;
	}

	private int updateOrderTotalRMB(int table) {
		int TOTALRMB = 0;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[table]);
			if (JSONOrderAry != null) {
				JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

				// Loop through each dish and add the Qty Total for the dish to the Order Total
				for (int i = 0; i < JSONdishesAry.length(); i++) {
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// Grab the PriceQty from the dish
					int priceqty = Integer.parseInt(jsonGetter2(jd, "priceQtyTotal").toString());
					// Running total ...
					TOTALRMB = TOTALRMB + priceqty;
				}
				//log("dish cnt=" + JSONdishesAry.length());
				//log("new dish price=" + TOTALRMB);

				// update total price
				JSONObject ary = new JSONObject();
				ary.put("ordertotal", TOTALRMB);
				JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);

				// replace it
				ary = new JSONObject();    // new object to store the new dishes
				ary.put("dishes", JSONdishesAry);
				// Replace the JSON dishes Object in the JSON order
				JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
			}
		} catch (JSONException e) {
			log("JSON updateOrderTotalRMB Exception=" + e);
		}
		return TOTALRMB;
	}

	private int updateOrderTotalJSON(JSONArray JSONOrderAry) {
		// Calculate the ordertotal from the JSON
		int TOTALRMB = 0;
		try {
			if (JSONOrderAry != null) {
				JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

				// Loop through each dish and add the Qty Total for the dish to the Order Total
				for (int i = 0; i < JSONdishesAry.length(); i++) {
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// Grab the PriceQty from the dish
					int priceqty = Integer.parseInt(jsonGetter2(jd, "priceQtyTotal").toString());
					// Running total ...
					TOTALRMB = TOTALRMB + priceqty;
				}
				//log("dish cnt=" + JSONdishesAry.length());
				//log("new dish price=" + TOTALRMB);

				// update total price
				JSONObject ary = new JSONObject();
				ary.put("ordertotal", TOTALRMB);
				JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);

				// replace it
				ary = new JSONObject();    // new object to store the new dishes
				ary.put("dishes", JSONdishesAry);
				// Replace the JSON dishes Object in the JSON order
				JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
			}
		} catch (JSONException e) {
			log("JSON updateOrderTotalRMB Exception=" + e);
		}
		return TOTALRMB;
	}

	// Until we have JSON menu, we need to extract the price from the dish name
	private static int getRMBnumber(String in) {
		int newRMB = 0;
		String foundRMB = "none";
		Pattern p = Pattern.compile("(RMB )(\\d*)");
		Matcher m = p.matcher(in);
		while (m.find()) {
			foundRMB = m.group(2).trim();    // load up the money
			newRMB = newRMB + Integer.valueOf(foundRMB);
		}
		return newRMB;
	}

	// Until we have JSON menu, we need to remove the price from the dish name
	private static String removeRMBnumber(String in) {
		String newName = in;
		Pattern p = Pattern.compile("(.*)(RMB )(\\d*)");
		Matcher m = p.matcher(in);
		while (m.find()) {
			newName = m.group(1).trim();    // load up the dish name before the RMB 99
		}
		return newName;
	}

	// Some string padding functions for the printers
	private String addPad(String str) {
		int addPad = Global.TicketCharWidth - str.length() + 1;
		for (int k = 1; k < addPad; k++) {
			str = str + " ";
		}
		//str = str + "\\r\\n";
		return str;
	}

	private String addPadKitc(String str) {
		int addPad = Global.KitcTicketCharWidth - str.length() + 1;
		//for(int k=1; k<addPad; k++)
		//{
		//	str = str + " ";
		//}
		str = str + "\\r\\n";
		return str;
	}

	private String addDots(String str) {
		int addPad = Global.TicketCharWidth - str.length() - 1;
		str = str + " ";
		for (int k = 1; k < addPad; k++) {
			str = str + ".";
		}
		str = str + " ";
		return str;
	}

	private String addDashLine(String str) {
		String strDash = "";
		for (int k = 1; k <= Global.TicketCharWidth; k++) {
			strDash = strDash + "-";
		}
		str = str + strDash;
		return str;
	}

	private String addBlankLineB4(String str) {
		String strBlanks = "";
		for (int k = 1; k <= Global.TicketCharWidth; k++) {
			strBlanks = strBlanks + " ";
		}
		str = strBlanks + str;
		return str;
	}

	private String addBlankLineAfter(String str) {
		String strBlanks = "";
		for (int k = 1; k <= Global.TicketCharWidth; k++) {
			strBlanks = strBlanks + " ";
		}
		str = str + strBlanks;
		return str;
	}

	private String addBlankLineAfterKitc(String str) {
		//String strBlanks = "";
		//for(int k=1; k<=Global.KitcTicketCharWidth; k++)
		//{
		//	strBlanks = strBlanks + " ";
		//}
		//str = str + strBlanks;
		str = str + "\\r\\n";
		return str;
	}

	BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equalsIgnoreCase(SmartMenuService.TICKET_BUMPED)) {
				invalidateOptionsMenu();
			}
			if (action.equalsIgnoreCase(SmartMenuService.PRINT_STATUS)) {
				Bundle extras = intent.getExtras();
				Integer prid = extras.getInt("printerid");
				prid = prid - 1; // need zero relative printer #
				Integer prstatus = extras.getInt("printerstatus");
				Integer tabid = extras.getInt("tableid");
				//log("onReceive: Print_Status: prid=" + prid + " prstatus=" + prstatus + " tableid=" + tabid);
				// Update the printer status here from the service result IF we are in print RoundTrip mode or it FAILED
				//
				// Update the PR status that gets sent back---
				// 0=blank, no update
				// 1=success
				// 2=failure
				printStatus[tabid][prid] = prstatus;

				if (numberOfDishes(tabid) > 0) {
					// load the order from json
					//JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
					// Mark all dishes as printed if PR1 comes back successful
					if (prid == 0 && prstatus == 1) {
						if (Global.PrintRoundTrip) {
							int size = numberOfDishes(tabid);
							for (int i = 0; i < size; i++) {
								// mark as printed
								setDishPrinted(tabid, i);
							}
							// Clear the flag so other tablets can send orders for this table
							TableSending[tabid] = false;
							// update the jsonstr
							try {
								JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
								// re-save it
								saveJsonTable(tabid, JSONtmp);
							} catch (JSONException e) {
								log("json except opn tab=" + e);
							}
						}
					}
				}

				mHandler.post(mUpdatePrinters);
				mHandler.post(mUpdateResults);
			}
			if (action.equalsIgnoreCase(SmartMenuService.NEW_MSG)) {
				Bundle extra = intent.getExtras();
				String newMsg = extra.getString("msgdata");
                log("POSActivity: MQTT Msg Received= " + newMsg);
				lastIncomingMsgData = newMsg;
				mHandler.post(mMsgArrived);
			}
			if (action.equalsIgnoreCase(SmartMenuService.NEW_ORDER)) {
				Bundle extras = intent.getExtras();
				final String value = extras.getString("orderdata");
                //log("POSActivity: Order Received");
				try {
					JSONArray JSONtmp2 = new JSONArray(value);
					// See if the order came from TO App and just needs to be printed and saved
					int src = (Integer) jsonGetter2(JSONtmp2, "source");
					// orderSource: 0=POS Direct
					//              1=Phone Order
					//              2=Order App
					//				3=Mobile App
					//				4=Internet Order
					//				5=TO Direct
					if ((src == 1) || (src == 5)) {
						// Process Take Out incoming ...
						new Thread(new Runnable() {
							public void run() {
								try {
									final JSONArray JSONtmp3 = new JSONArray(value);
									// set time and ticketnum
									Global.SendTime = Utils.GetTime();
									jsonSetter(JSONtmp3, "sendtime", Global.SendTime);
									jsonSetter(JSONtmp3, "ticketnum", Global.TicketNum);

									// Do the printing via the background service
									SmartMenuService.actionPrintTicket1(getApplicationContext(), JSONtmp3.toString(), false); // not a reprint
									// Consider filters for P2/P3
									int count2 = unprintedDishCountJSON(JSONtmp3.toString(), true, 2);
									int count3 = unprintedDishCountJSON(JSONtmp3.toString(), true, 3);
									if (count2 > 0) {
										SmartMenuService.actionPrintTicket2(getApplicationContext(), JSONtmp3.toString(), false);
									}
									if (count3 > 0) {
										SmartMenuService.actionPrintTicket3(getApplicationContext(), JSONtmp3.toString(), false);
									}

									// Close out the order and bump the counters
									String tmpsaletype = jsonGetter2(JSONtmp3, "saletype").toString();
									int SaleType = Integer.valueOf(tmpsaletype);
									int tot = getOrderTotalJSON(JSONtmp3);
									if (SaleType == 0) {
										Global.RegisterCash = Global.RegisterCash + tot;
									} else if (SaleType == 1) {
										Global.RegisterCredit = Global.RegisterCredit + tot;
									} else {
										Global.RegisterOther = Global.RegisterOther + tot;
									}
									Global.RegisterCashTotal = Global.RegisterCash + Global.RegisterFloat - Global.RegisterPayout;
									Global.RegisterSalesTotal = Global.RegisterCash + Global.RegisterCredit + Global.RegisterOther;
									Global.OrdersSent = Global.OrdersSent + 1;

									// send up the order. The order will be sent up to the server by the Service
									SmartMenuService.actionSave(getApplicationContext(), JSONtmp3.toString(), "0");

									// Bump the ticket number
									Global.TicketNum = Global.TicketNum + 1;
									prefEdit.putInt("ticketnum", Global.TicketNum);

									mHandler.post(mOrderArrived);
									mHandler.post(mUpdateResults);

								} catch (JSONException e) {
									log("json execption thread processing network order=" + e);
								}
							} // thread close
						}).start();
					} else {
						// Process the incoming order for TAB
						Global.OrderId = jsonGetter2(JSONtmp2, "orderid").toString();
						Integer tmpCurTable = (Integer) jsonGetter2(JSONtmp2, "currenttableid");
						Global.TableTime = jsonGetter2(JSONtmp2, "tabletime").toString();

						// grab additional order details which will get saved below in getNewJSONOrder
						Global.Guests = jsonGetter2(JSONtmp2, "guests").toString();
						Global.SendTime = jsonGetter2(JSONtmp2, "sendtime").toString();
						Global.IncomingServerName = jsonGetter2(JSONtmp2, "waiter").toString();
						incomingTableName = Global.tablenames.get(tmpCurTable);
						Global.TableName = incomingTableName;
						Global.TableID = Integer.valueOf(tmpCurTable);

						// If the table already has dishes we need to add the new dishes to the order (incremental order)

						// get number of existing dishes
						JSONArray JSONexisting = new JSONArray(JSONOrderStr[tmpCurTable]);
						JSONObject JSONdishObj = JSONexisting.getJSONObject(jsonGetter3(JSONexisting, "dishes"));
						JSONArray jdatmp = JSONdishObj.getJSONArray("dishes");
						int numdishex = jdatmp.length();
						//log("Number of existing dishes=" + numdishex);

						// get number of new dishes
						JSONdishObj = JSONtmp2.getJSONObject(jsonGetter3(JSONtmp2, "dishes"));
						JSONArray jdanew = JSONdishObj.getJSONArray("dishes");
						int numdishnew = jdanew.length();
						//log("Number of new dishes=" + numdishnew);
						if (numdishex > 0) {
							// update the existing JSON with the new items, get each dish and copy it over
							for (int i = 0; i < numdishnew; i++) {
								JSONArray jd = jdanew.getJSONArray(i);
								//log("existing b4=" + JSONexisting.toString());
								jdatmp.put(jd);    // append this dish to the JSON dishes
							}
							// there are a couple things we need to update now because of the appended dishes
							saveJsonTable(tmpCurTable, JSONexisting);
							JSONObject ary = new JSONObject();
							ary.put("ordertotal", updateOrderTotalRMB(tmpCurTable));
							JSONexisting.put(jsonGetter3(JSONexisting, "ordertotal"), ary);
							ary = new JSONObject();
							ary.put("guests", Global.Guests);
							JSONexisting.put(jsonGetter3(JSONexisting, "guests"), ary);
							ary = new JSONObject();
							ary.put("sendtime", Global.SendTime);
							JSONexisting.put(jsonGetter3(JSONexisting, "sendtime"), ary);
							//log("existing after=" + JSONexisting.toString());
							JSONtmp2 = JSONexisting;
						}
						saveJsonTable(tmpCurTable, JSONtmp2);
						// reset printers
						clearPrinterStatus(tmpCurTable);

						if (autoPrint) {
							jsonSetter(JSONtmp2, "tabstate", 1);
							saveJsonTable(tmpCurTable, JSONtmp2);
							saveTheTab(JSONtmp2.toString());
						}
						mHandler.post(mOrderArrived);
						mHandler.post(mUpdateResults);
					}
				} catch (JSONException e) {
					log("json execption processing network order=" + e);
				}
			}
		}
	};

	private void setupTablesView() {
		// setup table buttons
		tableButtons = new Button[MaxTABLES];

        int wide = (int) (Utils.getWidth(POSActivity.this) / 9.5);
		int high = (int) (wide / 1.7);
		for (int i = 0; i < MaxTABLES; i++) {
			String buttonID = "butt" + (i);
			int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
			tableButtons[i] = ((Button) findViewById(resID));
			String tmp = Global.tablenames.get(i);
			if (tmp.equalsIgnoreCase("blank")) {
				tableButtons[i].setVisibility(View.INVISIBLE);
			} else if (tmp.equalsIgnoreCase("gone")) {
				tableButtons[i].setVisibility(View.INVISIBLE);
			} else {
				tableButtons[i].setText(tmp);
			}
			//buttons[i].setTextColor(Global.ButFontColor);
			tableButtons[i].setMinWidth(wide);
			tableButtons[i].setMaxWidth(wide);
			tableButtons[i].setMinHeight(high);
			tableButtons[i].setMaxHeight(high);
			tableButtons[i].setTextSize(txtSize0);
			tableButtons[i].setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					tableButClick(v);
				}
			});

			try {
				JSONArray JSONtmp = new JSONArray(JSONOrderStr[i]);

				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 0) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[8]));  // no tab
					if (numberOfDishes(i) > 0) {
						tableButtons[i].setTextColor(Color.parseColor(textColors[4]));
						tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_btn_add, 0, 0, 0);
					} else {
						tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
				}
				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 1) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[4])); // tab open
					tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_more, 0, 0, 0);
				}
				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 2) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[4]));    // tab closed
					tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_more, 0, 0, 0);
				}
			} catch (JSONException e) {
				log("JSONOtmp SetupTables Exception=" + e);
			}
		}
		tableButtons[TakeOutTableID].setTextSize((float) (txtSize2));

		// ....... not gonna show the takeout table in the POS app ..............
		tableButtons[TakeOutTableID].setVisibility(View.INVISIBLE);
	}

	private void updateTableButtons() {
		// set the table button images
		for (int i = 0; i < MaxTABLES; i++) {
			String buttonID = "butt" + (i);
			int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
			tableButtons[i] = ((Button) findViewById(resID));

			try {
				JSONArray JSONtmp = new JSONArray(JSONOrderStr[i]);
				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 0) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[8]));  // no tab
					if (numberOfDishes(i) > 0) {
						tableButtons[i].setTextColor(Color.parseColor(textColors[4]));
						tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_btn_add, 0, 0, 0);
					} else {
						tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
				}
				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 1) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[4])); // tab open
					tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_more, 0, 0, 0);
				}
				if ((Integer) jsonGetter2(JSONtmp, "tabstate") == 2) {
					tableButtons[i].setTextColor(Color.parseColor(textColors[4]));    // tab closed
					tableButtons[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_more, 0, 0, 0);
				}
			} catch (JSONException e) {
				log("JSONOtmp updateTableButtons Exception=" + e);
			}

            tableButtons[i].setHeight((int) (Utils.getWindowButtonHeight(POSActivity.this)));
		}

		if (currentTableID != -1) {
			tableButtons[currentTableID].setTextColor(Color.parseColor(textColors[3]));
			clearButton.setVisibility(View.GONE);
			if (JSONOrderList.isEmpty()) {
				saveButton.setVisibility(View.GONE);
				closeButton.setVisibility(View.GONE);
				tvcc.setVisibility(View.GONE);
				p1Button.setVisibility(View.GONE);
				p2Button.setVisibility(View.GONE);
				p3Button.setVisibility(View.GONE);
			} else {
				if (unprintedDishCount(currentTableID, false, 1) > 0) {
					saveButton.setVisibility(View.VISIBLE);
					closeButton.setVisibility(View.VISIBLE);
					//tvcc.setVisibility(View.VISIBLE);
				} else {
					saveButton.setVisibility(View.GONE);
					closeButton.setVisibility(View.VISIBLE);
					//tvcc.setVisibility(View.VISIBLE);
				}
				p1Button.setVisibility(View.VISIBLE);
				p2Button.setVisibility(View.VISIBLE);
				p3Button.setVisibility(View.VISIBLE);
			}
		} else {
			saveButton.setVisibility(View.GONE);
			closeButton.setVisibility(View.GONE);
			tvcc.setVisibility(View.GONE);
			clearButton.setVisibility(View.GONE);
			p1Button.setVisibility(View.GONE);
			p2Button.setVisibility(View.GONE);
			p3Button.setVisibility(View.GONE);
		}
		tvautoprint.setVisibility(View.VISIBLE);
		LinearLayout llprintstatus = (LinearLayout) findViewById(R.id.printHeaderLL);
		llprintstatus.setVisibility(View.VISIBLE);
	}

	private void updatePrinters(int table) {
		if (table != -1) {
			if (printStatus[table][0] == 0) {
				ivpf1.setBackgroundResource(R.drawable.presence_invisible);
			} else if (printStatus[table][0] == 1) {
				ivpf1.setBackgroundResource(R.drawable.presence_online);
			} else if (printStatus[table][0] == 2) {
				ivpf1.setBackgroundResource(R.drawable.presence_busy);
			}

			if (printStatus[table][1] == 0) {
				ivpf2.setBackgroundResource(R.drawable.presence_invisible);
			} else if (printStatus[table][1] == 1) {
				ivpf2.setBackgroundResource(R.drawable.presence_online);
			} else if (printStatus[table][1] == 2) {
				ivpf2.setBackgroundResource(R.drawable.presence_busy);
			}

			if (printStatus[table][2] == 0) {
				ivpf3.setBackgroundResource(R.drawable.presence_invisible);
			} else if (printStatus[table][2] == 1) {
				ivpf3.setBackgroundResource(R.drawable.presence_online);
			} else if (printStatus[table][2] == 2) {
				ivpf3.setBackgroundResource(R.drawable.presence_busy);
			}

			ivpf1.setVisibility(View.VISIBLE);
			ivpf2.setVisibility(View.VISIBLE);
			ivpf3.setVisibility(View.VISIBLE);
		} else {
			ivpf1.setBackgroundResource(R.drawable.presence_invisible);
			ivpf2.setBackgroundResource(R.drawable.presence_invisible);
			ivpf3.setBackgroundResource(R.drawable.presence_invisible);
		}
	}

	private void setupButtons() {
        int widthL = (int) (Utils.getWidth(POSActivity.this) / 15.0);
        int widthM = (int) (Utils.getWidth(POSActivity.this) / 18.0);
        int widthS = (int) (Utils.getWidth(POSActivity.this) / 30.0);
		int widthfull = txtSize0;

		closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setTextSize(widthfull);
		closeButton.setMinWidth(widthL);
		closeButton.setMaxWidth(widthL);
		closeButton.setMinHeight(widthL);
		closeButton.setMaxHeight(widthL);
		closeButton.setText(getString(R.string.tab3_close));

		clearButton = (Button) findViewById(R.id.clearButton);
		clearButton.setTextSize(widthfull);
		clearButton.setMinWidth(widthL);
		clearButton.setMaxWidth(widthL);
		clearButton.setMinHeight(widthL);
		clearButton.setMaxHeight(widthL);
		clearButton.setText(getString(R.string.tab3_clear));

		saveButton = (Button) findViewById(R.id.sendButton);
		saveButton.setTextSize(widthfull);
		saveButton.setMinWidth(widthL);
		saveButton.setMaxWidth(widthL);
		saveButton.setMinHeight(widthL);
		saveButton.setMaxHeight(widthL);
		saveButton.setText(getString(R.string.tab3_save));

		p1Button = (Button) findViewById(R.id.printP1);
		p1Button.setTextSize(widthfull);
		p1Button.setMinWidth(widthL);
		p1Button.setMaxWidth(widthL);
		p1Button.setMinHeight(widthS);
		p1Button.setMaxHeight(widthS);

		p2Button = (Button) findViewById(R.id.printP2);
		p2Button.setTextSize(widthfull);
		p2Button.setMinWidth(widthL);
		p2Button.setMaxWidth(widthL);
		p2Button.setMinHeight(widthS);
		p2Button.setMaxHeight(widthS);

		p3Button = (Button) findViewById(R.id.printP3);
		p3Button.setTextSize(widthfull);
		p3Button.setMinWidth(widthL);
		p3Button.setMaxWidth(widthL);
		p3Button.setMinHeight(widthS);
		p3Button.setMaxHeight(widthS);

		ODButton = (Button) findViewById(R.id.openDrawer);
		ODButton.setTextSize(widthfull);
		ODButton.setMinWidth(widthL);
		ODButton.setMaxWidth(widthL);
		ODButton.setMinHeight(widthS);
		ODButton.setMaxHeight(widthS);
		ODButton.setText(getString(R.string.tab3_open));

		tvcc = (TextView) findViewById(R.id.textCredit);
		tvcc.setTextSize((float) (widthfull / 1.25));
		tvcc.setMinWidth(widthL);
		tvcc.setMaxWidth(widthL);
		tvcc.setMinHeight(widthM);
		tvcc.setMaxHeight(widthM);

		tvautoprint = (TextView) findViewById(R.id.textAutoPrint);
		tvautoprint.setTextSize((float) (widthfull / 1.25));
		tvautoprint.setMinWidth(widthL);
		tvautoprint.setMaxWidth(widthL);
		tvautoprint.setMinHeight(widthM);
		tvautoprint.setMaxHeight(widthM);
		if (autoPrint) {
			tvautoprint.setTextColor(Color.parseColor(textColors[0]));
			tvautoprint.setBackgroundColor(Color.parseColor(textColors[6]));
			tvautoprint.setText(getString(R.string.tab3_auto));
		} else {
			tvautoprint.setTextColor(Color.parseColor(textColors[0]));
			tvautoprint.setBackgroundColor(Color.parseColor(textColors[9]));
			tvautoprint.setText(getString(R.string.tab3_auto));
		}

		ivpf1 = (ImageView) findViewById(R.id.printfail1);
		ivpf2 = (ImageView) findViewById(R.id.printfail2);
		ivpf3 = (ImageView) findViewById(R.id.printfail3);
	}

	private void writeOutFile(File fildir, String fname, String fcontent) {
		File writeFile = new File(fildir, fname);
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
			writer.write(fcontent);
			writer.flush();
			writer.close();
		} catch (Exception e) {
            log("POSActivity: WriteOutFile Exception: Dir=" + fildir + " fname=" + fname);
		}
	}

	// Set up the intial blank JSON representation of the order.
	public JSONArray getInitialJSONOrder(int table) throws JSONException {
		JSONArray orderArr = new JSONArray();

		orderArr.put(createStr("orderid", ""));
		orderArr.put(createInt("source", 0));        // see the orderSource[] for values, type 0 is POS App Direct
		orderArr.put(createStr("storeid", Global.SMID));
		orderArr.put(createStr("customername", ""));
		orderArr.put(createStr("tablename", ""));
		orderArr.put(createStr("waiter", ""));
		orderArr.put(createStr("guests", ""));
		orderArr.put(createStr("saletype", "0"));
		orderArr.put(createStr("date", ""));
		orderArr.put(createStr("tabletime", ""));
		orderArr.put(createStr("sendtime", ""));
		orderArr.put(createStr("servertime", ""));    // set on the server side
		orderArr.put(createStr("sendtype", "1"));    // 1=normal, 2=resent
		orderArr.put(createStr("ticketnum", ""));
		orderArr.put(createInt("ordertotal", 0));
		orderArr.put(createStr("deliverynumber", ""));
		orderArr.put(createStr("deliveryaddress", ""));
		orderArr.put(createStr("deliveryaddress2", ""));
		orderArr.put(createInt("currenttableid", 0));
		orderArr.put(createInt("tabstate", 0));

		// Add the dish to the order ...
		// The dish information will be left blank and updated as the order is built
		orderArr.put(createArrayDishes());

		return orderArr;
	}

	// Set up the new blank JSON representation of the order.
	public JSONArray getNewJSONOrder(int table) throws JSONException {
		JSONArray orderArr = new JSONArray();

		orderArr.put(createStr("orderid", Global.OrderId));
		orderArr.put(createInt("source", 0));        // see the orderSource[] for values, type 0 is POS App Direct
		orderArr.put(createStr("storeid", Global.SMID));
		orderArr.put(createStr("customername", ""));
		orderArr.put(createStr("response", ""));
		orderArr.put(createStr("tablename", Global.TableName));
		orderArr.put(createStr("waiter", Global.IncomingServerName));
		orderArr.put(createStr("guests", Global.Guests));
		orderArr.put(createStr("saletype", "0"));
		orderArr.put(createStr("date", ""));
		orderArr.put(createStr("tabletime", Global.TableTime));
		orderArr.put(createStr("sendtime", Global.SendTime));
		orderArr.put(createStr("servertime", ""));    // set on the server side
		orderArr.put(createStr("sendtype", "1"));    // 1=normal, 2=resent
		//orderArr.put(createStr("ticketnum",Integer.toString(Global.TicketNum)));
		orderArr.put(createStr("ticketnum", ""));
		orderArr.put(createInt("ordertotal", 0));
		orderArr.put(createStr("deliverynumber", ""));
		orderArr.put(createStr("deliveryaddress", ""));
		orderArr.put(createStr("deliveryaddress2", ""));
		orderArr.put(createInt("currenttableid", table));
		orderArr.put(createInt("tabstate", 0));

		// Add the dish to the order ...
		// The dish information will be left blank and updated as the order is built
		orderArr.put(createArrayDishes());

		return orderArr;
	}

	public JSONObject createArrayDishes() throws JSONException {
		JSONArray JSONDishAry = new JSONArray();

		JSONObject ary = new JSONObject();

        /* Each of the dishes looks like this in JSON
        {  "dishes": [ { "dishId":  99 },
                       { "dishName":  aaa },
                       { "dishNameAlt":  aaa },
                       { "categoryName":  aaa },
                       { "categoryNameAlt":  aaa },
                       { "categoryId": 99 },
                       { "priceOptionId": 99 },
                       { "priceOptionName": aaa },
                       { "priceOptionNameAlt": aaa },
                       { "options": [ { "optionId": 99 },
                                      { "optionPrice": 99 },
                                      { "optionNameEn": aaa },
                                      { "optionNameCh": aaa } ]

                                    ,[ ... ]

                                    }
                       { "extras": [ { "extraId": 99 },
                                     { "extraPrice": 99 },
                                     { "extraNameEn": aaa },
                                     { "extraNameCh": aaa } ]

                                   ,[ ... ]

                                   }
                       { "qty":  99 },
                       { "priceUnitBase": 99 },
                       { "priceUnitTotal": 99 },
                       { "priceQtyTotal": 99 },
                       { "specIns": aaa },
                       { "dishPrinted": boolean },
                       [ "counterOnly": boolean }
        */

		ary.put("dishes", JSONDishAry);

		return ary;
	}

	public JSONObject createStr(String nam, String val) throws JSONException {
		JSONObject ary = new JSONObject();
		ary.put(nam, val);
		return ary;
	}

	public JSONObject createInt(String nam, Integer val) throws JSONException {
		JSONObject ary = new JSONObject();
		ary.put(nam, val);
		return ary;
	}

	public JSONObject createBoolean(String nam, Boolean val) throws JSONException {
		JSONObject ary = new JSONObject();
		ary.put(nam, val);
		return ary;
	}

	// Log helper function
	public static void log(String message) {
		log(message, null);
	}

	public static void log(String message, Throwable e) {
		if (mLog != null) {
			try {
				mLog.println(message);
			} catch (IOException ex) {
			}
		}
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

	private Object jsonGetter2(JSONArray json, String key) {
		Object value = null;
		for (int i = 0; i < json.length(); i++) {
			try {
				JSONObject obj = json.getJSONObject(i);
				if (obj.has(key)) {
					value = obj.get(key);
				}
			} catch (JSONException e) {
				log("jsonGetter2 Exception");
			}
		}
		return value;
	}

	private int jsonGetter3(JSONArray json, String key) {
		int v = -1;
		//log("-------------------------------");
		for (int i = 0; i < json.length(); i++) {
			try {
				JSONObject obj = json.getJSONObject(i);
				//log("i=" + i + " obj=" + obj.toString());
				if (obj.has(key)) {
					v = i;
					//log("obj has key=" + key);
				}
			} catch (JSONException e) {
				log("jsonGetter3 Exception=" + e);
			}
		}
		return v;
	}

	private void jsonSetter(JSONArray array, String key, Object replace) {
		for (int i = 0; i < array.length(); i++) {
			try {
				JSONObject obj = array.getJSONObject(i);
				if (obj.has(key)) {
					obj.putOpt(key, replace);
				}
			} catch (JSONException e) {
				log("jsonSetter exception");
			}
		}
	}

	public static JSONArray RemoveJSONArray(JSONArray jarray, int pos) {
		JSONArray Njarray = new JSONArray();
		try {
			for (int i = 0; i < jarray.length(); i++) {
				if (i != pos)
					Njarray.put(jarray.get(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Njarray;
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
			}
		} catch (Exception e) {
			downloadSuccess = false;
			log("Place Order:PingIP failed e=" + e);
		}
		return downloadSuccess;
	}

	private void populateField(Map<String, String> values, TextView view) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : values.entrySet()) {
			String fieldName = entry.getKey();
			String fieldValue = entry.getValue();
			sb.append(fieldName)
					.append(": ")
					.append("<b>").append(fieldValue).append("</b>");
		}
		view.setText(Html.fromHtml(sb.toString()));
	}

	private void updateConnectionStatus(CustomDialog customDialog) {
		// update the wi-fi status
		ImageView img = (ImageView) customDialog.findViewById(R.id.lit0a);
		img.setBackgroundResource(R.drawable.presence_invisible);
		if (checkInternetConnection()) {
			img.setBackgroundResource(R.drawable.presence_online);
		} else {
			img.setBackgroundResource(R.drawable.presence_busy);
		}

		// update the SERVER connection status
		img = (ImageView) customDialog.findViewById(R.id.lit1aServer);
		img.setBackgroundResource(R.drawable.presence_invisible);
		if (Global.CheckAvailability) {
			new ping204(img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		// update the Printer1 status
		if (Global.POS1Enable) {
			img = (ImageView) customDialog.findViewById(R.id.lit2a);
			img.setBackgroundResource(R.drawable.presence_invisible);
			new pingFetch(Global.POS1Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		// update the Printer2 status
		if (Global.POS2Enable) {
			img = (ImageView) customDialog.findViewById(R.id.lit3a);
			img.setBackgroundResource(R.drawable.presence_invisible);
			new pingFetch(Global.POS2Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		// update the Printer3 status
		if (Global.POS3Enable) {
			img = (ImageView) customDialog.findViewById(R.id.lit4a);
			img.setBackgroundResource(R.drawable.presence_invisible);
			new pingFetch(Global.POS3Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	// check for ping connectivity
	private class pingFetch extends AsyncTask<Void, String, Integer> {
		private String ip1;
		private Integer code;
		private InetAddress in2;
		private ImageView img;

		public pingFetch(String ip, ImageView imgv) {
			ip1 = ip;
			in2 = null;
			img = imgv;
			code = 0;
		}

		protected void onPreExecute(Void... params) {
		}

		protected Integer doInBackground(Void... params) {
			try {
				in2 = InetAddress.getByName(ip1);
			} catch (Exception e) {
				e.printStackTrace();
				code = 2;
			}
			try {
				if (in2.isReachable(Global.ConnectTimeout)) {
					code = 1;
				} else {
					code = 2;
				}
			} catch (Exception e) {
				e.printStackTrace();
				code = 2;
			}
			return 1;
		}

		protected void onProgressUpdate(String msg) {
		}

		protected void onPostExecute(Integer result) {
			if (code == 1) {
				img.setBackgroundResource(R.drawable.presence_online);
			}
			if (code == 2) {
				img.setBackgroundResource(R.drawable.presence_busy);
			}
		}
	}

	// Check for connectivity hitting the 204 script and update the UI
	public class ping204 extends AsyncTask<Void, String, Integer> {
		private Boolean code;
		private ImageView img;

		public ping204(ImageView imgv) {
			code = false;
			img = imgv;
		}

		protected void onPreExecute(Void... params) {
		}

		protected Integer doInBackground(Void... params) {
			try {
				String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
				int status = -1;
				code = false;
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
					if (status == 204) code = true;
				} catch (Exception e) {
					code = false;
				}
			} catch (Exception e) {
				code = false;
			}
			return 1;
		}

		protected void onProgressUpdate(String msg) {
		}

		protected void onPostExecute(Integer result) {
			if (code == true) {
				img.setBackgroundResource(R.drawable.presence_online);
			} else {
				img.setBackgroundResource(R.drawable.presence_busy);
			}
		}
	}

	private void createAndRunStatusThread(final Activity act, final CustomDialog cd) {
		m_bStatusThreadStop = false;
		m_statusThread = new Thread(new Runnable() {
			public void run() {
				while (m_bStatusThreadStop == false) {
					try {
						//anything touching the GUI has to run on the Ui thread
						act.runOnUiThread(new Runnable() {
							public void run() {
								updateConnectionStatus(cd);
							}
						});
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						m_bStatusThreadStop = true;
					}
				}
			}
		});
		m_statusThread.start();
	}

	protected void showTableFromDialog(final Button but) {
		// build the array of tables with orders. This is the from-table-list which can be moved
		// see if any of the tables has a tab open
		ArrayList<String> fromName = new ArrayList<String>();
		ArrayList<Integer> fromID = new ArrayList<Integer>();
		fromName.clear();
		fromID.clear();
		for (int i = 0; i < MaxTABLES; i++) {
			//if (!JSONORDERLIST.isEmpty()) {
			if (numberOfDishes(i) > 0) {
				fromName.add(Global.tablenames.get(i));
				fromID.add(i);
			}
		}
		final String[] tmpArr = new String[fromName.size()];
		final int[] tmpArrID = new int[fromName.size()];
		for (int i = 0; i < fromName.size(); i++) {
			tmpArr[i] = fromName.get(i);
			tmpArrID[i] = fromID.get(i);
		}
		DialogInterface.OnClickListener fromDialogListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FromTableID = tmpArrID[which];
				String str = tmpArr[which];
				but.setText(str);
				FromTableName = str;
				dialog.dismiss();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.table_from_title));
		builder.setSingleChoiceItems(tmpArr, -1, fromDialogListener);
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void showTableMergeDialog(final Button but) {
		// build the array of tables with orders. This is the merge-table-list
		// see if any of the tables has a tab open
		ArrayList<String> toName = new ArrayList<String>();
		ArrayList<Integer> toID = new ArrayList<Integer>();
		toName.clear();
		toID.clear();
		for (int i = 0; i < MaxTABLES; i++) {
			//if (!JSONORDERLIST.isEmpty()) {
			if (numberOfDishes(i) > 0) {
				toName.add(Global.tablenames.get(i));
				toID.add(i);
			}
		}
		final String[] tmpArr = new String[toName.size()];
		final int[] tmpArrID = new int[toName.size()];
		for (int i = 0; i < toName.size(); i++) {
			tmpArr[i] = toName.get(i);
			tmpArrID[i] = toID.get(i);
		}
		DialogInterface.OnClickListener fromDialogListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ToTableID = tmpArrID[which];
				String str = tmpArr[which];
				but.setText(str);
				ToTableName = str;
				dialog.dismiss();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.table_to_title));
		builder.setSingleChoiceItems(tmpArr, -1, fromDialogListener);
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void showTableToDialog(final Button but) {
		// build the array of tables with orders. This is the to-table-list which will get the moved order
		// see if any of the tables does not have a tab open
		ArrayList<String> toName = new ArrayList<String>();
		ArrayList<Integer> toID = new ArrayList<Integer>();
		toName.clear();
		toID.clear();
		for (int i = 0; i < MaxTABLES; i++) {
			if (numberOfDishes(i) == 0) {
				String tmp = Global.tablenames.get(i);
				if ((!tmp.equalsIgnoreCase("blank")) && (!tmp.equalsIgnoreCase("take out"))) {
					toName.add(tmp);
					toID.add(i);
				}
			}
		}
		final String[] tmpArr = new String[toName.size()];
		final int[] tmpArrID = new int[toName.size()];
		for (int i = 0; i < toName.size(); i++) {
			tmpArr[i] = toName.get(i);
			tmpArrID[i] = toID.get(i);
		}
		DialogInterface.OnClickListener toDialogListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ToTableID = tmpArrID[which];
				String str = tmpArr[which];
				but.setText(str);
				ToTableName = str;
				dialog.dismiss();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.table_to_title));
		builder.setSingleChoiceItems(tmpArr, -1, toDialogListener);
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void showTableClearDialog(final Button but) {
		// build the array of tables with orders. This is the list eligible to clear
		ArrayList<String> fromName = new ArrayList<String>();
		ArrayList<Integer> fromID = new ArrayList<Integer>();
		fromName.clear();
		fromID.clear();
		for (int i = 0; i < MaxTABLES; i++) {
			if (numberOfDishes(i) > 0) {
				fromName.add(Global.tablenames.get(i));
				fromID.add(i);
			}
		}
		final String[] tmpArr = new String[fromName.size()];
		final int[] tmpArrID = new int[fromName.size()];
		for (int i = 0; i < fromName.size(); i++) {
			tmpArr[i] = fromName.get(i);
			tmpArrID[i] = fromID.get(i);
		}
		DialogInterface.OnClickListener fromDialogListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FromTableID = tmpArrID[which];
				String str = tmpArr[which];
				but.setText(str);
				FromTableName = str;
				dialog.dismiss();
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.table_clear_title));
		builder.setSingleChoiceItems(tmpArr, -1, fromDialogListener);
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void setupCatList() {
		// Loop through each line of cat file and populate both the Category String Arrays
		CategoryEng.clear();
		CategoryAlt.clear();
		// also populate the printer filter arrays
		P2Filter.clear();
		P3Filter.clear();
		for (int i = 0; i < categoryAll.length; i++) {
			String line = categoryAll[i];
			String[] linColumns = line.split("\\|");
			String[] linLang = linColumns[1].split("\\\\");
			// if there are no special, then we dont want to add the special to the category selector
			// even though the specials category still always resides in the TXT file
			if ((Global.NumSpecials > 0) || !(linLang[0].equalsIgnoreCase("Specials"))) {
				CategoryEng.add(linLang[0]);
				CategoryAlt.add(linLang[1]);
				// print filters arrays
				if (Global.P2FilterCats.contains(linColumns[0])) {
					P2Filter.add(i, true);
				} else {
					P2Filter.add(i, false);
				}
				//prefEdit.putBoolean("p2filter" + i, P2Filter.get(i));
				if (Global.P3FilterCats.contains(linColumns[0])) {
					P3Filter.add(i, true);
				} else {
					P3Filter.add(i, false);
				}
				//prefEdit.putBoolean("p3filter" + i, P3Filter.get(i));
			}
		}
		//log("P2Filter=" + P2Filter.toString());
		//log("P3Filter=" + P3Filter.toString());
	}

	// Build an ArrayList for the orderAdapter from the Json dishes in table tabid
	private void setJSONOrderList(int tabid) {
		try {
			JSONOrderList.clear();
			if (tabid != -1) {
				JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
				JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
				JSONArray jda = JSONdishObj.getJSONArray("dishes");
				int numdish = jda.length();
				//log("Number of dishes=" + numdish);
				if (numdish > 0) {
					JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
					for (int i = 0; i < JSONdishesAry.length(); i++) {
						JSONArray jd = JSONdishesAry.getJSONArray(i);
						JSONOrderList.add(jd);
					}
				}
			}
		} catch (Exception e) {
			log("json setJSONOrderList Table=" + tabid + " Exception=" + e);
		}
	}

	// Returns the number of Dishes currently on a table
	private int numberOfDishes(int tabid) {
		int numdish = -1;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray jda = JSONdishObj.getJSONArray("dishes");
			if (jda != null) {
				numdish = jda.length();
			} else {
				log("numdish=-1 due to NULL");
			}
		} catch (Exception e) {
			log("json numberOfDishes Table=" + tabid + " Exception=" + e);
		}
		//log("Table=" + tabid +" Dish count=" + numdish);
		return numdish;
	}

	// Count the number of dishes on the table tab that have not been printed which can be printed (not filtered)
	// int TABLE ID
	// Boolean True=exclude categories
	// int printer number to determine which filter to use
	private int unprintedDishCount(int tabid, Boolean filter, int printernumber) {
		int udc = 0;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray jda = JSONdishObj.getJSONArray("dishes");
			int numdish = jda.length();
			//log("Number of total dishes=" + numdish);
			if (numdish > 0) {
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
				for (int i = 0; i < JSONdishesAry.length(); i++) {
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// got each dish, now check the printed status for this dish
					Boolean printed = (Boolean) jsonGetter2(jd, "dishPrinted");
					// Dont P2 (Kitchen) if counterOnly override
					Boolean counterOnly = (Boolean) jsonGetter2(jd, "counterOnly");
					// exclude item if needed based on category filter ...
					Integer dishcatid = (Integer) jsonGetter2(jd, "categoryId");
					//log("i=" + i + " dishcatid=" + dishcatid);
					if (filter) {
						if (printernumber == 2) {
							if (P2Filter.get(dishcatid)) {
								if (!printed) {
									if (!counterOnly) udc++;
									//udc++;
								}
							}
						} else if (printernumber == 3) {
							if ((P3Filter.get(dishcatid)) || (counterOnly)) {
								if (!printed) {
									udc++;
								}
							}
						} else {
							if (!printed) udc++;
						}
					} else {
						if (!printed) udc++;
					}
				}
			}
		} catch (Exception e) {
			log("json unprintedDishCount Table=" + tabid + " Exception=" + e);
		}
		//log("Table=" + tabid +" Unprinted dish count=" + udc);
		return udc;
	}

	// Count the number of dishes on the JSON that have not been printed which can be printed (not filtered)
	// String JSON order
	// Boolean True=exclude categories
	// int printer number to determine which filter to use
	private int unprintedDishCountJSON(String json, Boolean filter, int printernumber) {
		int udc = 0;
		try {
			JSONArray JSONOrderAry = new JSONArray(json);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray jda = JSONdishObj.getJSONArray("dishes");
			int numdish = jda.length();
			//log("Number of total dishes=" + numdish);
			if (numdish > 0) {
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
				for (int i = 0; i < JSONdishesAry.length(); i++) {
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// got each dish, now check the printed status for this dish
					Boolean printed = (Boolean) jsonGetter2(jd, "dishPrinted");
					// Dont P2 if counterOnly override
					Boolean counterOnly = (Boolean) jsonGetter2(jd, "counterOnly");
					// exclude item if needed based on category filter ...
					Integer dishcatid = (Integer) jsonGetter2(jd, "categoryId");
					//log("i=" + i + " dishcatid=" + dishcatid);
					if (filter) {
						if (printernumber == 2) {
							if (P2Filter.get(dishcatid)) {
								if (!printed) {
									//udc++;
									if (!counterOnly) udc++;
								}
							}
						} else if (printernumber == 3) {
							if ((P3Filter.get(dishcatid)) || (counterOnly)) {
								if (!printed) {
									udc++;
								}
							}
						} else {
							if (!printed) udc++;
						}
					} else {
						if (!printed) udc++;
					}
				}
			}
		} catch (Exception e) {
			log("json unprintedDishCountJSON Exception=" + e);
		}
		//log("Unprinted dish count json=" + udc);
		return udc;
	}

	// Count the number of dishes on the table tab that have been printed
	private int printedDishCount(int tabid) {
		int pdc = 0;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			JSONArray jda = JSONdishObj.getJSONArray("dishes");
			int numdish = jda.length();
			//log("Number of dishes=" + numdish);
			if (numdish > 0) {
				JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
				for (int i = 0; i < JSONdishesAry.length(); i++) {
					JSONArray jd = JSONdishesAry.getJSONArray(i);
					// got a dish, now check the printed status for this dish
					Boolean printed = (Boolean) jsonGetter2(jd, "dishPrinted");
					if (printed) pdc++;
				}
			}
		} catch (Exception e) {
			log("json printedDishCount Table=" + tabid + " Exception=" + e);
		}
		//log("Table=" + tabid +" printed dish count=" + pdc);
		return pdc;
	}

	// Check if a specific dish has been printed
	private boolean dishHasBeenPrinted(int tabid, int dishid) {
		boolean printed = false;
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			//JSONArray jda = JSONdishObj.getJSONArray("dishes");
			JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
			JSONArray jd = JSONdishesAry.getJSONArray(dishid);
			printed = (Boolean) jsonGetter2(jd, "dishPrinted");
		} catch (Exception e) {
			log("json dishHasBeenPrinted Table=" + tabid + " Exception=" + e);
		}
		//log("hasBeenPrinted(" + tabid + "," +dishid + ")= " + printed);
		return printed;
	}

	// Set a specific dish as printed
	private void setDishPrinted(int tabid, int dishid) {
		try {
			JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
			JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
			//JSONArray jda = JSONdishObj.getJSONArray("dishes");
			JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
			JSONArray jd = JSONdishesAry.getJSONArray(dishid);
			jsonSetter(jd, "dishPrinted", true);
			//jd.put(createBoolean("dishPrinted",false));
			saveJsonTable(tabid, JSONOrderAry);
			//log("aaa=" + JSONOrderAry);
		} catch (Exception e) {
			log("json setDishPrinted Table=" + tabid + " dishID=" + dishid + " Exception=" + e);
		}
		//log("setDishPrinted(" + tabid + "," + dishid + ")");
	}

	// Return true is a Table has an OrderID set up
	private boolean tableHasOrderID(int tabid) {
		boolean hasorderid = false;
		try {
			JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
			String id = jsonGetter2(JSONtmp, "orderid").toString();
			if (id.length() > 0) {
				hasorderid = true;
				//log("hasOrderID(" + tabid + ")= " + hasorderid + " id=" + id);
			}
		} catch (Exception e) {
			//log("json hasOrderID Table=" + tabid + " Exception=" + e);
		}
		//log("hasOrderID(" + tabid + ")= " + hasorderid);
		return hasorderid;
	}

	// Return true is a Table has TabState=1 or TabState=2
	private boolean tabIsOpen(int tabid) {
		boolean hastab = false;
		try {
			//JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
			//int tabstate = (Integer) jsonGetter2(JSONtmp,"tabstate");
			//if (tabstate > 0) {
			if (numberOfDishes(tabid) > 0) hastab = true;
			//log("tabIsOpen(" + tabid + ")= " + tabstate);
			//}
		} catch (Exception e) {
			log("json tabIsOpen Table=" + tabid + " Exception=" + e);
		}
		//log("hasOrderID(" + tabid + ")= " + hasorderid);
		return hastab;
	}

	// Save a JSON table for persistence
	private void saveJsonTable(int tabid, JSONArray json) {
		try {
			JSONOrderStr[tabid] = json.toString();
			prefEdit.putString("jsonorderstr" + tabid, JSONOrderStr[tabid]);
			prefEdit.commit();
		} catch (Exception e) {
			log("saveJsonTable Table=" + tabid + " Exception=" + e);
		}
	}

	// Remove a JSON table from persistence
	private void removeJsonTable(int tabid, JSONArray json) {
		try {
			JSONOrderStr[tabid] = json.toString();
			prefEdit.remove("jsonorderstr" + tabid);
			prefEdit.commit();
		} catch (Exception e) {
			log("removeJsonTable Table=" + tabid + " Exception=" + e);
		}
	}

	// Clear all the printers status (x3) for a table
	private void clearPrinterStatus(int tabid) {
		printStatus[tabid][0] = 0;
		printStatus[tabid][1] = 0;
		printStatus[tabid][2] = 0;
		mHandler.post(mUpdatePrinters);
	}

}