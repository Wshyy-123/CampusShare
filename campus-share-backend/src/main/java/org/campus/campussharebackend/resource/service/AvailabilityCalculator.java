package org.campus.campussharebackend.resource.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.resource.entity.BlockRecord;
import org.campus.campussharebackend.resource.entity.OpeningRecord;
import org.campus.campussharebackend.resource.entity.ReservationSlot;
import org.campus.campussharebackend.resource.vo.IntervalVO;

/** 统一半开区间规则：[start,end)，相邻开放区间可以连续使用。写入前仍须锁资源并重新检查。 */
public final class AvailabilityCalculator {
    private AvailabilityCalculator() {}
    private record Range(LocalDateTime start, LocalDateTime end) {}

    private static List<Range> openings(List<OpeningRecord> values) {
        return values.stream().map(v -> new Range(v.getStartAt(), v.getEndAt()))
                .filter(v -> v.start().isBefore(v.end())).sorted(Comparator.comparing(Range::start)).toList();
    }

    public static boolean covers(List<OpeningRecord> values, LocalDateTime start, LocalDateTime end) {
        LocalDateTime cursor = start;
        for (Range interval : openings(values)) {
            if (interval.start().isAfter(cursor)) return false;
            if (interval.end().isAfter(cursor)) cursor = interval.end();
            if (!cursor.isBefore(end)) return true;
        }
        return false;
    }

    public static List<IntervalVO> free(List<OpeningRecord> values, List<BlockRecord> blocks,
                                      List<ReservationSlot> reservations, LocalDateTime lower, LocalDateTime upper) {
        if (!lower.isBefore(upper)) return List.of();
        List<Range> merged = new ArrayList<>();
        for (Range original : openings(values)) {
            Range range = new Range(original.start().isBefore(lower) ? lower : original.start(), original.end().isAfter(upper) ? upper : original.end());
            if (!range.start().isBefore(range.end())) continue;
            if (!merged.isEmpty() && !range.start().isAfter(merged.getLast().end())) {
                Range last = merged.removeLast();
                merged.add(new Range(last.start(), last.end().isAfter(range.end()) ? last.end() : range.end()));
            } else merged.add(range);
        }
        List<Range> occupied = new ArrayList<>();
        blocks.forEach(v -> occupied.add(new Range(v.getStartAt(),v.getEndAt())));
        reservations.forEach(v -> occupied.add(new Range(v.getStartAt(),v.getEndAt())));
        occupied.sort(Comparator.comparing(Range::start));
        List<IntervalVO> result = new ArrayList<>();
        for (Range open : merged) {
            LocalDateTime cursor = open.start();
            for (Range block : occupied) {
                if (!block.start().isBefore(block.end()) || !block.end().isAfter(cursor)) continue;
                if (!block.start().isBefore(open.end())) break;
                if (block.start().isAfter(cursor)) result.add(new IntervalVO(TimeFormats.format(cursor),TimeFormats.format(block.start())));
                if (block.end().isAfter(cursor)) cursor = block.end();
                if (!cursor.isBefore(open.end())) break;
            }
            if (cursor.isBefore(open.end())) result.add(new IntervalVO(TimeFormats.format(cursor),TimeFormats.format(open.end())));
        }
        return result;
    }
}
