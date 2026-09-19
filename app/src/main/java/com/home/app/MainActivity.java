package com.home.app;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String DB_URL = "https://home-a303c-default-rtdb.asia-southeast1.firebasedatabase.app";
    private FirebaseAuth auth;
    private DatabaseReference db;
    private FrameLayout root;
    private EditText activeInput;
    private String verificationId, pendingPhone, pendingName;
    private EditText otpInput;
    private TextView[] otpBoxes = new TextView[6];
    private int W, H;

    private int blue = Color.rgb(24, 157, 239);
    private int navy = Color.rgb(19, 49, 83);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance(DB_URL).getReference();
        W = getResources().getDisplayMetrics().widthPixels;
        H = getResources().getDisplayMetrics().heightPixels;
        if (auth.getCurrentUser() != null) loadUser(); else showSplash();
    }

    private void fullscreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void normalBars() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
    }

    private void imageScreen(int res) {
        fullscreen();
        hideKeyboard();
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        ImageView bg = new ImageView(this);
        bg.setImageResource(res);
        bg.setScaleType(ImageView.ScaleType.FIT_XY);
        root.addView(bg, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        activeInput = null;
    }

    private void showSplash() {
        imageScreen(R.drawable.splash_design);
        root.postDelayed(this::showLogin, 1500);
    }

    private void pos(View v, float x, float y, float w, float h) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams((int)(W*w),(int)(H*h));
        p.leftMargin=(int)(W*x); p.topMargin=(int)(H*y); root.addView(v,p);
    }

    private void hit(float x,float y,float w,float h, View.OnClickListener l) {
        View v=new View(this); v.setBackgroundColor(Color.TRANSPARENT); v.setOnClickListener(l); pos(v,x,y,w,h);
    }

    private EditText edit(int type) {
        EditText e=new EditText(this);
        e.setSingleLine(true); e.setInputType(type); e.setTextColor(navy); e.setTextSize(16);
        e.setHintTextColor(Color.rgb(160,174,190)); e.setBackgroundColor(Color.TRANSPARENT); e.setPadding(0,0,0,0);
        return e;
    }

    private void showLogin() {
        imageScreen(R.drawable.ref_login);
        EditText e=edit(android.text.InputType.TYPE_CLASS_PHONE); activeInput=e;
        pos(e,.44f,.395f,.42f,.065f);
        hit(.20f,.455f,.70f,.075f,v->sendOtp());
        hit(.18f,.55f,.72f,.07f,v->toast("Phone sign-in is the active account method."));
        hit(.30f,.67f,.45f,.08f,v->toast("Use your phone number to continue."));
        e.requestFocus();
    }

    private void sendOtp() {
        String p=activeInput==null?"":activeInput.getText().toString().trim();
        if(p.length()<8){toast("Enter a valid phone number.");return;}
        if(!p.startsWith("+")){p="+"+p;}
        pendingPhone=p;
        PhoneAuthOptions o=PhoneAuthOptions.newBuilder(auth).setPhoneNumber(p).setTimeout(60, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                @Override public void onVerificationCompleted(PhoneAuthCredential c){ signIn(c); }
                @Override public void onVerificationFailed(FirebaseException e){ toast(e.getMessage()==null?"Phone verification failed":e.getMessage()); }
                @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken t){ verificationId=id; showOtp(); }
            }).build();
        PhoneAuthProvider.verifyPhoneNumber(o); toast("Sending OTP…");
    }

    private void showOtp() {
        imageScreen(R.drawable.ref_otp);
        for(int i=0;i<6;i++){
            TextView t=new TextView(this); t.setGravity(Gravity.CENTER); t.setTextSize(20); t.setTextColor(navy); t.setTypeface(null,1);
            GradientDrawable g=new GradientDrawable(); g.setColor(Color.TRANSPARENT); g.setStroke(dp(1),Color.rgb(214,225,237)); g.setCornerRadius(dp(10)); t.setBackground(g);
            otpBoxes[i]=t; pos(t,.223f+i*.092f,.275f,.075f,.055f);
        }
        otpInput=edit(android.text.InputType.TYPE_CLASS_NUMBER); otpInput.setTextColor(Color.TRANSPARENT); otpInput.setCursorVisible(false); otpInput.setBackgroundColor(Color.TRANSPARENT); otpInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); activeInput=otpInput;
        pos(otpInput,.18f,.26f,.64f,.08f);
        otpInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){renderOtp(s.toString()); if(s.length()==6) verifyOtp();} public void afterTextChanged(Editable e){}});
        hit(.15f,.42f,.70f,.075f,v->verifyOtp());
        hit(.20f,.49f,.60f,.07f,v->showLogin());
        otpInput.requestFocus();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(otpInput,InputMethodManager.SHOW_IMPLICIT);
    }

    private void renderOtp(String s){ for(int i=0;i<6;i++) if(otpBoxes[i]!=null) otpBoxes[i].setText(i<s.length()?String.valueOf(s.charAt(i)):""); }
    private void verifyOtp(){String c=otpInput==null?"":otpInput.getText().toString(); if(c.length()!=6){toast("Enter the 6-digit code.");return;} signIn(PhoneAuthProvider.getCredential(verificationId,c));}
    private void signIn(PhoneAuthCredential c){auth.signInWithCredential(c).addOnCompleteListener(this,t->{if(t.isSuccessful())loadUser();else toast("Verification failed. Please try again.");});}

    private void loadUser(){
        FirebaseUser u=auth.getCurrentUser(); if(u==null){showLogin();return;}
        db.child("profiles").child(u.getUid()).addListenerForSingleValueEvent(new ValueEventListener(){
            public void onDataChange(DataSnapshot s){if(s.exists()&&s.child("name").getValue()!=null)showHome();else showProfile();}
            public void onCancelled(DatabaseError e){showProfile();}
        });
    }

    private void showProfile(){
        imageScreen(R.drawable.ref_profile);
        EditText name=edit(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); activeInput=name; pos(name,.16f,.50f,.68f,.07f);
        EditText email=edit(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); pos(email,.16f,.60f,.68f,.07f);
        hit(.10f,.70f,.80f,.09f,v->{pendingName=name.getText().toString().trim(); if(pendingName.length()<2){toast("Enter your full name.");return;} showPassword();});
        hit(.38f,.36f,.24f,.09f,v->toast("Profile photo can be added locally; cloud photo storage requires Firebase Storage billing."));
    }

    private void showPassword(){
        imageScreen(R.drawable.ref_password);
        EditText pass=edit(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD); pass.setTransformationMethod(PasswordTransformationMethod.getInstance()); activeInput=pass; pos(pass,.15f,.42f,.70f,.07f);
        hit(.10f,.70f,.80f,.09f,v->{String s=pass.getText().toString(); if(s.length()<8){toast("Password must be at least 8 characters.");return;} if(!s.matches(".*[A-Z].*")||!s.matches(".*[0-9].*")){toast("Use at least 1 uppercase letter and 1 number.");return;} saveProfile(pendingName,s);});
    }

    private void saveProfile(String name,String password){
        FirebaseUser u=auth.getCurrentUser(); if(u==null){showLogin();return;}
        String phone=u.getPhoneNumber()==null?pendingPhone:u.getPhoneNumber();
        Map<String,Object> p=new HashMap<>(); p.put("name",name); p.put("online",true); p.put("lastSeen",ServerValue.TIMESTAMP); p.put("passwordHash",sha256(password));
        Map<String,Object> priv=new HashMap<>(); priv.put("phone",phone==null?"":phone);
        Map<String,Object> up=new HashMap<>(); up.put("profiles/"+u.getUid(),p); up.put("privateUsers/"+u.getUid(),priv); if(phone!=null)up.put("phoneIndex/"+sha256(phone),u.getUid());
        db.updateChildren(up).addOnCompleteListener(t->{if(t.isSuccessful())showCreated();else toast("Could not save your account.");});
    }

    private void showCreated(){imageScreen(R.drawable.ref_created); hit(.15f,.70f,.70f,.10f,v->showHome());}

    private void showHome(){
        imageScreen(R.drawable.ref_home);
        hit(.75f,.02f,.12f,.07f,v->showMe());
        hit(.07f,.56f,.80f,.07f,v->openChat("HOME Demo"));
        hit(.07f,.63f,.80f,.07f,v->openChat("May Thazin"));
        hit(.07f,.70f,.80f,.07f,v->openChat("Family Group"));
        hit(.05f,.82f,.22f,.14f,v->showHome());
        hit(.27f,.82f,.22f,.14f,v->showChats());
        hit(.49f,.82f,.22f,.14f,v->showContacts());
        hit(.71f,.82f,.22f,.14f,v->showMe());
        hit(.77f,.08f,.12f,.08f,v->showContacts());
    }

    private void showChats(){
        normalBars();
        LinearLayout l=base("Chats");
        addRow(l,"Thu Ya","See you tomorrow!",()->openChat("Thu Ya"));
        addRow(l,"May Thazin","Photo",()->openChat("May Thazin"));
        addRow(l,"Family Group","Aung: Okay 👍",()->openChat("Family Group"));
        addRow(l,"Ko Htet","Voice Message",()->openChat("Ko Htet"));
        bottom(l,1); setContentView(l);
    }

    private void showContacts(){
        normalBars();
        LinearLayout l=base("Contacts");
        EditText p=edit(android.text.InputType.TYPE_CLASS_PHONE); p.setHint("Phone number"); p.setPadding(dp(16),0,dp(16),0); rounded(p,Color.WHITE,Color.rgb(220,231,242)); l.addView(p,new LinearLayout.LayoutParams(-1,dp(54)));
        Button b=button("Find contact"); l.addView(b,new LinearLayout.LayoutParams(-1,dp(52))); b.setOnClickListener(v->findContact(p.getText().toString().trim(),l));
        bottom(l,2); setContentView(l);
    }

    private void findContact(String phone,LinearLayout l){if(phone.length()<8){toast("Enter a phone number.");return;} db.child("phoneIndex").child(sha256(phone.startsWith("+")?phone:"+"+phone)).addListenerForSingleValueEvent(new ValueEventListener(){public void onDataChange(DataSnapshot s){if(!s.exists()){toast("HOME user not found.");return;}String uid=String.valueOf(s.getValue()); db.child("profiles").child(uid).addListenerForSingleValueEvent(new ValueEventListener(){public void onDataChange(DataSnapshot p){String n=p.child("name").getValue(String.class);openChat(n==null?"HOME User":n,uid);}public void onCancelled(DatabaseError e){}});}public void onCancelled(DatabaseError e){toast("Search failed.");}});}

    private void openChat(String name){openChat(name,null);}
    private void openChat(String name,String otherUid){
        normalBars();
        LinearLayout rootL=new LinearLayout(this); rootL.setOrientation(LinearLayout.VERTICAL); rootL.setBackgroundColor(Color.rgb(247,250,253));
        TextView bar=new TextView(this); bar.setText("‹   "+name); bar.setTextColor(navy); bar.setTextSize(20); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(10),0,0,0); bar.setTypeface(null,1); rootL.addView(bar,new LinearLayout.LayoutParams(-1,dp(64))); bar.setOnClickListener(v->showHome());
        ScrollView sv=new ScrollView(this); LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(dp(14),dp(10),dp(14),dp(10)); sv.addView(messages); rootL.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout send=new LinearLayout(this); send.setPadding(dp(8),dp(8),dp(8),dp(8)); EditText e=edit(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); e.setHint("Message"); rounded(e,Color.WHITE,Color.rgb(220,231,242)); send.addView(e,new LinearLayout.LayoutParams(0,dp(52),1)); Button b=button("Send"); send.addView(b,new LinearLayout.LayoutParams(dp(82),dp(52))); rootL.addView(send);
        setContentView(rootL);
        FirebaseUser me=auth.getCurrentUser(); if(me!=null && otherUid!=null){String room=roomId(me.getUid(),otherUid); db.child("rooms").child(room).child("messages").addValueEventListener(new ValueEventListener(){public void onDataChange(DataSnapshot s){messages.removeAllViews();for(DataSnapshot m:s.getChildren()){String text=m.child("text").getValue(String.class);String from=m.child("from").getValue(String.class);TextView t=new TextView(MainActivity.this);t.setText((me.getUid().equals(from)?"You: ":name+": ")+text);t.setTextColor(navy);t.setTextSize(16);t.setPadding(dp(14),dp(10),dp(14),dp(10));messages.addView(t);}sv.post(()->sv.fullScroll(View.FOCUS_DOWN));}public void onCancelled(DatabaseError e){}}); b.setOnClickListener(v->sendMessage(room,e.getText().toString()));}
        else b.setOnClickListener(v->{toast("Open a real HOME contact from Contacts to enable realtime messaging.");});
    }

    private void sendMessage(String room,String text){if(text.trim().isEmpty())return;FirebaseUser u=auth.getCurrentUser();if(u==null)return;Map<String,Object> m=new HashMap<>();m.put("from",u.getUid());m.put("text",text.trim());m.put("timestamp",ServerValue.TIMESTAMP);db.child("rooms").child(room).child("messages").push().setValue(m);}
    private String roomId(String a,String b){return a.compareTo(b)<0?a+"_"+b:b+"_"+a;}

    private void showMe(){
        imageScreen(R.drawable.ref_me);
        hit(.05f,.82f,.22f,.14f,v->showHome()); hit(.27f,.82f,.22f,.14f,v->showChats()); hit(.49f,.82f,.22f,.14f,v->showContacts()); hit(.71f,.82f,.22f,.14f,v->showMe());
        hit(.10f,.20f,.80f,.07f,v->showEditProfile()); hit(.10f,.28f,.80f,.07f,v->toast("Privacy & Security settings are connected to Firebase Auth rules.")); hit(.10f,.36f,.80f,.07f,v->toast("Notifications use Firebase Cloud Messaging in the project build.")); hit(.10f,.44f,.80f,.07f,v->toast("Help & Support")); hit(.10f,.52f,.80f,.07f,v->toast("Settings")); hit(.10f,.69f,.80f,.08f,v->{auth.signOut();showLogin();});
    }

    private void showEditProfile(){
        normalBars(); LinearLayout l=base("Edit Profile"); EditText n=edit(android.text.InputType.TYPE_CLASS_TEXT); n.setHint("Full name"); l.addView(n,new LinearLayout.LayoutParams(-1,dp(56))); Button b=button("Save"); l.addView(b,new LinearLayout.LayoutParams(-1,dp(54))); b.setOnClickListener(v->{FirebaseUser u=auth.getCurrentUser();if(u!=null)db.child("profiles").child(u.getUid()).child("name").setValue(n.getText().toString().trim());showMe();}); setContentView(l);
    }

    private LinearLayout base(String title){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(16),dp(24),dp(16),dp(10));l.setBackgroundColor(Color.rgb(247,250,253));TextView h=new TextView(this);h.setText(title);h.setTextColor(navy);h.setTextSize(28);h.setTypeface(null,1);h.setPadding(0,0,0,dp(16));l.addView(h);return l;}
    private void addRow(LinearLayout l,String name,String sub,final Runnable r){TextView t=new TextView(this);t.setText(name+"\n"+sub);t.setTextColor(navy);t.setTextSize(17);t.setPadding(dp(16),dp(14),dp(16),dp(14));rounded(t,Color.WHITE,Color.rgb(226,235,244));l.addView(t,new LinearLayout.LayoutParams(-1,dp(76)));t.setOnClickListener(v->r.run());Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(8)));}
    private void bottom(LinearLayout l,int selected){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,0,1));LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.HORIZONTAL);String[] a={"Home","Chats","Contacts","Me"};for(int i=0;i<4;i++){Button x=button(a[i]);b.addView(x,new LinearLayout.LayoutParams(0,dp(58),1));final int k=i;x.setOnClickListener(v->{if(k==0)showHome();else if(k==1)showChats();else if(k==2)showContacts();else showMe();});}l.addView(b);}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);GradientDrawable g=new GradientDrawable();g.setColor(blue);g.setCornerRadius(dp(28));b.setBackground(g);return b;}
    private void rounded(View v,int fill,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setStroke(dp(1),stroke);g.setCornerRadius(dp(18));v.setBackground(g);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String sha256(String s){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] d=md.digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte q:d)x.append(String.format("%02x",q));return x.toString();}catch(Exception e){return s;}}
    private void hideKeyboard(){if(activeInput!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activeInput.getWindowToken(),0);activeInput=null;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
