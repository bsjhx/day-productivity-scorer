# TODO: DDD, CQRS & Event Sourcing Issues

## Summary

### Critical Issues
- [x] Fix event sourcing pattern: `rate()` and `lock()` must call `apply()` immediately
- [x] Add domain logic for duplicate rating handling. It's ok, user can update rates.

### High Priority
- [x] Review transaction boundaries in `DayProjectionUpdater`
- [x] Add optimistic locking / concurrency control to event store

### Medium Priority
- [x] Remove unnecessary `markAsNotNew()` call in `DayProjectionUpdater`
- [x] Improve `DayScore` value object (convert enum to record) - it is ok.

### Low Priority
- [ ] Add snapshot support for aggregate reconstitution
- [ ] Enhance events with metadata (timestamps, eventId, causation)
- [ ] Plan event versioning strategy (upcasters)

### Tests

- [ ] General testing
- [ ] Add test to transaction boundaries in `DayProjectionUpdater`

---

## Details

### 1. Fix event sourcing pattern: `rate()` and `lock()` must call `apply()` immediately

**File**: `DayAggregate.java:35-54`

**Problem**: The `rate()` and `lock()` methods add events to the `changes` list but don't apply them to the aggregate's internal state. This violates the event sourcing pattern and leaves the aggregate in an inconsistent state.

**Current broken flow**:
1. `rate(DayScore)` adds `DayRated` event to `changes`
2. Aggregate's `dayScore` field remains unchanged
3. Business logic that checks current state will use stale data
4. State only updates when aggregate is reconstructed from events

**Expected correct flow**:
1. `rate(DayScore)` creates `DayRated` event
2. Immediately call `apply(DayRated)` to update `dayScore`
3. Add event to `changes` list
4. State is always consistent

**Fix required**:
```java
public void rate(DayScore dayScore) {
    if (dayScore == null) {
        throw new IllegalArgumentException("DayScore cannot be null");
    }
    if (locked) {
        throw new IllegalStateException("DayScore cannot be changed when the day is locked");
    }
    if (dayId.id().isAfter(LocalDate.now())) {
        throw new IllegalArgumentException("Must not rate a day in the future");
    }

    DayRated event = new DayRated(dayId, dayScore);
    apply(event);  // ← Apply immediately!
    changes.add(event);
}

public void lock() {
    if (locked) {
        return;
    }
    DayLocked event = new DayLocked(dayId);
    apply(event);  // ← Apply immediately!
    changes.add(event);
}
```

**Impact**: Without this fix, any business logic that depends on the current state (e.g., checking if already rated, computing statistics) will fail or produce incorrect results.

---

### 2. Add domain logic for duplicate rating handling

**File**: `DayAggregate.java:35-47`

**Problem**: The `rate()` method doesn't prevent or handle duplicate ratings. You can call `rate()` multiple times on the same day without any validation.

**Questions to answer**:
- Should re-rating be allowed? (Business decision)
- If yes, should we track rating history?
- If no, should we throw an exception when already rated?

**Possible implementations**:

**Option A: Prevent re-rating**
```java
public void rate(DayScore dayScore) {
    // ... existing validation ...

    if (this.dayScore != DayScore.NONE) {
        throw new IllegalStateException("Day has already been rated with score: " + this.dayScore);
    }

    DayRated event = new DayRated(dayId, dayScore);
    apply(event);
    changes.add(event);
}
```

**Option B: Allow re-rating (update score)**
```java
public void rate(DayScore dayScore) {
    // ... existing validation ...

    // Allow re-rating, just update the score
    DayRated event = new DayRated(dayId, dayScore);
    apply(event);
    changes.add(event);
}
```

**Option C: Track rating history**
```java
// Add new event type
record DayReRated(DayId dayId, DayScore oldScore, DayScore newScore) implements DayDomainEvent {}

public void rate(DayScore dayScore) {
    // ... existing validation ...

    if (this.dayScore != DayScore.NONE) {
        DayReRated event = new DayReRated(dayId, this.dayScore, dayScore);
        apply(event);
        changes.add(event);
    } else {
        DayRated event = new DayRated(dayId, dayScore);
        apply(event);
        changes.add(event);
    }
}
```

**Recommendation**: Choose based on business requirements. Document the decision.

---

### 3. Review transaction boundaries in `DayProjectionUpdater`

**File**: `DayProjectionUpdater.java:20-22, 36-38`

**Problem**: The projection updater uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`. This means:

1. Command-side transaction commits first (event persisted)
2. Projection update runs in a **separate new transaction**
3. If projection update fails, command still succeeded
4. Read model becomes inconsistent with write model

**Risks**:
- **Lost updates**: Projection failure leaves read model stale
- **No atomicity**: Write and read updates not atomic
- **Race conditions**: Queries between event publish and projection update see stale data

**Current behavior** (eventual consistency):
```
[Command TX]     [Projection TX]
   Save Event   →   COMMIT   →   Update Projection   →   COMMIT
                       ↓
                   Query here sees stale data!
```

**Options**:

**Option A: Strong consistency (same transaction)**
```java
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
@Transactional(propagation = Propagation.MANDATORY)
public void on(DayRated event) {
    // Runs in same transaction as command
    // Either both commit or both rollback
}
```

**Option B: Eventual consistency (current approach)**
- Keep current implementation
- Add retry logic for projection failures
- Document that read model is eventually consistent
- Consider adding projection version tracking

**Option C: Outbox pattern**
- Store events in outbox table (same TX as command)
- Background process reads outbox and updates projections
- Guarantees at-least-once delivery

**Recommendation**:
- If queries must reflect writes immediately → Option A
- If eventual consistency is acceptable → Option B with retry + monitoring
- For production systems → Option C (most robust)

---

### 4. Add optimistic locking / concurrency control to event store

**File**: `EventStoreRepository.java:44-72`

**Problem**: No protection against concurrent modifications. Two processes could:
1. Both load aggregate at version 5
2. Both append events as version 6
3. Last write wins, first write is lost silently

**Current implementation**:
```java
@Override
@Transactional
public void save(DayAggregate day) {
    Integer lastVersion = jdbcRepository.findMaxVersionByAggregateId(...);
    int currentVersion = (lastVersion != null) ? lastVersion : 0;

    // ❌ No check if aggregate was modified since loaded!
    for (DayDomainEvent event : newEvents) {
        currentVersion++;
        // ... save event
    }
}
```

**Fix: Add optimistic locking**
```java
public class DayAggregate {
    private int expectedVersion = 0; // Version when loaded

    public static DayAggregate recreate(DayId dayId, List<DayDomainEvent> history) {
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.expectedVersion = history.size();
        // ... apply events
        return aggregate;
    }

    public int getExpectedVersion() {
        return expectedVersion;
    }
}

// In EventStoreRepository
@Override
@Transactional
public void save(DayAggregate day) {
    Integer lastVersion = jdbcRepository.findMaxVersionByAggregateId(...);
    int actualVersion = (lastVersion != null) ? lastVersion : 0;

    // ✅ Check for concurrent modifications
    if (day.getExpectedVersion() != actualVersion) {
        throw new ConcurrencyException(
            "Aggregate modified by another process. Expected version: " +
            day.getExpectedVersion() + ", actual: " + actualVersion
        );
    }

    // ... save events
}
```

**Alternative: Database constraint**
Add unique constraint on `(aggregate_id, version)` in event_store table. Database will reject duplicate versions automatically.

---

### 5. Remove unnecessary `markAsNotNew()` call in `DayProjectionUpdater`

**File**: `DayProjectionUpdater.java:44`

**Problem**: Calling `markAsNotNew()` after `save()` has no effect:

```java
current.setLocked(true);
dayProjectionRepository.save(current).markAsNotNew();  // ❌ Useless
```

**Why it's wrong**:
1. `save()` returns the saved entity (potentially a new instance)
2. Modifying the returned reference doesn't affect the database
3. The `isNew` flag only matters **during** save, not after
4. Spring Data already handles the insert/update logic

**Fix**: Simply remove the call:
```java
current.setLocked(true);
dayProjectionRepository.save(current);  // ✅ Clean
```

**Understanding Persistable**:
- `isNew()` tells Spring Data whether to INSERT (new=true) or UPDATE (new=false)
- The flag is evaluated **during** `save()`, not after
- Modifying it post-save serves no purpose

---

### 6. Improve `DayScore` value object (convert enum to record)

**File**: `DayScore.java:1-29`

**Problem**: Using an enum for scores is overly restrictive:
- Need to add enum values for new scores (e.g., score 6-10)
- Enum names (ZERO, ONE, TWO) don't add semantic value
- `NONE` (-1) is a special case mixing concerns

**Current implementation**:
```java
public enum DayScore {
    NONE(-1), ZERO(0), ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5);
    // To add score 6, must modify enum!
}
```

**Better approach: Record with validation**
```java
public record DayScore(int value) {
    public static final DayScore NONE = new DayScore(-1);

    public DayScore {
        if (value < -1 || value > 10) {
            throw new IllegalArgumentException("Score must be -1 (NONE) or 0-10");
        }
    }

    public static DayScore of(int value) {
        return new DayScore(value);
    }

    public boolean isRated() {
        return value >= 0;
    }
}
```

**Benefits**:
- Natural integer representation
- Easy to extend score range
- Type-safe (still a value object)
- Immutable (records are final)
- Clear semantics

**Migration path**:
1. Create new `DayScore` record
2. Update `DayAggregate` to use record
3. Update serialization logic in `EventStoreRepository`
4. Update database queries if needed

---

### 7. Add snapshot support for aggregate reconstitution

**File**: `EventStoreRepository.java:30-42`

**Problem**: As aggregates accumulate events over time, reconstructing from all events becomes expensive.

**Example scenario**:
- Day rated/locked 100 times over months (rating changes, locks, etc.)
- Loading aggregate requires deserializing 100 events
- Most events are historical and don't affect current state

**Solution: Snapshots**

Periodically save aggregate state snapshot:

```java
@Table("aggregate_snapshot")
public class AggregateSnapshot {
    @Id
    private Long id;
    private String aggregateId;
    private int version;
    private String aggregateState; // JSON serialized aggregate
    private Instant createdAt;
}

// In EventStoreRepository
@Override
public Optional<DayAggregate> findById(DayId dayId) {
    // 1. Load latest snapshot (if exists)
    Optional<AggregateSnapshot> snapshot = snapshotRepository
        .findLatestByAggregateId(dayId.id().toString());

    DayAggregate aggregate;
    int fromVersion;

    if (snapshot.isPresent()) {
        // 2. Deserialize aggregate from snapshot
        aggregate = deserializeAggregate(snapshot.get());
        fromVersion = snapshot.get().getVersion();
    } else {
        // 3. No snapshot, start from scratch
        aggregate = new DayAggregate(dayId);
        fromVersion = 0;
    }

    // 4. Load only events after snapshot
    List<EventStoreEntity> recentEvents = jdbcRepository
        .findByAggregateIdAndVersionGreaterThan(
            dayId.id().toString(),
            fromVersion
        );

    // 5. Apply recent events
    List<DayDomainEvent> events = recentEvents.stream()
        .map(this::deserializeEvent)
        .toList();

    events.forEach(aggregate::apply);

    return Optional.of(aggregate);
}

// Save snapshot every N events
@Override
public void save(DayAggregate day) {
    // ... save events ...

    int totalEvents = jdbcRepository.countByAggregateId(day.getId().id().toString());
    if (totalEvents % 50 == 0) { // Snapshot every 50 events
        saveSnapshot(day);
    }
}
```

**Trade-offs**:
- **Pro**: Faster aggregate loading
- **Pro**: Reduces memory/CPU for old aggregates
- **Con**: Adds complexity
- **Con**: Need snapshot versioning strategy

**When to implement**: When aggregates have >20-50 events regularly.

---

### 8. Enhance events with metadata (timestamps, eventId, causation)

**File**: `DayDomainEvent.java:1-9`

**Problem**: Events lack important metadata:
- No timestamp (when did it happen?)
- No event ID (for idempotency, tracing)
- No causation/correlation IDs (for debugging)

**Current implementation**:
```java
public sealed interface DayDomainEvent {
    record DayRated(DayId dayId, DayScore score) implements DayDomainEvent {}
    record DayLocked(DayId dayId) implements DayDomainEvent {}
}
```

**Enhanced version**:
```java
public sealed interface DayDomainEvent {
    String eventId();
    LocalDateTime occurredAt();
    String causationId(); // ID of command that caused this event
    String correlationId(); // ID linking related events (e.g., user session)

    record DayRated(
        String eventId,
        LocalDateTime occurredAt,
        String causationId,
        String correlationId,
        DayId dayId,
        DayScore score
    ) implements DayDomainEvent {}

    record DayLocked(
        String eventId,
        LocalDateTime occurredAt,
        String causationId,
        String correlationId,
        DayId dayId
    ) implements DayDomainEvent {}
}
```

**Benefits**:
- **Auditing**: Know exactly when events occurred
- **Idempotency**: Detect duplicate events by ID
- **Debugging**: Trace event flow through system
- **Analytics**: Query events by time range
- **Event sourcing replay**: Replay events up to specific timestamp

**Implementation notes**:
- Generate `eventId` using UUID
- Capture `occurredAt` when event is created
- Pass `causationId`/`correlationId` from command context
- Store metadata in separate columns for efficient querying

---

### 9. Plan event versioning strategy (upcasters)

**File**: `EventStoreRepository.java:82-89`

**Problem**: As the system evolves, event schemas will change. Need strategy to handle old events.

**Scenarios**:
1. **Add field**: `DayRated` needs new `ratedBy: UserId` field
2. **Rename field**: `score` → `productivityScore`
3. **Split event**: `DayRated` → `DayRated` + `ScoreExplanationAdded`
4. **Merge events**: `DayRated` + `DayLocked` → `DayCompleted`

**Current risk**:
```java
// EventStoreRepository.java:82-89
private DayDomainEvent deserializeEvent(EventStoreEntity entity) {
    Class<?> eventClass = Class.forName(entity.getEventType());
    return (DayDomainEvent) objectMapper.readValue(entity.getPayload(), eventClass);
    // ❌ Will fail if event schema changed!
}
```

**Solution: Event versioning with upcasters**

**Step 1: Version events**
```java
public sealed interface DayDomainEvent {
    int version();

    record DayRatedV1(
        String eventId,
        LocalDateTime occurredAt,
        DayId dayId,
        DayScore score
    ) implements DayDomainEvent {
        @Override public int version() { return 1; }
    }

    record DayRatedV2(
        String eventId,
        LocalDateTime occurredAt,
        DayId dayId,
        DayScore score,
        String ratedBy // ← New field
    ) implements DayDomainEvent {
        @Override public int version() { return 2; }
    }
}
```

**Step 2: Create upcasters**
```java
public interface EventUpcaster<From, To> {
    To upcast(From oldEvent);
}

public class DayRatedV1ToV2Upcaster implements EventUpcaster<DayRatedV1, DayRatedV2> {
    @Override
    public DayRatedV2 upcast(DayRatedV1 v1) {
        return new DayRatedV2(
            v1.eventId(),
            v1.occurredAt(),
            v1.dayId(),
            v1.score(),
            "unknown" // Default for old events
        );
    }
}
```

**Step 3: Apply upcasters during deserialization**
```java
private DayDomainEvent deserializeEvent(EventStoreEntity entity) {
    String eventType = entity.getEventType();

    // Deserialize to original version
    DayDomainEvent event = objectMapper.readValue(
        entity.getPayload(),
        Class.forName(eventType)
    );

    // Upcast to latest version
    return upcasterChain.upcast(event);
}
```

**Alternative: Schema at event level**
Store schema version with each event:
```java
@Table("event_store")
public class EventStoreEntity {
    private String eventType;      // "DayRated"
    private int schemaVersion;     // 1, 2, 3...
    private String payload;        // JSON
}
```

**Recommendation**:
- Start simple: keep V1 compatible as long as possible
- When breaking changes needed, implement upcaster pattern
- Document all event schema changes
- Consider event versioning from the start (easier than retrofitting)
