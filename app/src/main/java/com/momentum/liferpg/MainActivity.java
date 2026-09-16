package com.momentum.liferpg;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(6,10,18), SURFACE=Color.rgb(13,20,31), SURFACE2=Color.rgb(19,29,44);
    final int TEXT=Color.rgb(245,248,255), MUTED=Color.rgb(137,151,172), BLUE=Color.rgb(38,160,255);
    final int CYAN=Color.rgb(62,221,255), PURPLE=Color.rgb(149,91,255), GREEN=Color.rgb(67,214,145);
    final int ORANGE=Color.rgb(255,157,63), RED=Color.rgb(255,92,113);
    LinearLayout content, nav;
    SharedPreferences prefs;
    ArrayList<QuestCatalog.Quest> all;
    ArrayList<QuestCatalog.Quest> daily=new ArrayList<>();
    HashSet<String> interests=new HashSet<>();
    HashSet<Integer> completedQuestIds=new HashSet<>();
    int xp, completed, streak;
    String playerName;
    CountDownTimer timer;
    long remaining=25*60*1000L;
    boolean running=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
        prefs=getSharedPreferences("momentum2",MODE_PRIVATE);
        xp=prefs.getInt("xp",120); completed=prefs.getInt("completed",0); streak=prefs.getInt("streak",1);
        playerName=prefs.getString("name","Player");
        loadInterests(); loadCompleted(); all=QuestCatalog.buildAll(); makeDaily(); showHome();
        if(!prefs.getBoolean("onboarded",false)) new Handler().postDelayed(()->showInterests(true),350);
    }

    void loadInterests(){String s=prefs.getString("interests","");if(s.length()>0)for(String x:s.split(","))if(x.length()>0)interests.add(x);}
    void saveInterests(){StringBuilder b=new StringBuilder();for(String x:interests){if(b.length()>0)b.append(",");b.append(x);}prefs.edit().putString("interests",b.toString()).putBoolean("onboarded",true).apply();}
    void loadCompleted(){String s=prefs.getString("completed_ids","");if(s.length()>0)for(String x:s.split(","))try{completedQuestIds.add(Integer.parseInt(x));}catch(Exception ignored){}}
    void saveStats(){StringBuilder b=new StringBuilder();for(Integer x:completedQuestIds){if(b.length()>0)b.append(",");b.append(x);}prefs.edit().putInt("xp",xp).putInt("completed",completed).putInt("streak",streak).putString("name",playerName).putString("completed_ids",b.toString()).apply();}
    void makeDaily(){daily.clear();ArrayList<QuestCatalog.Quest> pool=new ArrayList<>();for(QuestCatalog.Quest q:all)if(interests.isEmpty()||interests.contains(q.categoryId))pool.add(q);Collections.shuffle(pool,new Random(daySeed()));HashSet<String> used=new HashSet<>();for(QuestCatalog.Quest q:pool){if(!used.contains(q.categoryId)){daily.add(q);used.add(q.categoryId);}if(daily.size()>=5)break;}for(QuestCatalog.Quest q:pool){if(daily.size()>=5)break;if(!daily.contains(q))daily.add(q);}}
    long daySeed(){Calendar c=Calendar.getInstance();return c.get(Calendar.YEAR)*1000L+c.get(Calendar.DAY_OF_YEAR);}

    interface Fn{void run();}
    void shell(String current){
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(18),dp(10),dp(14),dp(8));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);top.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setPadding(dp(10),0,0,0);brand.addView(tv("MOMENTUM",18,TEXT,true));brand.addView(tv("REAL LIFE RPG",9,Color.rgb(104,185,255),true));top.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView badge=pill("LVL "+(xp/500+1),PURPLE);top.addView(badge,new LinearLayout.LayoutParams(dp(72),dp(34)));outer.addView(top);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(15),dp(8),dp(15),dp(96));sc.addView(content);outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(6),dp(6),dp(6),dp(7));nav.setBackgroundColor(Color.rgb(9,15,24));
        addNav("⌂","Головна","home".equals(current),()->showHome());addNav("⚔","Квести","catalog".equals(current),()->showCatalog(null));addNav("◉","Фокус","focus".equals(current),()->showFocus());addNav("◆","Профіль","profile".equals(current),()->showProfile());
        outer.addView(nav,new LinearLayout.LayoutParams(-1,dp(68)));setContentView(outer);
    }
    void addNav(String icon,String label,boolean active,Fn f){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);TextView i=tv(icon,19,active?CYAN:MUTED,true);i.setGravity(Gravity.CENTER);TextView l=tv(label,9,active?TEXT:MUTED,active);l.setGravity(Gravity.CENTER);x.addView(i);x.addView(l);if(active)x.setBackground(round(Color.rgb(17,41,59),14));nav.addView(x,new LinearLayout.LayoutParams(0,-1,1));x.setOnClickListener(v->f.run());}

    void showHome(){
        shell("home");LinearLayout hero=gradientCard(BLUE,PURPLE);hero.addView(tv("ТВОЯ КАМПАНІЯ",10,Color.rgb(215,238,255),true));hero.addView(tv("Стань сильнішою версією себе.",24,Color.WHITE,true));hero.addView(tv("5 персональних квестів на сьогодні • "+all.size()+" у каталозі",13,Color.rgb(225,234,247),false));
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(500);pb.setProgress(xp%500);if(Build.VERSION.SDK_INT>=21)pb.setProgressTintList(ColorStateList.valueOf(Color.WHITE));LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,dp(7));pl.setMargins(0,dp(16),0,dp(3));hero.addView(pb,pl);hero.addView(tv((xp%500)+" / 500 XP до Level "+(xp/500+2),10,Color.WHITE,true));content.addView(hero);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stat(stats,"🔥",String.valueOf(streak),"серія");stat(stats,"⚔",String.valueOf(completed),"закрито");stat(stats,"✦",String.valueOf(xp),"XP");content.addView(stats,lp(-1,dp(88),0,10,0,0));
        content.addView(section("Квести дня"));for(QuestCatalog.Quest q:daily)addQuestCard(q);
        LinearLayout banner=card();banner.setBackground(gradient(Color.rgb(14,37,53),Color.rgb(31,21,61),18));banner.addView(tv("🎯  Налаштуй свої інтереси",18,TEXT,true));banner.addView(tv("Вибери категорії — щоденні квести та рекомендації підлаштуються під тебе.",12,MUTED,false));Button pref=button("Обрати категорії",true);banner.addView(pref);pref.setOnClickListener(v->showInterests(false));content.addView(banner,lp(-1,-2,0,16,0,0));
        content.addView(section("Швидкий доступ"));LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);quick.addView(quickCard("⚔","Каталог\n"+all.size()+"+",()->showCatalog(null)),new LinearLayout.LayoutParams(0,dp(104),1));quick.addView(quickCard("◉","Focus\n25 хв",()->showFocus()),new LinearLayout.LayoutParams(0,dp(104),1));quick.addView(quickCard("🧠","Coach\nAI план",()->coach()),new LinearLayout.LayoutParams(0,dp(104),1));content.addView(quick);
    }

    void showCatalog(String initialCategory){
        shell("catalog");content.addView(tv("Каталог квестів",28,TEXT,true));content.addView(tv(all.size()+" квестів • 32 категорії • 3 рівні складності",13,MUTED,false));
        EditText search=input("Пошук квестів…");search.setSingleLine(true);content.addView(search,lp(-1,dp(50),0,12,0,0));
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);Button allBtn=smallButton("Усі");Button intsBtn=smallButton("Мої інтереси");Button randomBtn=smallButton("🎲 Випадковий");filters.addView(allBtn,new LinearLayout.LayoutParams(0,dp(42),1));filters.addView(intsBtn,new LinearLayout.LayoutParams(0,dp(42),1));filters.addView(randomBtn,new LinearLayout.LayoutParams(0,dp(42),1));content.addView(filters,lp(-1,-2,0,8,0,0));
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);TextView chipAll=chip("Усі",true);chips.addView(chipAll);for(QuestCatalog.Category c:QuestCatalog.CATEGORIES){TextView ch=chip(c.emoji+" "+c.title,false);chips.addView(ch);ch.setOnClickListener(v->renderQuestResults((LinearLayout)content.getTag(),search.getText().toString(),c.id,false));}hs.addView(chips);content.addView(hs,lp(-1,dp(48),0,5,0,10));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.setTag(results);content.addView(results);renderQuestResults(results,"",initialCategory,false);chipAll.setOnClickListener(v->renderQuestResults(results,search.getText().toString(),null,false));allBtn.setOnClickListener(v->renderQuestResults(results,search.getText().toString(),null,false));intsBtn.setOnClickListener(v->renderQuestResults(results,search.getText().toString(),null,true));randomBtn.setOnClickListener(v->showQuestDialog(all.get(new Random().nextInt(all.size()))));search.setOnEditorActionListener((v,a,e)->{renderQuestResults(results,search.getText().toString(),null,false);return true;});
    }
    void renderQuestResults(LinearLayout results,String query,String category,boolean onlyInterests){results.removeAllViews();String q=query==null?"":query.toLowerCase(Locale.ROOT).trim();int shown=0,total=0;for(QuestCatalog.Quest x:all){boolean ok=(category==null||x.categoryId.equals(category))&&(!onlyInterests||interests.contains(x.categoryId))&&(q.length()==0||x.title.toLowerCase(Locale.ROOT).contains(q)||x.categoryTitle.toLowerCase(Locale.ROOT).contains(q));if(ok){total++;if(shown<60){addQuestCardTo(results,x);shown++;}}}TextView info=tv("Знайдено: "+total+(total>60?" • показано перші 60":""),11,MUTED,false);results.addView(info,0);}
    void addQuestCard(QuestCatalog.Quest q){addQuestCardTo(content,q);}
    void addQuestCardTo(LinearLayout parent,QuestCatalog.Quest q){boolean done=completedQuestIds.contains(q.id);LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(11),dp(12),dp(11));TextView ico=pill(q.emoji,categoryColor(q.categoryId));ico.setTextSize(20);row.addView(ico,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(10),0,dp(6),0);txt.addView(tv(q.title,13,done?Color.rgb(107,141,130):TEXT,true));txt.addView(tv(q.categoryTitle+" • "+q.minutes+" хв • "+difficulty(q.difficulty),10,MUTED,false));row.addView(txt,new LinearLayout.LayoutParams(0,-2,1));row.addView(tv(done?"✓":"+"+q.xp,12,done?GREEN:CYAN,true));parent.addView(row,lp(-1,-2,0,0,0,8));row.setOnClickListener(v->showQuestDialog(q));}
    void showQuestDialog(QuestCatalog.Quest q){boolean done=completedQuestIds.contains(q.id);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(6),dp(20),0);box.addView(tv(q.emoji+"  "+q.categoryTitle,13,categoryColor(q.categoryId),true));box.addView(tv(q.title,21,TEXT,true));box.addView(tv(q.minutes+" хв • "+difficulty(q.difficulty)+" • +"+q.xp+" XP",12,MUTED,false));AlertDialog d=new AlertDialog.Builder(this).setView(box).setPositiveButton(done?"Вже виконано":"Завершити квест",(a,w)->{if(!done){completedQuestIds.add(q.id);xp+=q.xp;completed++;saveStats();Toast.makeText(this,"+"+q.xp+" XP",Toast.LENGTH_SHORT).show();showHome();}}).setNegativeButton("Закрити",null).create();d.show();}

    void showInterests(boolean onboarding){
        shell("profile");content.addView(tv(onboarding?"Налаштуй Momentum":"Твої інтереси",28,TEXT,true));content.addView(tv("Обери категорії, які тобі реально цікаві. Можна змінити будь-коли.",13,MUTED,false));TextView count=tv(interests.size()+" обрано",11,CYAN,true);content.addView(count,lp(-1,-2,0,10,0,4));
        for(QuestCatalog.Category c:QuestCatalog.CATEGORIES){boolean active=interests.contains(c.id);LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView ico=pill(c.emoji,categoryColor(c.id));ico.setTextSize(20);row.addView(ico,new LinearLayout.LayoutParams(dp(46),dp(46)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);tx.addView(tv(c.title,14,TEXT,true));tx.addView(tv(c.bases.length*5+" квестів",10,MUTED,false));row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));CheckBox cb=new CheckBox(this);cb.setChecked(active);row.addView(cb);content.addView(row,lp(-1,-2,0,0,0,7));View.OnClickListener click=v->{if(interests.contains(c.id))interests.remove(c.id);else interests.add(c.id);cb.setChecked(interests.contains(c.id));count.setText(interests.size()+" обрано");};row.setOnClickListener(click);cb.setOnClickListener(click);}
        Button save=button(onboarding?"Почати кампанію":"Зберегти інтереси",true);content.addView(save);save.setOnClickListener(v->{saveInterests();makeDaily();showHome();});
    }

    void showFocus(){shell("focus");content.addView(tv("Focus Arena",28,TEXT,true));content.addView(tv("Таймер глибокої роботи. Завершення = +120 XP.",13,MUTED,false));LinearLayout arena=gradientCard(Color.rgb(11,49,67),Color.rgb(41,23,80));arena.setGravity(Gravity.CENTER_HORIZONTAL);TextView ring=tv(time(remaining),58,TEXT,true);ring.setGravity(Gravity.CENTER);arena.addView(ring,lp(-1,dp(100),0,12,0,0));TextView state=tv(running?"Фокус активний":"Готовий до спринту",12,Color.rgb(184,220,242),false);state.setGravity(Gravity.CENTER);arena.addView(state);Button start=button(running?"Пауза":"Почати 25 хв",true);arena.addView(start);content.addView(arena);LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);int[] mins={15,25,45};String[] names={"15\nSprint","25\nStudy","45\nDeep"};for(int i=0;i<3;i++){final int m=mins[i];Button b=smallButton(names[i]);modes.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));b.setOnClickListener(v->{if(timer!=null)timer.cancel();running=false;remaining=m*60000L;ring.setText(time(remaining));state.setText(m+" хв режим");start.setText("Почати");});}content.addView(modes,lp(-1,-2,0,12,0,0));start.setOnClickListener(v->{if(running){timer.cancel();running=false;start.setText("Продовжити");state.setText("Пауза");return;}running=true;start.setText("Пауза");state.setText("Не перемикайся. Просто зроби.");timer=new CountDownTimer(remaining,1000){public void onTick(long m){remaining=m;ring.setText(time(m));}public void onFinish(){running=false;remaining=25*60000L;xp+=120;completed++;saveStats();ring.setText("25:00");state.setText("Сесію завершено • +120 XP");start.setText("Ще раунд");}}.start();});}

    void showProfile(){shell("profile");content.addView(tv("Character",28,TEXT,true));content.addView(tv("Твій реальний прогрес у цифрах.",13,MUTED,false));LinearLayout hero=gradientCard(PURPLE,BLUE);hero.setGravity(Gravity.CENTER_HORIZONTAL);TextView av=pill("◆",Color.argb(100,255,255,255));av.setTextSize(32);hero.addView(av,new LinearLayout.LayoutParams(dp(68),dp(68)));hero.addView(tv(playerName,22,Color.WHITE,true));hero.addView(tv("LEVEL "+(xp/500+1)+" • "+xp+" XP",12,Color.WHITE,true));content.addView(hero);LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stat(stats,"⚔",String.valueOf(completed),"квестів");stat(stats,"🔥",String.valueOf(streak),"серія");stat(stats,"♥",String.valueOf(interests.size()),"інтересів");content.addView(stats,lp(-1,dp(90),0,10,0,0));Button interestsBtn=button("🎯 Мої інтереси та категорії",false);content.addView(interestsBtn);interestsBtn.setOnClickListener(v->showInterests(false));Button catalog=button("⚔ Відкрити повний каталог "+all.size()+"+",false);content.addView(catalog);catalog.setOnClickListener(v->showCatalog(null));LinearLayout nameCard=card();nameCard.addView(tv("Ім’я персонажа",12,MUTED,true));EditText n=input("Ім’я");n.setText(playerName);nameCard.addView(n);Button save=button("Зберегти",true);nameCard.addView(save);content.addView(nameCard,lp(-1,-2,0,14,0,0));save.setOnClickListener(v->{playerName=n.getText().toString().trim();if(playerName.length()==0)playerName="Player";saveStats();Toast.makeText(this,"Збережено",Toast.LENGTH_SHORT).show();});content.addView(section("Досягнення"));achievement("🌱 Перший крок","1 квест",1);achievement("🔥 Розігрів","10 квестів",10);achievement("⚙ Машина","50 квестів",50);achievement("👑 Легенда","250 квестів",250);}
    void achievement(String name,String desc,int need){LinearLayout c=card();c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);LinearLayout t=new LinearLayout(this);t.setOrientation(LinearLayout.VERTICAL);t.addView(tv(name,14,TEXT,true));t.addView(tv(desc,10,MUTED,false));c.addView(t,new LinearLayout.LayoutParams(0,-2,1));c.addView(tv(completed>=need?"UNLOCKED":completed+"/"+need,10,completed>=need?GREEN:MUTED,true));content.addView(c,lp(-1,-2,0,0,0,7));}

    void coach(){final EditText e=input("Що хочеш покращити? Напр. дисципліну, навчання, сон…");e.setMinLines(3);new AlertDialog.Builder(this).setTitle("AI Game Master").setView(e).setPositiveButton("Створити план",(d,w)->{String q=e.getText().toString().toLowerCase(Locale.ROOT);String cat="productivity";if(q.contains("сон"))cat="sleep";else if(q.contains("спорт")||q.contains("ваг"))cat="fitness";else if(q.contains("грош")||q.contains("фін"))cat="finance";else if(q.contains("вчит")||q.contains("навчан"))cat="learning";else if(q.contains("віднос")||q.contains("стос"))cat="relationships";final String targetCat=cat;ArrayList<String> plan=new ArrayList<>();for(QuestCatalog.Quest x:all)if(x.categoryId.equals(targetCat)&&plan.size()<3)plan.add("• "+x.title+" (+"+x.xp+" XP)");new AlertDialog.Builder(this).setTitle("Твій міні-квестлайн").setMessage(join(plan,"\n")).setPositiveButton("Відкрити категорію",(a,b)->showCatalog(targetCat)).setNegativeButton("OK",null).show();}).setNegativeButton("Закрити",null).show();}

    String join(ArrayList<String> x,String sep){StringBuilder b=new StringBuilder();for(String s:x){if(b.length()>0)b.append(sep);b.append(s);}return b.toString();}
    String difficulty(int d){return d==1?"Легкий":d==2?"Середній":"Складний";}
    int categoryColor(String id){int h=Math.abs(id.hashCode());int[] colors={BLUE,PURPLE,GREEN,ORANGE,RED,Color.rgb(45,198,189),Color.rgb(255,194,55),Color.rgb(215,76,180)};return colors[h%colors.length];}
    LinearLayout quickCard(String icon,String title,Fn fn){LinearLayout c=card();c.setGravity(Gravity.CENTER);TextView i=tv(icon,26,CYAN,true);i.setGravity(Gravity.CENTER);TextView t=tv(title,11,TEXT,true);t.setGravity(Gravity.CENTER);c.addView(i);c.addView(t);c.setOnClickListener(v->fn.run());return c;}
    void stat(LinearLayout p,String i,String v,String l){LinearLayout c=card();c.setGravity(Gravity.CENTER);TextView a=tv(i+"  "+v,17,TEXT,true);a.setGravity(Gravity.CENTER);TextView b=tv(l,9,MUTED,false);b.setGravity(Gravity.CENTER);c.addView(a);c.addView(b);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,-1,1);q.setMargins(dp(3),0,dp(3),0);p.addView(c,q);}
    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(14),dp(14),dp(14),dp(14));x.setBackground(round(SURFACE,18));return x;}
    LinearLayout gradientCard(int a,int b){LinearLayout x=card();x.setBackground(gradient(a,b,22));return x;}
    GradientDrawable gradient(int a,int b,int r){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(r));return g;}
    GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView section(String s){TextView x=tv(s,18,TEXT,true);x.setPadding(0,dp(20),0,dp(10));return x;}
    TextView tv(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setPadding(0,dp(3),0,dp(3));if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    TextView pill(String s,int c){TextView v=tv(s,11,TEXT,true);v.setGravity(Gravity.CENTER);v.setBackground(round(c,999));v.setPadding(dp(8),dp(4),dp(8),dp(4));return v;}
    TextView chip(String s,boolean active){TextView v=tv(s,11,active?TEXT:MUTED,true);v.setGravity(Gravity.CENTER);v.setBackground(round(active?Color.rgb(23,68,92):SURFACE2,999));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(38));p.setMargins(0,0,dp(7),0);v.setLayoutParams(p);v.setPadding(dp(14),0,dp(14),0);return v;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(94,111,132));e.setTextColor(TEXT);e.setTextSize(13);e.setPadding(dp(14),dp(11),dp(14),dp(11));e.setBackground(round(Color.rgb(10,16,25),14));return e;}
    Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(primary?gradient(BLUE,PURPLE,14):round(SURFACE2,14));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,dp(10),0,0);b.setLayoutParams(p);return b;}
    Button smallButton(String s){Button b=button(s,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);p.setMargins(dp(2),0,dp(2),0);b.setLayoutParams(p);return b;}
    LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    String time(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
}
