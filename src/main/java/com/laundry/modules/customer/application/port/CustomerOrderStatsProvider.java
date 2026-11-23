package com.laundry.modules.customer.application.port;

import java.util.Collection;
import java.util.Map;

/** Module order phụ thuộc customer, nên customer chỉ đọc thống kê đơn qua port này. */
public interface CustomerOrderStatsProvider {

    /** Khách không có đơn nào thì không có trong map. */
    Map<Long, CustomerOrderStats> statsFor(Collection<Long> customerIds);
}
