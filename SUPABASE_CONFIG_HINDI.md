# Supabase connection — V50

इस ZIP में live credentials जानबूझकर नहीं भरे गए हैं। बिना आपके Supabase project URL और anon key के कोई असली credential बनाना सुरक्षित/सही नहीं है।

## 1) App में भरना
`app/build.gradle` में इन दोनों जगहों पर अपना data डालें:
- `SUPABASE_URL` = `https://xxxxx.supabase.co`
- `SUPABASE_ANON_KEY` = Supabase Project Settings → API → anon/public key

Debug और Release दोनों में यही values भरें।

## 2) Database + Storage
Supabase SQL Editor में `SUPABASE_SETUP.sql` की पूरी script एक बार Run करें। इससे:
- profiles
- products
- orders
- order_items
- product-images Storage bucket
- RLS policies
बनेंगे।

## 3) असली Mobile OTP
Supabase Dashboard → Authentication → Providers → Phone में SMS provider configure करना जरूरी है। App `/auth/v1/otp` और `/auth/v1/verify` इस्तेमाल करता है। SMS provider configure नहीं होगा तो असली SMS नहीं आएगा।

## 4) Seller OTP
उसी real Phone Auth से seller भी OTP verify करता है और profile में role=`seller` save होता है।

## 5) Product photo
Gallery → Storage upload → public image URL → products table में image_url. Upload path user-id folder के अंदर है।

## 6) COD
Basket → COD Order → orders + order_items tables. Payment gateway नहीं जोड़ा गया है; यह COD flow है।
