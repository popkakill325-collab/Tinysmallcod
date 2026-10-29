package com.honor.cleaner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(13, 16, 24), panel = Color.rgb(25, 30, 42);
    private final int white = Color.rgb(241, 244, 250), muted = Color.rgb(151, 163, 184);
    private final int accent = Color.rgb(103, 232, 189);
    private LinearLayout root, list;
    private EditText search;
    private String filter = "all", query = "";
    private final List<AppEntry> apps = new ArrayList<>();

    static class AppEntry {
        String name, pkg;
        boolean system;
        AppEntry(String n, String p, boolean s) { name=n; pkg=p; system=s; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildShell();
        loadApps();
        renderApps();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l),dp(t),dp(r),dp(b)); }

    private void buildShell() {
        ScrollView outer = new ScrollView(this); outer.setFillViewport(true); outer.setBackgroundColor(bg);
        root = column(); pad(root,18,20,18,22); outer.addView(root);
        setContentView(outer);
        TextView brand = text("HONOR  /  CARE", 12, accent, true);
        root.addView(brand, lp(-1,24));
        TextView title = text("Чистка без\\nмагии и обмана", 29, white, true);
        title.setLineSpacing(dp(1),1f); root.addView(title, lp(-1,80));
        TextView sub = text("Приложения • память • контроль", 14, muted, false);
        root.addView(sub, lp(-1,28));
        LinearLayout info = column(); info.setBackground(shape(panel,18)); pad(info,16,14,16,14);
        TextView infoTitle = text("ВАЖНО ПРО ANDROID 8", 12, accent, true);
        info.addView(infoTitle, lp(-1,22));
        TextView infoBody = text("Android не разрешает обычным приложениям удалять чужой кэш напрямую. Эта утилита открывает системный экран нужного приложения, где ты сам можешь очистить кэш, данные или удалить приложение. Системные компоненты не трогаем автоматически.", 13, white, false);
        infoBody.setGravity(Gravity.TOP); infoBody.setLineSpacing(dp(3),1f); info.addView(infoBody, lp(-1,-2));
        LinearLayout.LayoutParams ip = lp(-1,-2); ip.topMargin=dp(16); root.addView(info,ip);

        Button storage = button("Открыть системное хранилище", true);
        LinearLayout.LayoutParams sp = lp(-1,48); sp.topMargin=dp(12); root.addView(storage,sp);
        storage.setOnClickListener(v -> openStorage());

        TextView section = text("УСТАНОВЛЕННЫЕ ПРИЛОЖЕНИЯ", 12, muted, true);
        LinearLayout.LayoutParams secp=lp(-1,34); secp.topMargin=dp(17); root.addView(section,secp);
        search = new EditText(this); search.setSingleLine(true); search.setTextColor(white);
        search.setHintTextColor(muted); search.setHint("Найти приложение…"); search.setTextSize(15);
        search.setBackground(shape(panel,14)); pad(search,14,0,14,0);
        root.addView(search,lp(-1,48));
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { query=s.toString().toLowerCase(Locale.ROOT); renderApps(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        LinearLayout tabs = row(); String[] names={"Все","Пользовательские","Системные"}; String[] ids={"all","user","system"};
        for(int i=0;i<names.length;i++) {
            final String id=ids[i]; Button b=button(names[i],false); b.setTextSize(11);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(40),1); bp.setMargins(dp(3),dp(10),dp(3),dp(8)); tabs.addView(b,bp);
            b.setOnClickListener(v->{filter=id;renderApps();});
        }
        root.addView(tabs,lp(-1,58));
        list=column(); root.addView(list,lp(-1,-2));
        TextView footer=text("HONOR CLEANER  •  LOCAL ONLY  •  NO ADS",10,muted,true);
        footer.setGravity(Gravity.CENTER); LinearLayout.LayoutParams fp=lp(-1,42); fp.topMargin=dp(12); root.addView(footer,fp);
    }

    private Button button(String label, boolean primary) {
        Button b=new Button(this); b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(primary ? bg : white); b.setBackground(shape(primary ? accent : Color.rgb(43,50,66),14));
        if(android.os.Build.VERSION.SDK_INT>=21) b.setStateListAnimator(null);
        return b;
    }

    private void loadApps() {
        PackageManager pm=getPackageManager();
        List<PackageInfo> installed=pm.getInstalledPackages(0);
        apps.clear();
        for(PackageInfo p:installed) {
            if(p.applicationInfo==null || p.packageName.equals(getPackageName())) continue;
            CharSequence label=p.applicationInfo.loadLabel(pm);
            boolean sys=(p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0 ||
                        (p.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0;
            apps.add(new AppEntry(label==null?p.packageName:label.toString(),p.packageName,sys));
        }
        Collections.sort(apps, (a,b)->{
            if(a.system!=b.system) return a.system?1:-1;
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    private void renderApps() {
        if(list==null) return;
        list.removeAllViews();
        int shown=0, userCount=0, sysCount=0;
        for(AppEntry a:apps) { if(a.system)sysCount++;else userCount++; }
        TextView count=text("Найдено: "+apps.size()+"   •   обычные: "+userCount+"   •   системные: "+sysCount,12,muted,false);
        list.addView(count,lp(-1,30));
        for(AppEntry a:apps) {
            if(!"all".equals(filter) && !("system".equals(filter)&&a.system) && !("user".equals(filter)&&!a.system)) continue;
            if(!a.name.toLowerCase(Locale.ROOT).contains(query) && !a.pkg.toLowerCase(Locale.ROOT).contains(query)) continue;
            shown++;
            LinearLayout card=row(); card.setBackground(shape(panel,15)); pad(card,12,9,10,9);
            LinearLayout details=column(); details.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=text(a.name,14,white,true); name.setMaxLines(1); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView meta=text((a.system?"СИСТЕМНОЕ":"УСТАНОВЛЕНО")+"  ·  "+a.pkg,10,muted,false);
            meta.setMaxLines(1); meta.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            details.addView(name,lp(-1,24)); details.addView(meta,lp(-1,20));
            card.addView(details,new LinearLayout.LayoutParams(0,dp(48),1));
            Button manage=button("Открыть",false); manage.setTextSize(11);
            card.addView(manage,lp(82,40));
            manage.setOnClickListener(v->showActions(a));
            LinearLayout.LayoutParams cp=lp(-1,66); cp.bottomMargin=dp(7); list.addView(card,cp);
        }
        if(shown==0) { TextView empty=text("Ничего не найдено.",14,muted,false); list.addView(empty,lp(-1,56)); }
    }

    private void showActions(AppEntry a) {
        String[] actions={"Открыть сведения и очистить кэш","Запустить приложение","Удалить приложение (если разрешено)","Отмена"};
        new AlertDialog.Builder(this).setTitle(a.name).setItems(actions,(dialog,which)->{
            if(which==0) openAppInfo(a);
            else if(which==1) launchApp(a);
            else if(which==2) uninstallApp(a);
        }).show();
    }

    private void openAppInfo(AppEntry a) {
        try { Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+a.pkg)); startActivity(i); }
        catch(Exception e) { Toast.makeText(this,"Не удалось открыть настройки приложения",Toast.LENGTH_SHORT).show(); }
    }
    private void launchApp(AppEntry a) {
        try { Intent i=getPackageManager().getLaunchIntentForPackage(a.pkg); if(i!=null)startActivity(i); else Toast.makeText(this,"У приложения нет значка запуска",Toast.LENGTH_SHORT).show(); }
        catch(Exception e) { Toast.makeText(this,"Не удалось запустить приложение",Toast.LENGTH_SHORT).show(); }
    }
    private void uninstallApp(AppEntry a) {
        if(a.system) {
            new AlertDialog.Builder(this).setTitle("Системное приложение")
                .setMessage("Удаление системных приложений обычно требует ADB или root. Безопаснее открыть сведения и проверить, доступно ли отключение.")
                .setPositiveButton("Открыть сведения",(d,w)->openAppInfo(a)).setNegativeButton("Отмена",null).show();
        } else {
            new AlertDialog.Builder(this).setTitle("Удалить "+a.name+"?")
                .setMessage("Откроется стандартное подтверждение Android. Приложение может потерять локальные данные.")
                .setPositiveButton("Продолжить",(d,w)->{
                    try { Intent i=new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+a.pkg)); startActivity(i); }
                    catch(Exception e) { openAppInfo(a); }
                }).setNegativeButton("Отмена",null).show();
        }
    }
    private void openStorage() {
        try { startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)); }
        catch(Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch(Exception ignored) { Toast.makeText(this,"Открой Настройки → Память",Toast.LENGTH_LONG).show(); }
        }
    }
}
