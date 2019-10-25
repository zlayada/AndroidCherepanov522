package com.netology.androidcherepanov522;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    private EditText mLogin;
    private EditText mPassword;
    private CheckBox mCheckbox_storage;

    private final String AUTH_FILE_NAME = "info.data";
    private boolean isStorageExternal;

    private OutputStreamWriter streamWriter;
    private FileWriter fileWriter;
    private BufferedWriter bufferedWriter;

    private InputStreamReader streamReader;
    private FileReader fileReader;
    private BufferedReader bufferedReader;

    private final String ATTEMPTS = "attempts";
    private final int MAX_AUTH_ATTEMPTS = 3;
    private int attempts_counter;

    private final String APP_PREFERENCES = "app_preferences";
    private final String STORAGE = "storage";
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        if (savedInstanceState != null) {
            attempts_counter = savedInstanceState.getInt(ATTEMPTS);
        } else {
            attempts_counter = 1;
        }

        preferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);

        if (preferences != null) {

            if (preferences.contains(STORAGE)) {

                isStorageExternal = preferences.getBoolean(STORAGE, false);
                mCheckbox_storage.setChecked(isStorageExternal);

                if (!isExternalStorageReadable()) {
                    mCheckbox_storage.setEnabled(false);
                }

            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);
        outState.putInt(ATTEMPTS, attempts_counter);
    }

    private void initView() {

        mLogin = findViewById(R.id.login);
        mPassword = findViewById(R.id.password);

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInputCorrect()) {
                    checkUserAuthData();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_date), Toast.LENGTH_LONG).show();
                    clearFields();
                }
            }
        });

        findViewById(R.id.reg_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInputCorrect()) {
                    saveUserAuthData();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_date), Toast.LENGTH_LONG).show();
                    clearFields();
                }
            }
        });

        mCheckbox_storage = findViewById(R.id.checkbox_storage);
        mCheckbox_storage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    relocateAuthFile(b);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void relocateAuthFile(boolean toExternal) throws IOException {

        if (toExternal && !isStorageExternal) {

            if (isExternalStorageWritable()) {

                deleteFile(true);
                bufferedReader = getBufferedReader(false);
                bufferedWriter = getBufferedWriter(true);
                rewriteAuthFile(bufferedReader, bufferedWriter);
                closeReaders(false);
                closeWriters(true);
                deleteFile(false);

            } else {

                Toast.makeText(getApplicationContext(), getString(R.string.not_ext_storage), Toast.LENGTH_LONG).show();
                mCheckbox_storage.setChecked(false);
                return;

            }
        }
        if (!toExternal && isStorageExternal) {

            if (isExternalStorageReadable()) {

                deleteFile(false);
                bufferedReader = getBufferedReader(true);
                bufferedWriter = getBufferedWriter(false);
                rewriteAuthFile(bufferedReader, bufferedWriter);
                closeReaders(true);
                closeWriters(false);
                deleteFile(true);

            } else {

                Toast.makeText(getApplicationContext(), getString(R.string.not_ext_storage), Toast.LENGTH_LONG).show();
                mCheckbox_storage.setChecked(true);
                return;

            }
        }

        preferences.edit().putBoolean(STORAGE, toExternal).apply();
        isStorageExternal = toExternal;
    }

    private void deleteFile(boolean isStorageExternal) {

        File authFile;

        if (isStorageExternal) {
            authFile = new File(getExternalFilesDir(null), AUTH_FILE_NAME);
        } else {
            authFile = new File(getFilesDir(), AUTH_FILE_NAME);
        }
        authFile.delete();
    }

    private void rewriteAuthFile(BufferedReader reader, BufferedWriter writer) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            writer.write(line + '\n');
            line = reader.readLine();
        }
    }

    private boolean isInputCorrect() {

        return !mLogin.getText().toString().equals("")
                && !mPassword.getText().toString().equals("");
    }

    private void checkUserAuthData() {

        boolean isUserAuth = false;
        try {
            bufferedReader = getBufferedReader(isStorageExternal);

            if (bufferedReader != null) {
                String line = bufferedReader.readLine(); // read login
                while (line != null) {
                    String login = line;
                    line = bufferedReader.readLine(); // read password
                    if (mLogin.getText().toString().equals(login)) { // check login
                        if (line != null && mPassword.getText().toString().equals(line)) { // check password
                            isUserAuth = true;
                            break;
                        }
                    }
                }

                closeReaders(isStorageExternal);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_authorization), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isUserAuth) {
            Toast.makeText(getApplicationContext(), getString(R.string.login_ok), Toast.LENGTH_LONG).show();
            clearFields();
        } else {
            userNotAuth();
        }
    }

    private void userNotAuth() {

        Toast.makeText(getApplicationContext(), getString(R.string.error_log_pas), Toast.LENGTH_LONG).show();
        clearFields();
        attempts_counter++;

        if (attempts_counter > MAX_AUTH_ATTEMPTS) {

            Toast.makeText(getApplicationContext(), getString(R.string.error_login), Toast.LENGTH_LONG).show();
            clearFields();
            finish();
        }
    }

    private void saveUserAuthData() {

        try {
            bufferedWriter = getBufferedWriter(isStorageExternal);

            if (bufferedWriter != null) {

                bufferedWriter.write(mLogin.getText().toString() + '\n');
                bufferedWriter.write(mPassword.getText().toString() + '\n');

                closeWriters(isStorageExternal);

                Toast.makeText(getApplicationContext(), getString(R.string.reg_ok), Toast.LENGTH_LONG).show();
                clearFields();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_authorization), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedWriter getBufferedWriter(boolean isStorageExternal) throws IOException {

        if (isStorageExternal) {
            if (isExternalStorageWritable()) {
                File authFile = new File(getExternalFilesDir(null), AUTH_FILE_NAME);
                fileWriter = new FileWriter(authFile, true);
                return new BufferedWriter(fileWriter);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.not_ext_storage), Toast.LENGTH_LONG).show();
                return null;
            }
        } else {
            FileOutputStream authFile = openFileOutput(AUTH_FILE_NAME, MODE_PRIVATE | MODE_APPEND);
            streamWriter = new OutputStreamWriter(authFile);
            return new BufferedWriter(streamWriter);
        }
    }

    private void closeWriters(boolean isStorageExternal) throws IOException {
        bufferedWriter.close();
        if (isStorageExternal) {
            fileWriter.close();
        } else {
            streamWriter.close();
        }
    }

    private BufferedReader getBufferedReader(boolean isStorageExternal) throws FileNotFoundException {
        if (isStorageExternal) {
            if (isExternalStorageReadable()) {
                File authFile = new File(getExternalFilesDir(null), AUTH_FILE_NAME);
                fileReader = new FileReader(authFile);
                return new BufferedReader(fileReader);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.not_ext_storage), Toast.LENGTH_LONG).show();
                return null;
            }
        } else {
            FileInputStream authFile = openFileInput(AUTH_FILE_NAME);
            streamReader = new InputStreamReader(authFile);
            return new BufferedReader(streamReader);
        }
    }

    private void closeReaders(boolean isStorageExternal) throws IOException {

        bufferedReader.close();

        if (isStorageExternal) {
            fileReader.close();
        } else {
            streamReader.close();
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return isExternalStorageWritable() || state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }

    private void clearFields() {
        mLogin.setText("");
        mPassword.setText("");
    }
}