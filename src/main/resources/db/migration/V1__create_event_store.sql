CREATE TABLE event_store (
                             id BIGSERIAL PRIMARY KEY,
                             aggregate_id VARCHAR(50) NOT NULL,
                             version INT NOT NULL,
                             event_type VARCHAR(100) NOT NULL,
                             payload TEXT NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                             CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, version)
);

-- Indeks do szybkiego podnoszenia strumienia zdarzeń dla konkretnego dnia
CREATE INDEX idx_event_store_aggregate_id ON event_store(aggregate_id);
