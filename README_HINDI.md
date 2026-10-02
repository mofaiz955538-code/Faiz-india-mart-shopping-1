# FAIZMART INDIA SHOPPING — V50 LIVE BACKEND BUILD

यह V50 package अब केवल demo/local product app नहीं है। इसमें Supabase REST integration जोड़ा गया है:

- 📱 Mobile OTP send + verify
- 📧 Gmail OTP verification
- 👤 Customer / Seller profile role
- 🛍 Online products database
- 📷 Gallery photo → Supabase Storage upload
- 🔗 Product image URL online database में
- 🛒 Basket/Cart
- 📦 COD online order + order items
- 💰 10% commission fields/order calculation
- 📋 My Orders
- 🔐 Database और Storage RLS policies

## बहुत जरूरी
इस ZIP में आपका असली Supabase URL और anon key जानबूझकर placeholder हैं क्योंकि इस chat में वे credentials उपलब्ध नहीं हैं। उन्हें `app/build.gradle` में भरना होगा।

इसके बाद `SUPABASE_SETUP.sql` को अपने Supabase SQL Editor में Run करें और Phone Auth में SMS provider configure करें।

फिर GitHub Actions से APK build करें।
