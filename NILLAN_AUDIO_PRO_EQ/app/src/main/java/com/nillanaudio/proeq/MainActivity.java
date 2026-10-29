package com.nillanaudio.proeq;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * NILLAN AUDIO PRO EQ.
 * Uses Android's system audio effects on session 0. Whether these effects process
 * other apps (including Brave) is firmware-dependent and must be tested on device.
 */
public class MainActivity extends Activity {
    private static final String PREFS = "nillan_pro_eq";
    private static final String CUSTOM_PREFIX = "custom_";
    private final int bg = Color.rgb(12, 20, 25), panel = Color.rgb(25, 39, 46), accent = Color.rgb(0, 207, 154);
    private SharedPreferences prefs;
    private LinearLayout root, eqContainer;
    private TextView status, eqModeLabel, bassValue, gainValue, compValue, limitValue;
    private DynamicsProcessing dynamics;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudness;
    private boolean effectsReady = false, applyingPreset = false, limiterEnabled = true, compressorEnabled = true, parametricEnabled = true;
    private int bandCount = 21;
    private float[] eqGains = new float[21];
    private float bassStrength = 0f, outputGainDb = 0f, compRatio = 3f, compThreshold = -18f, limiterThreshold = -1f;
    // Five parametric bands: center frequency (Hz), gain (dB), and Q.
    private final float[] peqFreq = {80f, 250f, 1000f, 4000f, 12000f};
    private final float[] peqGain = {0f, 0f, 0f, 0f, 0f};
    private final float[] peqQ = {1f, 1f, 1f, 1f, 1f};
    private LinearLayout peqContainer;
    private CheckBox peqToggle;
    private SeekBar bassSeek, gainSeek, ratioSeek, thresholdSeek, limiterSeek;
    private CheckBox compressorToggle, limiterToggle;
    private final String[] presetNames = {"Flat", "Vocal", "Bass", "Clear", "Live Vocal"};
    private final String[] cutoffLabels21 = {"31 Hz","43 Hz","58 Hz","79 Hz","108 Hz","148 Hz","202 Hz","276 Hz","378 Hz","517 Hz","707 Hz","967 Hz","1.32 kHz","1.80 kHz","2.46 kHz","3.36 kHz","4.59 kHz","6.27 kHz","8.56 kHz","11.7 kHz","16.0 kHz"};
    private final float[] cutoffs10 = {50f, 100f, 200f, 400f, 800f, 1600f, 3200f, 6400f, 12000f, 20000f};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadLastState();
        buildUi();
        initEffects();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(18),dp(16),dp(28)); root.setBackgroundColor(bg); scroll.addView(root);
        TextView title = new TextView(this); title.setText("NILLAN AUDIO"); title.setTextColor(accent); title.setTextSize(28); title.setGravity(Gravity.CENTER); title.setTypeface(null,1); root.addView(title,lp());
        TextView sub = new TextView(this); sub.setText("PRO EQ  •  GRAPHIC EQ / DYNAMICS / PRESET"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(12); sub.setGravity(Gravity.CENTER); root.addView(sub,lp());
        status = new TextView(this); status.setText("Memeriksa dukungan efek audio Android…"); status.setTextColor(Color.WHITE); status.setTextSize(13); status.setPadding(dp(12),dp(12),dp(12),dp(12)); status.setBackgroundColor(panel); LinearLayout.LayoutParams slp=lp(); slp.topMargin=dp(14); slp.bottomMargin=dp(8); root.addView(status,slp);

        sectionTitle("PRESET PABRIK");
        LinearLayout row = horizontal();
        addButton(row,"Flat",()->applyFactoryPreset(0)); addButton(row,"Vocal",()->applyFactoryPreset(1)); addButton(row,"Bass",()->applyFactoryPreset(2)); root.addView(row,lp());
        row=horizontal(); addButton(row,"Clear",()->applyFactoryPreset(3)); addButton(row,"Live Vocal",()->applyFactoryPreset(4)); addButton(row,"Simpan preset",this::saveCustomPreset); root.addView(row,lp());
        row=horizontal(); addButton(row,"Muat preset",this::loadCustomPreset); addButton(row,"Ekspor preset",this::exportPreset); addButton(row,"Impor teks",this::importPreset); root.addView(row,lp());

        sectionTitle("GRAPHIC EQUALIZER");
        row=horizontal(); addButton(row,"10 BAND",()->changeBandCount(10)); addButton(row,"21 BAND PRO",()->changeBandCount(21)); root.addView(row,lp());
        eqModeLabel = new TextView(this); eqModeLabel.setTextColor(Color.LTGRAY); eqModeLabel.setTextSize(12); eqModeLabel.setPadding(0,dp(4),0,dp(6)); root.addView(eqModeLabel,lp());
        eqContainer = new LinearLayout(this); eqContainer.setOrientation(LinearLayout.VERTICAL); root.addView(eqContainer,lp());

        sectionTitle("PARAMETRIC EQ PRO — 5 BAND");
        TextView peqInfo=smallLabel("Atur frekuensi pusat, gain, dan Q setiap band. Pengaturan ini dipetakan ke band DSP Android, jadi bentuk kurvanya pendekatan sesuai resolusi 10/21 band."); peqInfo.setTextColor(Color.LTGRAY); peqInfo.setTextSize(12); root.addView(peqInfo,lp());
        peqToggle=new CheckBox(this); peqToggle.setText("Aktifkan Parametric EQ"); peqToggle.setTextColor(Color.WHITE); peqToggle.setChecked(parametricEnabled); root.addView(peqToggle,lp());
        peqToggle.setOnCheckedChangeListener((b,c)->{parametricEnabled=c; applyCurrentState(); saveLastState();});
        peqContainer=new LinearLayout(this); peqContainer.setOrientation(LinearLayout.VERTICAL); root.addView(peqContainer,lp());

        sectionTitle("BASS BOOST"); bassValue = smallLabel("Bass 0%"); root.addView(bassValue,lp()); bassSeek=slider(100); bassSeek.setProgress(Math.round(bassStrength)); root.addView(bassSeek,lp()); bassSeek.setOnSeekBarChangeListener(listener(p->{bassStrength=p; bassValue.setText("Bass "+p+"%"); if(bassBoost!=null) safe(()->bassBoost.setStrength((short)(p*10))); saveLastState();}));

        sectionTitle("OUTPUT GAIN"); gainValue=smallLabel("Gain 0.0 dB"); root.addView(gainValue,lp()); gainSeek=slider(240); gainSeek.setProgress(Math.round(outputGainDb+12)*10); root.addView(gainSeek,lp()); gainSeek.setOnSeekBarChangeListener(listener(p->{outputGainDb=p/10f-12f; gainValue.setText(String.format(Locale.US,"Gain %.1f dB",outputGainDb)); if(dynamics!=null) safe(()->dynamics.setInputGainAllChannelsTo(outputGainDb)); if(loudness!=null) safe(()->loudness.setTargetGain(Math.max(0,Math.round((outputGainDb+12)*250)))); saveLastState();}));

        sectionTitle("MULTIBAND COMPRESSOR");
        compressorToggle=new CheckBox(this); compressorToggle.setText("Aktifkan compressor 4-band"); compressorToggle.setTextColor(Color.WHITE); compressorToggle.setChecked(compressorEnabled); root.addView(compressorToggle,lp()); compressorToggle.setOnCheckedChangeListener((b,c)->{compressorEnabled=c; updateDynamicsStages(); saveLastState();});
        compValue=smallLabel("Ratio 3.0:1"); root.addView(compValue,lp()); ratioSeek=slider(90); ratioSeek.setProgress(Math.round((compRatio-1)*10)); root.addView(ratioSeek,lp()); ratioSeek.setOnSeekBarChangeListener(listener(p->{compRatio=1+p/10f; compValue.setText(String.format(Locale.US,"Ratio %.1f:1",compRatio)); updateDynamicsStages(); saveLastState();}));
        thresholdToggleLabel("Threshold compressor"); thresholdSeek=slider(400); thresholdSeek.setProgress(Math.round(compThreshold+40)); root.addView(thresholdSeek,lp()); thresholdSeek.setOnSeekBarChangeListener(listener(p->{compThreshold=p-40; updateDynamicsStages(); saveLastState();}));

        sectionTitle("LIMITER / ANTI-CLIPPING");
        limiterToggle=new CheckBox(this); limiterToggle.setText("Aktifkan limiter output"); limiterToggle.setTextColor(Color.WHITE); limiterToggle.setChecked(limiterEnabled); root.addView(limiterToggle,lp()); limiterToggle.setOnCheckedChangeListener((b,c)->{limiterEnabled=c; updateDynamicsStages(); saveLastState();});
        limitValue=smallLabel("Limiter -1 dBFS"); root.addView(limitValue,lp()); limiterSeek=slider(120); limiterSeek.setProgress(Math.round(limiterThreshold+12)); root.addView(limiterSeek,lp()); limiterSeek.setOnSeekBarChangeListener(listener(p->{limiterThreshold=p-12; limitValue.setText(String.format(Locale.US,"Limiter %.0f dBFS",limiterThreshold)); updateDynamicsStages(); saveLastState();}));

        sectionTitle("CATATAN KOMPATIBILITAS");
        TextView note=new TextView(this); note.setText("Aplikasi ini memakai efek audio sistem Android (session 0). Efek global tidak dijamin oleh semua firmware. EQ 10/21 band, compressor, limiter, bass boost dan gain memakai API audio Android bila tersedia. Parametric EQ 5-band disediakan dengan kontrol frekuensi, gain, dan Q. Kurvanya dipetakan ke band graphic DSP Android, sehingga merupakan pendekatan (bukan biquad DSP mandiri). Efek global tetap bergantung pada firmware Oppo; uji suara Brave sebelum digunakan live."); note.setTextColor(Color.rgb(255,205,125)); note.setTextSize(12); note.setPadding(0,dp(4),0,dp(8)); root.addView(note,lp());
        Button reset=new Button(this); reset.setText("Reset semua ke Flat"); reset.setOnClickListener(v->applyFactoryPreset(0)); root.addView(reset,lp());
        setContentView(scroll);
        renderEqControls();
        renderParametricControls();
    }

    private void initEffects() {
        if(Build.VERSION.SDK_INT<28){status.setText("Perlu Android 9 atau lebih baru untuk DynamicsProcessing. HP kamu menggunakan Android "+Build.VERSION.SDK_INT+"."); return;}
        try {
            createDynamics();
            try { bassBoost = new BassBoost(0,0); bassBoost.setEnabled(true); } catch(Throwable ignored) { bassBoost=null; }
            try { loudness = new LoudnessEnhancer(0); loudness.setEnabled(true); } catch(Throwable ignored) { loudness=null; }
            effectsReady=true;
            status.setText("Mesin efek berhasil dibuat. Sekarang putar Brave dan uji apakah suara berubah saat EQ digeser. Dukungan audio global bergantung pada firmware Oppo.");
            applyCurrentState();
        } catch(Throwable e) {
            effectsReady=false;
            status.setText("Firmware menolak efek sistem: "+e.getClass().getSimpleName()+". Kontrol tetap tampil, tetapi audio belum tentu terproses.");
        }
    }

    private void createDynamics() {
        if(dynamics!=null){safe(()->dynamics.release()); dynamics=null;}
        int n=bandCount;
        DynamicsProcessing.Eq pre=new DynamicsProcessing.Eq(true,true,n);
        for(int i=0;i<n;i++) pre.setBand(i,new DynamicsProcessing.EqBand(true,cutoffFor(i,n), effectiveGainForBand(i,n)));
        DynamicsProcessing.Mbc mbc=new DynamicsProcessing.Mbc(true,compressorEnabled,4);
        float[] mbcCuts={180f,900f,4500f,20000f};
        for(int i=0;i<4;i++) mbc.setBand(i,new DynamicsProcessing.MbcBand(true,mbcCuts[i],10f,100f,compRatio,compThreshold,6f,-90f,1f,0f,0f));
        DynamicsProcessing.Eq post=new DynamicsProcessing.Eq(true,true,5);
        float[] postCuts={120f,500f,2000f,8000f,20000f};
        for(int i=0;i<5;i++) post.setBand(i,new DynamicsProcessing.EqBand(true,postCuts[i],0f));
        DynamicsProcessing.Limiter limiter=new DynamicsProcessing.Limiter(true,limiterEnabled,0,1f,60f,20f,limiterThreshold,0f);
        DynamicsProcessing.Config cfg=new DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,2,true,n,true,4,true,5,true)
                .setPreEqAllChannelsTo(pre).setMbcAllChannelsTo(mbc).setPostEqAllChannelsTo(post).setLimiterAllChannelsTo(limiter).setInputGainAllChannelsTo(outputGainDb).build();
        dynamics=new DynamicsProcessing(0,0,cfg); dynamics.setEnabled(true);
    }

    private float cutoffFor(int index,int count) {
        if(count==10) return cutoffs10[index];
        // 21 logarithmically spaced crossover points from 31.5 Hz to 20 kHz.
        return (float)(31.5*Math.pow(20000.0/31.5,index/(double)(count-1)));
    }
    private float gainForVisibleBand(int index,int count) {
        if(count==eqGains.length) return eqGains[index];
        int source=Math.min(eqGains.length-1,Math.round(index*(eqGains.length-1f)/(count-1f)));
        return eqGains[source];
    }

    /** Approximate parametric bell curves on the available Android graphic-EQ bands. */
    private float effectiveGainForBand(int index,int count) {
        float f=cutoffFor(index,count);
        float total=gainForVisibleBand(index,count);
        if(parametricEnabled) for(int b=0;b<peqFreq.length;b++) {
            float q=Math.max(0.3f,Math.min(10f,peqQ[b]));
            double sigma=Math.log((1.0/(2.0*q))+Math.sqrt(1.0+(1.0/(2.0*q))*(1.0/(2.0*q))));
            double x=Math.log(Math.max(1f,f)/Math.max(20f,peqFreq[b]));
            double shape=Math.exp(-0.5*x*x/(sigma*sigma));
            total += peqGain[b]*(float)shape;
        }
        return Math.max(-12f,Math.min(12f,total));
    }

    private void renderParametricControls() {
        if(peqContainer==null) return;
        peqContainer.removeAllViews();
        for(int b=0;b<peqFreq.length;b++) {
            final int band=b;
            TextView heading=smallLabel("PEQ "+(b+1)); heading.setTextColor(accent); heading.setTextSize(13); heading.setTypeface(null,1); peqContainer.addView(heading,lp());
            LinearLayout fr=horizontal(); fr.setGravity(Gravity.CENTER_VERTICAL);
            TextView fl=new TextView(this); fl.setText("Freq"); fl.setTextColor(Color.WHITE); fl.setTextSize(11); fl.setMinWidth(dp(48)); fr.addView(fl,new LinearLayout.LayoutParams(dp(48),-2));
            SeekBar fs=slider(1000); fs.setProgress(freqToProgress(peqFreq[b])); fr.addView(fs,new LinearLayout.LayoutParams(0,dp(40),1));
            TextView fv=new TextView(this); fv.setText(fmtFreq(peqFreq[b])); fv.setTextColor(Color.LTGRAY); fv.setTextSize(11); fv.setGravity(Gravity.RIGHT); fv.setMinWidth(dp(54)); fr.addView(fv,new LinearLayout.LayoutParams(dp(54),-2));
            fs.setOnSeekBarChangeListener(listener(p->{peqFreq[band]=progressToFreq(p);fv.setText(fmtFreq(peqFreq[band]));applyCurrentState();saveLastState();})); peqContainer.addView(fr,lp());

            LinearLayout gr=horizontal(); gr.setGravity(Gravity.CENTER_VERTICAL);
            TextView gl=new TextView(this); gl.setText("Gain"); gl.setTextColor(Color.WHITE); gl.setTextSize(11); gl.setMinWidth(dp(48)); gr.addView(gl,new LinearLayout.LayoutParams(dp(48),-2));
            SeekBar gs=slider(240); gs.setProgress(Math.round((peqGain[b]+12f)*10f)); gr.addView(gs,new LinearLayout.LayoutParams(0,dp(40),1));
            TextView gv=new TextView(this); gv.setText(String.format(Locale.US,"%.1f dB",peqGain[b])); gv.setTextColor(Color.LTGRAY); gv.setTextSize(11); gv.setGravity(Gravity.RIGHT); gv.setMinWidth(dp(54)); gr.addView(gv,new LinearLayout.LayoutParams(dp(54),-2));
            gs.setOnSeekBarChangeListener(listener(p->{peqGain[band]=p/10f-12f;gv.setText(String.format(Locale.US,"%.1f dB",peqGain[band]));applyCurrentState();saveLastState();})); peqContainer.addView(gr,lp());

            LinearLayout qr=horizontal(); qr.setGravity(Gravity.CENTER_VERTICAL);
            TextView ql=new TextView(this); ql.setText("Q"); ql.setTextColor(Color.WHITE); ql.setTextSize(11); ql.setMinWidth(dp(48)); qr.addView(ql,new LinearLayout.LayoutParams(dp(48),-2));
            SeekBar qs=slider(97); qs.setProgress(Math.round((peqQ[b]-0.3f)*10f)); qr.addView(qs,new LinearLayout.LayoutParams(0,dp(40),1));
            TextView qv=new TextView(this); qv.setText(String.format(Locale.US,"%.1f",peqQ[b])); qv.setTextColor(Color.LTGRAY); qv.setTextSize(11); qv.setGravity(Gravity.RIGHT); qv.setMinWidth(dp(54)); qr.addView(qv,new LinearLayout.LayoutParams(dp(54),-2));
            qs.setOnSeekBarChangeListener(listener(p->{peqQ[band]=0.3f+p/10f;qv.setText(String.format(Locale.US,"%.1f",peqQ[band]));applyCurrentState();saveLastState();})); peqContainer.addView(qr,lp());
        }
    }
    private int freqToProgress(float f){return Math.max(0,Math.min(1000,Math.round((float)(Math.log(Math.max(20f,Math.min(20000f,f))/20f)/Math.log(1000.0))*1000f)));}
    private float progressToFreq(int p){return (float)(20.0*Math.pow(1000.0,p/1000.0));}

    private void renderEqControls() {
        if(eqContainer==null) return;
        eqContainer.removeAllViews(); eqModeLabel.setText(bandCount+" band • gain per frequency zone • ±12 dB");
        for(int i=0;i<bandCount;i++) {
            final int index=i;
            LinearLayout row=horizontal(); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=new TextView(this); label.setText(bandLabel(i,bandCount)); label.setTextColor(Color.WHITE); label.setTextSize(11); label.setMinWidth(dp(64)); row.addView(label,new LinearLayout.LayoutParams(dp(64),-2));
            SeekBar s=slider(240); s.setProgress(Math.round((gainForVisibleBand(i,bandCount)+12)*10)); row.addView(s,new LinearLayout.LayoutParams(0,dp(40),1));
            TextView value=new TextView(this); value.setText(String.format(Locale.US,"%.1f",gainForVisibleBand(i,bandCount))); value.setTextColor(Color.LTGRAY); value.setTextSize(11); value.setGravity(Gravity.RIGHT); value.setMinWidth(dp(42)); row.addView(value,new LinearLayout.LayoutParams(dp(42),-2));
            s.setOnSeekBarChangeListener(listener(p->{float db=p/10f-12f; value.setText(String.format(Locale.US,"%.1f",db)); int real=mapVisibleToStorage(index,bandCount); eqGains[real]=db; if(dynamics!=null) safe(()->dynamics.setPreEqBandAllChannelsTo(index,new DynamicsProcessing.EqBand(true,cutoffFor(index,bandCount),db))); saveLastState();}));
            eqContainer.addView(row,lp());
        }
    }
    private int mapVisibleToStorage(int i,int n){return n==21?i:Math.min(20,Math.round(i*20f/Math.max(1,n-1)));}
    private String bandLabel(int i,int n){float upper=cutoffFor(i,n); if(i==0)return "≤ "+fmtFreq(upper); float lower=cutoffFor(i-1,n); return fmtFreq((float)Math.sqrt(lower*upper));}
    private String fmtFreq(float f){return f>=1000?String.format(Locale.US,"%.1fk",f/1000f):String.format(Locale.US,"%.0f",f);}

    private void updateDynamicsStages() {
        if(dynamics==null)return;
        safe(()->{
            DynamicsProcessing.Mbc mbc=new DynamicsProcessing.Mbc(true,compressorEnabled,4); float[] cuts={180f,900f,4500f,20000f};
            for(int i=0;i<4;i++)mbc.setBand(i,new DynamicsProcessing.MbcBand(true,cuts[i],10f,100f,compRatio,compThreshold,6f,-90f,1f,0f,0f));
            dynamics.setMbcAllChannelsTo(mbc);
            dynamics.setLimiterAllChannelsTo(new DynamicsProcessing.Limiter(true,limiterEnabled,0,1f,60f,20f,limiterThreshold,0f));
        });
    }
    private void applyCurrentState(){if(dynamics==null)return; safe(()->{dynamics.setInputGainAllChannelsTo(outputGainDb); for(int i=0;i<bandCount;i++)dynamics.setPreEqBandAllChannelsTo(i,new DynamicsProcessing.EqBand(true,cutoffFor(i,bandCount),effectiveGainForBand(i,bandCount)));}); if(bassBoost!=null)safe(()->bassBoost.setStrength((short)(bassStrength*10))); if(loudness!=null)safe(()->loudness.setTargetGain(Math.max(0,Math.round((outputGainDb+12)*250)))); updateDynamicsStages(); }

    private void changeBandCount(int n){if(bandCount==n)return; bandCount=n; try{createDynamics();renderEqControls();applyCurrentState();}catch(Throwable e){Toast.makeText(this,"Mode band ini tidak didukung firmware",Toast.LENGTH_LONG).show();}saveLastState();}
    private void applyFactoryPreset(int type){
        for(int i=0;i<eqGains.length;i++){float f=(float)(31.5*Math.pow(20000.0/31.5,i/20.0));float v=0;
            if(type==1){if(f>=250&&f<=4000)v=3; if(f<125)v=-2; if(f>8000)v=1;}
            if(type==2){if(f<200)v=5; else if(f<600)v=2; else if(f>5000)v=-1;}
            if(type==3){if(f<80)v=1.5f; if(f>=2000&&f<=8000)v=2.5f; if(f>12000)v=1;}
            if(type==4){if(f<100)v=-2; if(f>=250&&f<=5000)v=3; if(f>8000)v=1;}
            eqGains[i]=v;
        }
        for(int i=0;i<peqGain.length;i++)peqGain[i]=0f;
        bassStrength=type==2?45:0; outputGainDb=type==0?0:-1; compRatio=type==4?3.5f:3f; compThreshold=type==4?-20f:-18f; limiterThreshold=-1; compressorEnabled=true; limiterEnabled=true;
        if(bassSeek!=null)bassSeek.setProgress(Math.round(bassStrength)); if(gainSeek!=null)gainSeek.setProgress(Math.round((outputGainDb+12)*10)); if(ratioSeek!=null)ratioSeek.setProgress(Math.round((compRatio-1)*10)); if(thresholdSeek!=null)thresholdSeek.setProgress(Math.round(compThreshold+40)); if(limiterSeek!=null)limiterSeek.setProgress(Math.round(limiterThreshold+12)); if(compressorToggle!=null)compressorToggle.setChecked(compressorEnabled); if(limiterToggle!=null)limiterToggle.setChecked(limiterEnabled);
        try{createDynamics();renderEqControls();renderParametricControls();applyCurrentState();}catch(Throwable ignored){} saveLastState(); Toast.makeText(this,"Preset "+presetNames[type]+" diterapkan",Toast.LENGTH_SHORT).show();
    }

    private void saveCustomPreset(){EditText input=new EditText(this);input.setHint("Nama preset, contoh: TikTok Jernih");new AlertDialog.Builder(this).setTitle("Simpan preset PRO").setView(input).setPositiveButton("Simpan",(d,w)->{String name=input.getText().toString().trim();if(name.isEmpty())name="Preset Saya";try{JSONObject j=stateToJson();prefs.edit().putString(CUSTOM_PREFIX+name,j.toString()).putString("last_custom",name).apply();Toast.makeText(this,"Preset disimpan: "+name,Toast.LENGTH_LONG).show();}catch(Exception e){toast("Gagal menyimpan preset");}}).setNegativeButton("Batal",null).show();}
    private void loadCustomPreset(){List<String> names=new ArrayList<>();Map<String,?> all=prefs.getAll();for(String k:all.keySet())if(k.startsWith(CUSTOM_PREFIX))names.add(k.substring(CUSTOM_PREFIX.length()));if(names.isEmpty()){toast("Belum ada preset tersimpan");return;}String[] a=names.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Pilih preset tersimpan").setItems(a,(d,which)->{try{JSONObject j=new JSONObject(prefs.getString(CUSTOM_PREFIX+a[which],"{}"));applyJson(j);toast("Preset dimuat: "+a[which]);}catch(Exception e){toast("Preset rusak atau tidak bisa dibaca");}}).setNegativeButton("Batal",null).show();}
    private void exportPreset(){try{String data=stateToJson().toString(2);Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"NILLAN AUDIO PRO EQ preset");i.putExtra(Intent.EXTRA_TEXT,data);startActivity(Intent.createChooser(i,"Ekspor preset NILLAN"));}catch(Exception e){toast("Tidak bisa mengekspor preset");}}
    private void importPreset(){EditText input=new EditText(this);input.setHint("Tempel teks JSON preset di sini");input.setMinLines(4);new AlertDialog.Builder(this).setTitle("Impor preset JSON").setView(input).setPositiveButton("Terapkan",(d,w)->{try{applyJson(new JSONObject(input.getText().toString()));toast("Preset diimpor");}catch(Exception e){toast("Format JSON tidak valid");}}).setNegativeButton("Batal",null).show();}
    private JSONObject stateToJson()throws Exception{JSONObject j=new JSONObject();JSONArray a=new JSONArray();for(float v:eqGains)a.put(v);j.put("version",3);j.put("bandCount",bandCount);j.put("eqGains",a);JSONArray fa=new JSONArray(),ga=new JSONArray(),qa=new JSONArray();for(float v:peqFreq)fa.put(v);for(float v:peqGain)ga.put(v);for(float v:peqQ)qa.put(v);j.put("peqFreq",fa);j.put("peqGain",ga);j.put("peqQ",qa);j.put("parametricEnabled",parametricEnabled);j.put("bass",bassStrength);j.put("gain",outputGainDb);j.put("ratio",compRatio);j.put("compThreshold",compThreshold);j.put("limiterThreshold",limiterThreshold);j.put("compressorEnabled",compressorEnabled);j.put("limiterEnabled",limiterEnabled);return j;}
    private void applyJson(JSONObject j)throws Exception{
        JSONArray a=j.getJSONArray("eqGains");
        for(int i=0;i<eqGains.length;i++)eqGains[i]=(float)a.optDouble(i,0);
        JSONArray fa=j.optJSONArray("peqFreq"),ga=j.optJSONArray("peqGain"),qa=j.optJSONArray("peqQ");
        if(fa!=null)for(int i=0;i<peqFreq.length;i++)peqFreq[i]=(float)fa.optDouble(i,peqFreq[i]);
        if(ga!=null)for(int i=0;i<peqGain.length;i++)peqGain[i]=(float)ga.optDouble(i,peqGain[i]);
        if(qa!=null)for(int i=0;i<peqQ.length;i++)peqQ[i]=(float)qa.optDouble(i,peqQ[i]);
        parametricEnabled=j.optBoolean("parametricEnabled",true);
        bandCount=j.optInt("bandCount",21)==10?10:21;
        bassStrength=(float)j.optDouble("bass",0); outputGainDb=(float)j.optDouble("gain",0);
        compRatio=(float)j.optDouble("ratio",3); compThreshold=(float)j.optDouble("compThreshold",-18);
        limiterThreshold=(float)j.optDouble("limiterThreshold",-1);
        compressorEnabled=j.optBoolean("compressorEnabled",true); limiterEnabled=j.optBoolean("limiterEnabled",true);
        if(bassSeek!=null)bassSeek.setProgress(Math.round(bassStrength));
        if(gainSeek!=null)gainSeek.setProgress(Math.round((outputGainDb+12)*10));
        if(ratioSeek!=null)ratioSeek.setProgress(Math.round((compRatio-1)*10));
        if(thresholdSeek!=null)thresholdSeek.setProgress(Math.round(compThreshold+40));
        if(limiterSeek!=null)limiterSeek.setProgress(Math.round(limiterThreshold+12));
        if(compressorToggle!=null)compressorToggle.setChecked(compressorEnabled);
        if(limiterToggle!=null)limiterToggle.setChecked(limiterEnabled);
        if(peqToggle!=null)peqToggle.setChecked(parametricEnabled);
        createDynamics(); renderEqControls(); renderParametricControls(); applyCurrentState(); saveLastState();
    }
    private void loadLastState(){
        try{
            String raw=prefs.getString("last_state",null); if(raw==null)return;
            JSONObject j=new JSONObject(raw); JSONArray a=j.optJSONArray("eqGains");
            if(a!=null)for(int i=0;i<eqGains.length;i++)eqGains[i]=(float)a.optDouble(i,0);
            JSONArray fa=j.optJSONArray("peqFreq"),ga=j.optJSONArray("peqGain"),qa=j.optJSONArray("peqQ");
            if(fa!=null)for(int i=0;i<peqFreq.length;i++)peqFreq[i]=(float)fa.optDouble(i,peqFreq[i]);
            if(ga!=null)for(int i=0;i<peqGain.length;i++)peqGain[i]=(float)ga.optDouble(i,peqGain[i]);
            if(qa!=null)for(int i=0;i<peqQ.length;i++)peqQ[i]=(float)qa.optDouble(i,peqQ[i]);
            parametricEnabled=j.optBoolean("parametricEnabled",true);
            bandCount=j.optInt("bandCount",21)==10?10:21; bassStrength=(float)j.optDouble("bass",0);
            outputGainDb=(float)j.optDouble("gain",0); compRatio=(float)j.optDouble("ratio",3);
            compThreshold=(float)j.optDouble("compThreshold",-18); limiterThreshold=(float)j.optDouble("limiterThreshold",-1);
            compressorEnabled=j.optBoolean("compressorEnabled",true); limiterEnabled=j.optBoolean("limiterEnabled",true);
        }catch(Exception ignored){}
    }
    private void saveLastState(){try{prefs.edit().putString("last_state",stateToJson().toString()).apply();}catch(Exception ignored){}}

    private void sectionTitle(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(accent);t.setTextSize(15);t.setTypeface(null,1);t.setPadding(0,dp(18),0,dp(6));root.addView(t,lp());}
    private TextView smallLabel(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.LTGRAY);t.setTextSize(12);return t;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setPadding(0,dp(2),0,dp(2));return l;}
    private LinearLayout.LayoutParams lp(){return new LinearLayout.LayoutParams(-1,-2);}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private SeekBar slider(int max){SeekBar s=new SeekBar(this);s.setMax(max);return s;}
    private interface ProgressAction{void run(int p);}
    private SeekBar.OnSeekBarChangeListener listener(ProgressAction action){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){if(u&&!applyingPreset)action.run(p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
    private void addButton(LinearLayout row,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(10);b.setAllCaps(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(b,p);b.setOnClickListener(v->action.run());}
    private void thresholdToggleLabel(String text){TextView t=smallLabel(text+" (dB)");root.addView(t,lp());}
    private void safe(Runnable r){try{r.run();}catch(Throwable ignored){}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();safe(()->{if(dynamics!=null)dynamics.release();});safe(()->{if(bassBoost!=null)bassBoost.release();});safe(()->{if(loudness!=null)loudness.release();});}
}
