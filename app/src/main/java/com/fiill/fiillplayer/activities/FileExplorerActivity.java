package com.fiill.fiillplayer.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;

import com.fiill.fiillplayer.R;
import com.fiill.fiillplayer.fragments.FileListFragment;


public class FileExplorerActivity extends AppActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doOpenDirectory(Environment.getExternalStorageDirectory(), false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults. length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doOpenDirectory(Environment.getExternalStorageDirectory(), false);
            } else {
                Toast.makeText(this, "please grant reading storage permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void doOpenDirectory(File path, boolean addToBackStack) {
        Fragment newFragment = FileListFragment.newInstance(this, path);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.body, newFragment);

        if (addToBackStack)
            transaction.addToBackStack(null);
        transaction.commit();
    }
}