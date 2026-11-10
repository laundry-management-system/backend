// Sinh Postman collection + environment cho backend Laundry.
// Chạy từ thư mục backend: node postman/build-collection.mjs postman
import { writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'

const outDir = process.argv[2]
mkdirSync(outDir, { recursive: true })

// ---------- helpers ----------
const lines = (s) => s.replace(/^\n/, '').replace(/\n\s*$/, '').split('\n')

const SKIP_NO_STAFF = `
if (!pm.collectionVariables.get('staffToken')) {
  console.log('Bỏ qua: chưa có staffToken (xem README mục Tài khoản STAFF)');
  pm.execution.skipRequest();
}`

function req(name, method, path, opt = {}) {
  const { body, auth = 'admin', tests, pre, query, description } = opt
  const [rawPath, rawQuery] = path.split('?')
  const url = {
    raw: '{{baseUrl}}' + path,
    host: ['{{baseUrl}}'],
    path: rawPath.replace(/^\//, '').split('/'),
  }
  if (rawQuery) {
    url.query = rawQuery.split('&').map((kv) => {
      const [key, value = ''] = kv.split('=')
      return { key, value }
    })
  }
  const request = { method, header: [], url }
  if (description) request.description = description
  if (body !== undefined) {
    request.header.push({ key: 'Content-Type', value: 'application/json' })
    request.body = {
      mode: 'raw',
      raw: typeof body === 'string' ? body : JSON.stringify(body, null, 2),
      options: { raw: { language: 'json' } },
    }
  }
  if (auth === 'none') request.auth = { type: 'noauth' }
  else if (auth === 'staff')
    request.auth = { type: 'bearer', bearer: [{ key: 'token', value: '{{staffToken}}', type: 'string' }] }
  // auth === 'admin' kế thừa từ collection

  const event = []
  const preLines = [...(auth === 'staff' ? lines(SKIP_NO_STAFF) : []), ...(pre ? lines(pre) : [])]
  if (preLines.length) event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: preLines } })
  if (tests) event.push({ listen: 'test', script: { type: 'text/javascript', exec: lines(tests) } })
  return { name, event, request }
}

const folder = (name, description, item) => ({ name, description, item })

const status = (code) => `pm.test('HTTP ${code}', () => pm.response.to.have.status(${code}));`
const notOk = `pm.test('Không trả về 2xx', () => pm.expect(pm.response.code).to.not.be.within(200, 299));`

// ---------- 00 Khởi động ----------
const f00 = folder('00 - Khởi động & Đăng nhập', 'Chạy đầu tiên. Lưu adminToken cho toàn bộ collection.', [
  req('Health check', 'GET', '/api/health', { auth: 'none', tests: status(200) }),
  req('Đăng nhập Admin', 'POST', '/api/auth/login', {
    auth: 'none',
    body: { username: '{{adminUsername}}', password: '{{adminPassword}}' },
    tests: `
${status(200)}
const json = pm.response.json();
pm.test('Có accessToken', () => pm.expect(json.accessToken).to.be.a('string').and.not.empty);
pm.collectionVariables.set('adminToken', json.accessToken);`,
  }),
  req('Đăng nhập sai mật khẩu → 400', 'POST', '/api/auth/login', {
    auth: 'none',
    description: 'Dùng username ngẫu nhiên để không làm khóa tài khoản admin khi chạy nhiều lần.',
    pre: `pm.variables.set('wrongUser', 'postman_' + Date.now());`,
    body: { username: '{{wrongUser}}', password: 'sai-mat-khau' },
    tests: `
${status(400)}
pm.test('Trả về ProblemDetail', () => pm.expect(pm.response.json()).to.have.property('detail'));`,
  }),
  req('Gọi API không có token → 401', 'GET', '/api/users/me', { auth: 'none', tests: status(401) }),
  req('Thông tin tài khoản đang đăng nhập', 'GET', '/api/users/me', {
    tests: `
${status(200)}
pm.test('Là ADMIN', () => pm.expect(pm.response.json().role).to.eql('ADMIN'));`,
  }),
])

// ---------- 01 STAFF ----------
const f01 = folder(
  '01 - Tài khoản nhân viên (STAFF)',
  'Tùy chọn. Cần để chạy thư mục 08 (phân quyền). Xem README mục "Tài khoản STAFF".',
  [
    req('Tạo nhân viên mới', 'POST', '/api/users', {
      description: 'Bị bỏ qua nếu environment đã có staffUsername.',
      pre: `
if (pm.environment.get('staffUsername')) {
  console.log('Bỏ qua: đã có staffUsername');
  pm.execution.skipRequest();
}
const n = String(Date.now()).slice(-8);
pm.variables.set('newStaffPhone', '09' + n);
pm.variables.set('newStaffEmail', 'staff' + n + '@example.com');`,
      body: {
        fullName: 'Nhân viên Postman',
        phone: '{{newStaffPhone}}',
        email: '{{newStaffEmail}}',
        role: 'STAFF',
      },
      tests: `
${status(201)}
const json = pm.response.json();
pm.environment.set('staffUserId', json.id);
console.log('Đã tạo NV id=' + json.id + ', username=' + json.username + '. Lấy token đặt mật khẩu trong Redis rồi điền staffSetupToken.');
pm.environment.set('staffUsername', json.username);`,
    }),
    req('Đặt mật khẩu nhân viên', 'POST', '/api/users/setup-password?token={{staffSetupToken}}', {
      auth: 'none',
      description: 'Bị bỏ qua nếu environment chưa có staffSetupToken.',
      pre: `
if (!pm.environment.get('staffSetupToken')) {
  console.log('Bỏ qua: chưa có staffSetupToken');
  pm.execution.skipRequest();
}`,
      body: { password: '{{staffPassword}}' },
      tests: `
${status(200)}
pm.environment.set('staffSetupToken', '');`,
    }),
    req('Đăng nhập nhân viên', 'POST', '/api/auth/login', {
      auth: 'none',
      pre: `
if (!pm.environment.get('staffUsername') || !pm.environment.get('staffPassword')) {
  console.log('Bỏ qua: chưa có staffUsername / staffPassword');
  pm.execution.skipRequest();
}`,
      body: { username: '{{staffUsername}}', password: '{{staffPassword}}' },
      tests: `
${status(200)}
pm.collectionVariables.set('staffToken', pm.response.json().accessToken);`,
    }),
  ],
)

// ---------- 02 Settings ----------
const f02 = folder('02 - Cài đặt', 'WF-H09', [
  req('Danh sách tham số (Admin)', 'GET', '/api/settings', {
    tests: `
${status(200)}
const keys = pm.response.json().map(s => s.settingKey);
pm.test('Có WHITES_FEE và OVERDUE_DAYS', () => pm.expect(keys).to.include.members(['WHITES_FEE', 'OVERDUE_DAYS']));`,
  }),
  req('Cập nhật sai kiểu → 400', 'PATCH', '/api/settings/OVERDUE_DAYS', {
    body: { value: 'abc' },
    tests: status(400),
  }),
  req('Cập nhật OVERDUE_DAYS = 8', 'PATCH', '/api/settings/OVERDUE_DAYS', {
    body: { value: '8' },
    tests: `
${status(200)}
pm.test('Giá trị đã lưu', () => pm.expect(pm.response.json().value).to.eql('8'));`,
  }),
  req('Cập nhật nhiều tham số một lần (trả OVERDUE_DAYS về 7)', 'PATCH', '/api/settings', {
    body: { items: [{ settingKey: 'OVERDUE_DAYS', value: '7' }, { settingKey: 'STORE_PHONE', value: '0900000000' }] },
    tests: `
${status(200)}
pm.test('Trả về 2 tham số', () => pm.expect(pm.response.json()).to.have.length(2));`,
  }),
  req('Cập nhật nhiều tham số, một giá trị sai → 400, không lưu gì', 'PATCH', '/api/settings', {
    body: { items: [{ settingKey: 'OVERDUE_DAYS', value: '9' }, { settingKey: 'ORDER_CODE_PREFIX', value: 'quá dài' }] },
    tests: status(400),
  }),
  req('OVERDUE_DAYS vẫn là 7', 'GET', '/api/settings', {
    tests: `
pm.test('Không bị lưu dở dang', () => pm.expect(pm.response.json().find(s => s.settingKey === 'OVERDUE_DAYS').value).to.eql('7'));`,
  }),
])

// ---------- 03 Services ----------
const f03 = folder('03 - Dịch vụ & bảng giá', 'WF-H01, H02. Dịch vụ tạo thử sẽ được tắt ở cuối thư mục.', [
  req('Lấy dịch vụ DV01 (giặt sấy theo kg)', 'GET', '/api/services/by-code/DV01', {
    tests: `
${status(200)}
const s = pm.response.json();
pm.test('Đơn vị KG, đang hoạt động', () => { pm.expect(s.unit).to.eql('KG'); pm.expect(s.status).to.eql('ACTIVE'); });
pm.collectionVariables.set('svcKgId', s.id);
pm.collectionVariables.set('svcKgPrice', s.currentPrice);
pm.collectionVariables.set('svcKgMin', s.minQuantity);`,
  }),
  req('Danh sách dịch vụ đang hoạt động', 'GET', '/api/services/active', {
    tests: `
${status(200)}
pm.test('Không rỗng', () => pm.expect(pm.response.json()).to.be.an('array').that.is.not.empty);`,
  }),
  req('Tìm dịch vụ (phân trang)', 'GET', '/api/services?search=giặt&page=0&size=5', {
    tests: `
${status(200)}
pm.test('Có content', () => pm.expect(pm.response.json().content).to.be.an('array'));`,
  }),
  req('Tạo dịch vụ thử', 'POST', '/api/services', {
    pre: `pm.variables.set('svcName', 'Postman test ' + Date.now());`,
    body: {
      name: '{{svcName}}',
      unit: 'PIECE',
      currentPrice: 20000,
      standardHours: 24,
      expressSupported: true,
      expressSurcharge: 5000,
      expressHours: 6,
      minQuantity: 1,
      whitesApplicable: false,
    },
    tests: `
${status(201)}
const s = pm.response.json();
pm.test('Mã dạng DVxx', () => pm.expect(s.serviceCode).to.match(/^DV\\d+/));
pm.collectionVariables.set('newServiceId', s.id);`,
  }),
  req('Thời gian giặt nhanh ≥ thời gian chuẩn → 400', 'POST', '/api/services', {
    body: { name: 'Sai quy tắc', unit: 'PIECE', currentPrice: 1000, standardHours: 6, expressSupported: true, expressSurcharge: 0, expressHours: 6 },
    tests: status(400),
  }),
  req('Sửa giá dịch vụ thử', 'PUT', '/api/services/{{newServiceId}}', {
    body: { currentPrice: 25000, priceChangeReason: 'Postman: tăng giá' },
    tests: `
${status(200)}
pm.test('Giá mới = 25000', () => pm.expect(Number(pm.response.json().currentPrice)).to.eql(25000));`,
  }),
  req('Tắt dịch vụ thử', 'PATCH', '/api/services/{{newServiceId}}/status', {
    body: { status: 'INACTIVE' },
    tests: status(200),
  }),
  req('Tắt lần nữa → 400', 'PATCH', '/api/services/{{newServiceId}}/status', {
    body: { status: 'INACTIVE' },
    tests: status(400),
  }),
])

// ---------- 04 Customers ----------
const f04 = folder('04 - Khách hàng', 'WF-F01, F02', [
  req('Tạo khách hàng', 'POST', '/api/customers', {
    pre: `pm.collectionVariables.set('customerPhone', '08' + String(Date.now()).slice(-8));`,
    body: { fullName: 'Khách Postman', phone: '{{customerPhone}}', address: '1 Đường Test', note: 'Tạo bởi Postman' },
    tests: `
${status(201)}
const c = pm.response.json();
pm.test('Mã KHxxxxxx', () => pm.expect(c.customerCode).to.match(/^KH\\d{6}$/));
pm.collectionVariables.set('customerId', c.id);`,
  }),
  req('Trùng số điện thoại → 400', 'POST', '/api/customers', {
    body: { fullName: 'Trùng', phone: '{{customerPhone}}' },
    tests: status(400),
  }),
  req('SĐT sai định dạng → 400', 'POST', '/api/customers', {
    body: { fullName: 'Sai SĐT', phone: '12345' },
    tests: `
${status(400)}
pm.test('Có lỗi ở field phone', () => pm.expect(pm.response.json().errors).to.have.property('phone'));`,
  }),
  req('Xem khách theo id', 'GET', '/api/customers/{{customerId}}', { tests: status(200) }),
  req('Tìm khách theo SĐT', 'GET', '/api/customers/by-phone/{{customerPhone}}', {
    tests: `
${status(200)}
pm.test('Đúng khách', () => pm.expect(pm.response.json().id).to.eql(Number(pm.collectionVariables.get('customerId'))));`,
  }),
  req('Danh sách khách (search)', 'GET', '/api/customers?search={{customerPhone}}', {
    tests: `
${status(200)}
pm.test('Tìm thấy 1 khách', () => pm.expect(pm.response.json().totalElements).to.eql(1));`,
  }),
  req('Cập nhật khách', 'PUT', '/api/customers/{{customerId}}', {
    body: { fullName: 'Khách Postman (đã sửa)', phone: '{{customerPhone}}', address: '2 Đường Test', note: null },
    tests: `
${status(200)}
pm.test('Tên đã đổi', () => pm.expect(pm.response.json().fullName).to.eql('Khách Postman (đã sửa)'));`,
  }),
  req('Ngừng hoạt động khách', 'PATCH', '/api/customers/{{customerId}}/status', {
    body: { status: 'INACTIVE' },
    tests: status(200),
  }),
  req('Kích hoạt lại khách', 'PATCH', '/api/customers/{{customerId}}/status', {
    body: { status: 'ACTIVE' },
    tests: status(200),
  }),
])

// ---------- 05 Order happy path ----------
const intakeBody = (prefix) => ({
  requestId: `{{${prefix}RequestId}}`,
  customerPhone: '{{customerPhone}}',
  customerName: 'Khách Postman',
  orders: [{ orderType: 'NORMAL', serviceIds: ['{{svcKgId}}'], dueAt: `{{${prefix}DueAt}}`, washWhitesSeparately: false }],
})
const intakePre = (prefix) => `
pm.collectionVariables.set('${prefix}RequestId', pm.variables.replaceIn('{{$guid}}'));
pm.collectionVariables.set('${prefix}DueAt', new Date(Date.now() + 2 * 24 * 3600 * 1000).toISOString());`
// serviceIds là số: bỏ dấu nháy quanh biến khi gửi
const fixNum = (o) => JSON.stringify(o, null, 2).replace(/"\{\{(svcKgId)\}\}"/g, '{{$1}}')

const f05 = folder('05 - Luồng đơn chuẩn: nhận → phân loại → xử lý → thanh toán → giao', 'WF-C01, D01–D03, E01–E03, J01', [
  req('Tiếp nhận đồ (tạo lượt nhận)', 'POST', '/api/orders/intake', {
    pre: intakePre('order1'),
    body: fixNum(intakeBody('order1')),
    tests: `
${status(201)}
const r = pm.response.json();
pm.test('created = true, 1 đơn', () => { pm.expect(r.created).to.be.true; pm.expect(r.orders).to.have.length(1); });
const o = r.orders[0];
pm.test('Đơn Mới nhận, chưa có giá', () => { pm.expect(o.status).to.eql('RECEIVED'); pm.expect(Number(o.totalAmount)).to.eql(0); });
pm.test('Đã gắn khách hàng', () => pm.expect(r.customerId).to.eql(Number(pm.collectionVariables.get('customerId'))));
pm.collectionVariables.set('orderId', o.id);
pm.collectionVariables.set('orderCode', o.orderCode);`,
  }),
  req('Gửi lại cùng requestId → không tạo trùng', 'POST', '/api/orders/intake', {
    body: fixNum(intakeBody('order1')),
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('created = false, cùng mã đơn', () => {
  pm.expect(r.created).to.be.false;
  pm.expect(r.orders[0].orderCode).to.eql(pm.collectionVariables.get('orderCode'));
});`,
  }),
  req('Ngày hẹn trong quá khứ → 400', 'POST', '/api/orders/intake', {
    pre: `pm.variables.set('pastRequestId', pm.variables.replaceIn('{{$guid}}'));`,
    body: fixNum({
      requestId: '{{pastRequestId}}',
      customerPhone: '{{customerPhone}}',
      customerName: 'Khách Postman',
      orders: [{ orderType: 'NORMAL', serviceIds: ['{{svcKgId}}'], dueAt: '2020-01-01T00:00:00Z', washWhitesSeparately: false }],
    }),
    tests: status(400),
  }),
  req('Chi tiết đơn', 'GET', '/api/orders/{{orderId}}', {
    tests: `
${status(200)}
pm.test('Có lịch sử CREATED', () => pm.expect(pm.response.json().historyList.map(h => h.actionType)).to.include('CREATED'));`,
  }),
  req('In phiếu nhận đồ', 'GET', '/api/orders/{{orderId}}/receipt', {
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('Có QR tra cứu và dịch vụ tham khảo', () => { pm.expect(r.trackingQrUrl).to.be.a('string'); pm.expect(r.services).to.not.be.empty; });`,
  }),
  req('Danh sách đơn (tìm theo mã)', 'GET', '/api/orders?search={{orderCode}}', {
    tests: `
${status(200)}
pm.test('Tìm thấy đơn', () => pm.expect(pm.response.json().content.map(o => o.orderCode)).to.include(pm.collectionVariables.get('orderCode')));`,
  }),
  req('Phân loại: số lượng 0 → 400', 'PUT', '/api/orders/{{orderId}}/items', {
    body: fixNum({ items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: 0 }] }),
    tests: status(400),
  }),
  req('Phân loại & chốt giá (4.5 kg)', 'PUT', '/api/orders/{{orderId}}/items', {
    body: fixNum({ items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: 4.5, conditionNote: 'Ố nhẹ cổ áo' }] }),
    tests: `
${status(200)}
const o = pm.response.json();
const expected = Math.round(Number(pm.collectionVariables.get('svcKgPrice')) * 4.5);
pm.test('Tổng = đơn giá × 4.5 kg', () => pm.expect(Number(o.totalAmount)).to.eql(expected));
pm.test('Có 1 dòng đồ', () => pm.expect(o.items).to.have.length(1));
pm.collectionVariables.set('totalAmount', o.totalAmount);`,
  }),
  req('Chuyển Đang xử lý', 'PATCH', '/api/orders/{{orderId}}/status', {
    body: { newStatus: 'PROCESSING', note: 'Bắt đầu giặt' },
    tests: `
${status(200)}
pm.test('PROCESSING', () => pm.expect(pm.response.json().status).to.eql('PROCESSING'));`,
  }),
  req('Hoàn thành thiếu vị trí kệ → 400', 'POST', '/api/orders/{{orderId}}/complete', {
    body: { shelfLocation: '' },
    tests: status(400),
  }),
  req('Hoàn thành & xếp kệ', 'POST', '/api/orders/{{orderId}}/complete', {
    body: { shelfLocation: 'A1' },
    tests: `
${status(200)}
const o = pm.response.json();
pm.test('COMPLETED, kệ A1', () => { pm.expect(o.status).to.eql('COMPLETED'); pm.expect(o.shelfLocation).to.eql('A1'); });`,
  }),
  req('Soạn tin nhắn khách (Zalo)', 'POST', '/api/orders/{{orderCode}}/notifications/compose', {
    body: { channel: 'ZALO' },
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('Tin nhắn chứa mã đơn', () => pm.expect(r.message).to.include(pm.collectionVariables.get('orderCode')));
pm.test('notifyCount ≥ 1', () => pm.expect(r.notifyCount).to.be.at.least(1));`,
  }),
  req('Tạo mã VietQR', 'GET', '/api/orders/{{orderCode}}/payments/vietqr', {
    description: 'Trả 400 nếu chưa cấu hình biến môi trường VIETQR_*. Cả hai đều được coi là đạt.',
    tests: `
pm.test('200 hoặc 400 (chưa cấu hình VietQR)', () => pm.expect(pm.response.code).to.be.oneOf([200, 400]));
if (pm.response.code === 200) {
  pm.test('Số tiền QR = tổng đơn', () => pm.expect(Number(pm.response.json().amount)).to.eql(Number(pm.collectionVariables.get('totalAmount'))));
}`,
  }),
  req('Giao đồ khi chưa thanh toán → 400', 'POST', '/api/orders/{{orderCode}}/delivery', {
    body: {},
    tests: status(400),
  }),
  req('Thanh toán sai số tiền → 400', 'POST', '/api/orders/{{orderCode}}/payments', {
    pre: `pm.variables.set('wrongAmount', Number(pm.collectionVariables.get('totalAmount')) + 1000);`,
    body: '{\n  "amount": {{wrongAmount}},\n  "method": "CASH"\n}',
    tests: status(400),
  }),
  req('Xác nhận thanh toán tiền mặt', 'POST', '/api/orders/{{orderCode}}/payments', {
    body: '{\n  "amount": {{totalAmount}},\n  "method": "CASH"\n}',
    tests: `
${status(200)}
const p = pm.response.json();
pm.test('CONFIRMED, có số hóa đơn', () => { pm.expect(p.status).to.eql('CONFIRMED'); pm.expect(p.invoiceNo).to.match(/^INV/); });
pm.collectionVariables.set('paymentId', p.id);`,
  }),
  req('Thanh toán lần hai → 409', 'POST', '/api/orders/{{orderCode}}/payments', {
    body: '{\n  "amount": {{totalAmount}},\n  "method": "CASH"\n}',
    tests: status(409),
  }),
  req('Lịch sử thanh toán của đơn', 'GET', '/api/orders/{{orderCode}}/payments', {
    tests: `
${status(200)}
pm.test('Có 1 giao dịch', () => pm.expect(pm.response.json()).to.have.length(1));`,
  }),
  req('Người nhận hộ thiếu quan hệ → 400', 'POST', '/api/orders/{{orderCode}}/delivery', {
    body: { receiverName: 'Nguyễn Văn B' },
    tests: status(400),
  }),
  req('Giao đồ', 'POST', '/api/orders/{{orderCode}}/delivery', {
    body: {},
    tests: `
${status(200)}
pm.test('DELIVERED', () => pm.expect(pm.response.json().status).to.eql('DELIVERED'));`,
  }),
  req('Chi tiết đơn sau khi giao', 'GET', '/api/orders/{{orderId}}', {
    tests: `
${status(200)}
const o = pm.response.json();
pm.test('DELIVERED + PAID', () => { pm.expect(o.status).to.eql('DELIVERED'); pm.expect(o.paymentStatus).to.eql('PAID'); });`,
  }),
  req('Tra cứu công khai (không đăng nhập)', 'POST', '/api/public/orders/lookup', {
    auth: 'none',
    body: { orderCode: '{{orderCode}}', phone: '{{customerPhone}}' },
    tests: `
${status(200)}
pm.test('Trạng thái DELIVERED', () => pm.expect(pm.response.json().status).to.eql('DELIVERED'));`,
  }),
  req('Tra cứu công khai sai SĐT → 404', 'POST', '/api/public/orders/lookup', {
    auth: 'none',
    body: { orderCode: '{{orderCode}}', phone: '0999999999' },
    tests: status(404),
  }),
])

// ---------- 06 Admin actions ----------
const f06 = folder('06 - Thao tác Admin: dời hẹn, sửa giá, lùi trạng thái, hủy thanh toán, hủy đơn', 'WF-D04, D05, E04', [
  req('Tạo đơn thứ hai', 'POST', '/api/orders/intake', {
    pre: intakePre('order2'),
    body: fixNum(intakeBody('order2')),
    tests: `
${status(201)}
const o = pm.response.json().orders[0];
pm.collectionVariables.set('order2Id', o.id);
pm.collectionVariables.set('order2Code', o.orderCode);`,
  }),
  req('Phân loại dưới mức tối thiểu (2 kg → tính tối thiểu)', 'PUT', '/api/orders/{{order2Id}}/items', {
    body: fixNum({ items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: 2 }] }),
    tests: `
${status(200)}
const price = Number(pm.collectionVariables.get('svcKgPrice'));
const min = Number(pm.collectionVariables.get('svcKgMin'));
pm.test('Tính theo max(2, tối thiểu)', () => pm.expect(Number(pm.response.json().totalAmount)).to.eql(Math.round(price * Math.max(2, min))));`,
  }),
  req('Dời ngày hẹn thiếu lý do → 400', 'PATCH', '/api/orders/{{order2Id}}/reschedule', {
    body: { newDueAt: '2030-01-01T10:00:00Z', reason: '' },
    tests: status(400),
  }),
  req('Dời ngày hẹn', 'PATCH', '/api/orders/{{order2Id}}/reschedule', {
    pre: `pm.variables.set('newDueAt', new Date(Date.now() + 3 * 24 * 3600 * 1000).toISOString());`,
    body: { newDueAt: '{{newDueAt}}', reason: 'Khách xin lấy muộn' },
    tests: `
${status(200)}
pm.test('Có lịch sử RESCHEDULE', () => pm.expect(pm.response.json().historyList.map(h => h.actionType)).to.include('RESCHEDULE'));`,
  }),
  req('Sửa giá (Admin)', 'PATCH', '/api/orders/{{order2Id}}/price', {
    body: { newTotalAmount: 80000, reason: 'Khách quen' },
    tests: `
${status(200)}
pm.test('Tổng = 80000', () => pm.expect(Number(pm.response.json().totalAmount)).to.eql(80000));`,
  }),
  req('Chuyển Đang xử lý', 'PATCH', '/api/orders/{{order2Id}}/status', {
    body: { newStatus: 'PROCESSING' },
    tests: status(200),
  }),
  req('Lùi trạng thái về Mới nhận', 'POST', '/api/orders/{{order2Id}}/revert-status', {
    body: { targetStatus: 'RECEIVED', reason: 'NV bấm nhầm' },
    tests: `
${status(200)}
pm.test('RECEIVED', () => pm.expect(pm.response.json().status).to.eql('RECEIVED'));`,
  }),
  req('Thanh toán đơn hai', 'POST', '/api/orders/{{order2Code}}/payments', {
    body: { amount: 80000, method: 'BANK_TRANSFER', referenceCode: 'FT-POSTMAN' },
    tests: `
${status(200)}
pm.collectionVariables.set('payment2Id', pm.response.json().id);`,
  }),
  req('Hủy xác nhận thanh toán (Admin)', 'POST', '/api/orders/{{order2Code}}/payments/{{payment2Id}}/resolution', {
    body: { resolution: 'VOIDED', reason: 'Xác nhận nhầm đơn' },
    tests: `
${status(200)}
pm.test('VOIDED', () => pm.expect(pm.response.json().status).to.eql('VOIDED'));`,
  }),
  req('Đơn trở lại Chưa thanh toán', 'GET', '/api/orders/{{order2Id}}', {
    tests: `
${status(200)}
pm.test('UNPAID', () => pm.expect(pm.response.json().paymentStatus).to.eql('UNPAID'));`,
  }),
  req('Ghi nhận chênh lệch tiền', 'POST', '/api/orders/{{order2Code}}/payment-discrepancies', {
    body: { receivedAmount: 70000, note: 'Khách chuyển thiếu 10.000đ' },
    tests: status(200),
  }),
  req('Hủy đơn', 'POST', '/api/orders/{{order2Id}}/cancel', {
    body: { reason: 'Khách đổi ý' },
    tests: `
${status(200)}
pm.test('CANCELLED', () => pm.expect(pm.response.json().status).to.eql('CANCELLED'));`,
  }),
  req('Hủy lần nữa → 400', 'POST', '/api/orders/{{order2Id}}/cancel', {
    body: { reason: 'Thử lại' },
    tests: status(400),
  }),
])

// ---------- 07 Overdue & audit ----------
const f07 = folder('07 - Đồ tồn & Nhật ký hệ thống', 'WF-G01, I03', [
  req('Danh sách đồ tồn', 'GET', '/api/orders/overdue', {
    tests: `
${status(200)}
pm.test('Là mảng', () => pm.expect(pm.response.json()).to.be.an('array'));`,
  }),
  req('Nhật ký thanh toán', 'GET', '/api/log?entityType=PAYMENT&size=5', {
    tests: `
${status(200)}
const page = pm.response.json();
pm.test('Có ít nhất 1 dòng (vừa hủy xác nhận ở thư mục 06)', () => pm.expect(page.content).to.not.be.empty);
if (page.content.length) pm.collectionVariables.set('auditLogId', page.content[0].id);`,
  }),
  req('Chi tiết một dòng nhật ký', 'GET', '/api/log/{{auditLogId}}', {
    tests: `
${status(200)}
pm.test('Có oldData/newData', () => pm.expect(pm.response.json()).to.include.keys('oldData', 'newData'));`,
  }),
  req('Nhật ký cập nhật cài đặt', 'GET', '/api/log?action=SETTING_UPDATED&size=5', {
    tests: `
${status(200)}
pm.test('Có dòng SETTING_UPDATED', () => pm.expect(pm.response.json().content).to.not.be.empty);`,
  }),
])

// ---------- 08 Permissions ----------
const f08 = folder('08 - Phân quyền (cần tài khoản STAFF)', 'Tự bỏ qua nếu chưa có staffToken.', [
  req('NV xem danh sách đơn → 200', 'GET', '/api/orders?size=1', { auth: 'staff', tests: status(200) }),
  req('NV sửa cài đặt → 403', 'PATCH', '/api/settings/OVERDUE_DAYS', { auth: 'staff', body: { value: '7' }, tests: status(403) }),
  req('NV tạo dịch vụ → 403', 'POST', '/api/services', {
    auth: 'staff',
    body: { name: 'NV tạo', unit: 'PIECE', currentPrice: 1000, standardHours: 24 },
    tests: status(403),
  }),
  req('NV tạo tài khoản → 403', 'POST', '/api/users', {
    auth: 'staff',
    body: { fullName: 'X', phone: '0900000001', email: 'x@example.com', role: 'STAFF' },
    tests: status(403),
  }),
  req('NV xem nhật ký → 403', 'GET', '/api/log', { auth: 'staff', tests: status(403) }),
  req('NV hủy xác nhận thanh toán → 403', 'POST', '/api/orders/{{orderCode}}/payments/{{paymentId}}/resolution', {
    auth: 'staff',
    body: { resolution: 'VOIDED', reason: 'NV thử' },
    tests: status(403),
  }),
])

// ---------- 09 Voucher ----------
const classifyKg = (_unused, qty, voucher) => fixNum({
  items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: qty }],
  ...(voucher === undefined ? {} : { voucherCode: voucher }),
})

const f09 = folder('09 - Voucher (GĐ1)', 'WF-H05, WF-C03. Tạo voucher 1 lượt, áp vào đơn, hết lượt, hủy đơn trả lượt.', [
  req('Tạo voucher 10% (1 lượt, đơn từ 100.000đ)', 'POST', '/api/vouchers', {
    pre: `
pm.collectionVariables.set('voucherCode', 'PM' + String(Date.now()).slice(-8));
const d = new Date(Date.now() + 7 * 3600 * 1000).toISOString().slice(0, 10);
pm.collectionVariables.set('today', d);`,
    body: {
      code: '{{voucherCode}}', discountType: 'PERCENT', discountValue: 10,
      startDate: '{{today}}', endDate: '2099-12-31', minOrderValue: 100000, maxUses: 1, description: 'Postman',
    },
    tests: `
${status(201)}
const v = pm.response.json();
pm.test('ACTIVE, chưa dùng', () => { pm.expect(v.displayStatus).to.eql('ACTIVE'); pm.expect(v.usedCount).to.eql(0); });
pm.collectionVariables.set('voucherId', v.id);`,
  }),
  req('Tạo trùng mã → 400', 'POST', '/api/vouchers', {
    body: { code: '{{voucherCode}}', discountType: 'AMOUNT', discountValue: 1000, startDate: '{{today}}', endDate: '2099-12-31', maxUses: 1 },
    tests: status(400),
  }),
  req('Giảm 150% → 400', 'POST', '/api/vouchers', {
    body: { code: 'SAI{{voucherCode}}', discountType: 'PERCENT', discountValue: 150, startDate: '{{today}}', endDate: '2099-12-31', maxUses: 1 },
    tests: status(400),
  }),
  req('Kiểm tra mã: tiền hàng 200.000đ → giảm 20.000đ', 'GET', '/api/vouchers/check?code={{voucherCode}}&subtotal=200000', {
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('ok, giảm 20000', () => { pm.expect(r.ok).to.be.true; pm.expect(Number(r.discount)).to.eql(20000); });`,
  }),
  req('Kiểm tra mã: dưới tối thiểu → BELOW_MIN', 'GET', '/api/vouchers/check?code={{voucherCode}}&subtotal=50000', {
    tests: `pm.test('BELOW_MIN', () => pm.expect(pm.response.json().reason).to.eql('BELOW_MIN'));`,
  }),
  req('Kiểm tra mã không tồn tại → NOT_FOUND', 'GET', '/api/vouchers/check?code=KHONGCO_XYZ&subtotal=100000', {
    tests: `pm.test('NOT_FOUND', () => pm.expect(pm.response.json().reason).to.eql('NOT_FOUND'));`,
  }),
  req('Tạo đơn 5', 'POST', '/api/orders/intake', {
    pre: intakePre('order5'),
    body: fixNum(intakeBody('order5')),
    tests: `
${status(201)}
pm.collectionVariables.set('order5Id', pm.response.json().orders[0].id);`,
  }),
  req('Phân loại đơn 5 kèm voucher (5 kg)', 'PUT', '/api/orders/{{order5Id}}/items', {
    body: classifyKg('', 5, '{{voucherCode}}'),
    tests: `
${status(200)}
const o = pm.response.json();
const sub = Math.round(Number(pm.collectionVariables.get('svcKgPrice')) * 5);
pm.test('Giảm 10% tiền hàng', () => pm.expect(Number(o.discountAmount)).to.eql(Math.round(sub * 0.1)));
pm.test('Có voucherCode', () => pm.expect(o.voucherCode).to.eql(pm.collectionVariables.get('voucherCode')));`,
  }),
  req('Phân loại lại cùng mã → không trừ thêm lượt', 'PUT', '/api/orders/{{order5Id}}/items', {
    body: classifyKg('', 5, '{{voucherCode}}'),
    tests: status(200),
  }),
  req('Voucher đã dùng 1/1 → USED_UP', 'GET', '/api/vouchers/{{voucherId}}', {
    tests: `
${status(200)}
const v = pm.response.json();
pm.test('usedCount = 1, USED_UP', () => { pm.expect(v.usedCount).to.eql(1); pm.expect(v.displayStatus).to.eql('USED_UP'); });`,
  }),
  req('Tạo đơn 6', 'POST', '/api/orders/intake', {
    pre: intakePre('order6'),
    body: fixNum(intakeBody('order6')),
    tests: `
${status(201)}
pm.collectionVariables.set('order6Id', pm.response.json().orders[0].id);
pm.collectionVariables.set('order6Code', pm.response.json().orders[0].orderCode);`,
  }),
  req('Áp mã đã hết lượt vào đơn 6 → 400', 'PUT', '/api/orders/{{order6Id}}/items', {
    body: classifyKg('', 5, '{{voucherCode}}'),
    tests: `
${status(400)}
pm.test('Lỗi ở voucherCode', () => pm.expect(pm.response.json().errors).to.have.property('voucherCode'));`,
  }),
  req('Hủy đơn 5 → trả lượt voucher', 'POST', '/api/orders/{{order5Id}}/cancel', {
    body: { reason: 'Postman: thử trả lượt' },
    tests: status(200),
  }),
  req('Voucher còn lại 0/1', 'GET', '/api/vouchers/{{voucherId}}', {
    tests: `pm.test('usedCount = 0', () => pm.expect(pm.response.json().usedCount).to.eql(0));`,
  }),
  req('Tắt voucher', 'PATCH', '/api/vouchers/{{voucherId}}/status', {
    body: { status: 'DISABLED' },
    tests: status(200),
  }),
  req('Danh sách voucher', 'GET', '/api/vouchers?search={{voucherCode}}', {
    tests: `
${status(200)}
pm.test('Tìm thấy', () => pm.expect(pm.response.json().totalElements).to.eql(1));`,
  }),
])

// ---------- 10 Machines ----------
const f10 = folder('10 - Máy giặt & máy sấy (GĐ2)', 'WF-H04', [
  req('Thêm máy giặt', 'POST', '/api/machines', {
    body: { name: 'Máy giặt Postman', type: 'WASHER', capacityKg: 10, note: 'Tạo bởi Postman' },
    tests: `
${status(201)}
const m = pm.response.json();
pm.test('Mã MGxx, READY', () => { pm.expect(m.machineCode).to.match(/^MG\\d{2,}$/); pm.expect(m.status).to.eql('READY'); });
pm.collectionVariables.set('machineId', m.id);`,
  }),
  req('Danh sách máy giặt', 'GET', '/api/machines?type=WASHER', {
    tests: `
${status(200)}
pm.test('Chỉ có máy giặt', () => pm.response.json().forEach(m => pm.expect(m.type).to.eql('WASHER')));`,
  }),
  req('Chuyển Bảo trì không có lý do → 400', 'PATCH', '/api/machines/{{machineId}}/status', {
    body: { status: 'MAINTENANCE' },
    tests: status(400),
  }),
  req('Chuyển Bảo trì', 'PATCH', '/api/machines/{{machineId}}/status', {
    body: { status: 'MAINTENANCE', reason: 'Kêu to khi vắt' },
    tests: `
${status(200)}
pm.test('MAINTENANCE', () => pm.expect(pm.response.json().status).to.eql('MAINTENANCE'));`,
  }),
  req('Đổi loại máy khi sửa → 400', 'PUT', '/api/machines/{{machineId}}', {
    body: { name: 'Đổi loại', type: 'DRYER', capacityKg: 10 },
    tests: status(400),
  }),
  req('Ngừng sử dụng', 'PATCH', '/api/machines/{{machineId}}/retire', {
    body: { reason: 'Postman dọn dẹp' },
    tests: `
${status(200)}
pm.test('RETIRED', () => pm.expect(pm.response.json().status).to.eql('RETIRED'));`,
  }),
  req('Máy đã ngừng không đổi trạng thái được → 400', 'PATCH', '/api/machines/{{machineId}}/status', {
    body: { status: 'READY' },
    tests: status(400),
  }),
  req('NV thêm máy → 403', 'POST', '/api/machines', {
    auth: 'staff',
    body: { name: 'NV thêm', type: 'DRYER', capacityKg: 8 },
    tests: status(403),
  }),
])

// ---------- 11 Incidents + checkout ----------
const f11 = folder('11 - Phiếu sự cố & Thanh toán–giao một lần (GĐ3, GĐ4)', 'WF-H06, H07, E01–E03. Dùng đơn 6.', [
  req('Phân loại đơn 6 (5 kg, không voucher)', 'PUT', '/api/orders/{{order6Id}}/items', {
    body: classifyKg('', 5),
    tests: `
${status(200)}
pm.collectionVariables.set('order6Total', pm.response.json().totalAmount);
pm.collectionVariables.set('order6ItemId', pm.response.json().items[0].id);`,
  }),
  req('Chưa xử lý: lập phiếu Giặt lại → 400', 'POST', '/api/incidents', {
    body: fixNum({ orderId: '{{order6Id}}', type: 'FADED', description: 'Phai màu', resolution: 'REWASH', redoStage: 'WASH' }).replace('"{{order6Id}}"', '{{order6Id}}'),
    tests: status(400),
  }),
  req('Bắt đầu xử lý đơn 6', 'PATCH', '/api/orders/{{order6Id}}/status', {
    body: { newStatus: 'PROCESSING' },
    tests: status(200),
  }),
  req('Lập phiếu Giặt lại', 'POST', '/api/incidents', {
    body: '{\n  "orderId": {{order6Id}},\n  "orderItemId": {{order6ItemId}},\n  "type": "FADED",\n  "description": "Áo trắng bị loang màu",\n  "resolution": "REWASH",\n  "redoStage": "WASH"\n}',
    tests: `
${status(201)}
const i = pm.response.json();
pm.test('PENDING, mã SC-', () => { pm.expect(i.status).to.eql('PENDING'); pm.expect(i.incidentCode).to.match(/^SC-/); });
pm.collectionVariables.set('incidentId', i.id);`,
  }),
  req('NV duyệt phiếu → 403', 'POST', '/api/incidents/{{incidentId}}/approve', {
    auth: 'staff', body: {}, tests: status(403),
  }),
  req('Admin duyệt phiếu Giặt lại', 'POST', '/api/incidents/{{incidentId}}/approve', {
    body: { note: 'Đồng ý giặt lại' },
    tests: `
${status(200)}
pm.test('APPROVED, mở lần xử lý 2', () => { pm.expect(pm.response.json().status).to.eql('APPROVED'); pm.expect(pm.response.json().openedProcessingCount).to.eql(2); });`,
  }),
  req('Duyệt lại lần nữa → 400', 'POST', '/api/incidents/{{incidentId}}/approve', { body: {}, tests: status(400) }),
  req('Đơn 6: lần xử lý = 2, có INCIDENT_REOPEN', 'GET', '/api/orders/{{order6Id}}', {
    tests: `
const o = pm.response.json();
pm.test('processingCount = 2', () => pm.expect(o.processingCount).to.eql(2));
pm.test('Có lịch sử INCIDENT_REOPEN', () => pm.expect(o.historyList.map(h => h.actionType)).to.include('INCIDENT_REOPEN'));`,
  }),
  req('Lập phiếu Giảm giá 10.000đ', 'POST', '/api/incidents', {
    body: '{\n  "orderId": {{order6Id}},\n  "type": "TORN",\n  "description": "Rách nhẹ đường may",\n  "resolution": "DISCOUNT",\n  "compensationAmount": 10000\n}',
    tests: `
${status(201)}
pm.collectionVariables.set('incident2Id', pm.response.json().id);`,
  }),
  req('Admin duyệt phiếu Giảm giá → tổng đơn giảm 10.000đ', 'POST', '/api/incidents/{{incident2Id}}/approve', {
    body: {},
    tests: status(200),
  }),
  req('Tổng đơn 6 đã giảm', 'GET', '/api/orders/{{order6Id}}', {
    tests: `
const o = pm.response.json();
pm.test('total = trước − 10000', () => pm.expect(Number(o.totalAmount)).to.eql(Number(pm.collectionVariables.get('order6Total')) - 10000));
pm.collectionVariables.set('order6Total', o.totalAmount);`,
  }),
  req('Lập phiếu Bồi thường vượt mức → 400', 'POST', '/api/incidents', {
    body: '{\n  "orderId": {{order6Id}},\n  "type": "LOST",\n  "description": "Mất 1 chiếc tất",\n  "resolution": "COMPENSATE",\n  "compensationAmount": 999999999\n}',
    tests: status(400),
  }),
  req('Lập phiếu Bồi thường rồi từ chối', 'POST', '/api/incidents', {
    body: '{\n  "orderId": {{order6Id}},\n  "type": "LOST",\n  "description": "Mất 1 chiếc tất",\n  "resolution": "COMPENSATE",\n  "compensationAmount": 20000\n}',
    tests: `
${status(201)}
pm.collectionVariables.set('incident3Id', pm.response.json().id);`,
  }),
  req('Từ chối không lý do → 400', 'POST', '/api/incidents/{{incident3Id}}/reject', { body: { note: '' }, tests: status(400) }),
  req('Từ chối phiếu', 'POST', '/api/incidents/{{incident3Id}}/reject', {
    body: { note: 'Tìm thấy tất trong túi khác' },
    tests: `pm.test('REJECTED', () => pm.expect(pm.response.json().status).to.eql('REJECTED'));`,
  }),
  req('Sự cố của đơn 6', 'GET', '/api/orders/{{order6Id}}/incidents', {
    tests: `
${status(200)}
pm.test('3 phiếu', () => pm.expect(pm.response.json()).to.have.length(3));`,
  }),
  req('Danh sách sự cố chờ duyệt', 'GET', '/api/incidents?status=PENDING', { tests: status(200) }),
  req('Hoàn thành đơn 6', 'POST', '/api/orders/{{order6Id}}/complete', { body: { shelfLocation: 'C3' }, tests: status(200) }),
  req('Checkout sai số tiền → 400, không thu tiền', 'POST', '/api/orders/{{order6Code}}/checkout', {
    body: '{\n  "amount": 1,\n  "method": "CASH"\n}',
    tests: status(400),
  }),
  req('Checkout: thu tiền + giao trong một lần', 'POST', '/api/orders/{{order6Code}}/checkout', {
    body: '{\n  "amount": {{order6Total}},\n  "method": "CASH",\n  "receiverName": "Trần Thị C",\n  "receiverRelation": "Vợ"\n}',
    tests: `
${status(200)}
pm.test('DELIVERED, người nhận hộ', () => { pm.expect(pm.response.json().status).to.eql('DELIVERED'); pm.expect(pm.response.json().receiverName).to.eql('Trần Thị C'); });`,
  }),
  req('Chi tiết đơn 6: có phương thức & thời điểm thanh toán', 'GET', '/api/orders/{{order6Id}}', {
    tests: `
const o = pm.response.json();
pm.test('paymentMethod = CASH, có paidAt', () => { pm.expect(o.paymentMethod).to.eql('CASH'); pm.expect(o.paidAt).to.be.a('string'); });`,
  }),
])

// ---------- 12 Reports ----------
const f12 = folder('12 - Báo cáo & Dashboard (GĐ5)', 'WF-I01, I02, B01', [
  req('Doanh thu hôm nay theo ngày', 'GET', '/api/reports/revenue?from={{today}}&to={{today}}&groupBy=DAY', {
    pre: `pm.collectionVariables.set('today', new Date(Date.now() + 7 * 3600 * 1000).toISOString().slice(0, 10));`,
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('Có doanh thu (đã thu ở thư mục 05, 11)', () => pm.expect(Number(r.totalRevenue)).to.be.above(0));
pm.test('Chia theo phương thức', () => pm.expect(r.byMethod.map(m => m.method)).to.include('CASH'));`,
  }),
  req('Doanh thu theo tháng', 'GET', '/api/reports/revenue?from=2026-01-01&to={{today}}&groupBy=MONTH', {
    tests: `
${status(200)}
pm.test('Kỳ dạng yyyy-MM', () => pm.response.json().periods.forEach(p => pm.expect(p.period).to.match(/^\\d{4}-\\d{2}$/)));`,
  }),
  req('Khoảng ngày ngược → 400', 'GET', '/api/reports/revenue?from={{today}}&to=2020-01-01', { tests: status(400) }),
  req('Thiếu tham số → 400', 'GET', '/api/reports/revenue', { tests: status(400) }),
  req('Báo cáo đơn hàng & dịch vụ', 'GET', '/api/reports/orders?from={{today}}&to={{today}}', {
    tests: `
${status(200)}
const r = pm.response.json();
pm.test('Có số đơn và doanh thu dịch vụ', () => { pm.expect(r.totalOrders).to.be.above(0); pm.expect(r.byService).to.be.an('array'); });`,
  }),
  req('Dashboard', 'GET', '/api/dashboard/summary', {
    tests: `
${status(200)}
pm.test('Có đơn nhận hôm nay', () => pm.expect(pm.response.json().ordersReceivedToday).to.be.above(0));`,
  }),
  req('NV xem báo cáo → 403', 'GET', '/api/dashboard/summary', { auth: 'staff', tests: status(403) }),
])

// ---------- 99 Expected after plan ----------
const f99 = folder(
  '13 - Hồi quy theo plan.md',
  'Các kiểm tra hành vi đã sửa theo plan.md (GĐ0 → GĐ6). Mỗi test ghi giai đoạn trong tên; tất cả phải PASS.',
  [
    req('[GĐ0] NV đọc được cài đặt', 'GET', '/api/settings', { auth: 'staff', tests: status(200) }),
    req('[GĐ0] Có tham số STORE_PHONE và ORDER_CODE_PREFIX', 'GET', '/api/settings', {
      tests: `
const keys = pm.response.json().map(s => s.settingKey);
pm.test('Có 2 tham số mới', () => pm.expect(keys).to.include.members(['STORE_PHONE', 'ORDER_CODE_PREFIX']));`,
    }),
    req('[GĐ0] Nhật ký có reason, role, entityCode', 'GET', '/api/log?size=1', {
      tests: `
pm.test('Có 3 field mới', () => pm.expect(pm.response.json().content[0]).to.include.keys('reason', 'role', 'entityCode'));`,
    }),
    req('[GĐ1] Kiểm tra mã voucher', 'GET', '/api/vouchers/check?code=KHONGTONTAI&subtotal=100000', {
      tests: `
${status(200)}
pm.test('reason = NOT_FOUND', () => pm.expect(pm.response.json().reason).to.eql('NOT_FOUND'));`,
    }),
    req('[GĐ1] Client tự gửi discountAmount bị bỏ qua', 'POST', '/api/orders/intake', {
      pre: intakePre('order3'),
      body: fixNum(intakeBody('order3')),
      tests: `
pm.collectionVariables.set('order3Id', pm.response.json().orders[0].id);
pm.collectionVariables.set('order3Code', pm.response.json().orders[0].orderCode);`,
    }),
    req('[GĐ1] ... phân loại kèm discountAmount = 50000', 'PUT', '/api/orders/{{order3Id}}/items', {
      body: fixNum({ items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: 4 }], discountAmount: 50000 }),
      tests: `
pm.test('discountAmount = 0', () => pm.expect(Number(pm.response.json().discountAmount)).to.eql(0));`,
    }),
    req('[GĐ2] Danh sách máy', 'GET', '/api/machines', { tests: status(200) }),
    req('[GĐ3] NV lùi trạng thái → 403', 'POST', '/api/orders/{{order3Id}}/revert-status', {
      auth: 'staff',
      body: { targetStatus: 'RECEIVED', reason: 'NV thử' },
      tests: status(403),
    }),
    req('[GĐ3] NV sửa giá → 403', 'PATCH', '/api/orders/{{order3Id}}/price', {
      auth: 'staff',
      body: { newTotalAmount: 1000, reason: 'NV thử' },
      tests: status(403),
    }),
    req('[GĐ3] ... chuyển đơn 3 sang Đang xử lý', 'PATCH', '/api/orders/{{order3Id}}/status', {
      body: { newStatus: 'PROCESSING' },
    }),
    req('[GĐ3] ... hoàn thành đơn 3 (chưa thanh toán)', 'POST', '/api/orders/{{order3Id}}/complete', {
      body: { shelfLocation: 'B2' },
    }),
    req('[GĐ3] Endpoint giao đồ cũ không còn giao được đơn chưa thanh toán', 'POST', '/api/orders/{{order3Id}}/deliver', {
      description: 'Hiện tại endpoint này giao luôn đơn chưa thanh toán (lỗi). Sau GĐ 3 endpoint bị xóa.',
      body: {},
      tests: notOk,
    }),
    req('[GĐ3] ... tạo đơn 4', 'POST', '/api/orders/intake', {
      pre: intakePre('order4'),
      body: fixNum(intakeBody('order4')),
      tests: `pm.collectionVariables.set('order4Id', pm.response.json().orders[0].id);`,
    }),
    req('[GĐ3] Đơn chưa phân loại không được bắt đầu xử lý → 400', 'PATCH', '/api/orders/{{order4Id}}/status', {
      body: { newStatus: 'PROCESSING' },
      tests: `
${status(400)}
pm.test('Báo chưa phân loại', () => pm.expect(pm.response.json().detail).to.include('chưa phân loại'));`,
    }),
    req('[GĐ3] Không dùng API đổi trạng thái để giao đồ → 400', 'PATCH', '/api/orders/{{order4Id}}/status', {
      body: { newStatus: 'DELIVERED' },
      tests: status(400),
    }),
    req('[GĐ3] ... phân loại đơn 4', 'PUT', '/api/orders/{{order4Id}}/items', {
      body: fixNum({ items: [{ serviceId: '{{svcKgId}}', itemType: 'Giặt sấy quần áo', quantity: 3 }] }),
      tests: status(200),
    }),
    req('[GĐ3] ... chuyển đơn 4 sang Đang xử lý', 'PATCH', '/api/orders/{{order4Id}}/status', {
      body: { newStatus: 'PROCESSING' },
      tests: status(200),
    }),
    req('[GĐ3] Hủy đơn đang xử lý → 400 (BR-D05)', 'POST', '/api/orders/{{order4Id}}/cancel', {
      body: { reason: 'Thử hủy khi đang xử lý' },
      tests: status(400),
    }),
    req('[GĐ3] Thanh toán ghi lịch sử đơn', 'GET', '/api/orders/{{orderId}}', {
      tests: `
const list = pm.response.json().historyList;
pm.test('Có PAYMENT_CONFIRMED', () => pm.expect(list.map(h => h.actionType)).to.include('PAYMENT_CONFIRMED'));
pm.test('Giao đồ ghi STATUS_CHANGE → DELIVERED (FE hiển thị nhãn trạng thái)', () =>
  pm.expect(list.some(h => h.actionType === 'STATUS_CHANGE' && h.newValue === 'DELIVERED')).to.be.true);`,
    }),
    req('[GĐ3] PaymentResponse có tên người xác nhận', 'GET', '/api/orders/{{orderCode}}/payments', {
      tests: `
pm.test('Có confirmedByName', () => pm.expect(pm.response.json()[0].confirmedByName).to.be.a('string'));`,
    }),
    req('[GĐ3] Endpoint tra cứu /orders/track đã bị xóa', 'GET', '/api/orders/track?phone={{customerPhone}}', {
      auth: 'none',
      tests: notOk,
    }),
    req('[GĐ4] Danh sách sự cố', 'GET', '/api/incidents', { tests: status(200) }),
    req('[GĐ5] Dashboard', 'GET', '/api/dashboard/summary', { tests: status(200) }),
    req('[GĐ6] Đếm đơn theo trạng thái', 'GET', '/api/orders/counts', { tests: status(200) }),
    req('[GĐ6] Chi tiết đơn có thông tin thanh toán & nhắc khách', 'GET', '/api/orders/{{orderId}}', {
      tests: `
pm.test('Có field mới', () => pm.expect(pm.response.json()).to.include.keys('lastNotifiedAt', 'notifyCount', 'paymentMethod', 'paidAt', 'voucherCode'));`,
    }),
    req('[GĐ6] SĐT đủ 10 số → chỉ đơn của đúng khách đó', 'GET', '/api/orders?phone={{customerPhone}}&size=100', {
      tests: `
${status(200)}
const phone = pm.collectionVariables.get('customerPhone');
pm.test('Mọi đơn đều đúng SĐT', () => pm.response.json().content.forEach(o => pm.expect(o.customerPhone).to.eql(phone)));`,
    }),
    req('[GĐ6] SĐT một phần → tìm gần đúng (ô tìm kiếm danh sách đơn của FE)', 'GET', '/api/orders?phone={{phoneTail}}', {
      pre: `pm.variables.set('phoneTail', pm.collectionVariables.get('customerPhone').slice(-6));`,
      tests: `
${status(200)}
pm.test('Tìm thấy ít nhất 1 đơn', () => pm.expect(pm.response.json().totalElements).to.be.at.least(1));`,
    }),
    req('[GĐ6] Lọc đơn theo customerId', 'GET', '/api/orders?customerId={{customerId}}&size=100', {
      tests: `
${status(200)}
const id = Number(pm.collectionVariables.get('customerId'));
pm.test('Mọi đơn thuộc khách', () => pm.response.json().content.forEach(o => pm.expect(o.customerId).to.eql(id)));`,
    }),
    req('[GĐ6] Lọc khách theo trạng thái', 'GET', '/api/customers?status=INACTIVE&size=50', {
      tests: `
pm.test('Chỉ có khách INACTIVE', () => pm.response.json().content.forEach(c => pm.expect(c.status).to.eql('INACTIVE')));`,
    }),
    req('[GĐ6] Khách có thống kê đơn', 'GET', '/api/customers/{{customerId}}', {
      tests: `
pm.test('Có orderCount, totalSpent, lastOrderAt', () => pm.expect(pm.response.json()).to.include.keys('orderCount', 'totalSpent', 'lastOrderAt'));`,
    }),
    req('[GĐ6] JSON sai định dạng → 400 (hiện trả 500)', 'POST', '/api/customers', {
      body: '{ "fullName": "Thiếu ngoặc", ',
      tests: status(400),
    }),
  ],
)

// ---------- collection ----------
const collection = {
  info: {
    name: 'Laundry Backend API',
    description:
      'Bộ kiểm thử API cho backend quản lý tiệm giặt ủi.\n\nChạy cả collection theo thứ tự thư mục 00 → 13. Thư mục 01, 08 và các request NV cần tài khoản STAFF (tự bỏ qua nếu chưa có).\n\nXem postman/README.md.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{adminToken}}', type: 'string' }] },
  event: [
    {
      listen: 'test',
      script: {
        type: 'text/javascript',
        exec: lines(`
// Chạy sau mọi request: in lỗi ProblemDetail ra console để dễ dò
if (pm.response.code >= 400) {
  try { const p = pm.response.json(); console.log(pm.info.requestName + ' → ' + pm.response.code + ': ' + (p.detail || '') + (p.errors ? ' ' + JSON.stringify(p.errors) : '')); } catch (e) {}
}
pm.test('Không lỗi 500', () => pm.expect(pm.response.code).to.not.eql(500));`),
      },
    },
  ],
  variable: [
    'adminToken', 'staffToken', 'svcKgId', 'svcKgPrice', 'svcKgMin', 'newServiceId', 'customerId', 'customerPhone',
    'order1RequestId', 'order1DueAt', 'orderId', 'orderCode', 'totalAmount', 'paymentId',
    'order2RequestId', 'order2DueAt', 'order2Id', 'order2Code', 'payment2Id', 'auditLogId',
    'order3RequestId', 'order3DueAt', 'order3Id', 'order3Code', 'order4RequestId', 'order4DueAt', 'order4Id',
    'today', 'voucherCode', 'voucherId', 'order5RequestId', 'order5DueAt', 'order5Id',
    'order6RequestId', 'order6DueAt', 'order6Id', 'order6Code', 'order6Total', 'order6ItemId',
    'machineId', 'incidentId', 'incident2Id', 'incident3Id',
  ].map((key) => ({ key, value: '' })),
  item: [f00, f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f99],
}

const environment = {
  name: 'Laundry - Local',
  values: [
    { key: 'baseUrl', value: 'http://localhost:8080', type: 'default', enabled: true },
    { key: 'adminUsername', value: 'admin', type: 'default', enabled: true },
    { key: 'adminPassword', value: '', type: 'secret', enabled: true },
    { key: 'staffUsername', value: '', type: 'default', enabled: true },
    { key: 'staffPassword', value: 'Staff@123456', type: 'secret', enabled: true },
    { key: 'staffSetupToken', value: '', type: 'secret', enabled: true },
    { key: 'staffUserId', value: '', type: 'default', enabled: true },
  ],
  _postman_variable_scope: 'environment',
}

writeFileSync(join(outDir, 'laundry.postman_collection.json'), JSON.stringify(collection, null, 2) + '\n')
writeFileSync(join(outDir, 'local.postman_environment.json'), JSON.stringify(environment, null, 2) + '\n')

const count = (items) => items.reduce((n, i) => n + (i.item ? count(i.item) : 1), 0)
console.log('Requests:', count(collection.item))
