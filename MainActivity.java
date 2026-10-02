package com.faizmart.india;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 501;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<CartItem> cart = new ArrayList<>();
    private SupabaseClient api;
    private TextView status;
    private EditText phone, otp, email, emailOtp, productName, productPrice, productStock;
    private Uri selectedImage;
    private String accessToken = "";
    private String currentUserId = "";
    private String role = "customer";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        api = new SupabaseClient();
        showHome();
    }

    private LinearLayout root() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(24,24,24,24);
        ScrollView s = new ScrollView(this); s.addView(l); setContentView(s); return l;
    }
    private Button btn(String text, View.OnClickListener c) { Button b=new Button(this); b.setText(text); b.setOnClickListener(c); return b; }
    private EditText edit(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); return e; }
    private void msg(String s) { runOnUiThread(() -> { if(status!=null) status.setText(s); else Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }); }
    private void busy(boolean v) { runOnUiThread(() -> { if(status!=null) status.setText(v ? "कृपया प्रतीक्षा करें…" : "तैयार"); }); }

    private void showHome() {
        LinearLayout l=root();
        TextView title=new TextView(this); title.setText("FAIZMART INDIA SHOPPING\nV50"); title.setTextSize(25); title.setGravity(Gravity.CENTER); l.addView(title);
        status=new TextView(this); status.setText(SupabaseConfig.isConfigured()?"Supabase connected configuration मिली।":"Supabase URL/Anon Key अभी सेट नहीं हैं।"); l.addView(status);
        l.addView(btn("📱 Customer / Shopkeeper OTP Login", v -> showOtp(false)));
        l.addView(btn("🛍 Products / Shopping", v -> loadProducts()));
        l.addView(btn("🛒 Basket / Cart", v -> showCart()));
        l.addView(btn("➕ Seller: Add Product + Photo", v -> showSeller()));
        l.addView(btn("📦 My Orders", v -> loadOrders()));
        l.addView(btn("⚙ Supabase Setup Check", v -> showSetup()));
    }

    private void showOtp(boolean seller) {
        LinearLayout l=root(); TextView t=new TextView(this); t.setText(seller?"Seller / Shopkeeper OTP":"Mobile OTP Login"); t.setTextSize(22); l.addView(t);
        phone=edit("+91 मोबाइल नंबर"); l.addView(phone); l.addView(btn("OTP भेजें", v -> sendOtp(seller)));
        otp=edit("OTP डालें"); l.addView(otp); l.addView(btn("OTP Verify करें", v -> verifyOtp(seller)));
        email=edit("Gmail ID"); l.addView(email);
        emailOtp=edit("Gmail OTP"); l.addView(emailOtp);
        l.addView(btn("Gmail OTP भेजें", v -> sendEmailOtp()));
        l.addView(btn("Gmail OTP Verify करें", v -> verifyEmailOtp()));
        status=new TextView(this); l.addView(status); l.addView(btn("← Home",v->showHome()));
    }
    private void sendOtp(boolean seller) {
        String p=phone.getText().toString().trim(); if(p.length()<10){msg("सही मोबाइल नंबर डालें");return;}
        busy(true); io.execute(() -> { try { api.sendOtp(p); msg("OTP भेजने की request सफल हुई। SMS provider configured होना जरूरी है।"); } catch(Exception e){msg("OTP error: "+e.getMessage());} });
    }

    private void sendEmailOtp() {
        String em=email==null?"":email.getText().toString().trim();
        if(!em.toLowerCase().endsWith("@gmail.com")){msg("सही Gmail ID डालें");return;}
        busy(true); io.execute(() -> { try { api.sendEmailOtp(em); msg("Gmail verification OTP भेजने की request सफल हुई।"); } catch(Exception e){msg("Gmail OTP error: "+e.getMessage());} });
    }
    private void verifyEmailOtp() {
        String em=email==null?"":email.getText().toString().trim(), code=emailOtp==null?"":emailOtp.getText().toString().trim();
        if(code.length()<4){msg("Gmail OTP डालें");return;}
        busy(true); io.execute(() -> { try { JSONObject r=api.verifyEmailOtp(em,code); String t=r.optString("access_token"); if(!t.isEmpty()){accessToken=t; JSONObject u=r.optJSONObject("user"); if(u!=null)currentUserId=u.optString("id",currentUserId); saveSession(); } msg("Gmail verification सफल हुआ।"); } catch(Exception e){msg("Gmail verify error: "+e.getMessage());} });
    }

    private void verifyOtp(boolean seller) {
        String p=phone.getText().toString().trim(), code=otp.getText().toString().trim(); if(code.length()<4){msg("OTP डालें");return;}
        busy(true); io.execute(() -> { try { JSONObject r=api.verifyOtp(p,code); accessToken=r.optString("access_token"); JSONObject u=r.optJSONObject("user"); currentUserId=u==null?"":u.optString("id"); role=seller?"seller":"customer"; saveSession(); api.upsertProfile(currentUserId, role, email==null?"":email.getText().toString().trim(), p); msg("OTP verify हो गया। Role: "+role); } catch(Exception e){msg("OTP verify error: "+e.getMessage());} });
    }

    private void loadProducts(){
        LinearLayout l=root(); TextView h=new TextView(this);h.setText("🛍 Online Products");h.setTextSize(22);l.addView(h);status=new TextView(this);l.addView(status);
        io.execute(() -> { try { JSONArray a=api.getProducts(); runOnUiThread(() -> { for(int i=0;i<a.length();i++){ try{ JSONObject p=a.getJSONObject(i); Button b=btn(p.optString("name")+"  ₹"+p.optDouble("price"),v->addToCart(p)); l.addView(b);}catch(Exception ignored){} } if(a.length()==0)status.setText("कोई product नहीं मिला। Seller से product जोड़ें।"); }); }catch(Exception e){msg("Products error: "+e.getMessage());} });
        l.addView(btn("🛒 Basket",v->showCart())); l.addView(btn("← Home",v->showHome()));
    }
    private void addToCart(JSONObject p){ cart.add(new CartItem(p.optString("id"),p.optString("name"),p.optDouble("price"),1)); Toast.makeText(this,"Basket में जोड़ दिया",Toast.LENGTH_SHORT).show(); }

    private void showCart(){
        LinearLayout l=root(); TextView h=new TextView(this);h.setText("🛒 Basket / Cart");h.setTextSize(22);l.addView(h);double total=0;for(CartItem c:cart){l.addView(new TextView(this){{setText(c.name+" × "+c.qty+" = ₹"+(c.price*c.qty));}});total+=c.price*c.qty;}
        TextView tot=new TextView(this);tot.setText("Total: ₹"+total);tot.setTextSize(20);l.addView(tot);l.addView(btn("COD Order Place करें",v->placeCodOrder(total)));l.addView(btn("← Home",v->showHome()));
    }
    private void placeCodOrder(double total){
        if(accessToken.isEmpty()){msg("पहले Mobile OTP Login करें");return;}
        if(cart.isEmpty()){msg("Basket खाली है");return;}
        busy(true);io.execute(()->{try{JSONArray items=new JSONArray();for(CartItem c:cart){JSONObject x=new JSONObject();x.put("product_id",c.id);x.put("quantity",c.qty);x.put("unit_price",c.price);items.put(x);}JSONObject order=new JSONObject();order.put("user_id",currentUserId);order.put("total_amount",total);order.put("payment_method","COD");order.put("status","pending");order.put("commission_rate",10);order.put("commission_amount",Math.round(total*0.10*100.0)/100.0);String id=api.createOrder(order,items);cart.clear();msg("COD order बन गया: "+id);}catch(Exception e){msg("Order error: "+e.getMessage());}});
    }

    private void showSeller(){
        if(accessToken.isEmpty()){showOtp(true);return;}
        LinearLayout l=root();TextView h=new TextView(this);h.setText("➕ Seller Product Upload");h.setTextSize(22);l.addView(h);
        productName=edit("Product name");productPrice=edit("Price");productStock=edit("Stock");l.addView(productName);l.addView(productPrice);l.addView(productStock);
        l.addView(btn("📷 Gallery से Photo चुनें",v->pickImage()));status=new TextView(this);l.addView(status);l.addView(btn("☁ Photo + Product Online Save",v->saveProductOnline()));l.addView(btn("← Home",v->showHome()));
    }
    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK_IMAGE&&c==RESULT_OK&&d!=null){selectedImage=d.getData();try{getContentResolver().takePersistableUriPermission(selectedImage,d.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}msg("Photo select हो गई");}}
    private void saveProductOnline(){
        if(!SupabaseConfig.isConfigured()){msg("पहले SupabaseConfig में URL और anon key डालें");return;} if(selectedImage==null){msg("पहले photo चुनें");return;}if(accessToken.isEmpty()){msg("Seller OTP login करें");return;}
        String name=productName.getText().toString().trim(); if(name.isEmpty()){msg("Product name डालें");return;}double price=Double.parseDouble(productPrice.getText().toString().trim());int stock=Integer.parseInt(productStock.getText().toString().trim());
        busy(true);io.execute(()->{try{String ext="jpg";String path=""+currentUserId+"/"+UUID.randomUUID()+"."+ext;byte[] data=readUri(selectedImage);api.uploadImage(path,data,"image/jpeg");String publicUrl=api.publicImageUrl(path);JSONObject p=new JSONObject();p.put("seller_id",currentUserId);p.put("name",name);p.put("price",price);p.put("stock",stock);p.put("image_url",publicUrl);p.put("active",true);api.insertProduct(p);msg("Product + photo Supabase पर save हो गया");}catch(Exception e){msg("Upload error: "+e.getMessage());}});
    }
    private byte[] readUri(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();return out.toByteArray();}

    private void loadOrders(){LinearLayout l=root();TextView h=new TextView(this);h.setText("📦 My Orders");h.setTextSize(22);l.addView(h);status=new TextView(this);l.addView(status);if(accessToken.isEmpty()){msg("पहले login करें");return;}io.execute(()->{try{JSONArray a=api.getMyOrders(currentUserId);runOnUiThread(()->{for(int i=0;i<a.length();i++){try{JSONObject o=a.getJSONObject(i);TextView x=new TextView(this);x.setText("Order "+o.optString("id")+" | ₹"+o.optDouble("total_amount")+" | "+o.optString("status"));l.addView(x);}catch(Exception ignored){}}status.setText(a.length()==0?"कोई order नहीं":"Orders online database से आए हैं");});}catch(Exception e){msg("Orders error: "+e.getMessage());}});l.addView(btn("← Home",v->showHome()));}

    private void showSetup(){LinearLayout l=root();TextView h=new TextView(this);h.setText("⚙ Supabase Setup");h.setTextSize(22);l.addView(h);TextView x=new TextView(this);x.setText("1. Supabase URL + anon key\n2. Auth → Phone provider/SMS configure\n3. Storage bucket: product-images (public)\n4. SUPABASE_SETUP.sql run करें\n5. फिर GitHub Actions से APK build करें");l.addView(x);status=new TextView(this);l.addView(status);l.addView(btn("Test Supabase",v->io.execute(()->{try{api.getProducts();msg("Supabase REST connection OK");}catch(Exception e){msg("Connection error: "+e.getMessage());}})));l.addView(btn("← Home",v->showHome()));}

    private void saveSession(){getPreferences(0).edit().putString("access_token",accessToken).putString("user_id",currentUserId).putString("role",role).apply();}
    static class CartItem{String id,name;double price;int qty;CartItem(String i,String n,double p,int q){id=i;name=n;price=p;qty=q;}}

    private class SupabaseClient {
        private String base(){return SupabaseConfig.URL.replaceAll("/$","");}
        private HttpURLConnection open(String path,String method)throws Exception{URL u=new URL(base()+path);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod(method);c.setRequestProperty("apikey",SupabaseConfig.ANON_KEY);c.setRequestProperty("Authorization",accessToken.isEmpty()?"Bearer "+SupabaseConfig.ANON_KEY:"Bearer "+accessToken);c.setRequestProperty("Content-Type","application/json");c.setConnectTimeout(20000);c.setReadTimeout(30000);if(method.equals("POST")||method.equals("PATCH")||method.equals("DELETE")){c.setDoOutput(true);}return c;}
        private String body(HttpURLConnection c,String body)throws Exception{if(body!=null){c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream in=code>=400?c.getErrorStream():c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(in!=null){byte[]b=new byte[4096];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();}String s=out.toString(StandardCharsets.UTF_8.name());if(code>=400)throw new Exception(code+" "+s);return s;}
        void sendOtp(String p)throws Exception{HttpURLConnection c=open("/auth/v1/otp","POST");body(c,new JSONObject().put("phone",p).put("create_user",true).toString());}
        void sendEmailOtp(String em)throws Exception{HttpURLConnection c=open("/auth/v1/otp","POST");body(c,new JSONObject().put("email",em).put("create_user",true).toString());}
        JSONObject verifyEmailOtp(String em,String code)throws Exception{HttpURLConnection c=open("/auth/v1/verify","POST");return new JSONObject(body(c,new JSONObject().put("email",em).put("token",code).put("type","email").toString()));}
        JSONObject verifyOtp(String p,String code)throws Exception{HttpURLConnection c=open("/auth/v1/verify","POST");return new JSONObject(body(c,new JSONObject().put("phone",p).put("token",code).put("type","sms").toString()));}
        JSONArray getProducts()throws Exception{HttpURLConnection c=open("/rest/v1/products?select=*&active=eq.true&order=created_at.desc","GET");return new JSONArray(body(c,null));}
        void upsertProfile(String uid,String r,String em,String ph)throws Exception{JSONObject x=new JSONObject();x.put("id",uid);x.put("role",r);x.put("email",em);x.put("phone",ph);HttpURLConnection c=open("/rest/v1/profiles","POST");c.setRequestProperty("Prefer","resolution=merge-duplicates");body(c,x.toString());}
        void insertProduct(JSONObject p)throws Exception{HttpURLConnection c=open("/rest/v1/products","POST");c.setRequestProperty("Prefer","return=minimal");body(c,p.toString());}
        String publicImageUrl(String path){return base()+"/storage/v1/object/public/"+SupabaseConfig.STORAGE_BUCKET+"/"+path;}
        void uploadImage(String path,byte[] data,String contentType)throws Exception{URL u=new URL(base()+"/storage/v1/object/"+SupabaseConfig.STORAGE_BUCKET+"/"+path);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("apikey",SupabaseConfig.ANON_KEY);c.setRequestProperty("Authorization","Bearer "+accessToken);c.setRequestProperty("Content-Type",contentType);c.getOutputStream().write(data);int code=c.getResponseCode();if(code>=400)throw new Exception(code+" Storage upload failed");}
        String createOrder(JSONObject order,JSONArray items)throws Exception{HttpURLConnection c=open("/rest/v1/orders","POST");c.setRequestProperty("Prefer","return=representation");String res=body(c,order.toString());JSONArray a=new JSONArray(res);String id=a.getJSONObject(0).getString("id");for(int i=0;i<items.length();i++){JSONObject x=items.getJSONObject(i);x.put("order_id",id);HttpURLConnection d=open("/rest/v1/order_items","POST");d.setRequestProperty("Prefer","return=minimal");body(d,x.toString());}return id;}
        JSONArray getMyOrders(String uid)throws Exception{HttpURLConnection c=open("/rest/v1/orders?select=*&user_id=eq."+uid+"&order=created_at.desc","GET");return new JSONArray(body(c,null));}
    }
}
