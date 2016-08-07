package com.realmcountries;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realmcountries.realm.Country;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRealm();

        ObjectMapper objectMapper = new ObjectMapper();
        InputStream ins = getResources().openRawResource(R.raw.countries_to_cities);
        TypeReference<HashMap<String, List<String>>> typeRef
                = new TypeReference<HashMap<String, List<String>>>() {};
        Map<String, List<String>> map;
        try {
            map = objectMapper.readValue(ins, typeRef);
        } catch (IOException e) {
            map = new HashMap<>();
            Log.e("MainActivity", e.getMessage());
        }

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RealmList<Country> list = new RealmList<>();
        for (Map.Entry<String, String> entry : CountriesCodes.map.entrySet()) {
            String name = entry.getValue();
            String code = entry.getKey();
            Country country;
            if ( (country = isNew(list, name)) == null ) {
                country = new Country();
                country.setCode1(code);
                country.setName(name);
                if (map.get(entry.getValue()) == null) {
                    Log.e("MainActivity", entry.getValue() + " cities=null");
                } else {
                    country.setCities(TextUtils.join("|", map.get(entry.getValue())));
                }
                list.add(country);
            } else {
                country.setCode2(code);
            }
        }
        realm.copyToRealm(list);
        realm.commitTransaction();

        exportDatabase();
    }

    public void initRealm() {
        RealmConfiguration realmConfig = new RealmConfiguration.Builder(this)
                .name("myrealm.realm")
                .schemaVersion(1)
                .build();
        Realm.setDefaultConfiguration(realmConfig);
    }

    private Country isNew(RealmList<Country> list, String name) {
        for (Country country : list) {
            if ( TextUtils.equals(name, country.getName()) ) {
                return country;
            }
        }
        return null;
    }

    public void exportDatabase() {

        // init realm
        Realm realm = Realm.getDefaultInstance();

        File exportRealmFile = null;
        try {
            // get or create an "export.realm" file
            exportRealmFile = new File(this.getExternalCacheDir(), "export.realm");

            // if "export.realm" already exists, delete
            exportRealmFile.delete();

            // copy current realm to "export.realm"
            realm.writeCopyTo(exportRealmFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        realm.close();

        // init email intent and add export.realm as attachment
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, "m.levytskiy@gmail.com");
        intent.putExtra(Intent.EXTRA_SUBJECT, "test");
        intent.putExtra(Intent.EXTRA_TEXT, "test");
        Uri u = Uri.fromFile(exportRealmFile);
        intent.putExtra(Intent.EXTRA_STREAM, u);

        // start email intent
        startActivity(Intent.createChooser(intent, "YOUR CHOOSER TITLE"));
    }
}
