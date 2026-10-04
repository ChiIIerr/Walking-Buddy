package com.walkingbuddy.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.InputFilter;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Small, fully native Android app. No WebView, account, ads, analytics, or network permission. */
public final class MainActivity extends Activity {
    public static final int INK=Color.rgb(37,58,53), MUTED=Color.rgb(112,129,118), GREEN=Color.rgb(40,125,103);
    public static final int BG=Color.rgb(250,248,243), MINT=Color.rgb(233,241,231), LINE=Color.rgb(227,230,218), WHITE=Color.WHITE;
    private StateStore store;
    private GameState state;
    private LinearLayout root, content, nav;
    private ScrollView scroll;
    private int tab, setup, chosen;
    private boolean resumed;
    private PixelView hero;
    private TextView stepNumber, goalLabel, remainingLabel, leafLabel, moodLabel, healthLabel, streakLabel, trackLabel;
    private ProgressBar stepBar, healthBar, foodBar, waterBar, cleanBar;
    private TextView foodLabel, waterLabel, cleanLabel;
    private EditText setupName;
    private int setupGoal=6000;
    private String exportKind;
    private LocalDate renderedDay;
    private int renderedSteps, renderedUnlocked;
    private String restoredNickname;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
    private final Runnable clock=new Runnable(){@Override public void run(){if(resumed){refresh();handler.postDelayed(this,30000);}}};

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); store=StateStore.get(this);state=store.read();
        if(saved!=null){tab=saved.getInt("tab");setup=saved.getInt("setup");chosen=saved.getInt("chosen");setupGoal=saved.getInt("setupGoal",6000);exportKind=saved.getString("exportKind");restoredNickname=saved.getString("setupName");}
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Build.VERSION.SDK_INT>=27?BG:INK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        // Android 15 edge-to-edge: reserve actual system insets, including gesture navigation.
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets a=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(a.left,a.top,a.right,a.bottom);}
            else root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        if(Build.VERSION.SDK_INT<35)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(root);root.requestApplyInsets();render();
    }
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerLegacyReceiver() {
        // Before Android 13, a signature permission protects this app-only receiver.
        registerReceiver(updates, new IntentFilter(StepService.UPDATE), getPackageName()+".permission.INTERNAL", null);
    }
    @Override protected void onResume(){super.onResume();resumed=true;
        if(Build.VERSION.SDK_INT>=33)registerReceiver(updates,new IntentFilter(StepService.UPDATE),Context.RECEIVER_NOT_EXPORTED);
        else registerLegacyReceiver();
        state=store.read();
        if(state.tracking){if(StepService.allowed(this)&&StepService.available(this))startServiceSafely();else store.update(s->s.tracking=false);}
        refresh();handler.postDelayed(clock,30000);
    }
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(clock);unregisterReceiver(updates);store.save();super.onPause();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("tab",tab);b.putInt("setup",setup);b.putInt("chosen",chosen);b.putInt("setupGoal",setupGoal);b.putString("exportKind",exportKind);if(setupName!=null)b.putString("setupName",setupName.getText().toString());}
    private void refresh(){
        state=store.read();
        if(renderedDay!=null&&!renderedDay.equals(state.day)){render();return;}
        if(state.adopted&&setup!=2&&((tab==1&&renderedUnlocked!=state.unlockedCount())||(tab==2&&renderedSteps!=state.steps()))){int y=scroll.getScrollY();render();scroll.post(()->scroll.scrollTo(0,y));return;}
        if(tab==0&&state.adopted&&setup!=2&&stepNumber!=null){
            stepNumber.setText(n(state.steps()));goalLabel.setText("  of "+n(state.goal)+" steps");stepBar.setProgress(Math.min(100,state.steps()*100/state.goal));
            remainingLabel.setText(state.steps()>=state.goal?"Goal reached. Look at you go!":n(state.goal-state.steps())+" steps to a happier buddy");
            leafLabel.setText(" "+n(state.leaves));moodLabel.setText(state.mood());healthLabel.setText(state.health+"%");healthBar.setProgress(state.health);healthBar.setContentDescription("Buddy health "+state.health+" percent");
            streakLabel.setText(state.streak()+" day"+(state.streak()==1?"":"s")+" streak");
            foodBar.setProgress(state.food);waterBar.setProgress(state.water);cleanBar.setProgress(state.clean);
            foodLabel.setText(state.food+"%");waterLabel.setText(state.water+"%");cleanLabel.setText(state.clean+"%");
            trackLabel.setText(state.tracking?"Step tracking is on":"Enable step tracking");
            hero.setBuddy(state.buddy,state.theme,state.reduceMotion,state.health==0);
        }
    }
    private void render(){
        state=store.read();renderedDay=state.day;renderedSteps=state.steps();renderedUnlocked=state.unlockedCount();root.removeAllViews();hero=null;stepNumber=null;
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setVerticalScrollBarEnabled(false);
        content=column();content.setPadding(dp(22),dp(14),dp(22),dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!state.adopted||setup==2){if(setup==0)chooseScreen();else if(setup==1)personalizeScreen();else trackingScreen();return;}
        switch(tab){case 0:today();break;case 1:buddies();break;case 2:journal();break;default:settings();}
        bottomNav();
    }
    private void heading(String eyebrow,String title,String sub){
        TextView e=text(eyebrow,11,GREEN,true);e.setLetterSpacing(.13f);content.addView(e);gap(content,8);
        content.addView(text(title,29,INK,true));if(sub!=null){gap(content,7);content.addView(text(sub,14,MUTED,false));}gap(content,22);
    }
    private void chooseScreen(){
        LinearLayout brand=row();brand.addView(icon("leaf",GREEN,26));TextView mark=text("  Walking Buddy",18,INK,true);brand.addView(mark);content.addView(brand);gap(content,30);
        heading("YOUR NEXT ADVENTURE", "A little friend.\nA little more walking.","Turn your everyday steps into a happy life for a tiny buddy. Pick your first companion.");
        for(int r=0;r<2;r++){
            LinearLayout line=row();
            for(int c=0;c<2;c++){
                int i=r*2+c;LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setPadding(dp(8),dp(10),dp(8),dp(13));
                cell.setBackground(round(i==chosen?MINT:WHITE,20,i==chosen?GREEN:LINE));cell.setTag("starter_"+i);
                PixelView pixel=new PixelView(this,i,0,false);cell.addView(pixel,new LinearLayout.LayoutParams(-1,dp(105)));
                cell.addView(text(GameState.NAMES[i],17,INK,true));gap(cell,3);cell.addView(text(GameState.SPECIES[i],12,MUTED,false));
                cell.setContentDescription("Choose "+GameState.NAMES[i]+", "+GameState.SPECIES[i]);click(cell,()->{chosen=i;render();});
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(c==0?0:dp(6),0,c==0?dp(6):0,dp(12));line.addView(cell,lp);
            }
            content.addView(line);
        }
        gap(content,9);content.addView(button("Choose "+GameState.NAMES[chosen]+"  →","adopt_continue",()->{setup=1;render();},true));
        gap(content,15);TextView foot=text("A small daily habit. A friend for every step.",12,MUTED,false);foot.setGravity(Gravity.CENTER);content.addView(foot);
    }
    private void personalizeScreen(){
        backButton(()->{setup=0;render();});gap(content,18);heading("LET'S MAKE IT YOURS","Meet your walking buddy.",GameState.TRAITS[chosen]);
        FrameLayout frame=new FrameLayout(this);frame.setBackground(round(MINT,22,0));frame.setClipToOutline(true);
        frame.addView(new PixelView(this,chosen,0,true),new FrameLayout.LayoutParams(-1,dp(170)));content.addView(frame);gap(content,22);
        content.addView(text("What's their name?",15,INK,true));gap(content,8);setupName=input(restoredNickname==null?GameState.NAMES[chosen]:restoredNickname,false);restoredNickname=null;setupName.setTag("buddy_name");setupName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});content.addView(setupName);gap(content,20);
        content.addView(text("Your daily step goal",15,INK,true));gap(content,4);content.addView(text("Choose something that fits your day. You can change it anytime.",13,MUTED,false));gap(content,10);
        TextView value=text(n(setupGoal)+" steps",27,GREEN,true);value.setGravity(Gravity.CENTER);content.addView(value);gap(content,9);
        LinearLayout presets=row();for(int g:new int[]{3000,6000,10000}){
            TextView preset=chip(n(g),g==setupGoal?GREEN:MINT,g==setupGoal?WHITE:GREEN);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.setMargins(dp(3),0,dp(3),0);presets.addView(preset,lp);
            click(preset,()->{setupGoal=g;value.setText(n(g)+" steps");for(int j=0;j<presets.getChildCount();j++){TextView t=(TextView)presets.getChildAt(j);boolean active=t==preset;t.setBackground(round(active?GREEN:MINT,14,0));t.setTextColor(active?WHITE:GREEN);}});
        }content.addView(presets);gap(content,22);
        content.addView(button("Start our adventure  →","setup_adopt",()->{
            String name=setupName.getText().toString().trim();if(name.isEmpty()){setupName.setError("Give your buddy a name");return;}
            store.update(s->{s.setGoal(setupGoal);s.adopt(chosen,name,System.currentTimeMillis());});
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(setupName.getWindowToken(),0);
            setup=2;render();
        },true));
    }
    private void trackingScreen(){
        gap(content,34);LinearLayout circle=column();circle.setGravity(Gravity.CENTER);circle.addView(icon("foot",GREEN,62));content.addView(circle);gap(content,28);
        heading("ONE LAST LITTLE STEP","Let your steps count.","Your phone can count steps for you, even while Walking Buddy is in the background.");
        LinearLayout details=card();details.addView(text("Movement permission",17,INK,true));gap(details,7);details.addView(text("Allow physical activity access to use your phone's low-power step counter. A quiet notification keeps tracking active.",14,MUTED,false));gap(details,16);
        details.addView(text("Your walks stay with you",17,INK,true));gap(details,7);details.addView(text("Your progress is saved on this phone. No account, ads, or data uploads.",14,MUTED,false));content.addView(details);gap(content,28);
        content.addView(button("Enable step tracking","setup_track",()->{setup=0;tab=0;render();enableTracking();},true));gap(content,10);
        content.addView(button("I'll enter steps myself","setup_manual",()->{setup=0;tab=0;render();},false));
    }
    private void today(){
        LinearLayout brand=row();brand.setGravity(Gravity.CENTER_VERTICAL);brand.addView(icon("leaf",GREEN,25));
        TextView title=text("  Walking Buddy",21,INK,true);brand.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout leaves=row();leaves.setPadding(dp(12),dp(7),dp(12),dp(7));leaves.setGravity(Gravity.CENTER);leaves.setBackground(round(MINT,30,0));leaves.addView(icon("leaf",GREEN,17));leafLabel=text(" "+n(state.leaves),13,GREEN,true);leaves.addView(leafLabel);leaves.setContentDescription(state.leaves+" leaves for pet snacks");click(leaves,()->info("A little reward for every walk","Earn 1 leaf for every 500 steps and 3 bonus leaves when you reach your goal. A snack costs 1 leaf; water and cleaning are free. You begin with 4 leaves."));brand.addView(leaves);content.addView(brand);
        gap(content,18);TextView date=text(state.day.format(DateTimeFormatter.ofPattern("EEEE, MMMM d",Locale.getDefault())).toUpperCase(Locale.getDefault()),11,MUTED,true);date.setLetterSpacing(.12f);content.addView(date);gap(content,10);
        FrameLayout meadow=new FrameLayout(this);meadow.setBackground(round(MINT,22,0));meadow.setClipToOutline(true);hero=new PixelView(this,state.buddy,state.theme,true);hero.setBuddy(state.buddy,state.theme,state.reduceMotion,state.health==0);meadow.addView(hero,new FrameLayout.LayoutParams(-1,-1));
        TextView place=chip(new String[]{"MEADOW DAYS","GOLDEN HOUR","ALPINE AIR"}[state.theme],Color.argb(220,250,248,243),GREEN);place.setTextSize(10);place.setLetterSpacing(.1f);
        FrameLayout.LayoutParams placeLp=new FrameLayout.LayoutParams(-2,dp(30),Gravity.TOP|Gravity.START);placeLp.setMargins(dp(14),dp(14),0,0);meadow.addView(place,placeLp);
        LinearLayout friend=row();friend.setGravity(Gravity.CENTER_VERTICAL);TextView name=text(state.name,23,INK,true);friend.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout streak=row();streak.setGravity(Gravity.CENTER_VERTICAL);streak.addView(icon("fire",Color.rgb(181,123,69),16));streakLabel=text(" "+state.streak()+" day"+(state.streak()==1?"":"s")+" streak",11,INK,true);streak.addView(streakLabel);friend.addView(streak);
        content.addView(meadow,new LinearLayout.LayoutParams(-1,dp(222)));gap(content,10);content.addView(friend);gap(content,3);moodLabel=text(state.mood(),13,MUTED,false);content.addView(moodLabel);gap(content,18);
        LinearLayout steps=card();LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left=column();TextView label=text("TODAY'S STEPS",10,MUTED,true);label.setLetterSpacing(.12f);left.addView(label);gap(left,3);
        LinearLayout totals=row();totals.setGravity(Gravity.BOTTOM);stepNumber=text(n(state.steps()),36,INK,true);totals.addView(stepNumber);goalLabel=text("  of "+n(state.goal)+" steps",13,MUTED,false);goalLabel.setPadding(0,0,0,dp(6));totals.addView(goalLabel);left.addView(totals);top.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout foot=new FrameLayout(this);foot.setBackground(round(MINT,30,0));foot.addView(icon("foot",GREEN,25),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));top.addView(foot,new LinearLayout.LayoutParams(dp(50),dp(50)));steps.addView(top);gap(steps,8);
        stepBar=bar(GREEN,8);stepBar.setProgress(Math.min(100,state.steps()*100/state.goal));steps.addView(stepBar);gap(steps,9);remainingLabel=text(state.steps()>=state.goal?"Goal reached. Look at you go!":n(Math.max(0,state.goal-state.steps()))+" steps to a happier buddy",12,GREEN,true);steps.addView(remainingLabel);content.addView(steps);gap(content,16);
        LinearLayout careHeader=row();careHeader.setGravity(Gravity.CENTER_VERTICAL);careHeader.addView(text("A little daily care",16,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        careHeader.addView(icon("heart",GREEN,15));healthLabel=text(" "+state.health+"%",12,GREEN,true);careHeader.addView(healthLabel);content.addView(careHeader);gap(content,7);
        healthBar=bar(GREEN,3);healthBar.setProgress(state.health);healthBar.setContentDescription("Buddy health "+state.health+" percent");content.addView(healthBar);gap(content,12);
        LinearLayout needs=row();foodBar=bar(Color.rgb(205,156,90),5);waterBar=bar(Color.rgb(115,164,174),5);cleanBar=bar(Color.rgb(152,140,174),5);
        foodLabel=text(state.food+"%",10,MUTED,true);waterLabel=text(state.water+"%",10,MUTED,true);cleanLabel=text(state.clean+"%",10,MUTED,true);
        needs.addView(need("Food",foodBar,foodLabel,state.food),weighted(0,5));needs.addView(need("Water",waterBar,waterLabel,state.water),weighted(5,5));needs.addView(need("Clean",cleanBar,cleanLabel,state.clean),weighted(5,0));content.addView(needs);gap(content,14);
        LinearLayout actions=row();actions.addView(careButton("feed","food","Feed","1 leaf",Color.rgb(249,238,216)),weighted(0,5));actions.addView(careButton("water","water","Water","Free",Color.rgb(229,240,242)),weighted(5,5));actions.addView(careButton("clean","spark","Clean","Free",Color.rgb(240,235,245)),weighted(5,0));content.addView(actions);gap(content,18);
        if(state.health==0){LinearLayout memorial=card();memorial.addView(text("A new beginning",18,INK,true));gap(memorial,7);memorial.addView(text(state.name+"'s journey has ended after missed daily goals. Keep their memory and adopt a new friend.",13,MUTED,false));gap(memorial,12);memorial.addView(button("Adopt a new buddy","adopt_again",()->{setup=0;store.update(s->s.adopted=false);render();},true));content.addView(memorial);gap(content,14);}
        LinearLayout track=row();track.setGravity(Gravity.CENTER_VERTICAL);track.addView(icon("foot",GREEN,18));trackLabel=text(state.tracking?"  Step tracking is on":"  Enable step tracking",12,GREEN,true);track.addView(trackLabel,new LinearLayout.LayoutParams(0,-2,1));TextView manual=text("Enter steps",12,GREEN,true);manual.setPadding(dp(10),dp(10),0,dp(10));manual.setTag("manual_steps");click(manual,this::manualDialog);track.addView(manual);click(track,()->{if(state.tracking)info("Your steps are being counted","Keep your phone with you while you walk. Tracking starts from the moment you enable it and can be paused in Settings. Device power settings can interrupt background tracking; open the app again to resume. Steps taken before installation are not imported.");else enableTracking();});content.addView(track);
        gap(content,8);TextView help=text("Hit your goal to restore health. Missed days cost 20 health.\nTap your buddy for a little hello.",11,MUTED,false);help.setGravity(Gravity.CENTER);content.addView(help);
        click(hero,()->{hero.celebrate();toast(state.health==0?"A walk to remember.":state.name+" is cheering you on!");});
    }
    private void buddies(){
        heading("BETTER TOGETHER","Your little walking club.",state.unlockedCount()+" of 8 buddies ready to explore. Your lifetime steps unlock new friends.");
        LinearLayout milestone=card();LinearLayout mrow=row();mrow.addView(icon("foot",GREEN,26));TextView total=text("  "+n(state.lifetime())+" lifetime steps",18,INK,true);mrow.addView(total);milestone.addView(mrow);gap(milestone,9);
        int next=7;for(int i=4;i<8;i++)if(!state.unlocked(i)){next=i;break;}
        ProgressBar journey=bar(GREEN,7);journey.setProgress((int)Math.min(100,state.lifetime()*100/GameState.UNLOCKS[next]));milestone.addView(journey);gap(milestone,9);
        milestone.addView(text(state.unlockedCount()==8?"The whole club is here. Keep making memories.":n(Math.max(0,GameState.UNLOCKS[next]-state.lifetime()))+" more steps to meet "+GameState.NAMES[next],12,GREEN,true));content.addView(milestone);gap(content,20);
        for(int r=0;r<4;r++){
            LinearLayout line=row();for(int c=0;c<2;c++){
                int i=r*2+c;boolean ready=state.unlocked(i);boolean active=i==state.buddy;
                LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(10),dp(9),dp(10),dp(15));tile.setBackground(round(active?MINT:WHITE,20,active?GREEN:LINE));
                PixelView pet=new PixelView(this,i,0,false);pet.setBuddy(i,0,state.reduceMotion,false);if(!ready)pet.setAlpha(.48f);tile.addView(pet,new LinearLayout.LayoutParams(-1,dp(108)));
                tile.addView(text(GameState.NAMES[i],18,INK,true));gap(tile,2);tile.addView(text(GameState.SPECIES[i],12,MUTED,false));gap(tile,9);
                tile.addView(chip(active?"Walking with you":ready?"Ready to explore":n(GameState.UNLOCKS[i])+" steps",active?GREEN:ready?MINT:BG,active?WHITE:ready?GREEN:MUTED));
                tile.setTag("buddy_"+i);tile.setContentDescription(GameState.NAMES[i]+", "+GameState.SPECIES[i]+(ready?", unlocked":", unlock at "+GameState.UNLOCKS[i]+" steps"));click(tile,()->buddyDialog(i));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(c==0?0:dp(6),0,c==0?dp(6):0,dp(13));line.addView(tile,lp);
            }content.addView(line);
        }
        content.addView(text("Your first four buddies are always available. Switching companions keeps your health and care levels.",12,MUTED,false));
    }
    private void buddyDialog(int i){
        LinearLayout box=column();box.setPadding(dp(24),dp(12),dp(24),dp(12));box.setGravity(Gravity.CENTER);PixelView p=new PixelView(this,i,0,false);p.setBuddy(i,0,state.reduceMotion,false);box.addView(p,new LinearLayout.LayoutParams(-1,dp(150)));
        box.addView(text(GameState.TRAITS[i],15,INK,true));gap(box,10);
        boolean ready=state.unlocked(i);TextView explanation=text(ready?"A tiny companion for your everyday adventures.":"Walk "+n(Math.max(0,GameState.UNLOCKS[i]-state.lifetime()))+" more steps to invite "+GameState.NAMES[i]+" into your walking club.",14,MUTED,false);explanation.setGravity(Gravity.CENTER);box.addView(explanation);
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(GameState.NAMES[i]+" · "+GameState.SPECIES[i]).setView(box).setNegativeButton("Close",null);
        if(ready&&i!=state.buddy&&state.health>0)b.setPositiveButton("Walk with "+GameState.NAMES[i],(d,w)->{store.update(s->s.choose(i));tab=0;render();});b.show();
    }
    private void journal(){
        heading("ONE STEP AT A TIME","Look how far you've come.","Little walks add up to wonderful things.");
        LinearLayout metrics=row();metrics.addView(statCard(n(state.lifetime()),"Lifetime steps","foot"),weighted(0,6));metrics.addView(statCard(state.bestStreak()+" day"+(state.bestStreak()==1?"":"s"),"Best streak","fire"),weighted(6,0));content.addView(metrics);gap(content,14);
        LinearLayout week=card();LinearLayout wh=row();wh.addView(text("Your week in steps",17,INK,true),new LinearLayout.LayoutParams(0,-2,1));wh.addView(text("LAST 7 DAYS",10,MUTED,true));week.addView(wh);gap(week,14);
        week.addView(new WeekChart(this,state),new LinearLayout.LayoutParams(-1,dp(160)));gap(week,10);
        long weekSteps=state.steps();for(int i=1;i<7;i++){GameState.Day d=state.history.get(state.day.minusDays(i));if(d!=null)weekSteps+=d.steps();}
        week.addView(text(n(weekSteps)+" steps · about "+String.format(Locale.getDefault(),"%.1f",weekSteps*.00072)+" km",13,GREEN,true));gap(week,4);week.addView(text("Distance is an estimate using a 0.72 m stride.",11,MUTED,false));content.addView(week);gap(content,22);
        section("Little milestones");
        String[] titles={"First adventure","Finding your rhythm","A wonderful week","Trail explorer","The whole walking club","Care comes naturally"};
        String[] descriptions={"Reach your daily goal once","Build a 3-day streak","Build a 7-day streak","Walk 20,000 lifetime steps","Walk 100,000 lifetime steps","Care for your buddy 10 times"};
        boolean[] earned={state.goalDays()>=1,state.bestStreak()>=3,state.bestStreak()>=7,state.lifetime()>=20000,state.lifetime()>=100000,state.careCount>=10};
        String[] icons={"foot","fire","trophy","map","paw","heart"};
        for(int i=0;i<titles.length;i++){
            LinearLayout item=row();item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(14),dp(14),dp(14),dp(14));item.setBackground(round(earned[i]?MINT:WHITE,16,LINE));
            item.addView(icon(icons[i],earned[i]?GREEN:MUTED,25));LinearLayout words=column();words.setPadding(dp(13),0,dp(8),0);words.addView(text(titles[i],14,INK,true));gap(words,3);words.addView(text(descriptions[i],11,MUTED,false));item.addView(words,new LinearLayout.LayoutParams(0,-2,1));item.addView(icon(earned[i]?"check":"lock",earned[i]?GREEN:MUTED,18));content.addView(item);gap(content,7);
        }gap(content,17);section("Your personal bests");
        List<Map.Entry<LocalDate,GameState.Day>> records=new ArrayList<>(state.history.entrySet());
        if(state.steps()>0)records.add(new AbstractMap.SimpleEntry<>(state.day,new GameState.Day(state.sensorSteps,state.manualSteps,state.goal)));
        records.sort((a,b)->Integer.compare(b.getValue().steps(),a.getValue().steps()));
        if(records.isEmpty()){LinearLayout empty=card();empty.addView(text("Your first record is waiting.",15,INK,true));gap(empty,5);empty.addView(text("Take a walk or enter your steps to start your journal.",13,MUTED,false));content.addView(empty);}
        else for(int i=0;i<Math.min(5,records.size());i++){
            Map.Entry<LocalDate,GameState.Day> e=records.get(i);LinearLayout record=row();record.setGravity(Gravity.CENTER_VERTICAL);record.setPadding(dp(5),dp(11),dp(5),dp(11));
            TextView rank=chip(String.valueOf(i+1),i==0?MINT:BG,GREEN);record.addView(rank,new LinearLayout.LayoutParams(dp(33),dp(33)));
            LinearLayout words=column();words.setPadding(dp(12),0,0,0);words.addView(text(e.getKey().format(DateTimeFormatter.ofPattern("MMM d, yyyy",Locale.getDefault())),14,INK,true));gap(words,2);words.addView(text(e.getValue().hit()?"Daily goal reached":"A little progress",11,MUTED,false));record.addView(words,new LinearLayout.LayoutParams(0,-2,1));record.addView(text(n(e.getValue().steps()),17,GREEN,true));content.addView(record);
        }
        gap(content,20);section("Recent walks");
        if(state.history.isEmpty())content.addView(text("Completed days appear here tomorrow. Your journal keeps recorded days and the goal you chose for each one.",13,MUTED,false));
        else {
            int count=0;for(Map.Entry<LocalDate,GameState.Day> e:state.history.descendingMap().entrySet()){
                if(count++>=14)break;LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(0,dp(9),0,dp(9));
                line.addView(text(e.getKey().format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.getDefault())),13,INK,false),new LinearLayout.LayoutParams(0,-2,1));
                line.addView(text(n(e.getValue().steps())+" / "+n(e.getValue().goal),12,MUTED,false));line.addView(icon(e.getValue().hit()?"check":"foot",GREEN,18));content.addView(line);
            }
        }
        if(!state.memories.isEmpty()){gap(content,20);section("Friends we remember");for(String memory:state.memories){content.addView(text(memory,13,MUTED,false));gap(content,8);}}
        gap(content,22);content.addView(button("Share my progress","share_progress",this::share,false));
    }
    private void settings(){
        heading("YOUR WALK, YOUR WAY","Make yourself at home.","A few little things to make Walking Buddy yours.");
        LinearLayout profile=card();LinearLayout r=row();PixelView pet=new PixelView(this,state.buddy,0,false);pet.setBuddy(state.buddy,0,state.reduceMotion,false);r.addView(pet,new LinearLayout.LayoutParams(dp(65),dp(65)));
        LinearLayout words=column();words.setGravity(Gravity.CENTER_VERTICAL);words.setPadding(dp(13),0,0,0);words.addView(text(state.name,20,INK,true));gap(words,5);words.addView(text(GameState.SPECIES[state.buddy]+" · your walking companion",12,MUTED,false));r.addView(words,new LinearLayout.LayoutParams(0,dp(65),1));profile.addView(r);content.addView(profile);gap(content,22);
        section("Daily rhythm");
        option("foot","Daily step goal",n(state.goal)+" steps","edit_goal",this::goalDialog);
        option("paw","Buddy's nickname",state.name,"edit_name",this::renameDialog);
        option("map","Walking scenery",new String[]{"Meadow days","Golden hour","Alpine air"}[state.theme],"edit_scenery",()->new AlertDialog.Builder(this).setTitle("Choose your scenery").setSingleChoiceItems(new String[]{"Meadow days","Golden hour","Alpine air"},state.theme,(d,w)->{store.update(s->s.theme=w);d.dismiss();render();}).setNegativeButton("Close",null).show());
        gap(content,22);section("Step tracking");
        option("foot",state.tracking?"Pause automatic tracking":"Enable automatic tracking",state.tracking?"Counting steps in the background":StepService.available(this)?"Use your phone's step counter":"No step counter on this phone","toggle_tracking",()->{if(state.tracking){store.update(s->{s.tracking=false;s.lastSensor=-1;});stopService(new Intent(this,StepService.class));render();}else enableTracking();});
        option("plus","Enter today's steps","Add steps from another tracker","settings_manual",this::manualDialog);
        LinearLayout note=column();note.setPadding(dp(6),dp(12),dp(6),0);note.addView(text("Tracking starts when enabled. Keep your phone with you. Battery-saving settings and force-stopping the app can interrupt counting. Reopen Walking Buddy to resume.",12,MUTED,false));content.addView(note);
        gap(content,22);section("Comfort & progress");
        option("moon","Reduce animation",state.reduceMotion?"On · still sprites":"Off · animated buddies","reduce_motion",()->{store.update(s->s.reduceMotion=!s.reduceMotion);render();});
        option("chart","Export walking history","Save a CSV of your recorded days","export_csv",()->export("csv"));
        option("leaf","Back up my buddy","Save all progress to a file","export_backup",()->export("backup"));
        option("back","Restore a backup","Bring back a saved Walking Buddy file","restore_backup",this::restore);
        gap(content,22);section("Good to know");
        option("heart","How to keep a happy buddy","Steps, leaves, and little acts of care","how_to",()->info("A happy little life","Reach your daily goal to restore 15 health and earn 3 bonus leaves. Every 500 steps earns another leaf. Miss a daily goal and your buddy loses 20 health. At zero health, their journey ends and you can adopt a new friend.\n\nFood, water, and cleanliness slowly decline. Snacks cost 1 leaf; water and cleaning are free. Care actions have a one-minute cooldown.\n\nThe first four companions are available immediately. Lifetime steps unlock the frog, panda, penguin, and dragon. Switching an unlocked buddy keeps health and care levels. Your journal celebrates your personal bests."));
        option("lock","Privacy & original art","Your data stays on this phone","privacy",()->info("Private by design","Walking Buddy stores steps, goals, care levels, and buddy details locally on this phone. It has no network permission, accounts, ads, subscriptions, or analytics. Sharing and file export happen only when you choose them. Uninstalling removes local progress, so save a backup first.\n\nAll pixel pets, scenery, and icons were created for this app. Walking Buddy is an independent app inspired by virtual-pet step trackers and is not affiliated with Step Pals. Art is licensed CC0; source is MIT.\n\nVersion 1.0.0 · Android 8.0 or later."));
        option("back","Reset all progress","Start fresh on this phone","reset_progress",()->new AlertDialog.Builder(this).setTitle("Start a new adventure?").setMessage("This erases your steps, buddies, and journal from this phone. Save a backup first if you'd like to keep them.").setNegativeButton("Keep my progress",null).setPositiveButton("Reset progress",(d,w)->{stopService(new Intent(this,StepService.class));store.reset();state=store.read();tab=0;setup=0;render();}).show());
        gap(content,26);TextView footer=text("WALKING BUDDY  1.0.0\nMade for the little walks that matter.",11,MUTED,false);footer.setGravity(Gravity.CENTER);footer.setLineSpacing(dp(7),1);content.addView(footer);
    }
    private void bottomNav(){
        nav=row();nav.setPadding(dp(12),dp(8),dp(12),dp(7));nav.setBackgroundColor(BG);String[] labels={"Today","Buddies","Journal","Settings"};String[] icons={"home","paw","chart","settings"};
        for(int i=0;i<4;i++){
            final int selected=i;LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setPadding(0,dp(7),0,dp(7));cell.setBackground(round(i==tab?MINT:BG,18,0));
            cell.addView(icon(icons[i],i==tab?GREEN:MUTED,22));gap(cell,4);TextView caption=text(labels[i],10,i==tab?GREEN:MUTED,i==tab);caption.setGravity(Gravity.CENTER);cell.addView(caption);cell.setTag("tab_"+i);
            cell.setContentDescription(labels[i]+(i==tab?", selected":""));cell.setSelected(i==tab);click(cell,()->{tab=selected;render();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(61),1);lp.setMargins(dp(3),0,dp(3),0);nav.addView(cell,lp);
        }
        View border=new View(this);border.setBackgroundColor(LINE);root.addView(border,new LinearLayout.LayoutParams(-1,dp(1)));root.addView(nav);
    }
    private void enableTracking(){
        if(!StepService.available(this)){info("This phone needs a little help","This device doesn't have a hardware step counter. You can still care for your buddy and use every part of the app by entering steps from another tracker.");return;}
        if(!StepService.allowed(this)){
            new AlertDialog.Builder(this).setTitle("Count your steps automatically?").setMessage("Walking Buddy needs physical activity permission to read your phone's step counter. Your steps stay on this phone. A quiet notification lets counting continue in the background.")
                .setNegativeButton("Use manual entry",null).setPositiveButton("Allow step counting",(d,w)->requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},11)).show();
        }else{startTracking();requestNotification();}
    }
    private void startTracking(){
        store.update(s->{if(!s.tracking)s.lastSensor=-1;s.tracking=true;});startServiceSafely();render();
    }
    private void startServiceSafely(){
        try{startForegroundService(new Intent(this,StepService.class));}
        catch(IllegalStateException|SecurityException e){store.update(s->s.tracking=false);toast("Tracking couldn't start. Try enabling it again from Settings.");}
    }
    private void requestNotification(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==11){if(StepService.allowed(this)){startTracking();requestNotification();}else{toast("You can enter steps manually anytime.");render();}}
    }
    private void manualDialog(){
        LinearLayout box=column();box.setPadding(dp(24),dp(10),dp(24),dp(8));box.addView(text("Enter only steps that aren't already counted by this app. This replaces today's manual entry, so you can correct it later.",14,MUTED,false));gap(box,12);
        box.addView(text("Phone sensor: "+n(state.sensorSteps)+" steps",13,GREEN,true));gap(box,12);EditText entry=input(String.valueOf(state.manualSteps),true);entry.setTag("manual_input");box.addView(entry);gap(box,6);box.addView(text("Manual and sensor steps both count toward goals and rewards.",11,MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Today's manual steps").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save steps",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("manual_save");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{int count=Integer.parseInt(entry.getText().toString());store.update(s->s.setManual(count));dialog.dismiss();render();if(hero!=null)hero.celebrate();}
            catch(NumberFormatException e){entry.setError("Enter a whole number of steps");}
            catch(IllegalArgumentException e){entry.setError(e.getMessage());}
        });});dialog.show();
    }
    private void goalDialog(){
        LinearLayout box=column();box.setPadding(dp(24),dp(10),dp(24),dp(8));box.addView(text("Choose 1,000–50,000 steps. This updates today's goal and the goals for future days. Completed days keep their original goal.",14,MUTED,false));gap(box,14);EditText entry=input(String.valueOf(state.goal),true);entry.setTag("goal_input");box.addView(entry);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your daily step goal").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save goal",null).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag("goal_save");dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{int goal=Integer.parseInt(entry.getText().toString());store.update(s->s.setGoal(goal));dialog.dismiss();render();}catch(NumberFormatException e){entry.setError("Enter a whole number");}catch(IllegalArgumentException e){entry.setError(e.getMessage());}});});dialog.show();
    }
    private void renameDialog(){
        EditText entry=input(state.name,false);entry.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});LinearLayout box=column();box.setPadding(dp(24),dp(12),dp(24),dp(10));box.addView(entry);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your buddy's nickname").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save name",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String nickname=entry.getText().toString().trim();if(nickname.isEmpty()){entry.setError("Give your buddy a name");return;}store.update(s->s.name=nickname);dialog.dismiss();render();}));dialog.show();
    }
    private void share(){
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,"I walked "+n(state.steps())+" steps today with "+state.name+" in Walking Buddy! "+state.streak()+" day streak · "+n(state.lifetime())+" lifetime steps.");startActivity(Intent.createChooser(send,"Share your little adventure"));
    }
    private void export(String kind){
        exportKind=kind;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(kind.equals("csv")?"text/csv":"application/json");intent.putExtra(Intent.EXTRA_TITLE,"walking-buddy-"+state.day+(kind.equals("csv")?".csv":".json"));
        try{startActivityForResult(intent,20);}catch(ActivityNotFoundException e){info("File saving unavailable","This phone doesn't have a document picker. Install or enable a Files app to save your progress.");}
    }
    private void restore(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        try{startActivityForResult(intent,21);}catch(ActivityNotFoundException e){info("File opening unavailable","Enable a Files app to restore a backup.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==20){
            String body;
            if("csv".equals(exportKind)){
                StringBuilder csv=new StringBuilder("date,sensor_steps,manual_steps,total_steps,daily_goal,goal_reached\n");
                for(Map.Entry<LocalDate,GameState.Day> e:state.history.entrySet()){GameState.Day d=e.getValue();csv.append(e.getKey()).append(',').append(d.sensor).append(',').append(d.manual).append(',').append(d.steps()).append(',').append(d.goal).append(',').append(d.hit()).append('\n');}
                csv.append(state.day).append(',').append(state.sensorSteps).append(',').append(state.manualSteps).append(',').append(state.steps()).append(',').append(state.goal).append(',').append(state.steps()>=state.goal).append('\n');body=csv.toString();
            }else body=StateStore.encode(state);
            try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("File unavailable");out.write(body.getBytes(StandardCharsets.UTF_8));toast("Your progress is saved.");}
            catch(IOException|SecurityException e){info("Couldn't save the file","Please choose a different location and try again.");}
        }else if(request==21){
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new IOException("File unavailable");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[4096];int count;
                while((count=in.read(buf))!=-1){bytes.write(buf,0,count);if(bytes.size()>2000000)throw new IOException("Backup too large");}
                String raw=bytes.toString(StandardCharsets.UTF_8.name());if(raw.trim().isEmpty())throw new IOException("Empty backup");GameState restored=StateStore.decode(raw);
                new AlertDialog.Builder(this).setTitle("Restore this adventure?").setMessage("Buddy: "+restored.name+"\nLifetime steps: "+n(restored.lifetime())+"\n\nThis replaces your current progress. Automatic tracking will be paused until you enable it again.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Restore progress",(d,w)->{stopService(new Intent(this,StepService.class));store.replace(restored);state=store.read();setup=0;tab=0;render();toast("Welcome back, "+state.name+"!");}).show();
            }catch(IOException|RuntimeException e){info("Couldn't read this backup","Choose a valid Walking Buddy JSON backup. Your current progress has been kept.");}
        }
    }
    private LinearLayout need(String title,ProgressBar progress,TextView value,int level){
        LinearLayout box=column();LinearLayout label=row();label.addView(text(title,11,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));label.addView(value);box.addView(label);gap(box,6);progress.setProgress(level);box.addView(progress);return box;
    }
    private LinearLayout careButton(String action,String glyph,String label,String cost,int background){
        LinearLayout b=column();b.setGravity(Gravity.CENTER);b.setPadding(dp(8),dp(12),dp(8),dp(10));b.setBackground(round(background,17,0));b.addView(icon(glyph,INK,22));gap(b,5);TextView caption=text(label,13,INK,true);caption.setGravity(Gravity.CENTER);b.addView(caption);gap(b,2);TextView price=text(cost,10,MUTED,false);price.setGravity(Gravity.CENTER);b.addView(price);b.setTag("care_"+action);b.setContentDescription(label+" your buddy. "+cost);
        click(b,()->{String[] message={""};store.update(s->message[0]=s.care(action,System.currentTimeMillis()));refresh();if(hero!=null)hero.celebrate();toast(message[0]);});return b;
    }
    private LinearLayout statCard(String value,String label,String glyph){LinearLayout b=card();b.addView(icon(glyph,GREEN,22));gap(b,10);b.addView(text(value,23,INK,true));gap(b,4);b.addView(text(label,12,MUTED,false));return b;}
    private void option(String glyph,String title,String sub,String tag,Runnable action){
        LinearLayout b=row();b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(15),dp(16),dp(13),dp(16));b.setBackground(round(WHITE,16,LINE));b.addView(icon(glyph,GREEN,23));
        LinearLayout words=column();words.setPadding(dp(13),0,dp(10),0);words.addView(text(title,14,INK,true));gap(words,4);words.addView(text(sub,11,MUTED,false));b.addView(words,new LinearLayout.LayoutParams(0,-2,1));b.addView(icon("arrow",MUTED,17));b.setTag(tag);b.setContentDescription(title+". "+sub);click(b,action);content.addView(b);gap(content,7);
    }
    private void section(String title){content.addView(text(title,17,INK,true));gap(content,12);}
    private void backButton(Runnable action){LinearLayout b=row();b.setGravity(Gravity.CENTER_VERTICAL);b.addView(icon("back",GREEN,19));b.addView(text("  Back",13,GREEN,true));b.setPadding(0,dp(8),0,dp(8));click(b,action);content.addView(b);}
    private TextView button(String label,String tag,Runnable action,boolean primary){
        Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(primary?WHITE:GREEN);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(53));b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setStateListAnimator(null);
        b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(35,0,0,0)),round(primary?GREEN:MINT,16,0),null));b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));b.setTag(tag);b.setOnClickListener(v->action.run());return b;
    }
    private EditText input(String value,boolean number){
        EditText e=new EditText(this);e.setText(value);e.setTextSize(17);e.setTextColor(INK);e.setSingleLine(true);e.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);e.setBackground(round(WHITE,14,LINE));e.setPadding(dp(16),dp(13),dp(16),dp(13));e.setSelectAllOnFocus(true);e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(54)));return e;
    }
    private TextView text(String label,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(2),1);return t;}
    private TextView chip(String label,int background,int color){TextView t=text(label,11,color,true);t.setPadding(dp(11),dp(6),dp(11),dp(6));t.setGravity(Gravity.CENTER);t.setBackground(round(background,30,0));return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(17),dp(16),dp(17),dp(16));l.setBackground(round(WHITE,20,LINE));return l;}
    private LinearLayout.LayoutParams weighted(int left,int right){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(left),0,dp(right),0);return p;}
    private IconView icon(String name,int color,int size){IconView v=new IconView(this,name,color);v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return v;}
    private ProgressBar bar(int color,int height){
        ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);b.setMax(100);
        LayerDrawable layers=new LayerDrawable(new android.graphics.drawable.Drawable[]{round(MINT,10,0),new ClipDrawable(round(color,10,0),Gravity.START,ClipDrawable.HORIZONTAL)});layers.setId(0,android.R.id.background);layers.setId(1,android.R.id.progress);b.setProgressDrawable(layers);b.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));return b;
    }
    private GradientDrawable round(int color,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    private void click(View view,Runnable action){view.setOnClickListener(v->action.run());view.setFocusable(true);}
    private void gap(LinearLayout l,int height){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(height)));}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private String n(long value){return String.format(Locale.getDefault(),"%,d",value);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void info(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Got it",null).show();}
}
