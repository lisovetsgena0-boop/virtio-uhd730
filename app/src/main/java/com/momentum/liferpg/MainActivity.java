package com.momentum.liferpg;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(9,11,16), CARD=Color.rgb(18,23,34), CARD2=Color.rgb(24,31,45);
    final int TEXT=Color.rgb(247,248,251), MUTED=Color.rgb(141,152,171), PURPLE=Color.rgb(139,108,255);
    final int GREEN=Color.rgb(72,213,151), YELLOW=Color.rgb(246,198,91);
    LinearLayout content, nav;
    SharedPreferences prefs;
    int xp, completed, streak;
    String name;
    boolean[] done=new boolean[4];
    String[] titles={"20 хвилин руху","25 хвилин глибокої роботи","5 хвилин порядку","Записати 3 думки"};
    String[] cats={"BODY","FOCUS","LIFE","MIND"};
    String[] icons={"🏃","⚡","🧹","🧠"};
    int[] qxp={80,100,50,60};
    CountDownTimer timer;
    long remaining=25*60*1000L;
    boolean running=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
        prefs=getSharedPreferences("momentum",MODE_PRIVATE);
        xp=prefs.getInt("xp",120);completed=prefs.getInt("completed",0);streak=prefs.getInt("streak",1);name=prefs.getString("name","Player");
        for(int i=0;i<4;i++)done[i]=prefs.getBoolean("q"+i,false);
        showToday();
    }

    void save(){SharedPreferences.Editor e=prefs.edit().putInt("xp",xp).putInt("completed",completed).putInt("streak",streak).putString("name",name);for(int i=0;i<4;i++)e.putBoolean("q"+i,done[i]);e.apply();}

    void shell(){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(16),dp(7),dp(12),dp(7));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.addView(tv("MOMENTUM",18,TEXT,true));brand.addView(tv("REAL LIFE RPG",9,MUTED,true));top.addView(brand,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView pro=tv("PRO ✦",11,Color.rgb(215,205,255),true);pro.setGravity(Gravity.CENTER);pro.setBackground(round(Color.rgb(42,33,71),999));top.addView(pro,new LinearLayout.LayoutParams(dp(72),dp(36)));pro.setOnClickListener(v->showPaywall());
        outer.addView(top);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(15),dp(10),dp(15),dp(90));scroll.addView(content);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setBackgroundColor(Color.rgb(17,23,34));
        addNav("⚔\nToday",()->showToday());addNav("⚡\nFocus",()->showFocus());addNav("✎\nLog",()->showJournal());addNav("◎\nSquads",()->showSquads());addNav("◆\nYou",()->showProfile());
        outer.addView(nav,new LinearLayout.LayoutParams(-1,dp(62)));setContentView(outer);
    }
    interface Fn{void run();}
    void addNav(String t,Fn fn){TextView v=tv(t,10,TEXT,true);v.setGravity(Gravity.CENTER);nav.addView(v,new LinearLayout.LayoutParams(0,-1,1));v.setOnClickListener(x->fn.run());}

    void showToday(){shell();int level=xp/500+1,in=xp%500,count=0;for(boolean d:done)if(d)count++;
        LinearLayout hero=card();hero.addView(tv("LEVEL "+level,10,Color.rgb(190,179,255),true));hero.addView(tv(name+", твій день — це карта.",24,TEXT,true));hero.addView(tv("Закривай квести. Набирай XP. Не ламай серію.",13,MUTED,false));
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(500);pb.setProgress(in);if(Build.VERSION.SDK_INT>=21)pb.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(9));pp.setMargins(0,dp(15),0,dp(4));hero.addView(pb,pp);hero.addView(tv(in+" / 500 XP",10,MUTED,true));content.addView(hero);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);addStat(stats,"🔥",String.valueOf(streak),"серія");addStat(stats,"⚔",String.valueOf(completed),"квестів");addStat(stats,"✓",(count*25)+"%","сьогодні");content.addView(stats,margin(-1,dp(84),0,10,0,0));
        content.addView(section("Квести дня"));
        for(int i=0;i<4;i++){final int k=i;LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView ico=tv(done[i]?"✓":icons[i],22,done[i]?GREEN:TEXT,true);ico.setGravity(Gravity.CENTER);row.addView(ico,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.addView(tv(titles[i],14,done[i]?Color.rgb(120,150,136):TEXT,true));txt.addView(tv(cats[i],10,MUTED,true));row.addView(txt,new LinearLayout.LayoutParams(0,-2,1));row.addView(tv("+"+qxp[i]+" XP",12,done[i]?GREEN:YELLOW,true));content.addView(row,margin(-1,-2,0,0,0,8));row.setOnClickListener(v->{if(!done[k]){done[k]=true;xp+=qxp[k];completed++;save();Toast.makeText(this,"+"+qxp[k]+" XP",Toast.LENGTH_SHORT).show();showToday();}});}
        Button reset=button("Скинути квести дня",false);content.addView(reset);reset.setOnClickListener(v->{for(int i=0;i<4;i++)done[i]=false;save();showToday();});
        LinearLayout coach=card();coach.setBackground(round(Color.rgb(24,20,43),18));coach.addView(tv("AI GAME MASTER",10,Color.rgb(190,179,255),true));coach.addView(tv("Застряг? Розклади проблему на квест.",20,TEXT,true));coach.addView(tv("Локальний coach працює навіть без інтернету.",13,MUTED,false));Button ask=button("Запитати Coach →",true);coach.addView(ask);ask.setOnClickListener(v->coach());content.addView(coach,margin(-1,-2,0,16,0,0));
    }

    void showFocus(){shell();content.addView(tv("Focus Arena",28,TEXT,true));content.addView(tv("Завершена сесія = +120 XP.",14,MUTED,false));LinearLayout c=card();c.setGravity(Gravity.CENTER_HORIZONTAL);TextView clock=tv(time(remaining),58,TEXT,false);clock.setGravity(Gravity.CENTER);c.addView(clock);EditText mins=input("25");mins.setInputType(2);c.addView(mins);Button start=button(running?"Пауза":"Старт фокусу",true);c.addView(start);Button reset=button("Скинути",false);c.addView(reset);content.addView(c,margin(-1,-2,0,16,0,0));
        start.setOnClickListener(v->{if(running){if(timer!=null)timer.cancel();running=false;start.setText("Старт фокусу");return;}if(remaining<=0)remaining=num(mins.getText().toString(),25)*60000L;running=true;start.setText("Пауза");timer=new CountDownTimer(remaining,1000){public void onTick(long m){remaining=m;clock.setText(time(m));}public void onFinish(){running=false;remaining=25*60000L;xp+=120;completed++;save();clock.setText("25:00");new AlertDialog.Builder(MainActivity.this).setTitle("Фокус завершено").setMessage("+120 XP").setPositiveButton("OK",null).show();}}.start();});
        reset.setOnClickListener(v->{if(timer!=null)timer.cancel();running=false;remaining=num(mins.getText().toString(),25)*60000L;clock.setText(time(remaining));start.setText("Старт фокусу");});
        content.addView(section("Швидкі режими"));String[] ls={"Sprint — 15 хв","Deep Work — 45 хв","Study — 25 хв","Create — 60 хв"};int[] mm={15,45,25,60};for(int i=0;i<4;i++){final int m=mm[i];Button b=button(ls[i],false);content.addView(b);b.setOnClickListener(v->{if(timer!=null)timer.cancel();running=false;remaining=m*60000L;mins.setText(String.valueOf(m));clock.setText(time(remaining));start.setText("Старт фокусу");});}
    }

    void showJournal(){shell();content.addView(tv("Mind Log",28,TEXT,true));content.addView(tv("Короткий запис = +20 XP.",14,MUTED,false));EditText e=input("Що сьогодні відбулося?");e.setMinLines(5);e.setGravity(Gravity.TOP);content.addView(e,margin(-1,-2,0,14,0,0));Button b=button("Зберегти +20 XP",true);content.addView(b);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);loadJournal(list);b.setOnClickListener(v->{String t=e.getText().toString().trim();if(t.length()==0)return;String old=prefs.getString("journal","");String d=new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date());prefs.edit().putString("journal",d+"|"+t+"\n§\n"+old).apply();xp+=20;save();e.setText("");loadJournal(list);});}
    void loadJournal(LinearLayout list){list.removeAllViews();list.addView(section("Останні записи"));String all=prefs.getString("journal","");if(all.length()==0){list.addView(tv("Тут з’являться твої записи.",13,MUTED,false));return;}for(String x:all.split("\\n§\\n")){String[] p=x.split("\\|",2);if(p.length<2)continue;LinearLayout c=card();c.addView(tv(p[0],10,Color.rgb(190,179,255),true));c.addView(tv(p[1],14,TEXT,false));list.addView(c,margin(-1,-2,0,0,0,8));}}

    void showSquads(){shell();content.addView(tv("Squads",28,TEXT,true));content.addView(tv("Соціальні челенджі — наступний серверний модуль.",14,MUTED,false));LinearLayout c=card();c.addView(tv("LIVE CHALLENGE",10,Color.rgb(190,179,255),true));c.addView(tv("7 днів без нульових днів",22,TEXT,true));c.addView(tv("Виконай хоча б 1 квест щодня.",13,MUTED,false));Button j=button("Приєднатися",true);c.addView(j);content.addView(c);j.setOnClickListener(v->Toast.makeText(this,"MVP: офлайн-режим. Online Squads буде через backend.",Toast.LENGTH_LONG).show());}

    void showProfile(){shell();content.addView(tv("Character",28,TEXT,true));content.addView(tv("Ти — головний персонаж.",14,MUTED,false));LinearLayout c=card();EditText n=input(name);n.setText(name);c.addView(n);Button sv=button("Зберегти ім’я",true);c.addView(sv);content.addView(c);sv.setOnClickListener(v->{name=n.getText().toString().trim();if(name.length()==0)name="Player";save();Toast.makeText(this,"Збережено",Toast.LENGTH_SHORT).show();});content.addView(section("Статистика"));LinearLayout st=card();st.addView(tv("Level "+(xp/500+1),20,TEXT,true));st.addView(tv(xp+" total XP",14,MUTED,false));st.addView(tv(completed+" виконаних квестів",14,MUTED,false));content.addView(st);Button pro=button("Відкрити Momentum PRO",true);content.addView(pro);pro.setOnClickListener(v->showPaywall());}

    void coach(){EditText e=input("Напр. постійно відкладаю навчання");e.setMinLines(3);new AlertDialog.Builder(this).setTitle("AI Game Master").setView(e).setPositiveButton("Побудувати план",(d,w)->{String q=e.getText().toString().toLowerCase(Locale.ROOT),a="Зменш ціль до дії на 10–20 хвилин, постав конкретний час і зроби перший крок до появи мотивації.";if(q.contains("втом")||q.contains("сил"))a="Мінімальна перемога: вода, 10 хвилин руху, одна важлива справа і ранній відбій.";if(q.contains("грош")||q.contains("зароб"))a="Спринт: одна пропозиція або контакт щодня, 30 хвилин на навичку і щотижневий підсумок цифр.";if(q.contains("вчит")||q.contains("навчан"))a="Запусти 25 хв фокусу, постав один конкретний результат і після сесії запиши 3 тези.";new AlertDialog.Builder(this).setTitle("Наступний квест").setMessage(a).setPositiveButton("OK",null).show();}).setNegativeButton("Закрити",null).show();}
    void showPaywall(){new AlertDialog.Builder(this).setTitle("✦ Momentum PRO").setMessage("AI Game Master\nПерсональні кампанії\nРозширена статистика\nPrivate Squads\nCloud sync\n\n$7.99 / місяць\n$49.99 / рік").setPositiveButton("7 днів безкоштовно",(d,w)->Toast.makeText(this,"Billing підключається перед Play Store release",Toast.LENGTH_LONG).show()).setNegativeButton("Закрити",null).show();}

    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(14),dp(14),dp(14));x.setBackground(round(CARD,18));return x;}
    TextView section(String s){TextView x=tv(s,17,TEXT,true);x.setPadding(0,dp(20),0,dp(10));return x;}
    TextView tv(String s,int sp,int color,boolean bold){TextView x=new TextView(this);x.setText(s);x.setTextSize(sp);x.setTextColor(color);x.setPadding(0,dp(3),0,dp(3));if(bold)x.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return x;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(104,114,133));e.setTextColor(TEXT);e.setTextSize(14);e.setPadding(dp(13),dp(11),dp(13),dp(11));e.setBackground(round(Color.rgb(13,17,25),14));return e;}
    Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextColor(TEXT);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(primary?PURPLE:CARD2,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(10),0,0);b.setLayoutParams(lp);return b;}
    void addStat(LinearLayout p,String i,String v,String l){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setBackground(round(CARD,16));c.addView(tv(i,18,TEXT,false));c.addView(tv(v,18,TEXT,true));c.addView(tv(l,10,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(3),0,dp(3),0);p.addView(c,lp);}
    GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}int num(String s,int d){try{return Math.max(1,Integer.parseInt(s));}catch(Exception e){return d;}}
    String time(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
}
