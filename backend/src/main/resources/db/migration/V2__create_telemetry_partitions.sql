DO $$
DECLARE
    month_start DATE := date_trunc('month', CURRENT_DATE)::DATE - INTERVAL '1 month';
    partition_start DATE;
    partition_end DATE;
    partition_name TEXT;
BEGIN
    FOR offset_month IN 0..13 LOOP
        partition_start := (month_start + (offset_month || ' month')::INTERVAL)::DATE;
        partition_end := (partition_start + INTERVAL '1 month')::DATE;
        partition_name := 'telemetry_' || to_char(partition_start, 'YYYY_MM');
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF telemetry FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            partition_start,
            partition_end
        );
    END LOOP;
END $$;

