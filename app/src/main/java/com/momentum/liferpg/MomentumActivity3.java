package com.momentum.liferpg;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import com.android.billingclient.api.ProductDetails;
import java.util.*;

public class MomentumActivity3 extends Activity implements BillingManager.Listener {
    final int BG=Color.rgb(5,8,16), PANEL=Color.rgb(13,19,32), PANEL2=Color.rgb(20,29,48), TEXT=Color.rgb(244,248,255), MUTED=Color.rgb(143,159,185);
    final int CYAN=Color.rgb(61,225,255), BLUE=Color.rgb(43,126,255), PURPLE=Color.rgb(157,86,255), PINK=Color.rgb(255,78,190), GREEN=Color.rgb(74,226,155), GOLD=Color.rgb(255,195,72);
    SharedPreferences prefs;
    ArrayList<QuestCatalog.Quest> all;
    HashSet<Integer> done=new HashSet<>();
    LinearLayout content, nav;
    BillingManager billing;
    Map<String,ProductDetails> products=new HashMap<>();
    boolean pro=false, tablet=false;
    int xp, completed, streak;
    CountDownTimer timer;
    long remaining=25*60*1000L;
    boolean running=false;

    static final String[] RANKS={"Spark","Vector","Vanguard","Riftwalker","Nova","Zenith","Mythic"};
    static final String[] RANK_EMOJI={"✦","➤","◆","◈","✺","♛","✧"};
    static final String[] RANK_PERKS={"Швидкий старт","+1 reroll щодня","Елітні квести","Подвійний Focus бонус","Nova challenges","Zenith streak shield","Mythic aura"};
    static final int[] RANK_XP={0,1200,3000,6000,10000,16000,24000,34000};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=21){getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
        tablet=getResources().getConfiguration().smallestScreenWidthDp>=600;
        prefs=getSharedPreferences("momentum3",MODE_PRIVATE);
        xp=prefs.getInt("xp",120);completed=prefs.getInt("completed",0);streak=prefs.getInt("streak",1);pro=prefs.getBoolean("pro",false);
        loadDone();all=QuestCatalog.buildAll();
        billing=new BillingManager(this,this);billing.connect();showHome();
    }

    @Override public void onDestroy(){super.onDestroy();if(timer!=null)timer.cancel();if(billing!=null)billing.close();}
    @Override public void onReady(Map<String,ProductDetails> p){products=p;if("plus".equals(prefs.getString("screen","")))showPlus();}
    @Override public void onEntitlement(boolean active){pro=active;prefs.edit().putBoolean("pro",active).apply();}

    void loadDone(){String s=prefs.getString("done","");if(s.length()>0)for(String x:s.split(","))try{done.add(Integer.parseInt(x));}catch(Exception ignored){}}
    void save(){StringBuilder b=new StringBuilder();for(Integer i:done){if(b.length()>0)b.append(',');b.append(i);}prefs.edit().putInt("xp",xp).putInt("completed",completed).putInt("streak",streak).putString("done",b.toString()).apply();}

    interface Fn{void run();}
    void shell(String current){
        prefs.edit().putString("screen",current).apply();
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(BG);
        DepthView depth=new DepthView(this);root.addView(depth,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout vertical=new LinearLayout(this);vertical.setOrientation(LinearLayout.VERTICAL);root.addView(vertical,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(tablet?32:16),dp(12),dp(tablet?32:16),dp(8));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_launcher);top.addView(logo,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.setPadding(dp(10),0,0,0);title.addView(txt("MOMENTUM",tablet?21:18,TEXT,true));title.addView(txt(pro?"PLUS • REAL LIFE RPG":"REAL LIFE RPG",10,pro?GOLD:CYAN,true));top.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        TextView rank=pill(RANK_EMOJI[rankIndex()]+" "+RANKS[rankIndex()],rankColor(rankIndex()));top.addView(rank,new LinearLayout.LayoutParams(dp(tablet?150:116),dp(36)));vertical.addView(top);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);FrameLayout stage=new FrameLayout(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(tablet?24:14),dp(8),dp(tablet?24:14),dp(110));
        int max=tablet?dp(980):-1;FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(max,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);stage.addView(content,cp);sc.addView(stage);vertical.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(tablet?18:5),dp(7),dp(tablet?18:5),dp(7));nav.setBackground(round(Color.argb(245,8,13,24),20));
        addNav("⌂","Головна","home".equals(current),()->showHome());addNav("⚔","Квести","quests".equals(current),()->showQuests());addNav("◉","Фокус","focus".equals(current),()->showFocus());addNav("♛","Ранги","ranks".equals(current),()->showRanks());addNav("✦","Plus","plus".equals(current),()->showPlus());
        vertical.addView(nav,new LinearLayout.LayoutParams(-1,dp(tablet?76:70)));setContentView(root);animateIn(content);
    }

    void addNav(String icon,String label,boolean active,Fn fn){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setPadding(dp(3),dp(2),dp(3),dp(2));x.addView(centerTxt(icon,tablet?21:18,active?CYAN:MUTED,true));x.addView(centerTxt(label,tablet?10:8,active?TEXT:MUTED,active));if(active){x.setBackground(gradient(Color.rgb(21,50,78),Color.rgb(49,28,81),16));x.setTranslationZ(dp(8));}nav.addView(x,new LinearLayout.LayoutParams(0,-1,1));x.setOnClickListener(v->{press3d(x);fn.run();});}

    void showHome(){
        shell("home");
        LinearLayout hero=glassCard();hero.setBackground(gradient(BLUE,PURPLE,28));hero.setPadding(dp(tablet?28:20),dp(22),dp(tablet?28:20),dp(22));tilt3d(hero);
        hero.addView(txt("YOUR REAL-LIFE CAMPAIGN",10,Color.rgb(215,238,255),true));hero.addView(txt("Прокачуй життя як персонажа.",tablet?34:27,Color.WHITE,true));hero.addView(txt(all.size()+"+ квестів • адаптивні місії • живий ранг",13,Color.rgb(224,235,252),false));
        LinearLayout rankRow=new LinearLayout(this);rankRow.setGravity(Gravity.CENTER_VERTICAL);rankRow.setPadding(0,dp(17),0,0);TextView emblem=pill(RANK_EMOJI[rankIndex()],rankColor(rankIndex()));emblem.setTextSize(tablet?28:23);rankRow.addView(emblem,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout rt=new LinearLayout(this);rt.setOrientation(LinearLayout.VERTICAL);rt.setPadding(dp(12),0,0,0);rt.addView(txt(RANKS[rankIndex()]+" • Division "+division(),17,Color.WHITE,true));rt.addView(txt(RANK_PERKS[rankIndex()],11,Color.rgb(227,233,255),false));rankRow.addView(rt,new LinearLayout.LayoutParams(0,-2,1));hero.addView(rankRow);hero.addView(rankProgress());content.addView(hero,lp(-1,-2,0,0,0,14));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stat(stats,"✦",String.valueOf(xp),"XP");stat(stats,"⚔",String.valueOf(completed),"закрито");stat(stats,"🔥",String.valueOf(streak),"серія");content.addView(stats,lp(-1,dp(tablet?105:90),0,0,0,16));
        content.addView(section("Квести дня"));addDailyGrid();
        content.addView(section("Portal deck"));GridLayout grid=new GridLayout(this);grid.setColumnCount(tablet?4:2);grid.setUseDefaultMargins(false);addPortal(grid,"◉","Focus Arena","+120 XP",()->showFocus());addPortal(grid,"♛","Rank Path",RANKS[rankIndex()],()->showRanks());addPortal(grid,"⚔","Quest Vault",all.size()+" місій",()->showQuests());addPortal(grid,"✦","Momentum Plus",pro?"Активовано":"Преміум",()->showPlus());content.addView(grid);
    }

    void addDailyGrid(){GridLayout g=new GridLayout(this);g.setColumnCount(tablet?2:1);ArrayList<QuestCatalog.Quest> pool=new ArrayList<>(all);Collections.shuffle(pool,new Random(daySeed()));for(int i=0;i<Math.min(6,pool.size());i++){QuestCatalog.Quest q=pool.get(i);View c=questCard(q);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=tablet?dp(455):-1;p.height=-2;p.setMargins(0,0,tablet?dp(10):0,dp(10));g.addView(c,p);}content.addView(g);}
    long daySeed(){Calendar c=Calendar.getInstance();return c.get(Calendar.YEAR)*1000L+c.get(Calendar.DAY_OF_YEAR);}

    void showQuests(){
        shell("quests");content.addView(txt("Quest Vault",tablet?34:29,TEXT,true));content.addView(txt(all.size()+" місій у 32 категоріях. Натисни квест, щоб забрати XP.",13,MUTED,false));
        final EditText search=input("Пошук квесту або категорії…");content.addView(search,lp(-1,dp(52),0,14,0,10));
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);TextView allChip=chip("Усі",true);chips.addView(allChip);for(QuestCatalog.Category c:QuestCatalog.CATEGORIES)chips.addView(chip(c.emoji+" "+c.title,false));hs.addView(chips);content.addView(hs,lp(-1,dp(48),0,0,0,10));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);content.addView(results);renderQuest(results,"");search.setOnEditorActionListener((v,a,e)->{renderQuest(results,search.getText().toString());return true;});allChip.setOnClickListener(v->renderQuest(results,search.getText().toString()));
    }
    void renderQuest(LinearLayout box,String query){box.removeAllViews();String s=query==null?"":query.toLowerCase(Locale.ROOT).trim();int shown=0,total=0;for(QuestCatalog.Quest q:all){boolean ok=s.length()==0||q.title.toLowerCase(Locale.ROOT).contains(s)||q.categoryTitle.toLowerCase(Locale.ROOT).contains(s);if(ok){total++;if(shown<80){box.addView(questCard(q),lp(-1,-2,0,0,0,9));shown++;}}}box.addView(txt("Знайдено: "+total+(total>80?" • показано 80":""),11,MUTED,false),0);}
    View questCard(final QuestCatalog.Quest q){boolean isDone=done.contains(q.id);LinearLayout c=glassCard();c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(13),dp(12),dp(13),dp(12));TextView ico=pill(q.emoji,categoryColor(q.categoryId));ico.setTextSize(21);c.addView(ico,new LinearLayout.LayoutParams(dp(50),dp(50)));LinearLayout mid=new LinearLayout(this);mid.setOrientation(LinearLayout.VERTICAL);mid.setPadding(dp(11),0,dp(8),0);mid.addView(txt(q.title,13,isDone?Color.rgb(112,146,134):TEXT,true));mid.addView(txt(q.categoryTitle+" • "+q.minutes+" хв • "+difficulty(q.difficulty),10,MUTED,false));c.addView(mid,new LinearLayout.LayoutParams(0,-2,1));c.addView(txt(isDone?"✓":"+"+q.xp,12,isDone?GREEN:CYAN,true));tilt3d(c);c.setOnClickListener(v->questDialog(q));return c;}
    void questDialog(final QuestCatalog.Quest q){boolean isDone=done.contains(q.id);LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(22),dp(8),dp(22),0);b.addView(txt(q.emoji+"  "+q.categoryTitle,13,categoryColor(q.categoryId),true));b.addView(txt(q.title,21,TEXT,true));b.addView(txt(q.minutes+" хв • "+difficulty(q.difficulty)+" • +"+q.xp+" XP",12,MUTED,false));new AlertDialog.Builder(this).setView(b).setPositiveButton(isDone?"Виконано":"Завершити",(d,w)->{if(!isDone){done.add(q.id);xp+=q.xp;completed++;save();Toast.makeText(this,"+"+q.xp+" XP ✦",Toast.LENGTH_SHORT).show();showHome();}}).setNegativeButton("Закрити",null).show();}

    void showFocus(){
        shell("focus");content.addView(txt("Focus Arena",tablet?34:29,TEXT,true));content.addView(txt("Глибока робота перетворюється на XP.",13,MUTED,false));LinearLayout arena=glassCard();arena.setBackground(gradient(Color.rgb(9,49,69),Color.rgb(61,24,92),28));arena.setGravity(Gravity.CENTER_HORIZONTAL);tilt3d(arena);TextView clock=centerTxt(time(remaining),tablet?74:60,Color.WHITE,true);arena.addView(clock,lp(-1,dp(tablet?125:105),0,10,0,0));TextView state=centerTxt(running?"FOCUS LOCKED":"READY FOR IMMERSION",11,CYAN,true);arena.addView(state);Button go=button(running?"Пауза":"Почати 25 хв",true);arena.addView(go);content.addView(arena,lp(-1,-2,0,14,0,12));go.setOnClickListener(v->{if(running){if(timer!=null)timer.cancel();running=false;showFocus();}else{running=true;timer=new CountDownTimer(remaining,1000){public void onTick(long m){remaining=m;clock.setText(time(m));}public void onFinish(){running=false;remaining=25*60*1000L;xp+=120;save();Toast.makeText(MomentumActivity3.this,"Focus clear: +120 XP",Toast.LENGTH_LONG).show();showHome();}}.start();go.setText("Пауза");state.setText("FOCUS LOCKED");}});
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);for(final int min:new int[]{15,25,45}){Button m=smallButton(min+" хв");modes.addView(m,new LinearLayout.LayoutParams(0,dp(46),1));m.setOnClickListener(v->{if(timer!=null)timer.cancel();running=false;remaining=min*60*1000L;showFocus();});}content.addView(modes);
    }

    void showRanks(){
        shell("ranks");content.addView(txt("Order of Momentum",tablet?34:29,TEXT,true));content.addView(txt("Не просто рівні: кожен ранг відкриває новий стиль гри та перк.",13,MUTED,false));
        LinearLayout current=glassCard();current.setBackground(gradient(rankColor(rankIndex()),Color.rgb(38,28,78),28));current.setGravity(Gravity.CENTER_HORIZONTAL);TextView sig=centerTxt(RANK_EMOJI[rankIndex()],tablet?62:52,Color.WHITE,true);current.addView(sig);current.addView(centerTxt(RANKS[rankIndex()]+" • Division "+division(),tablet?29:24,Color.WHITE,true));current.addView(centerTxt(RANK_PERKS[rankIndex()],12,Color.rgb(233,238,255),false));current.addView(rankProgress());tilt3d(current);content.addView(current,lp(-1,-2,0,14,0,16));
        content.addView(section("Rank constellation"));for(int i=0;i<RANKS.length;i++){boolean unlocked=rankIndex()>=i;LinearLayout row=glassCard();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);TextView e=pill(RANK_EMOJI[i],unlocked?rankColor(i):Color.rgb(62,69,83));e.setTextSize(22);row.addView(e,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);info.addView(txt(RANKS[i],15,unlocked?TEXT:MUTED,true));info.addView(txt(RANK_PERKS[i]+" • від "+RANK_XP[i]+" XP",10,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(txt(unlocked?"UNLOCKED":"LOCKED",9,unlocked?GREEN:MUTED,true));content.addView(row,lp(-1,-2,0,0,0,9));}
    }

    void showPlus(){
        shell("plus");content.addView(txt("Momentum Plus",tablet?34:29,TEXT,true));content.addView(txt("Преміум-шлях для тих, хто хоче більше прогресії та персоналізації.",13,MUTED,false));LinearLayout hero=glassCard();hero.setBackground(gradient(Color.rgb(109,56,255),Color.rgb(255,65,177),28));hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.addView(centerTxt("✦",tablet?58:48,Color.WHITE,true));hero.addView(centerTxt(pro?"PLUS ACTIVE":"UNLOCK YOUR NEXT FORM",tablet?27:22,Color.WHITE,true));hero.addView(centerTxt("Елітні квести • рангові бусти • додаткові теми • future AI coach",12,Color.rgb(244,232,255),false));tilt3d(hero);content.addView(hero,lp(-1,-2,0,14,0,14));
        addBenefit("⚔","Elite questlines","Спеціальні серії місій і складніші нагороди");addBenefit("♛","Rank boost","Додаткові рангові випробування та косметичні аури");addBenefit("◉","Focus modes","Розширені режими 15/25/45/60 хв і статистика");
        if(pro){Button active=button("Momentum Plus активовано ✓",false);active.setEnabled(false);content.addView(active);Button restore=button("Відновити покупки",false);content.addView(restore);restore.setOnClickListener(v->billing.restore());return;}
        String mp=price(BillingManager.MONTHLY,"ціна з Play");String yp=price(BillingManager.YEARLY,"ціна з Play");Button monthly=button("Місяць • "+mp,true);Button yearly=button("Рік • "+yp,false);content.addView(monthly,lp(-1,dp(54),0,12,0,8));content.addView(yearly,lp(-1,dp(54),0,0,0,8));monthly.setOnClickListener(v->billing.buy(BillingManager.MONTHLY));yearly.setOnClickListener(v->billing.buy(BillingManager.YEARLY));Button restore=smallButton("Відновити покупки");content.addView(restore);restore.setOnClickListener(v->billing.restore());content.addView(txt("Підписки обробляє Google Play. Дані банківської картки не зберігаються в Momentum.",10,MUTED,false),lp(-1,-2,0,12,0,0));
    }
    String price(String id,String fallback){try{ProductDetails p=products.get(id);if(p==null||p.getSubscriptionOfferDetails()==null||p.getSubscriptionOfferDetails().isEmpty())return fallback;return p.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();}catch(Exception e){return fallback;}}
    void addBenefit(String e,String t,String s){LinearLayout r=glassCard();r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);TextView i=pill(e,PURPLE);i.setTextSize(20);r.addView(i,new LinearLayout.LayoutParams(dp(50),dp(50)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(12),0,0,0);tx.addView(txt(t,14,TEXT,true));tx.addView(txt(s,10,MUTED,false));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));content.addView(r,lp(-1,-2,0,0,0,9));}

    int rankIndex(){for(int i=RANKS.length-1;i>=0;i--)if(xp>=RANK_XP[i])return i;return 0;}
    int division(){int i=rankIndex();int start=RANK_XP[i],end=RANK_XP[Math.min(i+1,RANK_XP.length-1)];if(i==RANKS.length-1)return 1;float p=(xp-start)/(float)Math.max(1,end-start);return p<.34?3:p<.67?2:1;}
    View rankProgress(){int i=rankIndex();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int start=RANK_XP[i],end=RANK_XP[Math.min(i+1,RANK_XP.length-1)];ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,end-start));pb.setProgress(Math.min(end-start,xp-start));if(Build.VERSION.SDK_INT>=21)pb.setProgressTintList(ColorStateList.valueOf(Color.WHITE));box.addView(pb,lp(-1,dp(7),0,12,0,3));box.addView(txt(i==RANKS.length-1?xp+" XP • MAX RANK":xp+" / "+end+" XP до "+RANKS[i+1],10,Color.WHITE,true));return box;}
    int rankColor(int i){int[] c={CYAN,BLUE,Color.rgb(72,204,180),PURPLE,Color.rgb(255,91,173),GOLD,Color.rgb(220,225,255)};return c[Math.max(0,Math.min(i,c.length-1))];}

    LinearLayout glassCard(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(15),dp(14),dp(15),dp(14));x.setBackground(gradient(Color.argb(236,22,30,48),Color.argb(236,12,18,31),22));if(Build.VERSION.SDK_INT>=21){x.setElevation(dp(5));x.setTranslationZ(dp(2));}return x;}
    TextView txt(String s,float sp,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(android.graphics.Typeface.DEFAULT,bold?1:0);v.setLineSpacing(0,1.08f);return v;}
    TextView centerTxt(String s,float sp,int color,boolean bold){TextView v=txt(s,sp,color,bold);v.setGravity(Gravity.CENTER);return v;}
    TextView section(String s){TextView v=txt(s,16,TEXT,true);v.setPadding(0,dp(8),0,dp(10));return v;}
    TextView pill(String s,int color){TextView v=centerTxt(s,11,Color.WHITE,true);v.setBackground(round(color,16));v.setPadding(dp(9),0,dp(9),0);return v;}
    TextView chip(String s,boolean active){TextView v=centerTxt(s,11,active?Color.WHITE:MUTED,true);v.setPadding(dp(13),0,dp(13),0);v.setBackground(round(active?Color.rgb(35,73,109):PANEL2,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(38));p.setMargins(0,0,dp(7),0);v.setLayoutParams(p);return v;}
    Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setTypeface(null,1);b.setBackground(gradient(primary?BLUE:PANEL2,primary?PURPLE:Color.rgb(30,39,58),16));return b;}
    Button smallButton(String s){Button b=button(s,false);b.setTextSize(10);return b;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(13);e.setSingleLine(true);e.setPadding(dp(14),0,dp(14),0);e.setBackground(round(PANEL2,16));return e;}
    void stat(LinearLayout row,String e,String n,String l){LinearLayout c=glassCard();c.setGravity(Gravity.CENTER);c.setPadding(dp(4),dp(8),dp(4),dp(8));c.addView(centerTxt(e+"  "+n,tablet?18:15,TEXT,true));c.addView(centerTxt(l,9,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(3),0,dp(3),0);row.addView(c,p);}
    void addPortal(GridLayout g,String e,String t,String s,Fn f){LinearLayout c=glassCard();c.setGravity(Gravity.CENTER);c.addView(centerTxt(e,tablet?30:25,CYAN,true));c.addView(centerTxt(t,13,TEXT,true));c.addView(centerTxt(s,9,MUTED,false));tilt3d(c);c.setOnClickListener(v->{press3d(c);f.run();});GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=tablet?dp(225):dp(160);p.height=dp(tablet?118:108);p.setMargins(dp(4),dp(4),dp(4),dp(4));g.addView(c,p);}
    GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable gradient(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(radius));return g;}
    LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    int categoryColor(String id){int h=Math.abs(id.hashCode());return Color.rgb(60+(h%120),80+((h/7)%120),110+((h/17)%110));}
    String difficulty(int d){return d<=1?"Easy":d==2?"Medium":"Hard";}
    String time(long ms){long s=ms/1000;return String.format(Locale.US,"%02d:%02d",s/60,s%60);}

    void animateIn(View v){v.setAlpha(0f);v.setTranslationY(dp(18));v.animate().alpha(1f).translationY(0).setDuration(420).setInterpolator(new DecelerateInterpolator()).start();}
    void press3d(View v){v.animate().scaleX(.96f).scaleY(.96f).translationZ(0).setDuration(70).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).translationZ(dp(4)).setDuration(120).start()).start();}
    void tilt3d(final View v){if(Build.VERSION.SDK_INT<21)return;v.setCameraDistance(dp(900));v.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN||event.getAction()==MotionEvent.ACTION_MOVE){float nx=(event.getX()/Math.max(1f,view.getWidth())-.5f)*2f,ny=(event.getY()/Math.max(1f,view.getHeight())-.5f)*2f;view.setRotationY(nx*4.5f);view.setRotationX(-ny*3.5f);view.setTranslationZ(dp(10));}else{view.animate().rotationX(0).rotationY(0).translationZ(dp(2)).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();}return false;});}

    class DepthView extends View {
        Paint p=new Paint(1);Random r=new Random(7);float[] xs=new float[28],ys=new float[28],zs=new float[28];long last;
        DepthView(Context c){super(c);for(int i=0;i<xs.length;i++){xs[i]=r.nextFloat();ys[i]=r.nextFloat();zs[i]=.2f+r.nextFloat()*.8f;}p.setStrokeWidth(1);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);long now=System.currentTimeMillis();float dt=last==0?0:(now-last)/1000f;last=now;p.setColor(Color.rgb(11,17,30));c.drawColor(BG);for(int i=0;i<xs.length;i++){ys[i]+=dt*(.006f+.012f*zs[i]);if(ys[i]>1.04f)ys[i]=-.04f;float x=xs[i]*getWidth(),y=ys[i]*getHeight(),rad=1f+3f*zs[i];p.setColor(Color.argb((int)(35+65*zs[i]),80+(int)(80*zs[i]),100+(int)(80*zs[i]),210));c.drawCircle(x,y,rad,p);}postInvalidateDelayed(33);}
    }
}
