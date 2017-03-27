package com.xilingyuli.hitwlan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    @BindView(R.id.editText)
    AutoCompleteTextView userEditText;
    @BindView(R.id.editText2)
    EditText passwordEditText;
    @BindView(R.id.checkBox)
    CheckBox watchPasswordCheckBox;
    @BindView(R.id.checkBox2)
    CheckBox savePasswordCheckBox;
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.button2)
    Button button2;

    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences2;

    private OkHttpClient client;

    private WifiManager wifiManager;

    private Pattern p = Pattern.compile("<div class=\"weui_toptips weui_warn js_tooltips\">(.*)</div>");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        client = new OkHttpClient();

        sharedPreferences = getSharedPreferences("account", Context.MODE_PRIVATE);
        sharedPreferences2 = getSharedPreferences("default_account", Context.MODE_PRIVATE);

        initSetting();

        userEditText.setAdapter(new ArrayAdapter<String>(this, R.layout.listitem_account, sharedPreferences.getAll().keySet().toArray(new String[]{})));
        userEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String s = (((TextView) view).getText() + "");
                passwordEditText.setText(sharedPreferences.getString(s, ""));
            }
        });

        watchPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                else
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(new String[]{"注销", "管理账号", "关于"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                disconnect();
                                break;
                            case 1:
                                accountManagerDialog();
                                break;
                            case 2:
                                aboutMessage();
                                break;
                        }
                    }
                });
                builder.show();
            }
        });

    }

    private void saveAccount(String userName, String password)
    {
        sharedPreferences2.edit().putString("account",userName).commit();
        sharedPreferences.edit().putString(userName, password).commit();
    }

    private void initSetting()
    {
        String userName = sharedPreferences2.getString("account","");
        userEditText.setText(userName);
        passwordEditText.setText(sharedPreferences.getString(userName,""));

        if(!(passwordEditText.getText()+"").equals(""))
            savePasswordCheckBox.setChecked(true);
    }

    private void connect()
    {
        final String userName = userEditText.getText() + "";
        final String password = passwordEditText.getText() + "";

        if (!userName.equals("") && !password.equals("")) {
            RequestBody body = new FormBody.Builder()
                    .add("action","login")
                    .add("username",userName)
                    .add("password",password)
                    .add("ac_id","1")
                    .add("user_ip","")
                    .add("nas_ip","")
                    .add("user_mac","")
                    .add("url","")
                    .add("save_me","1")
                    .build();
            Request request = new Request.Builder().url("http://192.168.52.11/srun_portal_phone.php?ac_id=1&").post(body).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    dealWithLoginInfo("error:"+e.getMessage(),userName,password);
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    final String warn = getBackInfo(response.body().string());
                    dealWithLoginInfo(warn,userName,password);
                }
            });
        }
    }

    private String getBackInfo(String response){
        String result = response;
        Matcher matcher = p.matcher(response);
        if(matcher.find()) {
            result = matcher.group();
            result = result.substring(48,result.length()-6);
        }
        return result;
    }

    private void dealWithLoginInfo(final String info, final String userName, final String password){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!info.isEmpty())
                    Toast.makeText(MainActivity.this,info,Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(MainActivity.this,"登录成功",Toast.LENGTH_LONG).show();
                    saveAccount(userName,password);
                    finish();
                }
            }
        });
    }

    private void dealWithLogoutInfo(final String info){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!info.isEmpty())
                    Toast.makeText(MainActivity.this,info,Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(MainActivity.this,"注销成功",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void disconnect()
    {
        final String userName = userEditText.getText() + "";
        RequestBody body = new FormBody.Builder()
                .add("action","auto_logout")
                .add("info","")
                .add("username",userName)
                .add("user_ip",getLocalIpAddress())
                .build();
        Request request = new Request.Builder().url("http://192.168.52.11/srun_portal_phone.php?ac_id=1&").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                dealWithLogoutInfo("error:"+e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String warn = getBackInfo(response.body().string());
                dealWithLogoutInfo(warn);
            }
        });
    }

    private void accountManagerDialog()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] account = sharedPreferences.getAll().keySet().toArray(new String[]{});
        final boolean[] isChoosed = new boolean[account.length];
        builder.setTitle("管理账号");
        builder.setMultiChoiceItems(account, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                isChoosed[i] = b;
            }
        });
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (int j = 0; j < account.length; j++)
                    if (isChoosed[j])
                        sharedPreferences.edit().remove(account[j]).commit();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void aboutMessage()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("关于");
        builder.setMessage("开发者：西陵鱼璃\n联系我：786979248@qq.com");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private String getLocalIpAddress() {
        if(wifiManager==null)
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
