# PROJECT REQUIREMENT: MINIVERSE - E-COMMERCE & LIBRARY MANAGEMENT SYSTEM
## 1. Context & Role
Bạn là một **Senior Solution Architect** và **Project Manager**. Nhiệm vụ của bạn là phân tích yêu cầu dự án "MiniVerse" dưới đây để đưa ra một bản kế hoạch triển khai (Project Roadmap) chia theo từng giai đoạn (Phases) và kế hoạch chi tiết (Sprint Plan).

**Tech Stack yêu cầu:**
- Backend: Java Spring Boot.
- IDE: VS Code 2022.
- Database: MySQL (chạy trên Laragon 6.0 / phpMyAdmin).
- Frontend: (Tùy chọn theo đề xuất của AI để phù hợp với Spring Boot).

1. Giới thiệu
1.1 Tên đề tài
•	MiniVerse – website cung cấp sản phầm về sách tích hợp AI Chatbot.

1.2 Vai trò website
•	Website cung cấp các sản phẩm về sách với nhiều lĩnh vực như: sách giáo khoa, truyện tranh, truyện kinh dị, tiểu thuyết, sách dạy nấu ăn,...
•	Website như 1 thư viện cho mượn sách, giúp các bạn trẻ như học sinh, sinh viên mở rộng kiến thức hơn.
•	Tích hợp AI Chatbot: hỗ trợ mượn trả sách online nhanh chóng, gợi ý sản phẩm theo nhu cầu học tập.
•	Trang Blog giúp những người yêu sách kết nối với nhau.

2. Giao diện website + giới thiệu các trang
2.1 Giao diện người dùng (USER)
2.1.1 Header: các nút theo thứ tự từ trái qua
•	Menu sidebar xổ xuống: nơi chứa các button như:
o	Đơn hàng của tôi: các đơn hàng đã đặt
o	Đánh giá : đánh giá sản phẩm đã mua
o	Wishlist: danh sách yêu thích
o	About Us: Trang giới thiệu về shop
o	Blog: diễn đàn nơi các bạn trẻ đăng tải những điều mình muốn nói, hình ảnh, video, trò chuyện cũng nhau.
•	Logo Miniverse: trỏ đến trang HOME (khi click vào logo ảnh)
•	Home: Trang chủ website
•	Product: các sản phẩm sách, gồm nhiều lĩnh vực như: sách giáo khoa, sách dạy nấu ăn, truyện tranh, tiểu thuyết,..Mỗi lĩnh vực gồm nhiều thể loại: ví dụ: truyện tranh thuộc thể loại truyện kinh dị/truyện trinh thám,...
•	Service: dịch vụ mượn/trả sách (trang này sẽ hiển thị giá tiền, cách thức mượn trả online/offline sách như thế nào).
•	Chuông: chuông thông báo về đơn hàng đã được xác nhận, bật trang thái nào rồi. Thông báo dịch vụ dã được xác nhận mượn/trả như nào.
•	Thanh tìm kiếm: tìm kiếm sản phẩm theo tên sản phẩm (không quan trọng chữ thường/chữ hoa).
•	Giỏ hàng: nơi chứa các sản phẩm người dùng thêm vào để tiếp tục bước thanh toán.
•	Hồ sơ (biểu tượng User): chứa các button như
o	Đăng nhập/Đăng ký tài khoản (nếu người dùng đã có tài khoản, đã đăng nhập vào rồi nút này sẽ ẩn đi).
o	Hồ sơ cá nhân
o	Đổi mật khẩu
o	Đăng xuất
o	ADMIN (Nút này là nút “Ẩn” chỉ có vai trò admin thấy)
o	LIBRARIAN (Nút này là nút “Ẩn” chỉ có vai trò thủ thư thấy)

2.1.2 Footer
•	About Us
•	Câu hỏi thường gặp
•	Phương thức thanh toán: hiển thị 4 logo: VISA,VIETQR, JCB, Tiền mặt
•	Kết nối với chúng tôi: 3 logo mạng xã hội: FACEBOOK, TIKTOK, INSTAGRAM.

2.1.3 Chức năng chỉnh
•	Xem, tìm kiếm, đặt mua các sản phẩm, lĩnh vực, thể loại sách shop cung cấp.
•	Xem sản phẩm theo thương hiệu, giá tiền, tên.
•	Biết được số lượng của thông tin sản phẩm: còn hàng hoặc hết hàng.
•	Thêm sản phẩm vào giỏ hàng, đặt hàng.
•	Xem được trạng thái đơn hàng: Chờ xác nhận, Đang xử lý, Đang giao hàng, Hoàn thành, Đã hủy.
•	Thêm sản phẩm vào danh sách yêu thích.
•	Sử dụng trang Blog để: đăng bài viết, hình ảnh, video. Trò chuyện với nhiều người trong cộng đồng yêu sách trên toàn cầu.
•	Chat với AI Chatbot: hỏi về cách thức mượn/trả sách online/offline, nhờ AI Chatbot đặt mua sản phẩm dùm, nhờ tư vấn sách phù hợp theo nhu cầu cá nhân.
•	Thanh toán các sản phẩm bằng VietQR
•	Sau khi thanh toán nhận được hóa đơn bằng file pdf.

2.2 Giao diện ADMIN (Quản trị viên)
2.2.1 Header: như giao diện USER nhưng thêm nút ẩn “ADMIN”
2.2.2 Footer: như giao diện USER
2.2.3 Trang nút “ADMIN” (Từ trái qua phải)
•	Menu sidebar xổ xuống (để chuột vào có thể mở rộng ngang qua /đóng lại cho gọn trang)
o	Dashboard: Màn hình quản trị hệ thống, nơi chứa dữ liệu, biểu đồ tròn, biểu đồ cột thống kê’
	ADMIN + LIBRARIAN: xem được 
	Nút xuất file EXCEL: xuất tất cả thống kê
	Màn hình vuông hiển thị tiêu đề, số lượng, nút “Làm mới dữ liệu”
	Tổng số sản phẩm tồn kho
	Tổng số thể loại tồn kho
	Tổng số lĩnh vực tồn kho
	Tổng số đọc giả đang có trong hệ thống (Các đọc giả đã mượn sách)
	Tổng số đọc giả đang mượn sách
	Tổng số đọc giả quá hạn trả sách
	Màn hình Biểu đồ cột ngang: Báo cáo số lượng thể loại sách hiện có
	Màn hình biểu đồ cột ngang: Báo cáo số lượng thể loại sản phầm tồn kho
	Màn hình biểu đồ cột dọc: Báo cáo số lượng lĩnh vực hiện có
	Màn hình biểu đồ cột dọc: Báo cáo số lượng lĩnh vực tồn kho


	Chỉ “ADMIN” xem được
	Màn hình vuông chỉ hiển thị số
	Doanh thu (tháng)
	Số Đơn hàng (tháng)
	Đơn hàng hôm nay
	Đơn chờ xử lý
	Tổng quan doanh thu hôm nay
	Màn hình biểu đồ tròn: hiển thị % 
	Thống kế tất cả sản phẩm: Thống kê sản phẩm bán chạy nhất chiếm bao nhiêu % tổng, Thống kê sản phẩm ít bán chạy nhất.
	Thống kê lĩnh vực/thể loại được bán chạy – ít bán chạy nhất.
	Thống kê phương thức thanh toán hiện được chọn nhiều nhất: Thanh toán khi nhận hàng, VietQR.
	Màn hình chữ nhật dài chứa ảnh avatar, tên 
	Thống kê khách hàng yêu sách nhất trong tuần (VIP: mua nhiều nhất trong tháng)
	Thống kê nhân viên gương mẫu nhất tháng (chấm công đúng giờ (1 ngày chấm công đủ 4 lần không trễ ngày nào), bán được nhiều tiền nhất trong tháng)
	Thống kê user có bài viết được tim nhiều nhất trong tuần
o	Quản lý sản phẩm
	Xem danh sách sản phẩm, Tìm kiếm, Thêm/Xóa/Sửa các sản phẩm
o	Quản lý đơn hàng
	Xem danh sách đơn hàng
	Tìm kiếm
	Xác nhận đơn hàng/ Bật trạng thái: Chờ xác nhận, Đang xử lý, Đang giao hàng, Hoàn thành, Đã hủy
	Nút “Xuất hóa đơn file PDF”
o	Quản lý lĩnh vực
	Xem danh sách lĩnh vực, Tìm kiếm, Thêm/Xóa/Sửa các lĩnh vực
o	Quản lý thể loại
	Xem danh sách thể loại, Tìm kiếm, Thêm/Xóa/Sửa các thể loại
o	Quản lý dịch vụ
	Xem danh sách dịch vụ, Tìm kiếm, Thêm/Xóa/Sửa dịch vụ
o	Quản lý User 
	Xem danh sách User , Tìm kiếm.
	Phân quyền: nâng cấp/xuống cấp các vai trò: Admin, User, Librarian (thủ thư)
	Khóa tài khoản (User /Librarian) không thể đăng nhập vào tài khoản được nữa)
	Mở tài khoản
o	Quản lý Thủ thư 
	Xem danh sách nhân sự
	Xem báo cáo của nhân sự
	Đọc, Gửi trả lời lại báo cáo
	Tìm kiếm.
	Phân quyền: nâng cấp/xuống cấp các vai trò: Admin, User, Librarian (thủ thư)
	Khóa tài khoản
	Mở tài khoản
	Xem thống kê log chấm công (lịch làm/log chấm công)

2.2.4 Chức năng chính Admin
•	Quản lý thống kê doanh thu, Sản phẩm, Đơn hàng, Lĩnh vực, Thể loại, Khách hàng, Nhân sự,..
•	Tìm kiếm/Thêm/Xóa/Sửa/Xem chi tiết Sản phẩm, Đơn hàng, Lĩnh vực, Thể loại, Khách hàng, Nhân sự,..
•	Xuất hóa đơn đơn hàng bằng file PDF, Excel.
•	Xem profile, Phân quyền: Admin/User/Librarian, Đặt lại mật khẩu, Khóa tài khoản người dùng.
•	Quản lý trang Blog: 
	Khóa chức năng comment nếu comment những từ ngữ vi phạm tiêu chuẩn cộng đồng, có hành vi lăng mạ người khác quá 3 lần.
	Xem được danh sách bài viết trên toàn trang Blog (tất cả người dùng)
	Thống kê trên dashboard những bài viết: mới nhất, lượt thích nhiều nhất/ít nhất

2.3 Giao diện thủ thư (Nhân viên vận hành)
Quản lý vòng đời của cuốn sách và tương tác trực tiếp với yêu cầu của khách hàng. Đây là vai trò "thực thi" để Admin không phải làm những việc lặp đi lặp lại hàng ngày.
2.3.1 Header + Footer: Như User nhưng sẽ thêm nút ẩn “LIBRARIAN”
2.3.2 Trang nút “LIBRARIAN”
•	Menu xổ xuống
o	Dashboard thống kê: giống dashboard ADMIN nhưng chỉ xem được
	ADMIN + LIBRARIAN: xem được 
	Nút xuất file EXCEL: xuất tất cả thống kê
	Màn hình vuông hiển thị tiêu đề, số lượng, nút “Làm mới dữ liệu”
	Tổng số sản phẩm tồn kho
	Tổng số thể loại tồn kho
	Tổng số lĩnh vực tồn kho
	Tổng số đọc giả đang có trong hệ thống (Các đọc giả đã mượn sách)
	Tổng số đọc giả đang mượn sách
	Tổng số đọc giả quá hạn trả sách
	Màn hình Biểu đồ cột ngang: Báo cáo số lượng thể loại sách hiện có
	Màn hình biểu đồ cột ngang: Báo cáo số lượng thể loại sản phầm tồn kho
	Màn hình biểu đồ cột dọc: Báo cáo số lượng lĩnh vực hiện có
	Màn hình biểu đồ cột dọc: Báo cáo số lượng lĩnh vực tồn kho

o	Phê duyệt Mượn/Trả: Tiếp nhận yêu cầu mượn từ USER, xác nhận khi khách đến lấy sách hoặc trả sách.
o	Xử lý Vi phạm: Ghi nhận sách trả muộn, tính phí phạt nếu sách bị hỏng hoặc mất.
o	Báo cáo nghiệp vụ: Xuất danh sách sách đang cho mượn, sách quá hạn, sách cần nhập thêm.

2.3.3 Luồng hoạt động của Thủ thư
2.3.3.1: Luồng duyệt mượn sách (dịch vụ)
Xác nhận người dùng đã có thẻ thư viện -> chưa -> yêu cầu lập -> có thẻ -> Kiểm tra thẻ (có quá hạn sách nào không) -> có -> yêu cầu đóng tiền rồi mới cho mượn sách tiếp, không quá hạn -> cho người dùng chọn sách -> kiểm tra sách -> sách có hỏng/rách gì không? ->nếu có -> yêu cầu chọn sách khác -> nếu không hỏng -> duyệt -> Lập phiếu mượn sách.

2.3.3.2: Luồng trả sách
1. Thủ thư kiểm tra -> Người dùng quá hạn -> gửi thông báo qua gmail. Nếu quá hạn 5 ngày -> Gửi thông báo đến Admin -> Admin khóa tài khoản
2. Người dùng trả sách -> Kiểm tra thẻ thư viện -> Không quá hạn ->Duyệt.

3. Chi tiết trang
3.1 HOME
•	Trang chủ chứa đầy đủ header, footer.
•	Khi cuộn xuống thêm hiệu ứng không mất thanh header.
•	Khung banner: hình ảnh + Câu sloggan, làm rõ được shop muốn bán/cung cấp dịch vụ gì.
•	Lĩnh vực: các ô biểu tượng các lĩnh vực như: truyện tranh, tiểu thuyết, sách giáo khoa (biểu tượng đơn giản, dễ nhìn), kéo qua lại để xem tiếp các lĩnh vực.
•	Thể loại nổi bật: các ô danh sách các thể loại nổi bật như: truyện tranh thiếu nhi, truyện ngôn tình, sách toán lớp 12,...kéo qua lại xem tiếp các thể loại.
•	Sản phẩm bán chạy nhất: các ô sản phẩm có ảnh, giá, nút “Mua ngay”, “Thêm vào giỏ hàng” các sản phẩm bán chạy nhất. Chỉ để tối đa 4 sản phẩm ở trang đầu ở dưới sẽ có 2 nút mũi tên trái, phải click vào sẽ hiển thị tiếp các sản phẩm.
•	Nút biểu tượng AI Chatbot tròn nhỏ góc phải màn hình

3.2 Product
•	Đầy đủ header, footer mặc định
•	Góc trái màn hình là ô danh sách lĩnh vực: mỗi lĩnh vực có nhiều thể loại, mỗi thể loại có nhiều sản phẩm (làm gọn sao cho khách hàng dễ mua hàng).
•	Ở giữa hiển thị tất cả sản phẩm
•	Thanh tìm kiếm: tìm kiếm tên sản phẩm (chữ thường/chữ hoa đều được). 
•	Nút chọn sắp xếp: Sắp xếp theo tên chữ cái (A->Z), Sắp xếp theo giá tiền (làm nút tròn kéo giá tiền mong muốn được: đầu trái là giá thấp nhất -> đầu cuối là giá cao nhất), Sắp xếp chọn theo lĩnh vực, Sắp xếp chọn theo thể loại.

3.3 Service
•	Hiển thị danh sách dịch vụ
•	Cách thức mượn sách, trả sách
•	Xử phạt khi không trả sách đúng hạn
•	Để được mượn sách khách hàng phải làm thẻ thư viện.

3.4 Blog
•	Thanh trạng thái: thêm bài viết/hình ảnh/video, nhập cảm nghĩ của bạn
•	Danh sách các bài viết của mọi người
•	Góc phải màn hình: Chat cùng bạn bè
•	Khi click vào tên người dùng nào sẽ hiển thị profile trang họ
o	Trang profile: 1. Nút follow/unfollow, 2. Người follow (Người follow họ), 3. Đang follow (Người họ follow). Ở dưới sẽ hiển thị các nút: Tất cả bài viết, Tất cả hình ảnh/Video (nếu có)

3.5 About Us
•	Giới thiệu shop: Phong cách Ngắn gọn & Hiện đại (Minimalist)
o	MiniVerse: Đọc để chạm tới những vì sao.
o	Vũ trụ nhỏ, tri thức lớn.
o	Chạm vào vô cực từ trang giấy.
•	Địa chỉ shop: hiển thị bản đồ GoogleMap đến địa chỉ shop: “10/80c Song Hành Xa Lộ Hà Nội, Phường Tân Phú, Thủ Đức, Thành phố Hồ Chí Minh, Việt Nam”
•	Số điện thoại shop: +84 329222698.
•	Mạng xã hội: Facebook: MiniVerse: link facebook: “ https://www.facebook.com/han.bao.835638/?locale=vi_VN “, Instagram: b._.baohan

3.6 Wishlist
Hiển thị danh sách sản phẩm người dùng cho vào danh sách yêu thích



3.7 Giỏ hàng
3.7.1 Giỏ hàng trống
•	Hiển thị biểu tượng giỏ hàng trống.
•	Nút “Mua sắm ngay”.
3.7.1 Giỏ hàng có sản phẩm
Hiển thị danh sách các sản phẩm thêm vào, kèm thêm tổng giá tiền các sản phẩm.

3.8 Đơn hàng
Trang này cho thấy bảng đơn hàng sản phẩm, đơn  hàng dịch vụ (mượn sách) gồm các ô trạng thái
•	Đơn hàng sẽ có 5 trạng thái: Chờ xác nhận, Đang xử lý, Đang giao hàng, Hoàn thành, Đã hủy.
3.8.1 Chờ xác nhận
Trang này cho biết sẽ phải chờ Admin xác nhận đơn hàng 

3.8.2 Đang xử lý
Trang này cho biết shop đang chuẩn bị hàng, chờ Admin bật trạng thái tiếp theo

3.8.3 Đang giao hàng
Trang này cho biết shipper đang giao hàng, chờ đến giao hàng, Admin vẫn bật trạng thái tiếp theo được. 

3.8.4 Hoàn thành
Trang này cho biết đơn hàng đã hoàn tất, đã giao thành công. Sẽ hiển thị thêm nút kế bên “Đánh giá”, sẽ dẫn bạn đến trang đánh giá để đánh giả sản phẩm.

3.8.4: Đã hủy
Trang này hiển thị đơn hàng user chọn hủy do vì lí do nào đó

3.9 Đánh giá
Trang này hiển thị các đánh giá của khách hàng (đơn hàng đã hoàn thành rồi mới được dánh giá). (Rating 1-5 sao) 

3.10 Chatbot AI
•	Trang giao diện chat giữa người dùng và AI 
•	Hiển thị các lựa chọn dễ dàng trước như
1.	AI sẽ nhận đặt lịch mượn sách online dùm: nút “Mượn sách”: 
o	AI sẽ xem profile của user đã có “thẻ thư viện” chưa, có mới được quyền mượn sách. 
o	Nếu “không có”: AI sẽ hiển thị nút “Lập thẻ thư viện” dẫn đến nút thứ 3 ở dưới để lập thẻ rồi mới được mượn.
o	Nếu “có” AI sẽ lấy thông tin từ thẻ thư viện và yêu cầu người dùng gửi thêm thông tin sản phẩm muốn mượn: tên lĩnh vực/thể loại/sản phẩm muốn mượn, 
o	Đưa ra quy định mượn/trả sách online/offline. 

	BẢNG QUY ĐỊNH MƯỢN/ TRẢ SÁCH
Chỉ mục	Quy định chi tiết
Số lượng mượn tối đa	Tối đa 3 cuốn/lần đối với tài khoản có thẻ mượn sách mới, 5 cuốn với VIP (Tài khoản có thẻ mượn sách: trả đúng hạn với 3 đơn hàng trở lên)
Thời hạn mượn	14 ngày kể từ ngày phê duyệt.
Gia hạn	Được phép gia hạn 01 lần (thêm 7 ngày) nếu sách chưa quá hạn.
Phí trả muộn	10,000 VNĐ / ngày / cuốn.
Trạng thái sách	Phải trùng khớp với trạng thái lúc mượn.
Đặt chỗ 	Ưu tiên người đặt trước nếu sách đang được mượn hết.

o	Hiển thị 2 nút “Xác nhận”, “Không xác nhận” cho khách hàng chọn. Nếu chọn xác nhận: AI sẽ tạo thành 1 tin nhắn tổng lại hết gửi yêu cầu khách hàng xác nhận đầy đủ thông tin trên và gửi yêu cầu mượn sách vào cho LIBRARIAN/ADMIN duyệt. Nếu chọn nút không xác nhận: AI cảm ơn khách hàng ví dụ: “MiniVerse xin cảm ơn quý khách đã lựa chọn sách bên mình, không biết lí do gì mà khách hàng lại không chịu mượn sách bên em dza” (có thể chọn câu nói khác phong cách GenZ, dễ thương hơn). 
2.	Nút “Tư vấn sản phẩm”: AI sẽ nhận yêu cầu về nhu cầu sử dụng, muốn học những gì,...sau đó đưa ra lời gợi ý + sản phẩm trong cơ sở dữ liệu đưa ra cho khách hàng chọn mua.
	Ví dụ: Khách hàng nhắn: “Tôi muốn mua sách tiếng anh cho người mới bắt đầu để thi bằng B1 cấp tốc 1 tháng, thì nên mua sách nào?”. AI sẽ đưa ra lời gợi ý ngay sản phẩm hoặc chưa đủ dữ liệu thì AI sẽ hỏi thêm: “Tôi chưa rõ bạn muốn nâng cấp về nào rõ hơn, ví dụ writting, speaking,..hay chỉ cần làm nhiều đề trắc nghiệm nên hãy nói rõ cho tôi hơn nhé”, nếu người dùng nói rõ hơn hãy tư vấn: “Vâng với nhu cầu học tiếng anh cho người mới bắt đầu, cải thiện rõ trình độ speaking thì tôi recomment bạn hãy mua các loại sách sau.....”, đưa ra sản phẩm trong cơ sở dữ liệu ra.
3.	Nút “Lập thẻ thư viện” để mượn sách: ở nút này sẽ yêu cầu người dùng nhập: họ tên, email, số điện thoại, giới tính, ngày tháng năm sinh. AI sẽ xác nhận lập thẻ, thẻ sẽ gồm:
1. Họ tên người dùng, 2. Email người dùng, 3. Số điện thoại người dùng, 4. Giới tính người dùng, 5. Ngày tháng năm sinh người dùng, 6. Ngày lập thẻ, 7. Ngày hết hạn thẻ (2 năm kể từ ngày lập thẻ) (người dùng có thể đến gia hạn). 7. Avatar người dùng sẽ được AI tự tạo: phong cách chibi dễ thương hoạt hình (random không trùng bất kì người dùng nào: có khuôn mặt động vật kết hợp tóc con người vào nhưng tùy vào giới tính mà tóc khác nhau (nam: tóc ngắn, nữ: tóc kiểu nữ dài hoặc tóc ngắn nữ) ).

3.11 Trang Đăng ký/Đăng nhập
3.11.1 Đăng ký
•	Username, email, mật khẩu, xác nhận lại mật khẩu.
•	Remember me
•	Xác nhận tôi không phải robot bằng: CAPTCHA hình ảnh (chọn hình ảnh theo yêu cầu), giọng nói (nghe giọng gõ chữ), nhìn chữ/số: nhập y chan lại.
•	Giao diện đơn giản hình ảnh bookstore phong cách dễ thương.

3.11.2 Đăng nhập
•	Username
•	Password
•	Đăng nhập phương thức bên thứ 3: Google, Github.
•	Tạo tài khoản mới
•	Quên mật khẩu: gửi mật khẩu vào gmail 1 mật khẩu được mã hóa kết hợp: AES + RSA. Người dùng vào gmail chọn vào nút “Tạo mật khẩu mới” sẽ dẫn đến trang “Tạo mật khẩu mới”.

3.11.3 Tạo mật khẩu mới
•	3 trường nhập:1. Nhập mật khẩu mã hóa vừa được gửi vào gmail, 2. Nhập mật khẩu mới (nhập trên 6 ký tự: có chữ hoa, chữ thưởng; số; kí tự đặt biệt), 3. Xác nhận mật khẩu mới.
•	Nút “Xác nhận mật khẩu mới”.
•	Sẽ dẫn đến trang “Đăng nhập” lại.

3.12 Trang Thanh toán
•	Sau khi xác nhận số tiền ở giỏ hàng sẽ dẫn đến trang thanh toán
•	Trang này yêu cầu người dùng nhập địa chỉ, có 2 cách:
o	Thủ công/Lựa chọn địa chỉ: người dùng nhập địa chỉ text bình thường, chọn các ô quận, huyện, thành phố. Có thể áp dụng api bên ngoài: API công khai tại địa chỉ https://provinces.open-api.vn để truy xuất thông tin tỉnh/thành phục vụ chức năng lựa chọn địa chỉ.
o	Chọn trên map: người dùng nhập đơn giản địa chỉ nhà, hoặc chọn trên bản đồ sẽ hiển thị đúng địa chỉ VIETNAM. (dùng googlemap hoặc các api khác)
•	Chọn mục ship: 1. Hỏa tốc: Giá tiền tự tính theo km từ địa chỉ shop: địa chỉ shop: “10/80c Song Hành Xa Lộ Hà Nội, Phường Tân Phú, Thủ Đức, Thành phố Hồ Chí Minh, Việt Nam” đến địa chỉ người dùng. 2. Giao nội thành (tính phí ship ở tỉnh Thành phố Hồ Chí Minh): 25,000 nghìn VND đồng, 3. Giao ngoại thành (tỉnh khác): 35,000 nghìn VND đồng.
•	Chọn phương thức thanh toán
o	Thanh toán khi nhận hàng
o	Chuyển khoản: quét mã vạch VIETQR để thanh toán. (Số tài khoản shop: 22653537, ACB bank)

3.13 Trang lập thẻ thư viện
•	Trang này giúp người dùng lập thẻ thư viện rồi mới được mượn sách.
•	Sẽ nhập các trường như: Nhập Họ tên, Email, Số điện thoại, Giới tính: chọn Nam, Nữ, Địa chỉ.
•	Sẽ có 2 nút “Lập thẻ thủ công”, “Nhờ MiniVerse (AI Chatbot) hỗ trợ lập thẻ” -> dẫn đến trang Chatbot.




--------------------------------------------------------------------------------------------------------
4. Cơ sở dữ liệu (Ghi toàn bộ bằng tiếng anh)
4.1 Nhà xuất bản
Id nhà xuất bản: khóa chính, tên nhà xuất bản, Email nhà xuất bản, số điện thoại nhà xuất bản, địa chỉ nhà xuất bản, nghệ danh nhà xuất bản, kinh nghiệm, bằng cấp nhà xuất bản, khóa ngoại: sách.

4.2 Sách
Id sách: khóa chính, tiêu đề sách, ISBN, Ảnh bìa, Số trang, số lượng tổng, số lượng tồn kho, Tái bản lần thứ, khóa ngoại: thể loại; tác giả; nhà xuất bản; phiếu mượn sách; giỏ hàng, số lượng tồn kho sách, ngày xuất bản sách, giá sách.
Hệ thống tự động cập nhật khi mượn – trả sách.

4.3 Lĩnh vực
Id lĩnh vực: khóa chính, tên lĩnh vực, ảnh lĩnh vực, khóa ngoại: thể loại, miêu tả lĩnh vực

4.4 Thể loại
Id thể loại: khóa chính, tên thể loại, ảnh thể loại, khóa ngoại: lĩnh vực; sách, miêu tả thể loại.

4.5 User
Id user: khóa chính, tên user, email user, số điện thoại user, mật khẩu: mã hóa admin không thể xem, địa chỉ user, giới tính, khóa ngoại: thẻ thư viện; đơn hàng; phiếu mượn sách; giỏ hàng; phiếu phạt, avatar user, trạng thái hoạt động.

4.6 Thủ thư (Librarian)
Id librarian: khóa chính, tên librarian, ảnh thủ thư, email librarian, số điện thoại librarian, mật khẩu: mã hóa, giới tính librarian, địa chỉ librarian, khóa ngoại: user; lĩnh vực, thể loại, sách; thẻ thư viện; phiếu phạt; đơn hàng, phiếu mượn sách.

4.7 Thẻ thư viện
Id thẻ thư viện: khóa chính, avatar thẻ thư viện, ngày lập thẻ, ngày hết hạn thẻ, khóa ngoại: user, thủ thư, đơn hàng, phiếu mượn sách.

4.8 Phiếu phạt
Id phiếu phạt: khóa chính, lí do phạt, số tiền phạt, khóa ngoại: user; thủ thư; phiếu mượn sách.

4.9 Đơn hàng
Id đơn hàng: khóa chính, khóa ngoại: user; thủ thư; giỏ hàng; thẻ thư viện; sách, trạng thái: Chờ xác nhận, Đang xử lý, Đang giao hàng, Hoàn thành, Đã hủy.

4.10 Giỏ hàng
Id giỏ hàng: khóa chính, khóa ngoại: đơn hàng; user; sách; số lượng, ngày giờ bỏ vào giỏ hàng, ngày giờ xóa khỏi giỏ hàng.

4.11 Bài viết Blog
Id bài viết: khóa chỉnh, Text: nếu người dùng đăng chữ, Image: nếu người dùng đăng ảnh, Video: nếu người dùng đăng video, Ngày giờ thêm bài viết, Ngày giờ xóa bài viết, Ngày giờ cập nhật sửa bài viết, Khóa ngoại: User.

4.12 Lịch sử cuộc trò chuyện chatbot
Id lịch sử chatbot: khóa chính, số lượng text trong phiên trò chuyện, ngày giờ bắt đầu trò chuyện, ngày giờ kết thúc trò chuyện, xóa cuộc trò chuyện: hiển thị xóa “1”, không xóa “0”, khóa ngoại: user; lịch sử chi tiết chatbot.

4.13 Lịch sử chi tiết chatbot
Id chi tiết lịch sử: khóa chỉnh, khóa ngoại: lịch sử cuộc trò chuyện chatbot, Text chatbot: lời nhắn của ai chatbot, text user: lời nhắn của user.

4.14 Lịch sử trò chuyện Blog
Id lịch sử trò chuyện blog: khóa chỉnh, số lượng text trong phiên trò chuyện, ngày giờ bắt đầu trò chuyện, ngày giờ kết thúc trò chuyện, xóa cuộc trò chuyện: hiển thị xóa “1”, không xóa “0”, khóa ngoại: user; lịch sử chi tiết trò chuyện Blog.

4.15 Lịch sử chi tiết trò chuyện Blog
Id chi tiết lịch sử trò chuyện Blog: khóa chỉnh, khóa ngoại: lịch sử trò chuyện Blog, Text chatUser: lời nhắn của người A chat (người kia), Text bản thân: lời nhắn của người B (Bản thân mình gửi), Image chatUser: hình ảnh gửi,...

4.16 Chuông thông báo
Id chuông thông báo: khóa chỉnh, Nội dung thông báo, Khóa ngoại: User; Thủ thư, Giỏ hàng; Đơn hàng; Thẻ thư viện; Phiếu phạt; Bài viết Blog; Comment, Like, Lịch sử trò chuyện Blog; Lịch sử cuộc trò chuyện chatbot; Sách..Ngày giờ thông báo, Trạng thái thông báo: “1”: đã xem, “0”: chưa xem.

4.17 Comment
Id comment: khóa chính, Nội dung comment, Ngày giờ bắt đầu comment, Trạng thái xóa comment: “1”: hiển thị, “0”: đã xóa, Ngày giờ sửa comment. Khóa ngoại: user, Bài viết Blog, Chuông thông báo.

4.18 Like
Id like: khóa chính, Ngày giờ bắt đầu tim, Ngày giờ bỏ tim, Trạng thái: “1”: tim, “0”: bỏ tim, khóa ngoại: User, Bài viết Blog, Chuông thông báo..

4.19 Tác giả
Id tác giả: khóa chính, Tên tác giả, Ngày sinh tác giả, Ngày tháng năm sinh mất (nếu có), Tiểu sử, khóa ngoại: lĩnh vực, thể loại, sách.

4.20 Phiếu mượn sách
Id phiếu mượn sách: khóa chính, tên user mượn: khóa ngoại của user, tên thủ thư xác nhận: khóa ngoại thủ thư, Ngày bắt đầu mượn, Ngày trả dự kiến, Ngày trả thực tế, Trạng thái: đang mượn / đã trả / quá hạn, Ghi chú, Khóa ngoại: User; Thủ thư; Thẻ thư viện.

4.21 Chi tiết phiếu mượn sách: Mỗi phiếu mượn có thể mượn nhiều sách.
Id chi tiết phiếu mượn: khóa chính, tên sách mượn: khóa ngoại sách, số lượng mượn, Tình trạng sách khi mượn: Mới, cũ, Tình trạng sách khi trả: Mới, rách, hư hỏng nặng, mất sách, Tiền phạt (nếu có), Ghi chú.
Hệ thống hỗ trợ xử lí khi sách bị hư hỏng nhẹ, nặng, mất sách
