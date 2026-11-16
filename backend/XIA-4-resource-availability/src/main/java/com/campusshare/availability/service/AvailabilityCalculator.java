package com.campusshare.availability.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AvailabilityCalculator {

    public List<TimeInterval> merge(List<TimeInterval> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> sorted = source.stream()
                .sorted(Comparator.comparing(TimeInterval::startAt)
                        .thenComparing(TimeInterval::endAt))
                .toList();

        List<TimeInterval> merged = new ArrayList<>();
        LocalDateTime currentStart = sorted.getFirst().startAt();
        LocalDateTime currentEnd = sorted.getFirst().endAt();

        for (int i = 1; i < sorted.size(); i++) {
            TimeInterval next = sorted.get(i);

            if (!next.startAt().isAfter(currentEnd)) {
                if (next.endAt().isAfter(currentEnd)) {
                    currentEnd = next.endAt();
                }
            } else {
                merged.add(new TimeInterval(currentStart, currentEnd));
                currentStart = next.startAt();
                currentEnd = next.endAt();
            }
        }

        merged.add(new TimeInterval(currentStart, currentEnd));
        return List.copyOf(merged);
    }

    public List<TimeInterval> subtract(
            List<TimeInterval> openPeriods,
            List<TimeInterval> unavailablePeriods) {

        List<TimeInterval> opens = merge(openPeriods);
        List<TimeInterval> busy = merge(unavailablePeriods);

        if (opens.isEmpty() || busy.isEmpty()) {
            return opens;
        }

        List<TimeInterval> result = new ArrayList<>();

        for (TimeInterval open : opens) {
            List<TimeInterval> fragments = new ArrayList<>();
            fragments.add(open);

            for (TimeInterval blocked : busy) {
                if (fragments.isEmpty()) {
                    break;
                }

                List<TimeInterval> nextFragments = new ArrayList<>();
                for (TimeInterval fragment : fragments) {
                    nextFragments.addAll(subtractOne(fragment, blocked));
                }
                fragments = nextFragments;
            }

            result.addAll(fragments);
        }

        return List.copyOf(result);
    }

    private List<TimeInterval> subtractOne(
            TimeInterval base,
            TimeInterval blocked) {

        if (blocked.endAt().compareTo(base.startAt()) <= 0
                || blocked.startAt().compareTo(base.endAt()) >= 0) {
            return List.of(base);
        }

        List<TimeInterval> result = new ArrayList<>(2);

        if (blocked.startAt().isAfter(base.startAt())) {
            LocalDateTime leftEnd = blocked.startAt().isBefore(base.endAt())
                    ? blocked.startAt()
                    : base.endAt();
            if (base.startAt().isBefore(leftEnd)) {
                result.add(new TimeInterval(base.startAt(), leftEnd));
            }
        }

        if (blocked.endAt().isBefore(base.endAt())) {
            LocalDateTime rightStart = blocked.endAt().isAfter(base.startAt())
                    ? blocked.endAt()
                    : base.startAt();
            if (rightStart.isBefore(base.endAt())) {
                result.add(new TimeInterval(rightStart, base.endAt()));
            }
        }

        return result;
    }
}
