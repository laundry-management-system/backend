**GIỚI THIỆU ĐỀ TÀI**

**HỆ THỐNG QUẢN LÝ TIỆM GIẶT ỦI – LAUNDRY MANAGEMENT SYSTEM**
# **I. HOẠT ĐỘNG GIẶT ỦI LÀ GÌ?**
Giặt ủi là hoạt động dịch vụ gia công theo đơn: khách hàng giao tài sản cá nhân (quần áo, chăn, ga, rèm, giày, thú nhồi bông…) cho cơ sở giặt; cơ sở xử lý qua các công đoạn kỹ thuật (phân loại → giặt → sấy → ủi → gấp/đóng gói) rồi hoàn trả nguyên trạng cho khách và thu phí dịch vụ.

**Ba đặc điểm nghiệp vụ quyết định cách thiết kế phần mềm:**

- Tài sản của khách nằm trong tay cửa hàng một khoảng thời gian, do đó phát sinh nhu cầu định danh đồ, ghi nhận tình trạng ban đầu và xác định trách nhiệm bồi thường khi mất hoặc hư hỏng.
- Giá được tính theo khối lượng hoặc số lượng chứ không theo món cố định, nên phải cân/đếm tại quầy và giá phải được chốt tại thời điểm tạo đơn.
- Nhiều đơn được xử lý song song trên tài nguyên hữu hạn là máy giặt và máy sấy, hình thành bài toán phân bổ tài nguyên và theo dõi tiến độ.
# **II. CÁC MỨC ĐỘ PHẠM VI TỔ CHỨC**

|**Mức**|**Mô tả**|**Đặc trưng quản lý**|**Nhu cầu phần mềm**|
| :-: | :-: | :-: | :-: |
|M1. Cá nhân / hộ gia đình|Giặt tại nhà, nhận vài đơn của khách quen|Ghi sổ tay, nhớ miệng|Gần như không cần|
|M2. Tiệm tự phục vụ|Khách tự bỏ đồ vào máy, trả tiền bằng xu hoặc thẻ|Quản lý máy và doanh thu máy, không quản lý đồ|Cần phần mềm quản lý máy, không cần quản lý đơn|
|**M3. Tiệm dịch vụ 1 chi nhánh (đề tài chọn)**|Nhân viên nhận đồ, xử lý và giao trả. Khoảng 10–30 đơn/ngày|Quản lý đơn, khách, máy, doanh thu, trách nhiệm nhân viên|Cần phần mềm ở mức đầy đủ|
|M4. Chuỗi nhiều chi nhánh|Nhiều điểm nhận đồ, có thể có xưởng giặt trung tâm|Thêm điều chuyển đồ giữa chi nhánh, doanh thu và phân quyền theo chi nhánh|Phần mềm đa chi nhánh|
|M5. Doanh nghiệp / giặt công nghiệp|Phục vụ khách sạn, bệnh viện, nhà hàng theo hợp đồng|Hợp đồng, công nợ, hóa đơn VAT, KPI chất lượng, kho vải|Hệ thống ERP chuyên ngành|

Nhóm chọn mức M3 – tiệm dịch vụ một chi nhánh, đúng với phạm vi nêu trong PRD mục 1: một cửa hàng, khoảng 10–30 đơn/ngày, 2–5 nhân viên, khoảng 6–10 máy giặt và máy sấy.
# **III. CÁC ĐỐI TƯỢNG THAM GIA**
## **3.1. Đối tượng con người trực tiếp**

|**Đối tượng**|**Vai trò trong nghiệp vụ**|
| :-: | :-: |
|Chủ tiệm / Admin|Sở hữu cửa hàng, quyết định bảng giá, đầu tư máy, tuyển nhân viên, theo dõi doanh thu và báo cáo|
|Nhân viên quầy (lễ tân)|Tiếp nhận khách, cân đồ, ghi nhận tình trạng, tạo đơn, in phiếu, thu tiền, giao đồ|
|Nhân viên vận hành máy|Phân đơn vào máy, giặt và sấy, cập nhật trạng thái đơn, xác nhận hoàn thành|
|Khách hàng|Mang đồ đến, theo dõi đơn, thanh toán, nhận lại đồ|

Trong phạm vi đề tài, hai vai trò nhân viên được gộp chung thành vai trò Nhân viên, phù hợp với quy mô một tiệm nhỏ nơi một người thường kiêm cả hai công việc.
## **3.2. Đối tượng phát sinh (tác nhân phụ)**

|**Đối tượng**|**Lý do xuất hiện**|
| :-: | :-: |
|Người nhận đồ hộ|Khách bận nhờ người thân đến lấy, cần quy tắc xác thực bằng mã đơn và số điện thoại|
|Khách vãng lai không tài khoản|Chỉ giặt một lần, không muốn đăng ký, cần cơ chế khách chỉ có tên và số điện thoại|
|Dịch vụ thông báo (Web / Email / SMS)|Tác nhân hệ thống, gửi tin khi đơn hoàn thành|
|Cổng thanh toán / QR ngân hàng|Tác nhân hệ thống, xác nhận giao dịch chuyển khoản|
|Thợ sửa máy|Không đăng nhập hệ thống nhưng làm phát sinh trạng thái máy Bảo trì|
# **IV. HIỆN TRẠNG VÀ SỰ CẦN THIẾT CỦA PHẦN MỀM**
## **4.1. Hiện trạng tổ chức**
Cách làm thủ công hiện nay tại các tiệm giặt ủi quy mô một chi nhánh:

- Nhận đồ thì ghi vào sổ tay hoặc viết phiếu giấy hai liên, một liên đưa khách, một liên kẹp vào túi đồ.
- Cân đồ bằng cân đồng hồ, nhẩm giá theo bảng giá dán trên tường.
- Túi đồ được dán giấy ghi tên khách hoặc số thứ tự.
- Trạng thái đơn nằm trong trí nhớ nhân viên hoặc thể hiện qua vị trí vật lý của túi đồ trên kệ.
- Doanh thu được cộng cuối ngày bằng máy tính cầm tay rồi ghi vào sổ thu chi.
- Khách phải gọi điện đến cửa hàng để hỏi đồ đã giặt xong chưa.

Về phần mềm, hầu hết tiệm nhỏ chưa sử dụng phần mềm nào, hoặc chỉ dùng chung một file Excel / Google Sheets. Cách làm này không phân quyền, không lưu vết chỉnh sửa, dễ bị sửa xóa số liệu và không dùng được đồng thời tại quầy lẫn khu vực máy giặt.
## **4.2. Các vấn đề phát sinh**

|**Vấn đề**|**Hệ quả**|
| :-: | :-: |
|Phiếu giấy bị mất, mờ chữ|Tranh chấp về khối lượng và giá tiền|
|Không biết đơn đang ở công đoạn nào|Nhân viên phải đi tìm túi đồ trong cửa hàng|
|Không biết máy nào đang chạy đơn nào|Mở nhầm máy, lấy nhầm đồ của khách khác|
|Nhầm đồ giữa hai khách trùng họ tên|Phải bồi thường, mất uy tín|
|Quên đơn dẫn đến giao trễ|Khách khiếu nại|
|Không truy được ai thực hiện thao tác sai|Không quy được trách nhiệm|
|Không có lịch sử khách hàng|Không xây dựng được khách thân thiết, không xác định được khách VIP|
|Đổi bảng giá làm sai lệch đơn cũ|Thất thoát doanh thu hoặc thu sai của khách|
## **4.3. Có cần xây dựng phần mềm không?**
**Cần thiết. Lý do:**

- Định danh và truy vết: mỗi đơn có một mã duy nhất dạng LD-20260916-001, mỗi lần thay đổi trạng thái đều ghi nhận thời gian và người thực hiện. Đây là điều sổ tay giấy không làm được.
- Đồng bộ đa điểm: quầy tiếp nhận và khu vực máy giặt là hai vị trí vật lý khác nhau; nhân viên cần cập nhật trạng thái ngay tại máy bằng điện thoại.
- Chốt giá theo thời điểm: yêu cầu giá của đơn không đổi khi bảng giá thay đổi không thể thực hiện thủ công một cách đáng tin cậy.
- Doanh thu tức thời: chủ tiệm không cần có mặt tại cửa hàng vẫn biết hôm nay có bao nhiêu đơn và thu được bao nhiêu tiền.
- Chủ động thông báo khách: giảm số cuộc gọi hỏi thăm và giảm số đơn khách quên đến nhận.
## **4.4. Mục đích sử dụng phần mềm của từng đối tượng**

|**Người dùng**|**Mục đích sử dụng**|
| :-: | :-: |
|Chủ tiệm|Biết đồ của khách đang ở đâu, máy nào đang xử lý đồ nào, hôm nay cửa hàng thu được bao nhiêu tiền. Kiểm soát nhân viên, quyết định bảng giá, đánh giá hiệu quả máy|
|Nhân viên quầy|Tạo đơn nhanh và chính xác, in phiếu nhận đồ, tra cứu khách cũ theo số điện thoại, không phải nhẩm giá bằng tay|
|Nhân viên vận hành máy|Biết đơn nào cần làm trước (đơn gấp, đơn trễ), máy nào còn trống, cập nhật trạng thái ngay tại chỗ|
|Khách hàng|Tự tra cứu trạng thái đơn mà không cần gọi điện, xem số tiền phải trả và điểm tích lũy|
## **4.5. Các đối tượng vật**

|**Đối tượng vật**|**Vai trò**|
| :-: | :-: |
|Đơn giặt (Order)|Vật thể trung tâm của toàn hệ thống|
|Chi tiết đồ giặt (OrderItem)|Loại đồ, số lượng hoặc khối lượng, dịch vụ áp dụng|
|Ảnh tình trạng đồ (OrderImage)|Bằng chứng về tình trạng ban đầu của đồ|
|Dịch vụ (Service)|Giặt thường, giặt và sấy, giặt khô, ủi, giặt chăn…|
|Lịch sử giá (ServicePriceHistory)|Giá của dịch vụ theo từng mốc thời gian|
|Máy giặt / máy sấy (Machine)|Tài nguyên hữu hạn của cửa hàng|
|Lượt chạy máy (MachineJob)|Liên kết đơn với máy kèm khung thời gian, trạng thái lượt chạy và lần xử lý của đơn|
|Phiếu nhận đồ / hóa đơn|Chứng từ xác lập và kết thúc giao dịch|
|Thanh toán (Payment)|Số tiền, phương thức, thời điểm, người xác nhận|
|Voucher / Điểm tích lũy|Công cụ khuyến mãi và giữ chân khách hàng|
|Thông báo (Notification)|Tin nhắn gửi cho khách khi đơn hoàn thành|
|Nhật ký hệ thống (AuditLog)|Lưu vết các hành động nhạy cảm|
## **4.6. Thông tin cần thu thập của các đối tượng**

|**Đối tượng**|**Thông tin cần thu thập**|
| :-: | :-: |
|Khách hàng|Mã khách hàng, họ tên, số điện thoại (khóa tra cứu chính), tài khoản đăng nhập, loại khách (Mới / Thường / VIP), điểm tích lũy, ngày tạo, tổng số đơn, tổng chi tiêu, dịch vụ thường dùng|
|Nhân viên|Mã nhân viên, họ tên, số điện thoại, tài khoản, vai trò, trạng thái làm việc, ngày vào làm|
|Đơn hàng|Mã đơn, mã hiển thị, khách hàng, nhân viên lập đơn, ngày nhận, ngày hẹn trả cam kết, ETA dự báo nội bộ, tổng tiền, trạng thái đơn, trạng thái thanh toán, loại đơn hàng (Thường / Giặt nhanh), độ ưu tiên, cờ trễ, số lần xử lý, ghi chú|
|Chi tiết đồ|Loại đồ, số lượng hoặc khối lượng, đơn vị tính, dịch vụ áp dụng, đơn giá tại thời điểm tạo đơn, thành tiền, ghi chú tình trạng|
|Dịch vụ|Mã dịch vụ, tên, đơn vị tính (kg / món / bộ / sản phẩm), giá hiện hành, thời gian xử lý dự kiến, cờ hỗ trợ giặt nhanh, trạng thái hoạt động|
|Máy|Mã máy, tên máy, loại máy, công suất, trạng thái, đơn đang xử lý, giờ bắt đầu, giờ dự kiến kết thúc|
|Lượt chạy máy|Mã lượt chạy, mã đơn, mã máy, lần xử lý của đơn, nhân viên thực hiện, giờ bắt đầu, giờ dự kiến kết thúc, giờ kết thúc thực tế, trạng thái lượt chạy, lý do gián đoạn, lượt chạy thay thế cho|
|Thanh toán|Số tiền, phương thức, thời gian, nguồn xác nhận (nhân viên hoặc webhook), mã tham chiếu giao dịch, trạng thái đối soát|
|Voucher|Mã code, loại giảm giá, giá trị, ngày bắt đầu, ngày kết thúc, giá trị đơn tối thiểu, số lần sử dụng, trạng thái|
|Nhật ký hệ thống|Người dùng, hành động, đối tượng tác động, thời gian, dữ liệu cũ, dữ liệu mới|
# **V. LUỒNG NGHIỆP VỤ**
## **5.1. Khách tham khảo thông tin và liên hệ**
Khách hỏi bảng giá, thời gian giặt, tiệm có nhận giặt chăn hoặc rèm hay không. Nhân viên tra bảng giá dịch vụ trên hệ thống và trả lời thời gian dự kiến theo thông tin đã cấu hình cho từng dịch vụ. Giai đoạn này chỉ đọc dữ liệu, chưa phát sinh đơn hay hồ sơ khách.
## **5.2. Khách mang đồ đến giặt (luồng chính)**
- Khách mang đồ đến cửa hàng.
- Nhân viên đăng nhập, tra cứu khách theo số điện thoại. Nếu đã có thì lấy hồ sơ cũ, nếu chưa có thì tạo khách mới với họ tên và số điện thoại.
- Kiểm tra và tiếp nhận đồ.
- Cân, đếm và phân loại đồ.
- Chụp ảnh và ghi chú tình trạng đồ: có vết bẩn, bị rách, dễ phai màu, không dùng nước xả, giặt riêng.
- Chọn dịch vụ cho từng dòng đồ.
- Hệ thống tính giá, áp dụng voucher nếu khách có mã hợp lệ.
- Khách đồng ý, hệ thống tạo đơn và sinh mã đơn duy nhất dạng LD-YYYYMMDD-NNN.
- In phiếu nhận đồ thành hai liên: khách giữ một liên, một liên kẹp vào túi đồ.
- Đơn ở trạng thái Mới nhận.
## **5.3. Xử lý đơn (giai đoạn không có mặt khách)**
- Nhân viên mở hàng đợi công việc. Danh sách được sắp xếp giảm dần theo độ ưu tiên do hệ thống tính; các đơn cùng độ ưu tiên thì xếp theo giờ nhận (FIFO).
- Kiểm tra máy sẵn sàng theo loại máy và công suất còn trống. Nếu có máy thì phân đơn vào máy và ghi nhận lượt chạy máy; nếu hết máy hoặc máy đang bảo trì thì xếp hàng chờ và cảnh báo khi đơn sắp trễ hạn.
- Cập nhật trạng thái Đang giặt, hệ thống ghi thời gian và người thực hiện.
- Giặt xong thì chuyển sang máy sấy, trạng thái Đang sấy.
- Gấp và đóng gói, chuyển trạng thái Hoàn thành.
- Hệ thống tự động tạo thông báo gửi cho khách hàng.
## **5.4. Khách thanh toán và nhận đồ**

|**Tình huống**|**Cách xử lý**|
| :-: | :-: |
|Nhận đúng hạn|Xác nhận thanh toán, cộng điểm tích lũy, chuyển đơn sang trạng thái Đã giao|
|Trả tiền trước, nhận đồ sau|Trạng thái thanh toán là Đã thanh toán nhưng trạng thái đơn vẫn là Hoàn thành; đơn chỉ đóng khi có xác nhận giao đồ|
|Khách muốn nhận sớm khi đồ chưa xong|Không cho chuyển Đã giao, hoặc hủy công đoạn còn lại, tính tiền theo phần dịch vụ đã thực hiện và ghi rõ lý do|
|Nhận trễ so với ngày hẹn|Đánh dấu đồ tồn, cảnh báo trên dashboard, gửi nhắc lần hai; sau số ngày quy định thì chuyển sang danh sách đồ lưu kho lâu ngày|
|Người khác nhận hộ|Bắt buộc đọc đúng mã đơn hoặc số điện thoại của khách, ghi nhận tên người nhận vào đơn|
|Khách xin nợ tiền|Chỉ Admin được duyệt; đơn ở trạng thái Đã giao kèm Chưa thanh toán và xuất hiện trong báo cáo công nợ|
## **5.5. Các sự cố phát sinh**

|**Sự cố**|**Nghiệp vụ xử lý**|
| :-: | :-: |
|Đồ bị hỏng khi giặt (phai màu, rách, co rút)|Đối chiếu ảnh ban đầu và ghi chú tình trạng, lập phiếu sự cố, thương lượng bồi thường hoặc giảm giá|
|Thất lạc đồ|Tra lượt chạy máy để biết đơn đã qua máy nào, khung giờ nào, ai vận hành để khoanh vùng tìm kiếm|
|Khách khiếu nại thiếu món|Đối chiếu với số lượng đã ghi và đã ký nhận trên phiếu nhận đồ|
|Máy hỏng giữa chừng|Chuyển máy sang trạng thái Bảo trì, lượt chạy hiện tại chuyển sang trạng thái Gián đoạn kèm lý do, tạo lượt chạy mới trên máy khác trong cùng lần xử lý, dời lại giờ dự kiến hoàn thành; trạng thái đơn không đổi|
|Khách hủy đơn khi chưa giặt|Cho phép hủy và hoàn tiền nếu đã trả; nếu đã bắt đầu giặt thì không cho hủy|
## **5.6. Báo cáo thống kê nhằm mục đích gì?**

|**Báo cáo**|**Mục đích quản trị**|
| :-: | :-: |
|Doanh thu theo ngày và theo tháng|Nắm dòng tiền, so sánh tăng trưởng, đối chiếu tiền mặt cuối ca|
|Số đơn và giá trị đơn trung bình|Đo quy mô hoạt động, quyết định có cần tuyển thêm nhân viên hay không|
|Đơn trễ|Đo chất lượng vận hành, tìm nút thắt là thiếu máy hay thiếu người|
|Dịch vụ được dùng nhiều nhất và doanh thu theo dịch vụ|Quyết định giữ hay bỏ dịch vụ, điều chỉnh giá|
|Khách mới, khách quay lại, khách VIP|Đo độ trung thành, thiết kế chương trình khuyến mãi|
|Hiệu suất sử dụng máy|Quyết định mua thêm máy hay thanh lý máy cũ|
|Nhật ký hoạt động của nhân viên|Quy trách nhiệm khi có sai sót, phòng ngừa gian lận tiền mặt|
# **VI. PHỤ THUỘC, RÀNG BUỘC VÀ BIẾN ĐỘNG THEO THỜI GIAN**
## **6.1. Phụ thuộc giữa các đối tượng**

|**Quan hệ**|**Ràng buộc**|
| :-: | :-: |
|Đơn – Khách hàng|Một đơn thuộc đúng một khách. Không xóa khách còn đơn chưa đóng|
|Đơn – Chi tiết đồ|Một nhiều. Đơn phải có ít nhất một dòng chi tiết đồ|
|Chi tiết đồ – Dịch vụ|Nhiều một. Không xóa dịch vụ đã được sử dụng, chỉ chuyển sang Ngừng hoạt động|
|Đơn – Máy|Nhiều nhiều thông qua lượt chạy máy, vì một đơn có thể đi qua máy giặt rồi máy sấy|
|Đơn – Thanh toán|Một một. Mỗi đơn chỉ có đúng một giao dịch thanh toán, hệ thống không hỗ trợ trả nhiều lần|
|Đơn – Lịch sử trạng thái|Một nhiều, dữ liệu chỉ ghi thêm và không được xóa|
|Dịch vụ – Lịch sử giá|Một nhiều, chỉ ghi thêm, không sửa bản ghi cũ|
## **6.2. Ràng buộc thời gian**
- Ngày hẹn trả cam kết được tính khi tạo đơn theo thời điểm máy phù hợp rảnh sớm nhất, cộng dồn thời gian của các công đoạn nối tiếp trong đơn (giặt, sấy, gấp), cộng thời gian dự phòng do Admin cấu hình, sau đó làm tròn lên theo giờ mở cửa của tiệm.
- Ngày hẹn trả cam kết được chốt cứng tại thời điểm tạo đơn, in trên phiếu nhận đồ và là mốc duy nhất để xác định đơn TRỄ. ETA dự báo nội bộ được tính lại mỗi khi hàng đợi thay đổi, chỉ dùng để cảnh báo và không làm thay đổi ngày hẹn đã cam kết với khách.
- Độ ưu tiên của đơn được hệ thống tính lại định kỳ 5–10 phút và tính lại ngay tại mỗi lần đơn đổi trạng thái.
- Giờ dự kiến kết thúc của một lượt chạy máy phải sau giờ bắt đầu.
- Một máy không thể có hai lượt chạy chồng khung giờ; ràng buộc này chỉ xét các lượt chạy ở trạng thái Đã phân và Đang chạy.
- Trạng thái đơn chỉ đi tới trong phạm vi một lần xử lý. Đơn chỉ quay về công đoạn trước khi mở một lần xử lý mới gắn với phiếu sự cố đã được Admin duyệt. Riêng trường hợp nhân viên bấm nhầm, Admin được sửa trạng thái và phải ghi lý do vào nhật ký hệ thống.
- Voucher chỉ sử dụng được trong khoảng từ ngày bắt đầu đến ngày kết thúc.
- Thời điểm thanh toán không được trước thời điểm tạo đơn.
## **6.3. Ràng buộc thông tin và phạm vi giá trị**
- Số điện thoại khách gồm 10 chữ số theo định dạng Việt Nam và nên là duy nhất.
- Khối lượng phải lớn hơn 0; số lượng là số nguyên dương.
- Giá phải lớn hơn hoặc bằng 0; giá trị giảm giá không vượt quá tổng tiền hàng.
- Tổng tiền đơn bằng tổng thành tiền các dòng trừ giảm giá cộng phụ phí giặt nhanh.
- Khối lượng đồ phân vào máy không vượt quá công suất máy.
- Mã đơn phải duy nhất trong toàn hệ thống.
- Điểm tích lũy không được âm.
## **6.4. Ràng buộc pháp lý và trách nhiệm dân sự**
- Phiếu nhận đồ là chứng từ xác lập trách nhiệm giữ tài sản, phải ghi rõ số lượng, tình trạng, giá và ngày hẹn trả. Đây là căn cứ khi xảy ra tranh chấp.
- Điều khoản bồi thường, ví dụ tối đa bằng một số lần giá dịch vụ, nên được in trên phiếu.
- Nếu tiệm xuất hóa đơn giá trị gia tăng thì chịu các quy định về hóa đơn điện tử.
- Thông tin khách hàng là dữ liệu cá nhân, do đó phải hash mật khẩu, kiểm tra quyền ở tầng API và ghi nhật ký truy cập.
## **6.5. Kịch bản khi luồng nghiệp vụ không diễn ra lý tưởng**

|**Kịch bản lệch**|**Hệ quả**|**Cách hệ thống xử lý**|
| :-: | :-: | :-: |
|Đăng nhập sai tài khoản hoặc mật khẩu|Không vào được hệ thống|Đếm số lần sai và khóa tạm thời|
|Tạo đơn nhưng khách đổi ý|Phát sinh đơn rác|Cho phép hủy khi đơn còn ở trạng thái Mới nhận|
|Hết máy trống|Đơn phải chờ|Xếp hàng chờ và cảnh báo nguy cơ trễ hạn|
|Máy hỏng giữa chừng|Đơn dở dang|Chun myểáy, dời giờ dự kiến, ghi rõ lý do|
|Quên cập nhật trạng thái|Dashboard hiển thị sai|Cảnh báo đơn đứng yên quá lâu ở một trạng thái|
|Đơn quá hạn|Khách khiếu nại|Tự động gắn cờ TRỄ và đưa lên đầu danh sách công việc|
|Khách không đến nhận đồ|Đồ tồn trong kho|Nhắc lần hai và đưa vào danh sách đồ tồn|
|Giao đồ nhưng quên thu tiền|Thất thoát doanh thu|Chặn chuyển Đã giao khi chưa thanh toán, trừ khi Admin duyệt nợ|
|Nhân viên tự sửa giá đơn|Nguy cơ gian lận|Chỉ Admin được sửa, mọi thay đổi ghi vào nhật ký hệ thống|
|Voucher bị dùng quá số lần|Lỗ khuyến mãi|Kiểm tra số lần đã dùng so với số lần tối đa khi áp mã|
|Đồ hư hỏng trong quá trình giặt|Phải bồi thường|Đối chiếu ảnh và ghi chú ban đầu, lập phiếu sự cố|
## **6.6. Các thuộc tính thay đổi theo thời gian**

|**Thuộc tính**|**Cách thay đổi**|**Cách xử lý trong thiết kế**|
| :-: | :-: | :-: |
|Giá dịch vụ|Tăng hoặc giảm theo thị trường|Lưu bảng lịch sử giá; đơn cũ giữ nguyên giá tại thời điểm tạo|
|Trạng thái đơn|Mới nhận đến Đã giao|Lưu lịch sử trạng thái, không ghi đè bản ghi cũ|
|Trạng thái máy|Sẵn sàng, Đang chạy, Bảo trì|Lưu lịch sử để phục vụ tính hiệu suất máy|
|Loại khách hàng|Mới thành Thường rồi thành VIP|Tính lại định kỳ theo tổng chi tiêu, quy tắc cấu hình được|
|Điểm tích lũy|Cộng khi thanh toán, trừ khi đổi ưu đãi|Lưu sổ giao dịch điểm chứ không chỉ lưu số dư|
|Tỉ lệ quy đổi điểm|Admin thay đổi chính sách|Lưu cấu hình có hiệu lực theo thời gian|
|Trạng thái voucher|Còn hạn, hết hạn, hết lượt|Kiểm tra tại thời điểm áp dụng|
|Nhân viên|Nghỉ việc|Không xóa, chỉ chuyển sang trạng thái ngừng hoạt động để giữ lịch sử đơn cũ|
|Số điện thoại khách|Khách đổi số|Cho phép sửa nhưng phải ghi nhật ký hệ thống|
# **VII. DANH SÁCH BUSINESS RULE**
## **Nhóm A – Xác thực và phân quyền**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-A01|Mọi người dùng phải đăng nhập trước khi thao tác với hệ thống|
|BR-A02|Mỗi tài khoản thuộc đúng một vai trò: Admin, Nhân viên hoặc Khách hàng|
|BR-A03|Mật khẩu phải được hash, không lưu dưới dạng rõ|
|BR-A04|Nhân viên không được gọi API dành cho Admin, kể cả khi bypass giao diện|
|BR-A05|Khách hàng chỉ được xem dữ liệu của chính mình|
## **Nhóm B – Khách hàng**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-B01|Số điện thoại là phương thức tìm kiếm chính và nên là duy nhất trong hệ thống|
|BR-B02|Khách hàng phải có tối thiểu họ tên và số điện thoại|
|BR-B03|Không được xóa khách hàng còn đơn chưa đóng, chỉ được vô hiệu hóa|
|BR-B04|Loại khách hàng (Mới / Thường / VIP) được xác định theo quy tắc cấu hình, không hard-code|
|BR-B05|Lịch sử giao dịch của khách không được xóa|
## **Nhóm C – Dịch vụ và bảng giá**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-C01|Chỉ Admin được thay đổi bảng giá|
|BR-C02|Hệ thống phải lưu lịch sử thay đổi giá gồm giá cũ, giá mới, người sửa và thời gian|
|BR-C03|Giá của đơn được chốt tại thời điểm tạo đơn|
|BR-C04|Thay đổi bảng giá không được ảnh hưởng tới đơn đã tạo|
|BR-C05|Mỗi dịch vụ phải có đơn vị tính thuộc tập hợp kg, món, bộ, sản phẩm|
|BR-C06|Dịch vụ đã được sử dụng trong đơn không được xóa, chỉ chuyển sang Ngừng hoạt động|
|BR-C07|Dịch vụ giặt nhanh có phụ phí và thời gian xử lý ngắn hơn dịch vụ thường tương ứng|
## **Nhóm D – Đơn hàng**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-D01|Mỗi đơn phải có mã duy nhất theo định dạng LD-YYYYMMDD-NNN|
|BR-D02|Một đơn thuộc về đúng một khách hàng|
|BR-D03|Một đơn có thể chứa nhiều dịch vụ|
|BR-D04|Đơn phải có ít nhất một dòng chi tiết đồ|
|BR-D05|Khối lượng hoặc số lượng của mỗi dòng đồ phải lớn hơn 0|
|BR-D06|Tổng tiền đơn bằng tổng của đơn giá nhân số lượng, trừ giảm giá, cộng phụ phí giặt nhanh|
|BR-D07|Trạng thái đơn đi theo luồng Mới nhận, Đang giặt, Đang sấy, Hoàn thành, Đã giao và chỉ được đi tới trong phạm vi một lần xử lý; đơn chỉ được quay về công đoạn trước khi mở một lần xử lý mới theo BR-D17|
|BR-D08|Mọi thay đổi trạng thái đơn phải ghi nhận thời gian và người thực hiện|
|BR-D09|Đơn quá thời gian dự kiến mà chưa Hoàn thành thì bị đánh dấu TRỄ|
|BR-D10|Đơn chỉ được chuyển sang Đã giao sau khi nhân viên xác nhận giao đồ|
|BR-D11|Đơn chỉ được hủy khi đang ở trạng thái Mới nhận|
|BR-D12|Hàng đợi công việc được sắp xếp giảm dần theo độ ưu tiên; đồng điểm thì xếp theo giờ nhận (FIFO)|
|BR-D13|Ngày hẹn trả cam kết được tính tự động một lần khi tạo đơn theo máy rảnh sớm nhất và tổng thời gian các công đoạn nối tiếp, sau đó không tự thay đổi|
|BR-D14|Độ ưu tiên = điểm theo tỉ lệ R (thời gian còn lại đến hạn chia cho thời gian xử lý còn lại): R > 2 là 0, 1 < R ≤ 2 là 1, 0 < R ≤ 1 là 2, R ≤ 0 là 3; cộng thêm 2 nếu là đơn giặt nhanh. Các ngưỡng do Admin cấu hình, không hard-code|
|BR-D15|Độ ưu tiên và cờ TRỄ được cập nhật bằng job định kỳ 5–10 phút và được cập nhật ngay tại mỗi lần đơn đổi trạng thái|
|BR-D16|Loại đơn hàng thuộc tập hợp Thường và Giặt nhanh; loại đơn quyết định phụ phí, thời gian xử lý và mức cộng vào độ ưu tiên|
|BR-D17|Đơn chỉ được mở lần xử lý mới khi có phiếu sự cố đã được Admin duyệt; mỗi lần mở phải gắn với đúng một mã phiếu sự cố và làm số lần xử lý của đơn tăng thêm 1|
|BR-D18|Mở lần xử lý mới không làm thay đổi tổng tiền đơn và không làm thay đổi Ngày hẹn trả cam kết; chỉ ETA dự báo nội bộ và độ ưu tiên được tính lại|
## **Nhóm E – Máy giặt và máy sấy**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-E01|Chỉ Admin được quản lý máy gồm thêm, sửa, ngừng sử dụng và xóa|
|BR-E02|Máy ở trạng thái Bảo trì hoặc Ngừng sử dụng không được nhận công việc mới|
|BR-E03|Tại một thời điểm, một máy chỉ được có tối đa một lượt chạy ở trạng thái Đang chạy. Không được nạp thêm đồ vào máy khi lượt chạy đang diễn ra; mọi đợt đồ mới phải tạo một lượt chạy riêng sau khi lượt hiện tại kết thúc|
|BR-E04|Khối lượng đồ phân vào một lượt chạy không được vượt quá công suất của máy|
|BR-E05|Đơn chỉ được phân vào máy đúng loại: giặt vào máy giặt, sấy vào máy sấy|
|BR-E06|Mỗi lượt phân máy phải ghi mã lượt chạy, máy, đơn, lần xử lý, nhân viên, giờ bắt đầu, giờ dự kiến kết thúc, giờ kết thúc thực tế, trạng thái lượt chạy và lý do gián đoạn nếu có|
|BR-E07|Máy đang xử lý đơn chưa hoàn thành không được xóa khỏi hệ thống|
|BR-E08|Trạng thái lượt chạy thuộc tập hợp Đã phân, Đang chạy, Hoàn thành, Gián đoạn. Ràng buộc không chồng khung thời gian chỉ áp dụng cho lượt chạy ở trạng thái Đã phân và Đang chạy|
|BR-E09|Khi máy chuyển sang Bảo trì, mọi lượt chạy đang hoạt động trên máy đó tự động chuyển sang Gián đoạn kèm lý do; đơn liên quan quay lại hàng đợi và ETA dự báo nội bộ được tính lại, trạng thái đơn không đổi|
|BR-E10|Lượt chạy ở trạng thái Gián đoạn không được tính vào báo cáo hiệu suất sử dụng máy mà được thống kê riêng; lịch sử lượt chạy chỉ được ghi thêm, không được sửa hoặc xóa|
## **Nhóm F – Thanh toán**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-F01|Trạng thái thanh toán chỉ có hai giá trị là Chưa thanh toán và Đã thanh toán|
|BR-F02|Phương thức thanh toán thuộc tập hợp Tiền mặt và Chuyển khoản (QR)|
|BR-F03|Hệ thống sinh mã QR động (VietQR) đã mã hóa sẵn số tài khoản, số tiền và nội dung chuyển khoản là mã đơn; không cho nhập tay thông tin chuyển khoản|
|BR-F04|Thanh toán chuyển khoản được đối soát tự động qua webhook của cổng trung gian; đơn ở trạng thái Chờ đối soát cho tới khi nhận được xác nhận khớp và hệ thống tự chuyển sang Đã thanh toán|
|BR-F05|Webhook phải được xác thực bằng chữ ký hoặc khóa bí mật; mọi lần gọi đều được ghi nhật ký hệ thống và bị bỏ qua nếu không xác thực được|
|BR-F06|Mỗi giao dịch ngân hàng chỉ được ghi nhận một lần: hệ thống chống gọi trùng theo mã giao dịch ngân hàng (idempotency), nhận lại cùng mã thì bỏ qua|
|BR-F07|Giao dịch được coi là khớp khi số tiền bằng đúng tổng tiền đơn và nội dung chuyển khoản chứa mã đơn hợp lệ; giao dịch không khớp được đưa vào danh sách chờ xử lý thủ công|
|BR-F08|Chỉ Admin được ghép tay một giao dịch không khớp vào đơn hoặc hoàn tiền cho khách, và thao tác này phải được ghi nhật ký hệ thống|
|BR-F09|Mỗi đơn chỉ có đúng một giao dịch thanh toán. Hệ thống không hỗ trợ thanh toán một phần, trả góp hay trả nhiều lần cho cùng một đơn|
|BR-F10|Mỗi lần thanh toán phải lưu số tiền, phương thức, thời gian, mã tham chiếu giao dịch và nguồn xác nhận (nhân viên hoặc webhook)|
|BR-F11|Khách có thể thanh toán trước hoặc tại thời điểm nhận đồ|
|BR-F12|Đơn chưa thanh toán không được chuyển sang Đã giao, trừ khi Admin duyệt cho nợ|
|BR-F13|Số tiền thanh toán phải bằng tổng tiền của đơn|
## **Nhóm G – Khuyến mãi (Phase 2)**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-G01|Voucher chỉ áp dụng được trong khoảng ngày hiệu lực|
|BR-G02|Voucher chỉ áp dụng khi giá trị đơn lớn hơn hoặc bằng giá trị đơn tối thiểu|
|BR-G03|Số lần sử dụng thực tế không được vượt quá số lần tối đa của voucher|
|BR-G04|Mỗi đơn chỉ áp dụng tối đa một voucher|
|BR-G05|Giá trị giảm không được vượt quá tổng tiền hàng của đơn|
|BR-G06|Điểm tích lũy chỉ được cộng sau khi đơn đã thanh toán và được tính trên số tiền khách thực trả, tức sau giảm giá|
|BR-G07|Căn cứ tính điểm = Tổng tiền hàng − Giảm giá + Phụ phí giặt nhanh − Phần thanh toán bằng đổi điểm; số điểm được cộng là phần nguyên của căn cứ tính điểm chia cho tỉ lệ quy đổi, làm tròn xuống|
|BR-G08|Khi đơn bị hủy hoặc hoàn tiền sau khi đã cộng điểm, hệ thống ghi một giao dịch trừ điểm bù vào sổ điểm, không sửa hoặc xóa bản ghi cũ; số dư điểm được chặn tại 0|
|BR-G09|Mỗi giao dịch điểm phải lưu căn cứ tính điểm và tỉ lệ quy đổi áp dụng tại thời điểm phát sinh, không chỉ lưu số điểm và số dư|
|BR-G10|Tỉ lệ quy đổi điểm phải do Admin cấu hình, không hard-code|
|BR-G11|Điểm tích lũy không được âm|
## **Nhóm H – Thông báo**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-H01|Khi đơn chuyển sang Hoàn thành, hệ thống phải tạo thông báo cho khách|
|BR-H02|MVP bắt buộc có kênh Web Notification, Email và SMS là phần mở rộng|
|BR-H03|Nội dung thông báo phải chứa mã đơn|
## **Nhóm I – Nhật ký và báo cáo**

|**Mã**|**Phát biểu**|
| :-: | :-: |
|BR-I01|Các hành động nhạy cảm như đổi giá, xác nhận thanh toán, ghép tay giao dịch chuyển khoản, đổi trạng thái đơn, thêm hoặc xóa máy phải được ghi nhật ký|
|BR-I02|Nhật ký phải lưu người dùng, hành động, đối tượng, thời gian, dữ liệu cũ và dữ liệu mới|
|BR-I03|Nhật ký hệ thống chỉ được đọc, không được sửa hay xóa|
|BR-I04|Doanh thu chỉ tính trên các đơn đã thanh toán|
|BR-I05|Đơn đã hủy không được tính vào doanh thu|


# **VIII. XÁC ĐỊNH YÊU CẦU**
## **8.1. Danh sách các yêu cầu**

|**STT**|**Tên yêu cầu**|**Biểu mẫu**|**Qui định**|
| :-: | :-: | :-: | :-: |
|1|Tra cứu và lập hồ sơ khách hàng|BM1|QĐ1|
|2|Quản lý dịch vụ và bảng giá|BM2, BM3|QĐ2, QĐ3|
|3|Tiếp nhận đồ và lập phiếu nhận đồ (tạo đơn)|BM4|QĐ4, QĐ5, QĐ6|
|4|Phân đơn vào máy giặt và máy sấy|BM5, BM6|QĐ7, QĐ8|
|5|Cập nhật trạng thái đơn|BM7|QĐ9, QĐ10|
|6|Thông báo cho khách khi đơn hoàn thành|BM8|QĐ11|
|7|Xác nhận thanh toán và lập hóa đơn|BM9|QĐ12, QĐ13|
|8|Giao đồ cho khách và đóng đơn|BM10|QĐ14|
|9|Áp dụng voucher và tích điểm|BM11, BM12|QĐ15, QĐ16|
|10|Lập báo cáo doanh thu|BM13|QĐ17|
|11|Lập báo cáo đơn hàng, dịch vụ và khách hàng|BM14, BM15, BM16|QĐ18|
|12|Tra cứu nhật ký hệ thống|BM17|QĐ19|
|13|Xử lý sự cố đồ hư hỏng hoặc thất lạc|BM18|QĐ20|
|14|Quản lý đồ tồn khách chưa đến nhận|BM19|QĐ21|
## **8.2. Danh sách các biểu mẫu và qui định**
### ***8.2.1. Biểu mẫu 1 và qui định 1***

|**BM1:**|**HỒ SƠ KHÁCH HÀNG**|||
| :- | :- | :- | :- |
|Mã khách hàng:||Ngày tạo:||
|Họ và tên:||Số điện thoại:||
|Địa chỉ:||Email:||
|Tài khoản đăng nhập:||Trạng thái tài khoản:||
|Loại khách hàng (Mới / Thường / VIP):||Điểm tích lũy:||
|Tổng số đơn đã sử dụng:||Tổng số tiền đã chi tiêu:||
|Dịch vụ thường sử dụng:||Nhân viên tạo hồ sơ:||

*Lịch sử giao dịch của khách hàng:*

|**STT**|**Mã đơn**|**Ngày nhận**|**Dịch vụ**|**Tổng tiền**|**Trạng thái**|
| :-: | :-: | :-: | :-: | :-: | :-: |
|||||||
|||||||
|||||||

|<p>**QĐ1:** Mỗi khách hàng được cấp một mã định danh duy nhất trong hệ thống và không thay đổi kể cả khi khách ngừng sử dụng dịch vụ rồi quay lại.</p><p>–  Hồ sơ khách hàng bắt buộc có họ tên và số điện thoại. Số điện thoại là khóa tra cứu chính và phải duy nhất.</p><p>–  Không được xóa khách hàng còn đơn chưa đóng, chỉ được chuyển sang trạng thái ngừng hoạt động.</p><p>–  Loại khách hàng được hệ thống tính lại định kỳ theo tổng chi tiêu, theo quy tắc do Admin cấu hình chứ không cố định trong mã nguồn.</p>|
| :- |
### ***8.2.2. Biểu mẫu 2, biểu mẫu 3 và qui định 2, 3***

|**BM2:**|**BẢNG GIÁ DỊCH VỤ**|||
| :- | :- | :- | :- |
|Ngày áp dụng:||Người ban hành:||

|**Mã DV**|**Tên dịch vụ**|**Đơn vị tính**|**Đơn giá**|**Thời gian xử lý**|**Giặt nhanh**|**Trạng thái**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: |
||||||||
||||||||
||||||||
||||||||

|**BM3:**|**LỊCH SỬ THAY ĐỔI GIÁ**|||
| :- | :- | :- | :- |
|Kỳ tra cứu từ ngày:||Đến ngày:||

|**STT**|**Tên dịch vụ**|**Giá cũ**|**Giá mới**|**Người sửa**|**Thời gian**|**Lý do**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: |
||||||||
||||||||
||||||||
||||||||

|<p>**QĐ2:** Chỉ Admin được phép thay đổi bảng giá. Nhân viên chỉ được xem.</p><p>–  Mỗi dịch vụ phải có đơn vị tính thuộc một trong các giá trị: kg, món, bộ, sản phẩm.</p><p>–  Dịch vụ đã được sử dụng trong đơn không được xóa, chỉ được chuyển sang trạng thái Ngừng hoạt động.</p><p>–  Dịch vụ giặt nhanh phải có phụ phí và thời gian xử lý ngắn hơn dịch vụ thường tương ứng.</p>|
| :- |

|<p>**QĐ3:** Mọi thay đổi giá đều phải sinh một bản ghi trong lịch sử thay đổi giá gồm giá cũ, giá mới, người sửa và thời gian.</p><p>–  Giá của đơn được chốt tại thời điểm tạo đơn. Việc thay đổi bảng giá sau đó không được làm thay đổi tổng tiền của các đơn đã tạo.</p><p>–  Bản ghi trong lịch sử thay đổi giá chỉ được ghi thêm, không được sửa hoặc xóa.</p>|
| :- |
### ***8.2.3. Biểu mẫu 4 và qui định 4, 5, 6***

|**BM4:**|**PHIẾU NHẬN ĐỒ**|||
| :- | :- | :- | :- |
|Tên cửa hàng:||Địa chỉ / SĐT cửa hàng:||
|Mã đơn:||Ngày giờ nhận:||
|Họ tên khách hàng:||Số điện thoại:||
|Loại khách hàng:||Nhân viên lập phiếu:||
|Ngày giờ hẹn trả cam kết:||Giặt nhanh (Có / Không):||

*Chi tiết đồ giặt:*

|**STT**|**Loại đồ**|**Số lượng / khối lượng**|**Đơn vị**|**Dịch vụ**|**Đơn giá**|**Thành tiền**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: |
||||||||
||||||||
||||||||
||||||||

*Ghi chú tình trạng đồ (có vết bẩn / bị rách / dễ phai màu / không dùng nước xả / giặt riêng / khác):*

|**STT**|**Loại đồ**|**Ghi chú tình trạng**|**Ảnh đính kèm**|
| :-: | :-: | :-: | :-: |
|||||
|||||
|||||

||**TỔNG KẾT THANH TOÁN**|||
| :- | :- | :- | :- |
|Tổng tiền hàng:||Giảm giá (voucher):||
|Phụ phí giặt nhanh:||Tổng thanh toán:||
|Điều khoản bồi thường:||Thời hạn lưu giữ đồ:||
|Chữ ký nhân viên:||Chữ ký khách hàng:||

|<p>**QĐ4:** Mỗi đơn phải có một mã duy nhất trong toàn hệ thống theo định dạng LD-YYYYMMDD-NNN, ví dụ LD-20260916-001.</p><p>–  Một đơn thuộc về đúng một khách hàng và phải có ít nhất một dòng chi tiết đồ.</p><p>–  Khối lượng phải lớn hơn 0, số lượng phải là số nguyên dương.</p><p>–  Một đơn có thể chứa nhiều loại đồ và nhiều dịch vụ khác nhau.</p>|
| :- |

|<p>**QĐ5:** Tổng tiền đơn = Tổng (đơn giá × số lượng hoặc khối lượng) − Giảm giá + Phụ phí giặt nhanh.</p><p>–  Đơn giá ghi trên đơn là đơn giá tại thời điểm tạo đơn và được lưu cố định cùng đơn.</p><p>–  Giá trị giảm giá không được vượt quá tổng tiền hàng.</p>|
| :- |

|<p>**QĐ6:** Ngày hẹn trả cam kết = Thời điểm máy phù hợp rảnh sớm nhất + Tổng thời gian các công đoạn nối tiếp của đơn (giặt → sấy → gấp) + Thời gian dự phòng n, làm tròn lên theo giờ mở cửa của tiệm.</p><p>–  Đơn được đánh dấu giặt nhanh sẽ chịu phụ phí, có thời gian hoàn thành ngắn hơn và được ưu tiên hiển thị nổi bật trong danh sách công việc.</p><p>–  Mốc bắt đầu của mỗi công đoạn = max(thời điểm công đoạn trước kết thúc, thời điểm máy của loại đó rảnh sớm nhất). Thời gian dự phòng n do Admin cấu hình theo từng dịch vụ và có thể bằng 0.</p><p>–  Ngày hẹn trả cam kết được chốt tại thời điểm tạo đơn và in trên phiếu nhận đồ, không tự thay đổi về sau. ETA dự báo nội bộ được tính lại khi hàng đợi thay đổi và chỉ dùng để cảnh báo; khi ETA vượt ngày hẹn cam kết, hệ thống cảnh báo để nhân viên liên hệ khách xin dời hẹn và ghi lý do vào nhật ký hệ thống.</p><p>–  Phiếu nhận đồ được in thành hai liên: khách giữ một liên, một liên kẹp cùng túi đồ, và là căn cứ đối chiếu khi giao trả.</p><p>–  Đơn chỉ được hủy khi còn ở trạng thái Mới nhận.</p>|
| :- |
### ***8.2.4. Biểu mẫu 5, biểu mẫu 6 và qui định 7, 8***

|**BM5:**|**PHIẾU PHÂN ĐƠN VÀO MÁY**|||
| :- | :- | :- | :- |
|Mã đơn:||Nhân viên thực hiện:||
|Mã lượt chạy:||Lần xử lý:||
|Tên máy:||Loại máy (Giặt / Sấy):||
|Công suất máy (kg):||Khối lượng nạp (kg):||
|Giờ bắt đầu:||Giờ dự kiến kết thúc:||
|Giờ kết thúc thực tế:||Ghi chú sự cố:||
|Trạng thái lượt chạy (Đã phân / Đang chạy / Hoàn thành / Gián đoạn):||Lý do gián đoạn:||
|Lượt chạy thay thế cho (mã lượt chạy bị gián đoạn):||Mã sự cố liên quan:||

|**BM6:**|**BẢNG THEO DÕI TRẠNG THÁI MÁY**|||
| :- | :- | :- | :- |
|Ngày:||Thời điểm cập nhật:||

|**Tên máy**|**Loại máy**|**Công suất**|**Trạng thái**|**Đơn đang xử lý**|**Giờ bắt đầu**|**Giờ dự kiến xong**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: |
||||||||
||||||||
||||||||
||||||||

|<p>**QĐ7:** Chỉ Admin được thêm máy, sửa thông tin máy, cho máy ngừng sử dụng hoặc xóa máy. Nhân viên chỉ được cập nhật trạng thái vận hành.</p><p>–  Trạng thái máy thuộc một trong bốn giá trị: Sẵn sàng, Đang hoạt động, Bảo trì, Ngừng sử dụng.</p><p>–  Máy ở trạng thái Bảo trì hoặc Ngừng sử dụng không được nhận công việc mới.</p><p>–  Máy đang xử lý đơn chưa hoàn thành thì không được xóa khỏi hệ thống.</p><p>–  Trạng thái máy được suy ra từ lượt chạy đang hoạt động trên máy, không được nhập tay độc lập, để tránh hai nguồn dữ liệu mâu thuẫn.</p>|
| :- |

|<p>**QĐ8:** Khối lượng đồ phân vào một lượt chạy không được vượt quá công suất của máy đó.</p><p>–  Đơn chỉ được phân vào máy đúng loại: công đoạn giặt vào máy giặt, công đoạn sấy vào máy sấy.</p><p>–  Một máy không được có hai lượt chạy chồng khung thời gian; ràng buộc này chỉ xét các lượt chạy ở trạng thái Đã phân và Đang chạy.</p><p>–  Mỗi lượt phân máy phải ghi đầy đủ mã lượt chạy, máy, đơn, lần xử lý của đơn, nhân viên thực hiện, giờ bắt đầu, giờ dự kiến kết thúc, giờ kết thúc thực tế và trạng thái lượt chạy.</p><p>–  Khi máy hỏng giữa chừng, giờ dự kiến hoàn thành phải được cập nhật lại kèm lý do.</p><p>–  Tại một thời điểm, một máy chỉ được có tối đa một lượt chạy ở trạng thái Đang chạy. Không được nạp thêm đồ vào máy khi lượt chạy đang diễn ra; mọi đợt đồ mới phải tạo một lượt chạy riêng. Trong phạm vi đề tài, mỗi lượt chạy chỉ phục vụ một đơn; việc gộp đồ của nhiều đơn vào cùng một lượt chạy không được hỗ trợ.</p><p>–  Trạng thái lượt chạy thuộc một trong bốn giá trị: Đã phân, Đang chạy, Hoàn thành, Gián đoạn. Lượt chạy Gián đoạn bắt buộc phải có lý do.</p><p>–  Khi máy hỏng giữa chừng, lượt chạy chuyển sang Gián đoạn và hệ thống tạo một lượt chạy mới trên máy khác trong cùng lần xử lý; trạng thái đơn không đổi và số lần xử lý không tăng.</p><p>–  Lượt chạy ở trạng thái Gián đoạn không được tính vào báo cáo hiệu suất sử dụng máy. Bản ghi lượt chạy chỉ được ghi thêm, không được sửa hoặc xóa.</p>|
| :- |
### ***8.2.5. Biểu mẫu 7 và qui định 9, 10***

|**BM7:**|**LỊCH SỬ TRẠNG THÁI ĐƠN**|||
| :- | :- | :- | :- |
|Mã đơn:||Khách hàng:||
|Ngày nhận:||Ngày hẹn trả cam kết:||
|Trạng thái hiện tại:||Tình trạng trễ (Có / Không):||
|Số lần xử lý:||Mã sự cố gần nhất:||

|**STT**|**Trạng thái**|**Thời gian**|**Nhân viên thực hiện**|**Máy liên quan**|**Ghi chú**|**Lần xử lý**|**Mã sự cố**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
|||||||||
|||||||||
|||||||||
|||||||||
|||||||||

|<p>**QĐ9:** Trạng thái đơn đi theo luồng mặc định: Mới nhận → Đang giặt → Đang sấy → Hoàn thành → Đã giao.</p><p>–  Trong phạm vi một lần xử lý, trạng thái chỉ được đi tới, không được lùi.</p><p>–  Đơn chỉ được chuyển sang Đã giao sau khi nhân viên xác nhận đã giao đồ cho khách.</p><p>–  Một đơn có thể có nhiều lần xử lý nếu phải làm lại một công đoạn, ví dụ đồ bị phai màu phải giặt lại sau khi đã sang Đang sấy. Khi đó số lần xử lý của đơn tăng thêm 1 và trạng thái quay về công đoạn cần làm lại.</p><p>–  Việc quay lại công đoạn trước chỉ được thực hiện thông qua phiếu sự cố (BM18) đã được Admin duyệt, không phải bằng thao tác sửa trạng thái. Hệ thống ghi nhận đây là sự kiện Mở lần xử lý mới, khác với sự kiện Admin sửa sai trạng thái.</p><p>–  Quyền sửa sai trạng thái của Admin chỉ dùng cho trường hợp nhân viên bấm nhầm và vẫn phải ghi lý do vào nhật ký hệ thống.</p>|
| :- |

|<p>**QĐ10:** Mọi thay đổi trạng thái đơn phải ghi nhận thời gian và người thực hiện. Bản ghi lịch sử không được sửa hoặc xóa.</p><p>–  Nếu thời gian hiện tại vượt quá ngày dự kiến hoàn thành mà đơn chưa ở trạng thái Hoàn thành, hệ thống tự động đánh dấu đơn là TRỄ.</p><p>–  Hàng đợi công việc được sắp xếp giảm dần theo độ ưu tiên. Độ ưu tiên = điểm theo tỉ lệ R (thời gian còn lại đến hạn chia cho thời gian xử lý còn lại): R > 2 là 0, 1 < R ≤ 2 là 1, 0 < R ≤ 1 là 2, R ≤ 0 là 3; cộng thêm 2 nếu là đơn giặt nhanh; đồng điểm thì xếp theo giờ nhận. Các ngưỡng do Admin cấu hình.</p><p>–  Độ ưu tiên và cờ TRỄ được cập nhật bằng job định kỳ 5–10 phút, đồng thời được tính lại ngay tại mỗi lần đơn đổi trạng thái vì thời gian xử lý còn lại thay đổi đột ngột tại các thời điểm đó.</p><p>–  Hệ thống cảnh báo khi một đơn đứng yên ở cùng một trạng thái quá thời gian quy định.</p>|
| :- |
### ***8.2.6. Biểu mẫu 8 và qui định 11***

|**BM8:**|**THÔNG BÁO ĐƠN HOÀN THÀNH**|||
| :- | :- | :- | :- |
|Mã thông báo:||Mã đơn:||
|Người nhận:||Số điện thoại / Email:||
|Kênh gửi (Web / Email / SMS):||Thời gian gửi:||
|Trạng thái gửi:||Trạng thái đọc:||

||**NỘI DUNG THÔNG BÁO**|||
| :- | :- | :- | :- |
|Nội dung:||||

*Nội dung mẫu: “Đơn giặt LD-20260916-001 của bạn đã hoàn thành. Bạn có thể đến cửa hàng nhận đồ.”*

|<p>**QĐ11:** Khi đơn chuyển sang trạng thái Hoàn thành, hệ thống phải tự động tạo thông báo gửi cho khách hàng.</p><p>–  Nội dung thông báo bắt buộc chứa mã đơn.</p><p>–  Phiên bản MVP bắt buộc có kênh Web Notification. Email, SMS và các nền tảng nhắn tin là phần mở rộng ở giai đoạn sau.</p><p>–  Nếu sau số ngày quy định khách vẫn chưa đến nhận, hệ thống gửi thông báo nhắc lần hai.</p>|
| :- |
### ***8.2.7. Biểu mẫu 9 và qui định 12, 13***

|**BM9:**|**HÓA ĐƠN THANH TOÁN**|||
| :- | :- | :- | :- |
|Số hóa đơn:||Mã đơn:||
|Họ tên khách hàng:||Số điện thoại:||
|Ngày nhận đồ:||Ngày thanh toán:||
|Phương thức thanh toán:||Nguồn xác nhận (nhân viên / webhook):||

|**STT**|**Dịch vụ**|**Số lượng / khối lượng**|**Đơn giá**|**Thành tiền**|
| :-: | :-: | :-: | :-: | :-: |
||||||
||||||
||||||
||||||

||**TỔNG KẾT**|||
| :- | :- | :- | :- |
|Tổng tiền hàng:||Mã voucher áp dụng:||
|Giảm giá:||Tổng thanh toán:||
|Điểm tích lũy được cộng:||Tổng điểm hiện có:||

|<p>**QĐ12:** Phương thức thanh toán thuộc một trong các giá trị: Tiền mặt, Chuyển khoản (QR).</p><p>–  Trạng thái thanh toán chỉ có hai giá trị: Chưa thanh toán và Đã thanh toán.</p><p>–  Mỗi lần thanh toán phải lưu số tiền, phương thức, thời gian, mã tham chiếu giao dịch và nguồn xác nhận là nhân viên hay webhook.</p><p>–  Số tiền thanh toán phải bằng tổng tiền của đơn.</p><p>–  Không hỗ trợ thanh toán một phần. Mỗi đơn chỉ phát sinh đúng một giao dịch thanh toán với số tiền bằng tổng tiền đơn, nhờ đó trạng thái thanh toán chỉ cần hai giá trị Chưa thanh toán và Đã thanh toán.</p><p>–  Hệ thống chỉ sinh mã QR động (VietQR) đã mã hóa sẵn số tài khoản, số tiền và nội dung chuyển khoản là mã đơn; bỏ hoàn toàn phương án khách nhập tay.</p><p>–  Thanh toán chuyển khoản được đối soát tự động qua webhook của cổng trung gian (Casso, SePay, PayOS). Sau khi khách quét mã, đơn ở trạng thái Chờ đối soát; khi webhook báo về một giao dịch khớp, hệ thống tự chuyển đơn sang Đã thanh toán mà không cần nhân viên thao tác.</p><p>–  Giao dịch được coi là khớp khi số tiền bằng đúng tổng tiền đơn và nội dung chuyển khoản chứa mã đơn hợp lệ. Hệ thống xác thực chữ ký của webhook trước khi xử lý và chống ghi nhận trùng theo mã giao dịch ngân hàng.</p><p>–  Giao dịch không khớp, gồm khách chuyển thiếu, chuyển thừa, sai nội dung hoặc không tìm thấy mã đơn, được đưa vào danh sách chờ xử lý thủ công. Chỉ Admin được ghép tay vào đơn hoặc hoàn tiền cho khách, và thao tác phải ghi nhật ký hệ thống.</p><p>–  Nhân viên chỉ xác nhận thủ công đối với phương thức Tiền mặt, hoặc đối với giao dịch chuyển khoản đã được Admin duyệt ghép tay. Mọi lần gọi webhook đều được ghi nhật ký.</p>|
| :- |

|<p>**QĐ13:** Khách có thể thanh toán trước hoặc thanh toán tại thời điểm nhận đồ.</p><p>–  Đơn chưa thanh toán không được chuyển sang trạng thái Đã giao, trừ khi Admin duyệt cho nợ.</p><p>–  Đơn được duyệt nợ sẽ xuất hiện trong báo cáo công nợ cho đến khi khách thanh toán. Khi thanh toán, khách phải trả đủ toàn bộ tổng tiền đơn trong một lần; hệ thống không hỗ trợ trả góp hay trả nhiều lần.</p><p>–  Doanh thu chỉ được ghi nhận đối với các đơn đã thanh toán. Đơn đã hủy không được tính vào doanh thu.</p>|
| :- |
### ***8.2.8. Biểu mẫu 10 và qui định 14***

|**BM10:**|**PHIẾU GIAO ĐỒ (BIÊN BẢN BÀN GIAO)**|||
| :- | :- | :- | :- |
|Mã đơn:||Ngày giờ giao:||
|Họ tên khách hàng:||Số điện thoại:||
|Số kiện / túi đồ bàn giao:||Tình trạng đồ khi giao:||
|Tên người nhận:||Quan hệ với khách (nếu nhận hộ):||
|Trạng thái thanh toán:||Nhân viên giao đồ:||
|Chữ ký nhân viên:||Chữ ký người nhận:||

|<p>**QĐ14:** Đơn chỉ được đóng khi nhân viên xác nhận đã giao đồ và khách hoặc người được ủy quyền đã ký nhận.</p><p>–  Trường hợp người khác nhận hộ, người nhận phải cung cấp đúng mã đơn hoặc số điện thoại của khách; hệ thống phải ghi nhận tên người nhận vào đơn.</p><p>–  Số lượng kiện đồ bàn giao phải khớp với số lượng đã ghi trên phiếu nhận đồ.</p><p>–  Sau khi đơn chuyển sang Đã giao, doanh thu của đơn được ghi nhận vào dashboard và báo cáo.</p>|
| :- |
### ***8.2.9. Biểu mẫu 11, biểu mẫu 12 và qui định 15, 16***

|**BM11:**|**PHIẾU VOUCHER**|||
| :- | :- | :- | :- |
|Mã voucher (Code):||Trạng thái:||
|Loại giảm giá (% / Số tiền):||Giá trị giảm:||
|Ngày bắt đầu:||Ngày kết thúc:||
|Giá trị đơn tối thiểu:||Số lần sử dụng tối đa:||
|Số lần đã sử dụng:||Người tạo:||

*Ví dụ: Mã GIAT10 – giảm 10% cho đơn từ 100.000 VNĐ.*

|**BM12:**|**SỔ TÍCH ĐIỂM KHÁCH HÀNG**|||
| :- | :- | :- | :- |
|Mã khách hàng:||Họ và tên:||
|Số dư điểm hiện tại:||Tỉ lệ quy đổi đang áp dụng:||

|**STT**|**Mã đơn**|**Loại giao dịch (Cộng / Trừ)**|**Căn cứ tính điểm**|**Tỉ lệ quy đổi áp dụng**|**Số điểm**|**Số dư sau giao dịch**|**Thời gian**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
|||||||||
|||||||||
|||||||||
|||||||||

|<p>**QĐ15:** Voucher chỉ áp dụng được trong khoảng từ ngày bắt đầu đến ngày kết thúc hiệu lực.</p><p>–  Voucher chỉ áp dụng khi giá trị đơn lớn hơn hoặc bằng giá trị đơn tối thiểu đã cấu hình.</p><p>–  Số lần sử dụng thực tế không được vượt quá số lần tối đa của voucher.</p><p>–  Mỗi đơn chỉ được áp dụng tối đa một voucher và giá trị giảm không vượt quá tổng tiền hàng.</p>|
| :- |

|<p>**QĐ16:** Điểm tích lũy chỉ được cộng sau khi đơn đã được thanh toán và được tính trên số tiền khách thực trả, tức sau giảm giá.</p><p>–  Tỉ lệ quy đổi điểm, ví dụ 10.000 VNĐ tương ứng 1 điểm, phải do Admin cấu hình trong hệ thống chứ không cố định trong mã nguồn.</p><p>–  Số dư điểm tích lũy của khách không được âm.</p><p>–  Căn cứ tính điểm = Tổng tiền hàng − Giảm giá + Phụ phí giặt nhanh − Phần thanh toán bằng đổi điểm. Số điểm được cộng = phần nguyên của (Căn cứ tính điểm / Tỉ lệ quy đổi), làm tròn xuống. Phụ phí giặt nhanh được cộng vì đó là tiền khách trả thật; phần thanh toán bằng đổi điểm bị trừ ra để tránh vòng lặp tự sinh điểm.</p><p>–  Khi đơn bị hủy hoặc hoàn tiền sau khi đã cộng điểm, hệ thống ghi một giao dịch trừ điểm bù vào sổ giao dịch điểm, không sửa hoặc xóa bản ghi cũ; số dư được chặn tại 0 để không vi phạm ràng buộc điểm không âm.</p><p>–  Mỗi dòng trong sổ giao dịch điểm phải lưu kèm căn cứ tính điểm và tỉ lệ quy đổi áp dụng tại thời điểm phát sinh, để kiểm chứng lại số điểm mà không cần truy ngược sang đơn hàng hay bảng cấu hình.</p><p>–  Mọi lần cộng hoặc trừ điểm đều phải được ghi vào sổ giao dịch điểm, hệ thống không chỉ lưu số dư cuối cùng.</p>|
| :- |
### ***8.2.10. Biểu mẫu 13 và qui định 17***

|**BM13:**|**BÁO CÁO DOANH THU**|||
| :- | :- | :- | :- |
|Kỳ báo cáo (Ngày / Tháng):||Từ ngày – đến ngày:||
|Tổng số đơn:||Số đơn đã thanh toán:||
|Tổng doanh thu:||Giá trị đơn trung bình:||
|Doanh thu kỳ trước:||Tỉ lệ tăng trưởng:||
|Người lập báo cáo:||Ngày lập:||

|**STT**|**Phương thức thanh toán**|**Số đơn**|**Số tiền**|**Tỉ trọng**|
| :-: | :-: | :-: | :-: | :-: |
||||||
||||||
||||||

|<p>**QĐ17:** Doanh thu chỉ được tính trên các đơn đã thanh toán; đơn đã hủy không được đưa vào báo cáo doanh thu.</p><p>–  Giá trị đơn trung bình = Tổng doanh thu / Số đơn đã thanh toán trong kỳ.</p><p>–  Báo cáo doanh thu phải được xem theo hai mức: theo ngày và theo tháng.</p><p>–  Chỉ Admin được xem báo cáo doanh thu của toàn cửa hàng.</p>|
| :- |
### ***8.2.11. Biểu mẫu 14, 15, 16 và qui định 18***

|**BM14:**|**BÁO CÁO ĐƠN HÀNG**|||
| :- | :- | :- | :- |
|Kỳ báo cáo:||Người lập:||
|Tổng số đơn:||Số đơn hoàn thành:||
|Số đơn đang xử lý:||Số đơn trễ:||
|Tỉ lệ đơn trễ:||Số đơn đã hủy:||
|Số đơn phải xử lý lại:||Số đơn trễ do xử lý lại:||

|**BM15:**|**BÁO CÁO DỊCH VỤ**|||
| :- | :- | :- | :- |
|Kỳ báo cáo:||Người lập:||

|**STT**|**Tên dịch vụ**|**Số lượt sử dụng**|**Tổng khối lượng / số lượng**|**Doanh thu**|**Tỉ trọng**|
| :-: | :-: | :-: | :-: | :-: | :-: |
|||||||
|||||||
|||||||
|||||||

|**BM16:**|**BÁO CÁO KHÁCH HÀNG**|||
| :- | :- | :- | :- |
|Kỳ báo cáo:||Người lập:||
|Số khách hàng mới:||Số khách quay lại:||
|Số khách VIP:||Tần suất sử dụng trung bình:||

|**STT**|**Mã khách hàng**|**Họ và tên**|**Số đơn trong kỳ**|**Tổng chi tiêu**|**Loại khách**|
| :-: | :-: | :-: | :-: | :-: | :-: |
|||||||
|||||||
|||||||
|||||||

|<p>**QĐ18:** Các báo cáo được lập theo kỳ do người dùng chọn và chỉ sử dụng dữ liệu đã được ghi nhận trên hệ thống.</p><p>–  Khách quay lại được xác định là khách có từ hai đơn trở lên tính đến thời điểm báo cáo.</p><p>–  Đơn đã hủy không được tính vào số đơn hoàn thành và không tính vào doanh thu theo dịch vụ.</p><p>–  Chỉ Admin được xem các báo cáo tổng hợp; nhân viên chỉ được xem dữ liệu liên quan đến công việc của mình.</p><p>–  Báo cáo đơn hàng phải tách riêng số đơn phải xử lý lại và số đơn trễ do xử lý lại, để phân biệt nút thắt do thiếu máy với nút thắt do chất lượng xử lý.</p>|
| :- |
### ***8.2.12. Biểu mẫu 17 và qui định 19***

|**BM17:**|**NHẬT KÝ HỆ THỐNG (AUDIT LOG)**|||
| :- | :- | :- | :- |
|Kỳ tra cứu từ ngày:||Đến ngày:||
|Lọc theo người dùng:||Lọc theo loại hành động:||

|**STT**|**Thời gian**|**Người dùng**|**Vai trò**|**Hành động**|**Đối tượng**|**Dữ liệu cũ**|**Dữ liệu mới**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
|||||||||
|||||||||
|||||||||
|||||||||

*Ví dụ: Admin thay đổi giá 20.000 thành 25.000 / NV01 xác nhận thanh toán đơn LD-001 / NV02 chuyển đơn LD-003 sang Đang giặt / Admin thêm máy Washer 03.*

|<p>**QĐ19:** Hệ thống phải ghi nhật ký với mọi hành động nhạy cảm: thay đổi giá, xác nhận thanh toán, thay đổi trạng thái đơn, thêm hoặc xóa máy, phân quyền người dùng.</p><p>–  Mỗi bản ghi phải có người dùng, hành động, đối tượng tác động, thời gian, dữ liệu cũ và dữ liệu mới.</p><p>–  Nhật ký hệ thống chỉ được đọc, không được sửa hoặc xóa.</p><p>–  Chỉ Admin được truy cập nhật ký hệ thống.</p>|
| :- |
### ***8.2.13. Biểu mẫu 18 và qui định 20***

|**BM18:**|**PHIẾU SỰ CỐ VÀ BỒI THƯỜNG**|||
| :- | :- | :- | :- |
|Mã sự cố:||Ngày lập:||
|Mã đơn liên quan:||Khách hàng:||
|Loại sự cố (Mất / Rách / Phai màu / Co rút / Khác):||Món đồ liên quan:||
|Nhân viên liên quan:||Máy liên quan:||
|Ảnh tình trạng ban đầu:||Ảnh hiện tại:||
|Phương án xử lý (Giặt lại / Giảm giá / Bồi thường):||Số tiền bồi thường:||
|Công đoạn cần làm lại (Giặt / Sấy / Cả hai / Không):||Lần xử lý được mở:||
|Người lập phiếu:||Người duyệt (Admin):||
|Chữ ký nhân viên:||Chữ ký khách hàng:||

||**MÔ TẢ SỰ CỐ**|||
| :- | :- | :- | :- |
|Nội dung mô tả:||||

|<p>**QĐ20:** Mọi khiếu nại về tình trạng đồ đều phải được đối chiếu với ảnh và ghi chú tình trạng ghi nhận tại thời điểm nhận đồ.</p><p>–  Phiếu sự cố do nhân viên lập nhưng phải được Admin duyệt trước khi thực hiện bồi thường.</p><p>–  Mức bồi thường tối đa được áp dụng theo điều khoản đã in trên phiếu nhận đồ.</p><p>–  Khi xảy ra thất lạc, hệ thống phải tra được đơn đã đi qua những máy nào, trong khung giờ nào và do nhân viên nào vận hành.</p><p>–  Khi phương án xử lý là giặt lại, phiếu sự cố phải ghi rõ công đoạn cần làm lại và mở một lần xử lý mới cho đơn; mọi lượt chạy máy sau đó được gắn với lần xử lý này để truy vết.</p>|
| :- |
### ***8.2.14. Biểu mẫu 19 và qui định 21***

|**BM19:**|**DANH SÁCH ĐỒ TỒN CHƯA NHẬN**|||
| :- | :- | :- | :- |
|Ngày lập danh sách:||Người lập:||
|Tổng số đơn tồn:||Số đơn quá hạn lưu giữ:||

|**STT**|**Mã đơn**|**Khách hàng**|**Số điện thoại**|**Ngày hoàn thành**|**Số ngày tồn**|**Số lần nhắc**|**Vị trí lưu giữ**|**Trạng thái thanh toán**|
| :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
||||||||||
||||||||||
||||||||||
||||||||||

|<p>**QĐ21:** Đơn đã ở trạng thái Hoàn thành quá số ngày quy định mà khách chưa đến nhận sẽ được đưa vào danh sách đồ tồn và hiển thị cảnh báo trên dashboard.</p><p>–  Hệ thống gửi thông báo nhắc khách lần hai khi đơn bắt đầu bị đánh dấu là đồ tồn.</p><p>–  Sau thời hạn lưu giữ do cửa hàng quy định, đơn được chuyển sang trạng thái đồ lưu kho lâu ngày và có thể áp dụng phí lưu giữ nếu chính sách cửa hàng cho phép.</p><p>–  Đơn nằm trong danh sách đồ tồn vẫn giữ nguyên trạng thái thanh toán và không được tự động đóng.</p>|
| :- |
# **IX. KẾT LUẬN**
Đề tài được xác định ở phạm vi tiệm giặt ủi dịch vụ một chi nhánh với khoảng 10–30 đơn mỗi ngày. Từ hiện trạng quản lý thủ công bằng sổ tay và phiếu giấy, nhóm xác định được nhu cầu tin học hóa toàn bộ quy trình từ tiếp nhận đồ đến giao trả và ghi nhận doanh thu.

\
**Form góp ý:**

![](Aspose.Words.2ce7bd04-ad28-459e-8f91-c303ba8b68cd.001.png)
