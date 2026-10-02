# FAIZMART INDIA SHOPPING V50 — नई जाँच रिपोर्ट

## इस ZIP में जोड़ा गया
1. Supabase REST client: Auth, Database और Storage.
2. Mobile OTP send + verify API.
3. Customer और Shopkeeper/Seller दोनों का OTP login.
4. OTP के बाद profile role online database में save.
5. Online products table से product list.
6. Seller product add.
7. Gallery से product photo चुनना.
8. Supabase Storage `product-images` में photo upload.
9. Product में online image URL save.
10. Basket/Cart.
11. COD checkout.
12. Online `orders` और `order_items`.
13. My Orders online list.
14. Supabase RLS + Storage policies के लिए `SUPABASE_SETUP.sql`.
15. Setup के लिए `SUPABASE_CONFIG_HINDI.md`.

## अभी user-side configuration जरूरी है
- असली Supabase project URL और anon/public key इस ZIP में नहीं डाले गए हैं, क्योंकि वे इस conversation में उपलब्ध नहीं हैं।
- Supabase Phone Auth में SMS provider configure करना जरूरी है; तभी असली SMS OTP आएगा.
- `SUPABASE_SETUP.sql` को आपके Supabase project में एक बार Run करना जरूरी है.

## महत्वपूर्ण
यह ZIP code-level integration के साथ तैयार है, लेकिन live OTP/database/storage को बिना आपके Supabase project credentials और provider configuration के वास्तविक server पर verify नहीं किया जा सकता।
