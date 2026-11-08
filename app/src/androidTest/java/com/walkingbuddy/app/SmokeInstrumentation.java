package com.walkingbuddy.app;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Dependency-free device integration checks, isolated to the .debug app. */
public final class SmokeInstrumentation extends Instrumentation {
    private MainActivity activity;
    private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            StateStore store=StateStore.get(getTargetContext());
            android.content.SharedPreferences prefs=getTargetContext().getSharedPreferences("walking_buddy",Context.MODE_PRIVATE);
            prefs.edit().putString("previous_backup","old private progress").putString("unreadable_backup","damaged private progress").commit();
            runOnMainSync(store::reset);
            check(!prefs.contains("previous_backup")&&!prefs.contains("unreadable_backup"),"Reset deletes recovery copies");
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            assertTag("starter_0");capture("01-adoption");tap("starter_2");tap("adopt_continue");assertTag("buddy_name");
            runOnMainSync(()->((EditText)find(activity.getWindow().getDecorView(),"buddy_name")).setText("Scout"));tap("setup_adopt");assertTag("setup_manual");tap("setup_manual");
            check(store.read().adopted,"Buddy adopted");check(store.read().buddy==2,"Selected fox adopted");check(store.read().name.equals("Scout"),"Nickname saved");
            tap("manual_steps");setActiveInput("6500");tapText("Save steps");
            check(store.read().manualSteps==6500,"Manual dialog saves totals");check(store.read().goalRewarded,"Goal reward credited");check(store.read().streak()==1,"Streak displayed");
            tap("care_feed");check(store.read().food==100,"Feeding restores food");check(store.read().careCount==1,"Care counted");
            SystemClock.sleep(2200);capture("02-today");tap("tab_1");assertTag("buddy_7");capture("03-buddies");tap("buddy_4");tapText("Close");
            tap("tab_2");assertTag("share_progress");capture("04-journal");tap("tab_3");assertTag("edit_goal");capture("05-settings");
            tap("edit_goal");setActiveInput("0");tapText("Save goal");check(store.read().goal==6000,"Invalid goal rejected");setActiveInput("8000");tapText("Save goal");check(store.read().goal==8000,"Goal edited");
            tap("reduce_motion");check(store.read().reduceMotion,"Reduced animation toggled");
            tap("privacy");check(node(getUiAutomation().getRootInActiveWindow(),false,"Read online")!=null,"Offline privacy policy includes online link");tapText("Close");
            tap("about");tapText("Got it");
            GameState s=store.read();String raw=StateStore.encode(s);GameState copy=StateStore.decode(raw);
            check(copy.steps()==6500&&copy.name.equals("Scout")&&copy.buddy==2,"Saved state round trip");
            relaunch();
            check(StateStore.get(getTargetContext()).read().steps()==6500,"Progress survives activity restart");assertTag("tab_0");
            checkBackupRecovery(store);
            result.putString("stream","\nPASS: "+checks+" Walking Buddy integration checks. Screenshots in external files/qa.\n");result.putInt("checks",checks);finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","\nFAIL after "+checks+" checks: "+e+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private void checkBackupRecovery(StateStore store) throws Exception {
        String original = StateStore.encode(store.read());
        GameState incoming = new GameState(LocalDate.now(), System.currentTimeMillis());
        incoming.adopt(1, "Maple", System.currentTimeMillis());
        incoming.setManual(1234);
        incoming.tracking = true;
        incoming.lastSensor = 4000;
        incoming.lastSensorTimestamp = 2000;
        String backup = StateStore.encode(incoming);

        tap("tab_3");
        pickBackup("restore_backup", "invalid backup");
        tapText("Got it");
        check(store.read().steps() == 6500 && !store.hasPreviousProgress(), "Invalid backup keeps progress and recovery slot");
        pickBackup("restore_backup", backup);
        tapText("Cancel");
        check(store.read().steps() == 6500 && !store.hasPreviousProgress(), "Canceling file restore leaves progress alone");
        pickBackup("restore_backup", backup);
        tapText("Restore progress");
        GameState restored = store.read();
        check(restored.steps() == 1234 && restored.name.equals("Maple"), "Selected file restores the buddy and steps");
        check(!restored.tracking && restored.lastSensor == -1 && restored.lastSensorTimestamp == -1,
                "Restore pauses tracking and clears the previous sensor baseline");
        check(store.previousProgress().steps() == 6500, "Restore preserves the previous save");

        relaunch();
        tap("tab_3");
        tap("recover_previous");
        tapText("Cancel");
        check(store.read().steps() == 1234 && store.previousProgress().steps() == 6500,
                "Canceling recovery keeps both saves");
        tap("recover_previous");
        tapText("Recover progress");
        check(store.read().steps() == 6500 && store.read().name.equals("Scout"), "Recovery returns to the previous buddy");
        check(store.previousProgress().steps() == 1234, "Recovery retains the replaced save");

        runOnMainSync(store::reset);
        relaunch();
        pickBackup("setup_restore", original);
        tapText("Restore progress");
        assertTag("tab_0");
        check(store.read().steps() == 6500 && store.read().adopted, "Setup can restore an adventure before adopting a buddy");
    }
    private void pickBackup(String button, String contents) throws Exception {
        File file = File.createTempFile("walking-buddy-test-", ".json", getTargetContext().getCacheDir());
        try {
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(contents.getBytes(StandardCharsets.UTF_8));
            }
            IntentFilter filter = new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
            filter.addCategory(Intent.CATEGORY_OPENABLE);
            filter.addDataType("application/json");
            Intent data = new Intent().setData(Uri.fromFile(file));
            ActivityMonitor picker = addMonitor(filter, new ActivityResult(Activity.RESULT_OK, data), true);
            try {
                tap(button);
                check(picker.getHits() == 1, "Opened the backup file picker from " + button);
            } finally {
                removeMonitor(picker);
            }
        } finally {
            file.delete();
        }
    }
    private void relaunch() {
        runOnMainSync(activity::finish);
        waitForIdleSync();
        activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
    }
    private void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private View find(View view,String tag){if(tag.equals(view.getTag()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
    private void assertTag(String tag){check(find(activity.getWindow().getDecorView(),tag)!=null,"View present: "+tag);}
    private void tap(String tag){runOnMainSync(()->{View v=find(activity.getWindow().getDecorView(),tag);if(v==null)throw new AssertionError("Missing "+tag);v.performClick();});waitForIdleSync();SystemClock.sleep(180);}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo root,boolean editable,String text){
        if(root==null)return null;if(editable&&root.isEditable())return root;if(!editable&&root.getText()!=null&&root.getText().toString().equalsIgnoreCase(text))return root;
        for(int i=0;i<root.getChildCount();i++){AccessibilityNodeInfo n=node(root.getChild(i),editable,text);if(n!=null)return n;}return null;
    }
    private void setActiveInput(String value){
        AccessibilityNodeInfo field=node(getUiAutomation().getRootInActiveWindow(),true,null);if(field==null)throw new AssertionError("No active edit field");
        Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Input edited");waitForIdleSync();
    }
    private void tapText(String text){AccessibilityNodeInfo n=node(getUiAutomation().getRootInActiveWindow(),false,text);if(n==null)throw new AssertionError("No active button "+text);check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Clicked "+text);waitForIdleSync();SystemClock.sleep(250);}
    private void capture(String name)throws IOException{
        SystemClock.sleep(250);Bitmap bitmap=getUiAutomation().takeScreenshot();File dir=new File(getTargetContext().getExternalFilesDir(null),"qa");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
}
