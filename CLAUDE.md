# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot application for tracking daily productivity scores using Domain-Driven Design (DDD), CQRS, and Event Sourcing patterns.

## Build and Run Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.bsjhx.dayproductivityscore.DayProductivityScoreApplicationTests"

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

## Architecture

This application follows **CQRS (Command Query Responsibility Segregation)** with **event-driven projections**:

### Command Side (Write Model)
- **Domain Layer**: `DayAggregate` is the aggregate root that enforces business rules and emits domain events (`DayRated`, `DayLocked`)
- **Command Repository**: `InMemoryCommandDayRepository` stores aggregates in `InMemoryWriteMemoryDb` and publishes domain events via Spring's `ApplicationEventPublisher`
- **Command Handler**: `DayCommandHandler` processes commands (e.g., `RateDay`) by loading aggregates, executing business logic, and saving changes

### Query Side (Read Model)
- **Query Repository**: `InMemoryQueryDayRepository` reads from `InMemoryReadDb`, which contains denormalized projections (`DayScoreView`)
- **Projection Updater**: `DayProjectionUpdater` listens to domain events and updates the read model accordingly
- **Query Service**: `DayQueryService` handles queries (e.g., `GetDaysInRangeQuery`) by reading from the query repository

### Key Patterns
1. **Separate Write and Read Databases**: `InMemoryWriteMemoryDb` (command side) and `InMemoryReadDb` (query side) are completely separate
2. **Event-Driven Projections**: When an aggregate is saved, domain events are published. `DayProjectionUpdater` listens to these events and updates the read model
3. **Eventual Consistency**: The read model is eventually consistent with the write model via event listeners

### Package Structure
- `api.rest`: REST controllers and DTOs
- `application.command`: Command handlers and commands
- `application.query`: Query services and queries
- `domain`: Aggregate roots, value objects, and domain events
- `domain.port`: Repository interfaces (ports in hexagonal architecture)
- `infrastructure.db.command`: Command-side repository implementations
- `infrastructure.db.query`: Query-side repository implementations and projection updaters

## Important Implementation Details

- **Domain Events**: Aggregates track changes via `DayDomainEvent` (sealed interface with `DayRated` and `DayLocked` records). Events are published after save and then cleared
- **Aggregate Reconstitution**: Use `DayAggregate.reconstitute()` to rebuild aggregates from stored state without triggering business logic
- **Event Listeners**: Spring's `@EventListener` on `DayProjectionUpdater` methods automatically updates read model when domain events are published
- **Java 21**: Uses modern Java features including records and sealed interfaces

## REST API

- `POST /day/`: Rate a day (body: `{"day": "2026-07-08", "score": 8}`)
- `GET /day/?from=2026-07-01&to=2026-07-08`: Get days in range (to is optional)
