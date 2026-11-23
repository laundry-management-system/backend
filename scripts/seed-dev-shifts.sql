-- Dữ liệu test cho Ca làm việc (spec 015). CHỈ dùng cho DB dev.
--
-- Chạy:   docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-shifts.sql
-- Chạy lại bao nhiêu lần cũng được: xóa mọi ca (kể cả ca đang mở), gỡ liên kết ca khỏi thanh toán, rồi tạo lại.
-- Cần chạy seed-dev-dashboard-worklist.sql trước (tài khoản seed_staff / seed_admin, đơn mẫu).
--
-- Kết quả:
--   Không có ca đang mở  → thanh toán bị khóa, Danh sách công việc hiện "Chưa mở ca"
--   5 ca 1–3 ngày trước cho màn Đối chiếu tiền cuối ca: 2 đã duyệt, 3 chờ duyệt; có thiếu, dư, lệch chuyển khoản
--   CA-<hôm qua>-02 đã kết (Chờ duyệt), để lại két 500.000đ → số gợi ý tiền đầu ca khi mở ca mới

BEGIN;

DELETE FROM audit_logs WHERE entity_type = 'SHIFT';
UPDATE payments SET shift_id = NULL, resolved_shift_id = NULL
 WHERE shift_id IS NOT NULL OR resolved_shift_id IS NOT NULL;
DELETE FROM shifts;

-- Lịch sử vài ngày cho màn Đối chiếu tiền cuối ca: đã duyệt / chờ duyệt, thiếu / dư / khớp, lệch chuyển khoản
INSERT INTO shifts (code, status, opened_by, opened_at, opening_cash, closed_by, closed_at,
                    cash_in, cash_in_count, cash_out, cash_out_count, expected_cash, counted_cash, difference,
                    difference_reason, transfer_total, transfer_count, bank_reconciled, bank_total,
                    transfer_difference, transfer_difference_reason, keep_cash, hand_in_cash,
                    approved_by, approved_at, approval_note)
SELECT 'CA-' || to_char(day.d - h.days_ago, 'YYYYMMDD') || '-' || h.seq, h.status, staff.id,
       (day.d - h.days_ago + h.open_at) AT TIME ZONE 'Asia/Ho_Chi_Minh', 500000,
       staff.id, (day.d - h.days_ago + h.close_at) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       h.cash_in, h.cash_in_n, 0, 0, 500000 + h.cash_in, 500000 + h.cash_in + h.diff, h.diff,
       h.diff_reason, h.transfer, h.transfer_n, true, h.transfer + h.transfer_diff,
       h.transfer_diff, h.transfer_reason, 500000, h.cash_in + h.diff,
       CASE WHEN h.status = 'APPROVED' THEN admin.id END,
       CASE WHEN h.status = 'APPROVED' THEN (day.d - h.days_ago + h.close_at + interval '1 hour') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
       h.note
  FROM (VALUES
          (3, '01', time '07:05', time '14:00', 'APPROVED', 480000, 6, 0, NULL, 390000, 3, 0, NULL, NULL),
          (3, '02', time '14:01', time '21:10', 'APPROVED', 590000, 7, 5000, 'Khách để lại tiền thừa, chưa kịp trả', 475000, 4, 0, NULL, 'Đã ghi nhận tiền dư'),
          (2, '01', time '07:00', time '14:02', 'CLOSED', 820000, 8, -15000, 'Thối nhầm cho khách đơn LD-…', 610000, 5, 0, NULL, NULL),
          (2, '02', time '14:00', time '21:05', 'CLOSED', 700000, 6, 0, NULL, 530000, 4, -22000, 'Khách chuyển thiếu 22.000đ, đã báo Admin', NULL),
          (1, '01', time '07:02', time '13:58', 'CLOSED', 640000, 5, -20000, 'Thối nhầm 20.000đ', 280000, 2, 0, NULL, NULL)
       ) AS h(days_ago, seq, open_at, close_at, status, cash_in, cash_in_n, diff, diff_reason,
              transfer, transfer_n, transfer_diff, transfer_reason, note),
       (SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS d) day,
       (SELECT id FROM users WHERE username = 'seed_staff') staff,
       (SELECT id FROM users WHERE username = 'seed_admin') admin;


INSERT INTO shifts (code, status, opened_by, opened_at, opening_cash, closed_by, closed_at,
                    cash_in, cash_in_count, cash_out, cash_out_count, expected_cash, counted_cash, difference,
                    transfer_total, transfer_count, bank_reconciled, keep_cash, hand_in_cash)
SELECT 'CA-' || to_char(d, 'YYYYMMDD') || '-02', 'CLOSED', u.id,
       (d + time '14:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 500000,
       u.id, (d + time '21:04') AT TIME ZONE 'Asia/Ho_Chi_Minh',
       750000, 5, 0, 0, 1250000, 1250000, 0, 530000, 3, true, 500000, 750000
  FROM users u, (SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 1 AS d) day
 WHERE u.username = 'seed_staff';

COMMIT;

SELECT code, status, keep_cash FROM shifts;
