# 🚀 MINIVERSE - PROJECT ROADMAP
## E-Commerce & Library Management System with AI Chatbot

---

## 📋 TÓM TẮT PHÂN TÍCH HIỆN TRẠNG

### ✅ Đã có sẵn:
- **Spring Boot 4.0.2** với Java 25
- **Security Config** với dual authentication:
  - JWT (stateless) cho REST API `/api/**`
  - Session-based cho Web Application
- **OAuth2** integration (Google)
- **WebSocket** cho real-time chat
- **RabbitMQ** đã cấu hình
- **Entities cơ bản**: User, Role, Book, Category, Invoice, ItemInvoice, ChatMessage
- **Database**: MySQL (Laragon 6.0 / phpMyAdmin)

### ⚠️ Cần bổ sung:
- Thêm nhiều entities theo yêu cầu (Author, Publisher, Field, LibraryCard, BorrowSlip, etc.)
- Hệ thống thư viện (mượn/trả sách)
- AI Chatbot integration
- Blog system
- Dashboard analytics
- Wishlist, Notifications
- Payment integration (VietQR)

---

## 🎯 TỔNG QUAN PHASES

| Phase | Tên | Thời gian ước tính | Mô tả |
|-------|-----|-------------------|-------|
| 1 | Security & Auth Enhancement | 2 tuần | Tăng cường JWT, Cookie, CAPTCHA |
| 2 | Database & Entities | 2 tuần | Thiết kế CSDL đầy đủ |
| 3 | Core Features | 3 tuần | Sản phẩm, Category, Cart, Order |
| 4 | Library System | 3 tuần | Mượn/trả sách, thẻ thư viện |
| 5 | User Roles & Permissions | 2 tuần | Admin, Librarian, User |
| 6 | Blog & Social | 2 tuần | Blog, Comments, Likes, Follow |
| 7 | AI Chatbot | 3 tuần | Tích hợp AI, tự động hóa |
| 8 | Dashboard & Analytics | 2 tuần | Thống kê, báo cáo |
| 9 | Notifications & Realtime | 1 tuần | Chuông TB, WebSocket |
| 10 | Payment & Invoice | 2 tuần | VietQR, PDF Invoice |
| 11 | Frontend Enhancement | 2 tuần | UI/UX optimization |
| 12 | Testing & Deployment | 2 tuần | Testing, CI/CD |

**Tổng thời gian ước tính: 26 tuần (~6.5 tháng)**

---

## 📦 PHASE 1: SECURITY & AUTHENTICATION ENHANCEMENT
**Thời gian: 2 tuần | Priority: CRITICAL**

### Sprint 1.1: JWT Enhancement (Week 1)
```
□ Task 1.1.1: Tạo RefreshToken Entity
  - id, token, user_id, expiry_date, created_at
  - Lưu refresh token vào database
  
□ Task 1.1.2: Token Rotation Strategy
  - Auto-rotate refresh tokens khi refresh
  - Invalidate old refresh tokens
  
□ Task 1.1.3: Token Blacklist
  - Tạo entity TokenBlacklist
  - Kiểm tra blacklist trước khi validate
  - Scheduled job để cleanup expired tokens
  
□ Task 1.1.4: JWT in HttpOnly Cookie
  - Set JWT trong HttpOnly, Secure, SameSite=Strict cookie
  - XSS protection enhancement
```

### Sprint 1.2: Cookie Security & Sessions (Week 2)
```
□ Task 1.2.1: Secure Cookie Configuration
  spring:
    server:
      servlet:
        session:
          cookie:
            http-only: true
            secure: true
            same-site: strict
            max-age: 86400

□ Task 1.2.2: Remember Me với Persistent Token
  - Tạo PersistentLogin entity
  - Token-based remember me (không dùng user password)
  - Secure token generation với SecureRandom
  
□ Task 1.2.3: CSRF Protection Enhancement
  - CSRF token trong cookie và header
  - Double Submit Cookie pattern
  
□ Task 1.2.4: Rate Limiting
  - Bucket4j integration
  - Rate limit cho login, register, API calls
  - IP-based và User-based limiting
  
□ Task 1.2.5: CAPTCHA Integration
  - Google reCAPTCHA v3   - hCaptcha làm backup
  - Audio CAPTCHA cho accessibility
  
□ Task 1.2.6: Password Reset Flow
  - Tạo PasswordResetToken entity
  - AES + RSA hybrid encryption cho token
  - Email verification với thời hạn
```

### Deliverables Phase 1:
```java
// Entities mới
- RefreshToken.java
- TokenBlacklist.java
- PersistentLogin.java
- PasswordResetToken.java

// Security configs
- JwtCookieFilter.java
- RateLimitFilter.java
- CaptchaService.java
- TokenRotationService.java

// application.properties additions
jwt.cookie.name=MV_ACCESS_TOKEN
jwt.refresh.cookie.name=MV_REFRESH_TOKEN
jwt.cookie.secure=true
jwt.cookie.httponly=true
jwt.cookie.samesite=strict
```

---

## 📦 PHASE 2: DATABASE & ENTITIES DESIGN
**Thời gian: 2 tuần | Priority: HIGH**

### Sprint 2.1: Core Entities (Week 3)
```
□ Task 2.1.1: Field (Lĩnh vực) Entity
  CREATE TABLE field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    image VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );

□ Task 2.1.2: Enhanced Category Entity
  - Thêm field_id (FK -> Field)
  - Thêm image, description
  
□ Task 2.1.3: Author (Tác giả) Entity
  CREATE TABLE author (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    birth_date DATE,
    death_date DATE,
    biography TEXT,
    avatar VARCHAR(255),
    created_at TIMESTAMP
  );

□ Task 2.1.4: Publisher (Nhà xuất bản) Entity
  CREATE TABLE publisher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    pen_name VARCHAR(100),
    experience TEXT,
    qualification TEXT,
    created_at TIMESTAMP
  );

□ Task 2.1.5: Enhanced Book Entity
  - isbn VARCHAR(20) UNIQUE
  - cover_image VARCHAR(255)
  - page_count INT
  - total_quantity INT
  - stock_quantity INT
  - edition INT
  - publish_date DATE
  - FK: author_id, publisher_id, category_id
  - status ENUM('AVAILABLE', 'OUT_OF_STOCK', 'DISCONTINUED')
```

### Sprint 2.2: Library System Entities (Week 4)
```
□ Task 2.2.1: LibraryCard (Thẻ thư viện)
  CREATE TABLE library_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    card_number VARCHAR(20) UNIQUE NOT NULL,
    avatar VARCHAR(255),
    issue_date TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    issued_by_librarian_id BIGINT,
    card_type ENUM('STANDARD', 'VIP') DEFAULT 'STANDARD',
    status ENUM('ACTIVE', 'EXPIRED', 'BLOCKED') DEFAULT 'ACTIVE'
  );

□ Task 2.2.2: BorrowSlip (Phiếu mượn sách)
  CREATE TABLE borrow_slip (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    librarian_id BIGINT,
    library_card_id BIGINT NOT NULL,
    borrow_date TIMESTAMP NOT NULL,
    expected_return_date TIMESTAMP NOT NULL,
    actual_return_date TIMESTAMP,
    status ENUM('BORROWING', 'RETURNED', 'OVERDUE', 'EXTENDED') DEFAULT 'BORROWING',
    notes TEXT,
    extension_count INT DEFAULT 0
  );

□ Task 2.2.3: BorrowSlipDetail (Chi tiết phiếu mượn)
  CREATE TABLE borrow_slip_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrow_slip_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    borrow_condition ENUM('NEW', 'GOOD', 'FAIR', 'POOR') NOT NULL,
    return_condition ENUM('NEW', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'LOST'),
    fine_amount DECIMAL(10,2) DEFAULT 0,
    notes TEXT
  );

□ Task 2.2.4: Fine (Phiếu phạt)
  CREATE TABLE fine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    librarian_id BIGINT,
    borrow_slip_id BIGINT,
    reason TEXT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    paid BOOLEAN DEFAULT FALSE,
    paid_date TIMESTAMP,
    created_at TIMESTAMP
  );
  
□ Task 2.2.5: Enhanced User Entity
  - avatar VARCHAR(255)
  - address TEXT
  - gender ENUM('MALE', 'FEMALE', 'OTHER')
  - date_of_birth DATE
  - status ENUM('ACTIVE', 'LOCKED', 'PENDING') DEFAULT 'ACTIVE'
  - last_login TIMESTAMP
  - Quan hệ với LibraryCard, BorrowSlip, Fine
```

### Sprint 2.3: Order & Cart Entities
```
□ Task 2.3.1: Enhanced Cart
  CREATE TABLE cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
  );

□ Task 2.3.2: CartItem
  CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT DEFAULT 1,
    added_at TIMESTAMP,
    UNIQUE(cart_id, book_id)
  );

□ Task 2.3.3: Enhanced Order (Đơn hàng)
  CREATE TABLE `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_code VARCHAR(20) UNIQUE,
    total_amount DECIMAL(12,2),
    shipping_fee DECIMAL(10,2),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(12,2),
    shipping_address TEXT,
    shipping_type ENUM('EXPRESS', 'STANDARD', 'ECONOMY'),
    payment_method ENUM('COD', 'VIETQR', 'VISA'),
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED'),
    status ENUM('PENDING', 'PROCESSING', 'SHIPPING', 'COMPLETED', 'CANCELLED'),
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    confirmed_by BIGINT,
    confirmed_at TIMESTAMP
  );

□ Task 2.3.4: OrderItem
  CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL
  );

□ Task 2.3.5: Wishlist
  CREATE TABLE wishlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    added_at TIMESTAMP,
    UNIQUE(user_id, book_id)
  );
```

### Sprint 2.4: Blog & Notification Entities
```
□ Task 2.4.1: BlogPost
  CREATE TABLE blog_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT,
    media_type ENUM('TEXT', 'IMAGE', 'VIDEO', 'MIXED'),
    media_url VARCHAR(500),
    visibility ENUM('PUBLIC', 'FOLLOWERS', 'PRIVATE'),
    status ENUM('ACTIVE', 'HIDDEN', 'DELETED'),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
  );

□ Task 2.4.2: Comment
  CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_id BIGINT, -- for nested comments
    content TEXT NOT NULL,
    status ENUM('VISIBLE', 'HIDDEN', 'DELETED'),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
  );

□ Task 2.4.3: Like
  CREATE TABLE `like` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT,
    comment_id BIGINT,
    created_at TIMESTAMP,
    UNIQUE(user_id, post_id),
    UNIQUE(user_id, comment_id)
  );

□ Task 2.4.4: Follow
  CREATE TABLE follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    UNIQUE(follower_id, following_id)
  );

□ Task 2.4.5: Notification
  CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type ENUM('ORDER', 'BORROW', 'COMMENT', 'LIKE', 'FOLLOW', 'FINE', 'SYSTEM'),
    title VARCHAR(200),
    content TEXT,
    reference_id BIGINT, -- ID của entity liên quan
    reference_type VARCHAR(50), -- Loại entity (ORDER, POST, etc.)
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
  );
```

### Sprint 2.5: Chat & AI Entities
```
□ Task 2.5.1: ChatConversation (Lịch sử chatbot)
  CREATE TABLE chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    message_count INT DEFAULT 0,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
  );

□ Task 2.5.2: ChatMessage (Chi tiết chatbot)
  CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_type ENUM('USER', 'AI') NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP
  );

□ Task 2.5.3: BlogChatRoom (Phòng chat Blog)
  CREATE TABLE blog_chat_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    type ENUM('PRIVATE', 'GROUP'),
    created_at TIMESTAMP
  );

□ Task 2.5.4: BlogChatParticipant
  CREATE TABLE blog_chat_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP,
    UNIQUE(room_id, user_id)
  );

□ Task 2.5.5: BlogChatMessage
  CREATE TABLE blog_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    media_type ENUM('TEXT', 'IMAGE', 'VIDEO'),
    media_url VARCHAR(500),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
  );
```

### Entity Relationship Diagram (ERD):
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Field     │────<│  Category   │────<│    Book     │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                    ┌─────────────┐            │
                    │   Author    │────────────┤
                    └─────────────┘            │
                    ┌─────────────┐            │
                    │  Publisher  │────────────┘
                    └─────────────┘
                    
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    User     │────<│LibraryCard  │     │   Role      │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                    │
      │                   │                    │
      ▼                   ▼                    │
┌─────────────┐     ┌─────────────┐            │
│ BorrowSlip  │────<│BorrowDetail │            │
└─────────────┘     └─────────────┘            │
      │                                        │
      ▼                                        │
┌─────────────┐                     ┌─────────────┐
│    Fine     │                     │ user_role   │
└─────────────┘                     └─────────────┘
```

---

## 📦 PHASE 3: CORE FEATURES IMPLEMENTATION
**Thời gian: 3 tuần | Priority: HIGH**

### Sprint 3.1: Field & Category Management (Week 5)
```
□ Task 3.1.1: Field CRUD
  - FieldController, FieldService, FieldRepository
  - REST API: /api/v1/fields
  - Web views: /admin/fields/*
  - Image upload với validation
  
□ Task 3.1.2: Category CRUD Enhancement
  - Thêm relationship với Field
  - Nested category support (parent_id)
  - Category tree view
  
□ Task 3.1.3: Author & Publisher CRUD
  - AuthorController, PublisherController
  - REST APIs và Admin views
```

### Sprint 3.2: Book Management Enhancement (Week 5-6)
```
□ Task 3.2.1: Enhanced Book CRUD
  - Thêm các fields mới
  - Multiple image upload
  - ISBN validation (ISBN-10, ISBN-13)
  - Stock management
  
□ Task 3.2.2: Book Search & Filter
  - ElasticSearch integration (optional)
  - Full-text search on title, author, description
  - Filter by: price range, category, field, stock status
  - Pagination với Spring Data
  
□ Task 3.2.3: Book Sorting
  - Sort by: name (A-Z), price (low-high, high-low)
  - Sort by: popularity, newest, bestseller
```

### Sprint 3.3: Cart System (Week 6)
```
□ Task 3.3.1: Cart Service
  - Add to cart (with stock validation)
  - Update quantity
  - Remove item
  - Clear cart
  - Cart persistence (database for logged-in, session for guests)
  
□ Task 3.3.2: Cart Merge
  - Merge guest cart with user cart on login
  - Handle quantity conflicts
  
□ Task 3.3.3: Cart UI
  - Real-time cart count update
  - Cart preview popup
  - Full cart page with summary
```

### Sprint 3.4: Order System (Week 7)
```
□ Task 3.4.1: Checkout Flow
  - Address selection/input
  - Province API integration (https://provinces.open-api.vn)
  - Shipping calculator based on distance
  - Order summary preview
  
□ Task 3.4.2: Order Creation
  - Order code generation (MV + timestamp + random)
  - Stock deduction (with optimistic locking)
  - Transaction management
  
□ Task 3.4.3: Order Status Management
  - Status workflow: PENDING -> PROCESSING -> SHIPPING -> COMPLETED
  - Status change notifications
  - Order history for users
  
□ Task 3.4.4: Order Management Admin
  - Order list with filters
  - Bulk status update
  - Order detail view
  - Export PDF invoice
```

---

## 📦 PHASE 4: LIBRARY SYSTEM
**Thời gian: 3 tuần | Priority: HIGH**

### Sprint 4.1: Library Card Management (Week 8)
```
□ Task 4.1.1: Library Card Registration
  - Registration form (name, email, phone, gender, DOB, address)
  - Auto-generate card number
  - Auto-generate avatar (random animals + hairstyle based on gender)
  - Card validity: 2 years from issue date
  
□ Task 4.1.2: Card Validation
  - Check card status before borrowing
  - Check outstanding fines
  - Card renewal process
  
□ Task 4.1.3: Card Types
  - STANDARD: max 3 books/borrow
  - VIP: max 5 books/borrow (auto-upgrade after 3 successful returns)
```

### Sprint 4.2: Borrow System (Week 9)
```
□ Task 4.2.1: Borrow Request (Online)
  - Select books from catalog
  - Check availability in real-time
  - Submit borrow request
  - Notification to librarian
  
□ Task 4.2.2: Borrow Approval (Librarian)
  - View pending requests
  - Check book condition
  - Approve/Reject with reason
  - Generate borrow slip
  - Update book stock
  
□ Task 4.2.3: Borrow Rules Engine
  - Max books per card type
  - 14-day borrow period
  - 1 extension allowed (+7 days)
  - Check if book is reserved by others
```

### Sprint 4.3: Return & Fine System (Week 10)
```
□ Task 4.3.1: Return Process
  - Check book condition
  - Calculate overdue days
  - Auto-calculate fine: 10,000 VND/day/book
  - Update book stock
  
□ Task 4.3.2: Overdue Handling
  - Daily scheduled job to check overdue
  - Email notification for overdue books
  - 5+ days overdue: Notify Admin + account lock warning
  - Auto-update status to OVERDUE
  
□ Task 4.3.3: Fine Management
  - Fine creation with reason
  - Fine payment tracking
  - Block new borrows if unpaid fine
  - Fine report generation
  
□ Task 4.3.4: Book Condition Handling
  - Condition comparison (borrow vs return)
  - Damage fine calculation
  - Lost book handling (full price + penalty)
```

---

## 📦 PHASE 5: USER ROLES & PERMISSIONS
**Thời gian: 2 tuần | Priority: HIGH**

### Sprint 5.1: Role Enhancement (Week 11)
```
□ Task 5.1.1: Role Hierarchy
  - ROLE_ADMIN (full access)
  - ROLE_LIBRARIAN (library operations)
  - ROLE_USER (standard user)
  
□ Task 5.1.2: Permission System
  CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255)
  );
  
  CREATE TABLE role_permission (
    role_id BIGINT,
    permission_id BIGINT,
    PRIMARY KEY(role_id, permission_id)
  );
  
□ Task 5.1.3: Method-level Security
  @PreAuthorize("hasPermission(#id, 'Book', 'EDIT')")
  public Book updateBook(Long id, BookDTO dto)
  
□ Task 5.1.4: Hidden Menu Items
  - Thymeleaf sec:authorize for role-based menu
  - ADMIN button hidden for non-admins
  - LIBRARIAN button hidden for non-librarians
```

### Sprint 5.2: User Management Admin (Week 12)
```
□ Task 5.2.1: User List & Search
  - Paginated user list
  - Search by name, email, phone
  - Filter by role, status
  
□ Task 5.2.2: User Actions
  - View profile details
  - Upgrade/Downgrade role
  - Lock/Unlock account
  - Password reset (Admin)
  
□ Task 5.2.3: Librarian Management
  - Librarian dashboard
  - Work schedule/attendance log
  - Performance metrics
  - Report submission system
  
□ Task 5.2.4: Attendance System
  CREATE TABLE attendance_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    librarian_id BIGINT NOT NULL,
    check_type ENUM('CHECK_IN_MORNING', 'CHECK_OUT_MORNING', 
                   'CHECK_IN_AFTERNOON', 'CHECK_OUT_AFTERNOON'),
    check_time TIMESTAMP,
    is_late BOOLEAN DEFAULT FALSE,
    notes TEXT
  );
```

---

## 📦 PHASE 6: BLOG & SOCIAL FEATURES
**Thời gian: 2 tuần | Priority: MEDIUM**

### Sprint 6.1: Blog Core (Week 13)
```
□ Task 6.1.1: Post Management
  - Create post with text/image/video
  - Edit/Delete own posts
  - Visibility settings
  - Media upload (images, videos)
  
□ Task 6.1.2: Post Feed
  - Timeline view (newest first)
  - Infinite scroll pagination
  - Post preview with media
  
□ Task 6.1.3: Comments
  - Add/Edit/Delete comments
  - Nested comments (replies)
  - Comment moderation (Admin)
  
□ Task 6.1.4: Likes
  - Like/Unlike posts and comments
  - Like count display
  - Real-time update with WebSocket
```

### Sprint 6.2: Social Features (Week 14)
```
□ Task 6.2.1: Follow System
  - Follow/Unfollow users
  - Follower/Following counts
  - Following feed filter
  
□ Task 6.2.2: User Profile Page
  - Profile with avatar, bio
  - All posts by user
  - Media gallery (images/videos)
  - Follower/Following lists
  
□ Task 6.2.3: Real-time Chat (Blog)
  - Private chat between users
  - Online status indicator
  - Message history
  - Media sharing in chat
  
□ Task 6.2.4: Content Moderation
  - Bad word filter
  - Report system
  - 3 violations = comment block
  - Admin moderation dashboard
```

---

## 📦 PHASE 7: AI CHATBOT INTEGRATION
**Thời gian: 3 tuần | Priority: HIGH**

### Sprint 7.1: Chatbot Infrastructure (Week 15)
```
□ Task 7.1.1: OpenAI Integration
  - OpenAI API client service
  - GPT-4 / GPT-3.5-turbo integration
  - Prompt engineering for MiniVerse context
  - Rate limiting for API calls
  
□ Task 7.1.2: Conversation Management
  - Create/Continue conversations
  - Conversation history storage
  - Context window management
  - Delete conversation option
  
□ Task 7.1.3: Chat UI
  - Chat widget (floating button)
  - Full chat page
  - Message bubbles (user/AI)
  - Typing indicator
  - Quick action buttons
```

### Sprint 7.2: Chatbot Features - Library (Week 16)
```
□ Task 7.2.1: Library Card Creation via Chat
  Prompt flow:
  1. User: "Tôi muốn lập thẻ thư viện"
  2. AI: Thu thập thông tin (name, email, phone, gender, DOB)
  3. AI: Xác nhận thông tin
  4. AI: Tạo thẻ và generate avatar
  5. AI: Gửi kết quả với thông tin thẻ
  
□ Task 7.2.2: Book Borrowing via Chat
  Function calling:
  - check_library_card(user_id)
  - check_book_availability(book_id)
  - create_borrow_request(user_id, book_ids[])
  - Hiển thị quy định mượn trả
  - Xác nhận và tạo phiếu mượn
  
□ Task 7.2.3: Borrow Rules Display
  - Display borrowing regulations table
  - Answer FAQ about library services
  - Due date reminders
```

### Sprint 7.3: Chatbot Features - E-commerce (Week 17)
```
□ Task 7.3.1: Product Consultation
  Function calling:
  - search_books(query, filters)
  - get_book_details(book_id)
  - get_recommendations(user_preferences)
  
  Example:
  User: "Tôi muốn mua sách tiếng Anh cho người mới bắt đầu"
  AI: Phân tích -> Hỏi thêm (speaking/writing/reading?) -> Gợi ý sản phẩm
  
□ Task 7.3.2: Order Assistance
  - Add to cart via chat
  - Check order status
  - Order history query
  
□ Task 7.3.3: Avatar Generation
  - Integration với AI image generation (DALL-E / Stable Diffusion)
  - Chibi style animal face + human hair
  - Gender-based hairstyle selection
  - Unique avatar per user
```

---

## 📦 PHASE 8: DASHBOARD & ANALYTICS
**Thời gian: 2 tuần | Priority: MEDIUM**

### Sprint 8.1: Admin Dashboard (Week 18)
```
□ Task 8.1.1: Summary Cards
  - Monthly revenue (Admin only)
  - Monthly orders count
  - Today's orders
  - Pending orders
  - Today's revenue summary
  
□ Task 8.1.2: Product Analytics
  - Total products in stock
  - Total categories/fields
  - Low stock alerts
  - Bestsellers / Slow movers (pie chart)
  
□ Task 8.1.3: Library Analytics (Admin + Librarian)
  - Total registered readers
  - Currently borrowing count
  - Overdue borrows count
  - Category distribution (bar chart)
  - Field distribution (vertical bar chart)
```

### Sprint 8.2: Charts & Reports (Week 19)
```
□ Task 8.2.1: Chart Integration
  - Chart.js integration
  - Pie charts: Product/Category sales %
  - Bar charts: Inventory by category/field
  - Line charts: Revenue over time
  
□ Task 8.2.2: Payment Analytics
  - Payment method distribution (COD vs VietQR)
  - Payment success rate
  
□ Task 8.2.3: User Analytics
  - Top customer of the week (most purchases)
  - Best librarian of the month (attendance + sales)
  - Top blogger (most likes in week)
  
□ Task 8.2.4: Export Reports
  - Excel export (Apache POI)
  - PDF export (iText / JasperReports)
  - Scheduled report generation
```

---

## 📦 PHASE 9: NOTIFICATIONS & REAL-TIME
**Thời gian: 1 tuần | Priority: MEDIUM**

### Sprint 9.1: Notification System (Week 20)
```
□ Task 9.1.1: Notification Generation
  - Order status change -> Notification
  - Borrow approval -> Notification
  - Overdue reminder -> Notification
  - Fine issued -> Notification
  - New follower -> Notification
  - Post like/comment -> Notification
  
□ Task 9.1.2: Notification Bell UI
  - Bell icon in header
  - Unread count badge
  - Dropdown list of recent notifications
  - Mark as read/all read
  
□ Task 9.1.3: Real-time Notifications
  - WebSocket push notifications
  - Browser push notifications (optional)
  - Sound alert for new notifications
  
□ Task 9.1.4: Email Notifications
  - Order confirmation email
  - Borrow due reminder
  - Overdue warning (5+ days)
  - Password reset email
  - Welcome email
```

---

## 📦 PHASE 10: PAYMENT & INVOICE
**Thời gian: 2 tuần | Priority: HIGH**

### Sprint 10.1: Payment Integration (Week 21)
```
□ Task 10.1.1: VietQR Integration
  - VietQR API integration
  - QR code generation for payment
  - Bank account: 22653537 (ACB)
  - Payment verification webhook
  
□ Task 10.1.2: Shipping Fee Calculator
  - Google Maps Distance API
  - Express: dynamic fee based on distance
  - Standard (HCM): 25,000 VND
  - Economy (other provinces): 35,000 VND
  
□ Task 10.1.3: Checkout Page
  - Address input with autocomplete
  - Province/District/Ward selector
  - Map selection mode
  - Shipping type selection
  - Payment method selection
  - Order summary
```

### Sprint 10.2: Invoice System (Week 22)
```
□ Task 10.2.1: PDF Invoice Generation
  - iText PDF library
  - Invoice template design
  - Company info, order details, totals
  - QR code for verification
  
□ Task 10.2.2: Invoice Delivery
  - Download PDF from order history
  - Email invoice on order completion
  - Admin invoice export
  
□ Task 10.2.3: Receipt for Fines
  - Fine payment receipt
  - Library transaction history
```

---

## 📦 PHASE 11: FRONTEND ENHANCEMENT
**Thời gian: 2 tuần | Priority: MEDIUM**

### Sprint 11.1: UI/UX Improvements (Week 23)
```
□ Task 11.1.1: Responsive Design
  - Mobile-first approach
  - Sidebar menu for mobile
  - Touch-friendly interactions
  
□ Task 11.1.2: Homepage Enhancement
  - Hero banner with slogan
  - Field icons carousel
  - Featured categories
  - Bestseller products (4 per page, arrows)
  - Smooth scroll animations
  
□ Task 11.1.3: Sticky Header
  - Header stays visible on scroll
  - Compact mode when scrolled
  
□ Task 11.1.4: AI Chatbot Widget
  - Floating button bottom-right
  - Expandable chat window
  - Minimize/maximize animation
```

### Sprint 11.2: Interactive Features (Week 24)
```
□ Task 11.2.1: Real-time Stock Update
  - WebSocket for stock changes
  - "Out of Stock" badge update
  - Cart quantity validation
  
□ Task 11.2.2: Search Enhancement
  - Autocomplete search
  - Search suggestions
  - Recent searches
  - Voice search (optional)
  
□ Task 11.2.3: Product Page
  - Image gallery/lightbox
  - Related products
  - Customer reviews
  - Add to wishlist animation
  
□ Task 11.2.4: Error Pages
  - Custom 403, 404, 500 pages
  - Friendly error messages
  - Navigation suggestions
```

---

## 📦 PHASE 12: TESTING & DEPLOYMENT
**Thời gian: 2 tuần | Priority: CRITICAL**

### Sprint 12.1: Testing (Week 25)
```
□ Task 12.1.1: Unit Tests
  - Service layer tests
  - Repository tests with @DataJpaTest
  - Utility class tests
  - Aim for 80%+ code coverage
  
□ Task 12.1.2: Integration Tests
  - Controller tests with MockMvc
  - Security tests
  - API endpoint tests
  
□ Task 12.1.3: End-to-End Tests
  - Selenium/Playwright for UI tests
  - Critical user flows:
    - Registration/Login
    - Browse/Search products
    - Add to cart/Checkout
    - Borrow book flow
    - AI chat interaction
```

### Sprint 12.2: Deployment (Week 26)
```
□ Task 12.2.1: Environment Setup
  - Production application.properties
  - Environment variables for secrets
  - Database migration scripts
  
□ Task 12.2.2: Docker
  - Dockerfile for Spring Boot app
  - docker-compose.yml (app + MySQL + Redis)
  
□ Task 12.2.3: CI/CD Pipeline
  - GitHub Actions workflow
  - Build -> Test -> Deploy
  - Automated deployment to server
  
□ Task 12.2.4: Monitoring
  - Spring Boot Actuator
  - Prometheus metrics
  - Grafana dashboard
  - Log aggregation (ELK stack optional)
  
□ Task 12.2.5: Security Hardening
  - HTTPS configuration
  - Content Security Policy
  - Rate limiting in production
  - SQL injection prevention verification
  - XSS prevention verification
```

---

## 🔐 BỔ SUNG: JWT & COOKIE SECURITY BEST PRACTICES

### JWT Configuration chi tiết:
```java
// JwtProperties.java
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessExpiration = 900000;      // 15 minutes
    private long refreshExpiration = 604800000;  // 7 days
    private String cookieName = "MV_ACCESS_TOKEN";
    private String refreshCookieName = "MV_REFRESH_TOKEN";
    private boolean cookieSecure = true;
    private boolean cookieHttpOnly = true;
    private String cookieSameSite = "Strict";
    private String cookiePath = "/";
    private int cookieMaxAge = 86400; // 1 day
}
```

### Cookie Security Headers:
```java
// SecurityHeadersFilter.java
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self' 'unsafe-inline'");
        filterChain.doFilter(request, response);
    }
}
```

### Dual Token Strategy:
```
┌─────────────────────────────────────────────────────────────┐
│                    JWT Authentication Flow                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Login Request                                            │
│     POST /api/auth/login                                     │
│     Body: { username, password }                              │
│                                                               │
│  2. Server Response (Success)                                │
│     Set-Cookie: MV_ACCESS_TOKEN=jwt...; HttpOnly; Secure    │
│     Set-Cookie: MV_REFRESH_TOKEN=refresh...; HttpOnly; Secure│
│     Body: { user info, expires_in }                          │
│                                                               │
│  3. Subsequent API Requests                                  │
│     Cookie: MV_ACCESS_TOKEN=jwt...                           │
│     → Extracted by JwtCookieFilter                           │
│                                                               │
│  4. Access Token Expired                                     │
│     → 401 Response                                           │
│     → Frontend calls /api/auth/refresh                       │
│     → Server validates refresh token                         │
│     → New access token issued                                 │
│                                                               │
│  5. Logout                                                   │
│     POST /api/auth/logout                                    │
│     → Add tokens to blacklist                                 │
│     → Clear cookies                                          │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 CẤU TRÚC THƯ MỤC ĐỀ XUẤT

```
src/main/java/fit/hutech/BuiBaoHan/
├── BuiBaoHanApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── RabbitMQConfig.java
│   ├── OpenAIConfig.java
│   ├── CacheConfig.java
│   └── AsyncConfig.java
├── constants/
│   ├── RoleConstants.java
│   ├── OrderStatus.java
│   ├── BorrowStatus.java
│   └── NotificationType.java
├── controllers/
│   ├── api/
│   │   ├── AuthApiController.java
│   │   ├── BookApiController.java
│   │   ├── ChatApiController.java
│   │   └── ...
│   └── web/
│       ├── HomeController.java
│       ├── BookController.java
│       ├── AdminController.java
│       └── ...
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── ...
│   └── response/
│       ├── JwtResponse.java
│       ├── ApiResponse.java
│       └── ...
├── entities/
│   ├── User.java
│   ├── Role.java
│   ├── Book.java
│   ├── Category.java
│   ├── Field.java
│   ├── Author.java
│   ├── Publisher.java
│   ├── LibraryCard.java
│   ├── BorrowSlip.java
│   ├── BorrowSlipDetail.java
│   ├── Fine.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Wishlist.java
│   ├── BlogPost.java
│   ├── Comment.java
│   ├── Like.java
│   ├── Follow.java
│   ├── Notification.java
│   ├── ChatConversation.java
│   ├── ChatMessage.java
│   ├── RefreshToken.java
│   └── TokenBlacklist.java
├── repositories/
│   └── ...
├── services/
│   ├── impl/
│   │   └── ...
│   ├── UserService.java
│   ├── BookService.java
│   ├── LibraryService.java
│   ├── OrderService.java
│   ├── ChatbotService.java
│   ├── NotificationService.java
│   └── ...
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtCookieFilter.java
│   ├── RateLimitFilter.java
│   ├── SecurityHeadersFilter.java
│   └── CustomUserDetailsService.java
├── validators/
│   └── ...
├── exceptions/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
└── utils/
    ├── DateUtils.java
    ├── FileUtils.java
    └── StringUtils.java
```

---

## 🎯 MILESTONES & CHECKPOINTS

| Milestone | Phase | Deadline (Week) | Deliverable |
|-----------|-------|-----------------|-------------|
| M1 | Phase 1-2 | Week 4 | Security + Database Ready |
| M2 | Phase 3-4 | Week 10 | Core Features + Library System |
| M3 | Phase 5-6 | Week 14 | Roles + Blog Complete |
| M4 | Phase 7 | Week 17 | AI Chatbot Integrated |
| M5 | Phase 8-9 | Week 20 | Dashboard + Notifications |
| M6 | Phase 10 | Week 22 | Payment + Invoicing |
| M7 | Phase 11 | Week 24 | Frontend Polish |
| M8 | Phase 12 | Week 26 | Launch Ready |

---

## 📌 GHI CHÚ QUAN TRỌNG

1. **Security First**: Luôn implement security features trước khi add features mới
2. **Test Early**: Viết unit tests song song với code
3. **Database Migrations**: Sử dụng Flyway/Liquibase cho database migration
4. **API Documentation**: SwaggerUI cho REST API documentation
5. **Git Workflow**: GitFlow với branches: main, develop, feature/*, release/*
6. **Code Review**: Mỗi PR cần ít nhất 1 review

---

**Document Version**: 1.0  
**Created**: February 2026  
**Last Updated**: February 6, 2026  
**Author**: GitHub Copilot (Claude Opus 4.5)
