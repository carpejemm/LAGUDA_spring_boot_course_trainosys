# Postman Guide – Trainosys E-Commerce Application

How to test the `ecom-application` API in Postman. The API is secured with **Spring Security + JWT**.

## 1. Before you start

1. Run the app (`EcomApplication` in IntelliJ, or `./mvnw spring-boot:run`).
2. Wait for `Started EcomApplication` in the console.
3. Base URL: `http://localhost:8080`

> The database is H2 **in-memory**. All data is erased every time the app restarts.
> Two accounts are created automatically on every start:
>
> | Username | Password | Role |
> |---|---|---|
> | `admin` | `admin123` | ADMIN |
> | `user1` | `password123` | CUSTOMER |

## 2. Import the collection (fastest)

1. In Postman, click **Import**.
2. Choose `Trainosys-Ecom-Application.postman_collection.json` (on the Desktop).
3. Run **Auth → Sign In (admin)** first. The token is saved automatically and every other request uses it.

## 3. Who can call what

| Endpoint | Access |
|---|---|
| `POST /api/auth/signup`, `/signin`, `/signout` | public |
| `GET /api/products`, `GET /api/products/search` | public |
| `POST/PUT/DELETE /api/products/**` | **ADMIN** |
| `/api/users/**` | **ADMIN** |
| `/api/cart/**`, `/api/orders`, `GET /api/auth/username`, `GET /api/auth/user` | any logged-in user |

| Response | Meaning |
|---|---|
| `401 Unauthorized` (JSON with `"message"`) | no token, wrong token, or expired token: **sign in again** |
| `403 Forbidden` | you are logged in, but your role isn't allowed (e.g. `user1` creating a product) |

## 4. Set up Postman by hand (if you don't import)

### Collection and variables
1. **Collections → +** → name it `Trainosys Ecom Application`.
2. Collection → **Variables** tab:

   | Variable | Value |
   |---|---|
   | `baseUrl` | `http://localhost:8080` |
   | `jwtToken` | *(empty)* |

### Send the token on every request
Collection → **Authorization** tab → Type **Bearer Token** → Token `{{jwtToken}}`.
Every request inside uses this unless it says otherwise (**Inherit auth from parent** is the default).

### Save the token automatically after signing in
On the **Sign In** request → **Scripts** tab → **Post-response**:

```javascript
if (pm.response.code === 200) {
    pm.collectionVariables.set("jwtToken", pm.response.json().jwtToken);
}
```

Without the script: copy `jwtToken` from the response and paste it into the `jwtToken` variable.

### Adding a request
1. Hover over the collection → **⋯** → **Add request**, give it a name.
2. Pick the **method**, type the **URL** (e.g. `{{baseUrl}}/api/products`).
3. POST/PUT: **Body → raw → JSON**, paste the JSON.
4. **Save**, then **Send**.

## 5. Endpoints

### Auth – `/api/auth`

#### Sign Up (public)
- **POST** `{{baseUrl}}/api/auth/signup`
  ```json
  {
    "username": "juan",
    "email": "juan@example.com",
    "password": "secret123",
    "firstName": "Juan",
    "lastName": "Dela Cruz"
  }
  ```
- `200` `{"message":"User registered successfully!"}`
- `400` `Error: Username is already taken!` / `Error: Email is already in use!`, or invalid fields
  (username 3–20 chars, valid email, password 6–40 chars)
- New accounts are always **CUSTOMER**.

#### Sign In (public)
- **POST** `{{baseUrl}}/api/auth/signin`
  ```json
  { "username": "admin", "password": "admin123" }
  ```
- `200`:
  ```json
  { "id": 1, "username": "admin", "roles": ["ROLE_ADMIN"], "jwtToken": "eyJhbGciOiJIUzI1NiJ9...." }
  ```
  Also sets an HttpOnly cookie `trainosys-jwt` (Postman keeps it in **Cookies**).
- `401` `{"message":"Bad credentials"}`

#### Current Username / User Info (logged in)
- **GET** `{{baseUrl}}/api/auth/username` → `admin`
- **GET** `{{baseUrl}}/api/auth/user` → `{"id":1,"username":"admin","roles":["ROLE_ADMIN"],"jwtToken":null}`

#### Sign Out (public)
- **POST** `{{baseUrl}}/api/auth/signout` → `{"message":"You've been signed out!"}`
- Deletes the cookie. A copied Bearer token still works until it expires (24 h), so also clear `{{jwtToken}}`.

### Users – `/api/users` (ADMIN)

| Request | Method | URL | Body |
|---|---|---|---|
| Create User | POST | `{{baseUrl}}/api/users` | see below |
| Get All Users | GET | `{{baseUrl}}/api/users` | |
| Get User by ID | GET | `{{baseUrl}}/api/users/1` | |
| Update User | PUT | `{{baseUrl}}/api/users/1` | same as create |

```json
{
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "email": "juan.delacruz@example.com",
  "phone": "09171234567",
  "address": {
    "street": "123 Rizal Street",
    "city": "Makati",
    "state": "Metro Manila",
    "country": "Philippines",
    "zipcode": "1200"
  }
}
```

`Create User` makes a profile **without a login**. Use **Sign Up** for an account that can sign in.

### Products – `/api/products`

| Request | Method | URL | Access |
|---|---|---|---|
| Get All Products | GET | `{{baseUrl}}/api/products` | public |
| Search Products | GET | `{{baseUrl}}/api/products/search?keyword=mouse` | public |
| Create Product | POST | `{{baseUrl}}/api/products` | ADMIN → `201` |
| Update Product | PUT | `{{baseUrl}}/api/products/1` | ADMIN |
| Delete Product | DELETE | `{{baseUrl}}/api/products/1` | ADMIN → `204` (soft delete) |

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "price": 599.00,
  "stockQuantity": 50,
  "category": "Electronics",
  "imageUrl": "https://example.com/mouse.jpg"
}
```

### Cart – `/api/cart` (logged in)

The user comes from the **token**. There is no `X-User-ID` header anymore.

| Request | Method | URL | Body | Success |
|---|---|---|---|---|
| Add to Cart | POST | `{{baseUrl}}/api/cart` | `{"productId": 1, "quantity": 2}` | `201` |
| Get Cart | GET | `{{baseUrl}}/api/cart` | | `200` |
| Remove Item | DELETE | `{{baseUrl}}/api/cart/items/1` (product ID) | | `204` |

### Orders – `/api/orders` (logged in)

- **POST** `{{baseUrl}}/api/orders`, no body
- `201` with the order, then the cart is emptied. `400` if the cart is empty.

## 6. Full test flow

1. **Sign In (admin)** → `200`, token saved
2. **Create Product** → `201`
3. **Sign In (user1)** → `200`, token replaced
4. **Create Product** again → `403` (customers can't)
5. **Add to Cart** → `201`
6. **Get Cart** → 1 item, `price` = 599 × 2 = `1198.00`
7. **Place Order** → `201`, `totalAmount: 1198.00`
8. **Get Cart** → `[]`
9. Clear `jwtToken`, **Get Cart** → `401`

## 7. Security Tests folder (automatic checks)

The imported collection has a **Security Tests** folder: requests that are *supposed* to fail, each with its own pass/fail check.
Right-click the folder → **Run folder** → **Run**. All 24 requests should pass (43 checks).

| Group | Checks |
|---|---|
| Setup | signs up a fresh test account, signs in admin and user1 into `{{adminToken}}` / `{{userToken}}`, signs out to clear the cookie |
| Sign In / Sign Up | wrong password and unknown user both `401 Bad credentials`, blank fields `400`, username/email taken `400`, invalid fields `400` |
| No token | GET products `200` (public), create product / cart / order `401` with the JSON body |
| Customer | create product, list users, delete product → `403` |
| Admin | list users `200` with **no `password` field** |
| Bad tokens | forged payload (`sub` changed to `admin`), garbage, missing `Bearer `, Basic Auth → all `401` |
| Identity | `X-User-ID: 1` is ignored; `/api/auth/username` is `user1` |

The folder uses its own variables, so `{{jwtToken}}` is left alone.
It signs out first because Postman sends the `trainosys-jwt` cookie automatically. Without that, "no token" requests would still be logged in.

## 8. Check the data in H2 Console

1. Open `http://localhost:8080/h2-console`
2. JDBC URL `jdbc:h2:mem:test` · User Name `sa` · Password *(blank)*
3. ```sql
   SELECT ID, USERNAME, PASSWORD, ROLE FROM USER_TABLE;   -- passwords are BCrypt hashes ($2a$10$...)
   SELECT * FROM PRODUCTS;
   SELECT * FROM CART_ITEM;
   SELECT * FROM ORDERS;
   ```
   `ROLE` shows `0` for CUSTOMER and `1` for ADMIN (stored as the enum's position).

## 9. Known behavior (current version of the code)

- **`subTotal` in the order response is wrong.** The item `price` is already unit price × quantity,
  and `subTotal` multiplies by the quantity again (2 × ₱599 shows `subTotal: 2396.00`). `totalAmount` is correct.
- **Stock is not reduced** when an order is placed.
- **Update User without `address`** keeps the old address.

## 10. Common problems

| Problem | Cause / Fix |
|---|---|
| `Could not send request` / `ECONNREFUSED` | The app isn't running. |
| `401` after a restart (signed-up user) | The restart erased the account. Sign Up again. `admin`/`user1` tokens keep working because those users are re-created with the same username. |
| `401` `Full authentication is required` | No token, a typo in it, or `Bearer` missing. Check the **Authorization** tab. |
| `403 Forbidden` | Wrong role. Sign in as `admin` for products/users. |
| `400` on Sign Up with no message | A field breaks a rule (username 3–20, email format, password 6–40). |
| `415 Unsupported Media Type` | Body isn't **raw → JSON**. |
