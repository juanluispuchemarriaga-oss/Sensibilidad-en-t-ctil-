package com.example.sensiboost;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS = "backup";
    private static final String K_POINTER = "pointer_speed";
    private static final String K_TOUCH = "high_touch_sensitivity_enable";

    private TextView status;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p * 2, p, p);
        sv.addView(root);
        setContentView(sv);

        status = new TextView(this);
        status.setTextSize(15);
        root.addView(status);

        addBtn("1. Dar permiso de ajustes", v -> askPermission());
        addBtn("2. SENSIBILIDAD AL MÁXIMO", v -> apply());
        addBtn("Restaurar valores originales", v -> restore());
        addBtn("Respaldo: ajustes de pantalla", v -> open(Settings.ACTION_DISPLAY_SETTINGS));
        addBtn("Respaldo: opciones de desarrollador",
                v -> open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        addBtn("Respaldo: ajustes generales", v -> open(Settings.ACTION_SETTINGS));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh("");
    }

    private void addBtn(String t, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(t);
        btn.setOnClickListener(l);
        root.addView(btn);
    }

    private boolean canWrite() {
        try {
            return Settings.System.canWrite(this);
        } catch (Exception e) {
            return false;
        }
    }

    private void refresh(String extra) {
        ContentResolver cr = getContentResolver();
        status.setText("Permiso: " + (canWrite() ? "SÍ" : "NO")
                + "\nVelocidad puntero actual: " + Settings.System.getInt(cr, K_POINTER, 0)
                + "\nSensibilidad táctil alta: " + Settings.System.getInt(cr, K_TOUCH, 0)
                + "\n\n" + extra);
    }

    private void askPermission() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            open(Settings.ACTION_SETTINGS);
        }
    }

    private void open(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Exception e) {
            Toast.makeText(this, "No disponible en este teléfono", Toast.LENGTH_SHORT).show();
        }
    }

    private void apply() {
        if (!canWrite()) {
            Toast.makeText(this, "Primero da el permiso (botón 1)", Toast.LENGTH_LONG).show();
            askPermission();
            return;
        }
        ContentResolver cr = getContentResolver();
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!sp.contains("saved")) { // respaldo solo la primera vez
            sp.edit()
                    .putInt(K_POINTER, Settings.System.getInt(cr, K_POINTER, 0))
                    .putInt(K_TOUCH, Settings.System.getInt(cr, K_TOUCH, 0))
                    .putBoolean("saved", true)
                    .apply();
        }
        StringBuilder r = new StringBuilder();
        r.append(set(cr, K_POINTER, 7)).append("\n");
        r.append(set(cr, K_TOUCH, 1)).append("\n");
        r.append("\nNota: el puntero solo afecta mouse/trackpad. Si algo no es compatible, usa los botones de respaldo.");
        refresh(r.toString());
    }

    private String set(ContentResolver cr, String key, int value) {
        try {
            Settings.System.putInt(cr, key, value);
            boolean ok = Settings.System.getInt(cr, key, -999) == value;
            return key + ": " + (ok ? "OK" : "no soportado en este equipo");
        } catch (Exception e) {
            return key + ": falló (" + e.getClass().getSimpleName() + ")";
        }
    }

    private void restore() {
        if (!canWrite()) {
            askPermission();
            return;
        }
        ContentResolver cr = getContentResolver();
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        StringBuilder r = new StringBuilder();
        r.append(set(cr, K_POINTER, sp.getInt(K_POINTER, 0))).append("\n");
        r.append(set(cr, K_TOUCH, sp.getInt(K_TOUCH, 0))).append("\n");
        sp.edit().clear().apply();
        refresh("Restaurado:\n" + r);
    }
                       }
